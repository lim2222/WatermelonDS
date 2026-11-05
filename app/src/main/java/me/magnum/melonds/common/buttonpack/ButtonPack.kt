package me.magnum.melonds.common.buttonpack

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.max

/**
 * Loads button artwork from an optional button pack.
 *
 * A button pack is a set of images named like the built-in drawables, for example `button_a.png`,
 * `button_a_pressed.png` or `keypad_up.png`. Images found in the pack take precedence over the
 * built-in resources; missing images fall back to them. Folders inside an imported zip are
 * ignored, so the images may be stored at any depth.
 *
 * Multiple packs can be installed at once, each stored in its own directory under the private
 * files directory. Every layout remembers the name of the pack it uses and [setActivePack] selects
 * the pack that [load] reads from. Installing does not require any file permission and loading is
 * fast.
 */
object ButtonPack {
    private const val PACKS_DIR_NAME = "button_packs"
    private const val TEMP_DIR_NAME = "button_pack_tmp"

    // Images are stored under a neutral extension; BitmapFactory detects the format itself.
    private const val STORED_EXTENSION = ".img"

    const val MAX_FILES = 100
    const val MAX_FILE_BYTES = 8L * 1024 * 1024
    const val MAX_TOTAL_BYTES = 64L * 1024 * 1024
    private const val MAX_ZIP_ENTRIES = 1000

    // Bigger images are decoded smaller; the overlay shrinks them to the button size anyway.
    private const val MAX_DECODE_SIDE = 1024

    private val IMAGE_EXTENSIONS = setOf("png", "webp", "jpg", "jpeg")

    // Android resource names only use these characters.
    private val NAME_REGEX = Regex("[a-z0-9_]+")

    private val INVALID_PACK_NAME_CHARS = Regex("[^A-Za-z0-9._-]")

    private val bitmapCache = LruCache<String, Bitmap>(32)

    @Volatile
    private var activePackName: String? = null

    private fun packsDir(context: Context) = File(context.filesDir, PACKS_DIR_NAME)

    private fun packDir(context: Context, packName: String) = File(packsDir(context), packName)

    /**
     * Turns a zip file name into a safe pack name. "My Buttons.zip" becomes "My_Buttons".
     */
    fun sanitizePackName(name: String): String {
        val trimmed = name.trim()
        val withoutExtension = if (trimmed.endsWith(".zip", ignoreCase = true)) {
            trimmed.dropLast(4)
        } else {
            trimmed
        }
        return withoutExtension
            .replace(INVALID_PACK_NAME_CHARS, "_")
            .trim('.', '_')
            .ifBlank { "pack" }
    }

    /** Selects the pack used by [load]. Passing null uses the built-in images only. */
    fun setActivePack(name: String?) {
        val normalizedName = name?.let { sanitizePackName(it) }
        if (activePackName == normalizedName) return
        activePackName = normalizedName
        bitmapCache.evictAll()
    }

    fun getActivePackName(): String? = activePackName

    fun isInstalled(context: Context, packName: String): Boolean {
        return packDir(context, sanitizePackName(packName)).isDirectory
    }

    /** Names of all installed packs. */
    fun installedPackNames(context: Context): List<String> {
        return packsDir(context).listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    fun deletePack(context: Context, packName: String) {
        val normalizedName = sanitizePackName(packName)
        packDir(context, normalizedName).deleteRecursively()
        if (activePackName == normalizedName) {
            activePackName = null
        }
        bitmapCache.evictAll()
    }

    /**
     * The active pack's image that replaces [drawableId], or null when the pack has none (use the
     * built-in one then).
     */
    fun load(context: Context, drawableId: Int): Bitmap? {
        val name = try {
            context.resources.getResourceEntryName(drawableId)
        } catch (e: Resources.NotFoundException) {
            return null
        }
        return load(context, name)
    }

    /** The active pack's image with this resource name (e.g. "button_a"), or null. */
    fun load(context: Context, name: String): Bitmap? {
        val packName = activePackName ?: return null
        val dir = packDir(context, packName)
        if (!dir.isDirectory) return null
        val file = File(dir, name.lowercase(Locale.ROOT) + STORED_EXTENSION)
        if (!file.isFile) return null

        val cacheKey = "${file.absolutePath}:${file.lastModified()}"
        bitmapCache.get(cacheKey)?.let { return it }
        return decode(file)?.also { bitmapCache.put(cacheKey, it) }
    }

    private fun decode(file: File): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_DECODE_SIDE) sample *= 2

            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(file.path, options)
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /** "folder/Button_A.PNG" -> "button_a"; null when it is not an image we accept. */
    private fun imageKey(path: String): String? {
        val fileName = path.substringAfterLast('/').substringAfterLast('\\')
        // Hidden files, e.g. the "._button_a.png" files that macOS puts into zips.
        if (fileName.startsWith(".")) return null
        val dot = fileName.lastIndexOf('.')
        if (dot <= 0) return null
        val extension = fileName.substring(dot + 1).lowercase(Locale.ROOT)
        if (extension !in IMAGE_EXTENSIONS) return null
        val key = fileName.substring(0, dot).lowercase(Locale.ROOT)
        return if (NAME_REGEX.matches(key)) key else null
    }

    /**
     * Collects the images of a new pack in a temporary folder. The installed pack is only
     * replaced by [commit], so a failed or empty install changes nothing.
     */
    class Installer internal constructor(context: Context, packName: String) {
        private val appContext = context.applicationContext
        private val packName = sanitizePackName(packName)
        private val tempDir = File(appContext.filesDir, TEMP_DIR_NAME)
        private val keys = HashSet<String>()
        private var totalBytes = 0L
        private var finished = false

        init {
            tempDir.deleteRecursively()
            tempDir.mkdirs()
        }

        /**
         * Adds one image. [fileName] may contain folders, only its file name is used. The
         * stream is read to its end but is NOT closed. Returns true when the image was taken.
         */
        fun add(fileName: String, input: InputStream): Boolean {
            if (finished) return false
            val key = imageKey(fileName) ?: return false
            if (keys.size >= MAX_FILES || totalBytes >= MAX_TOTAL_BYTES) return false
            if (key in keys) return false // the first image with a name wins

            val target = File(tempDir, key + STORED_EXTENSION)
            var written = 0L
            var tooLarge = false
            try {
                target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        written += read
                        if (written > MAX_FILE_BYTES) {
                            tooLarge = true
                            break
                        }
                        output.write(buffer, 0, read)
                    }
                }
            } catch (e: IOException) {
                target.delete()
                return false
            }
            if (tooLarge) {
                target.delete()
                return false
            }

            // Only keep real images.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                target.delete()
                return false
            }

            keys.add(key)
            totalBytes += written
            return true
        }

        /** Replaces the installed pack with the collected images. Returns how many there are. */
        fun commit(): Int {
            if (finished) return 0
            finished = true
            if (keys.isEmpty()) {
                tempDir.deleteRecursively()
                return 0
            }
            packsDir(appContext).mkdirs()
            val dir = packDir(appContext, packName)
            dir.deleteRecursively()
            if (!tempDir.renameTo(dir)) {
                tempDir.copyRecursively(dir, overwrite = true)
                tempDir.deleteRecursively()
            }
            bitmapCache.evictAll()
            return keys.size
        }

        /** Throws the collected images away. */
        fun abort() {
            finished = true
            tempDir.deleteRecursively()
        }
    }

    fun beginInstall(context: Context, packName: String): Installer = Installer(context, packName)

    /**
     * Installs a pack from a zip file under [packName]. Folders inside the zip do not matter.
     * Returns the number of images installed (0 = the zip had no usable images and nothing was
     * changed), or the error. Call it on a background thread.
     */
    fun installFromZip(context: Context, input: InputStream, packName: String): Result<Int> {
        val installer = beginInstall(context, packName)
        return try {
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entries = 0
                var entry = zip.nextEntry
                while (entry != null && entries < MAX_ZIP_ENTRIES) {
                    entries++
                    if (!entry.isDirectory) {
                        installer.add(entry.name, zip)
                    }
                    entry = zip.nextEntry
                }
            }
            Result.success(installer.commit())
        } catch (e: Exception) {
            installer.abort()
            Result.failure(e)
        }
    }
}

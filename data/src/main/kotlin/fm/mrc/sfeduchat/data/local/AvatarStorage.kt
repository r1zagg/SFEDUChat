package fm.mrc.sfeduchat.data.local

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class AvatarStorage(private val context: Context) {

    fun saveAvatar(uid: String, sourceUri: Uri): String {
        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
        val dest = File(dir, "$uid.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        } ?: error("Не удалось прочитать изображение")
        return dest.absolutePath
    }

    fun avatarFile(path: String?): File? = path?.let { File(it) }?.takeIf { it.exists() }
}

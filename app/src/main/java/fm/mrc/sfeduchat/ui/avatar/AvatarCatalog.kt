package fm.mrc.sfeduchat.ui.avatar

import androidx.annotation.DrawableRes
import fm.mrc.sfeduchat.R

object AvatarCatalog {
    const val PRESET_PREFIX = "preset:"

    data class Preset(val id: String, @DrawableRes val drawableRes: Int)

    val presets = listOf(
        Preset("1", R.drawable.avatar_preset_1),
        Preset("2", R.drawable.avatar_preset_2),
        Preset("3", R.drawable.avatar_preset_3),
        Preset("4", R.drawable.avatar_preset_4),
        Preset("5", R.drawable.avatar_preset_5),
        Preset("6", R.drawable.avatar_preset_6),
        Preset("7", R.drawable.avatar_preset_7),
        Preset("8", R.drawable.avatar_preset_8),
    )

    fun pathForPreset(id: String): String = "$PRESET_PREFIX$id"

    fun resolveDrawableRes(avatarPath: String?): Int? {
        if (avatarPath == null || !avatarPath.startsWith(PRESET_PREFIX)) return null
        val id = avatarPath.removePrefix(PRESET_PREFIX)
        return presets.find { it.id == id }?.drawableRes
    }

    fun defaultPresetPath(): String = pathForPreset(presets.first().id)
}

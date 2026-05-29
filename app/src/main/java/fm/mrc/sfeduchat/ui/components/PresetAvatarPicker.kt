package fm.mrc.sfeduchat.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fm.mrc.sfeduchat.ui.avatar.AvatarCatalog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetAvatarPicker(
    selectedPresetId: String,
    onPresetSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvatarCatalog.presets.forEach { preset ->
            val selected = preset.id == selectedPresetId
            UserAvatar(
                displayName = preset.id,
                presetDrawableRes = preset.drawableRes,
                size = 64.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onPresetSelected(preset.id) }
                    .then(
                        if (selected) {
                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

package fm.mrc.sfeduchat.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fm.mrc.sfeduchat.ui.avatar.AvatarCatalog
import java.io.File

@Composable
fun UserAvatar(
    displayName: String,
    modifier: Modifier = Modifier,
    avatarPath: String? = null,
    avatarUri: Uri? = null,
    @androidx.annotation.DrawableRes presetDrawableRes: Int? = null,
    size: Dp = 48.dp,
) {
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val presetRes = presetDrawableRes ?: AvatarCatalog.resolveDrawableRes(avatarPath)
    val file = avatarPath?.let { File(it) }?.takeIf { it.exists() && presetRes == null }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        when {
            presetRes != null -> {
                Image(
                    painter = painterResource(presetRes),
                    contentDescription = displayName,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            file != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(file)
                        .crossfade(true)
                        .build(),
                    contentDescription = displayName,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            avatarUri != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = displayName,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            else -> {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * 0.38).sp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

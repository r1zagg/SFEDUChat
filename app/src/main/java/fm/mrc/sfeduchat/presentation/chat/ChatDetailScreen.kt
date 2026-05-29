package fm.mrc.sfeduchat.presentation.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fm.mrc.sfeduchat.R
import fm.mrc.sfeduchat.domain.model.DeliveryStatus
import fm.mrc.sfeduchat.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    participantName: String,
    participantAvatarPath: String?,
    onBack: () -> Unit,
    viewModel: ChatViewModel,
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            viewModel.sendImage(stream.readBytes(), context.contentResolver.getType(uri) ?: "image/*")
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            displayName = participantName,
                            avatarPath = participantAvatarPath,
                            size = 40.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(participantName)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = stringResource(R.string.image))
                    }
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = viewModel::onInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.message_hint)) },
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send,
                        ),
                    )
                    IconButton(
                        onClick = viewModel::sendMessage,
                        enabled = !state.isSending && state.inputText.isNotBlank(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages, key = { it.id }) { msg ->
                val isMine = msg.isOutgoing
                println("DEBUG ChatDetailScreen: msg id=${msg.id}, isOutgoing=$isMine, sender=${msg.senderUsername}")
                val align = if (isMine) Alignment.End else Alignment.Start
                val color = if (isMine) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (!isMine) {
                        UserAvatar(
                            displayName = msg.senderUsername,
                            avatarPath = msg.senderAvatarPath ?: participantAvatarPath,
                            size = 32.dp,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Column(horizontalAlignment = align) {
                        Card(colors = CardDefaults.cardColors(containerColor = color)) {
                            Column(Modifier.padding(12.dp)) {
                                val text = msg.plainText
                                val err = msg.decryptError
                                when {
                                    err != null -> Text(
                                        "⚠ $err",
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                    text != null -> Text(text)
                                    else -> Text(stringResource(R.string.encrypted_placeholder))
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    statusLabel(msg.deliveryStatus),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

private fun statusLabel(status: DeliveryStatus): String = when (status) {
    DeliveryStatus.SENDING -> "Отправка…"
    DeliveryStatus.SENT -> "Отправлено"
    DeliveryStatus.DELIVERED -> "Доставлено"
    DeliveryStatus.READ -> "Прочитано"
    DeliveryStatus.FAILED -> "Ошибка"
}

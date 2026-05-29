package fm.mrc.sfeduchat.data.mapper

import fm.mrc.sfeduchat.data.local.entity.ChatEntity
import fm.mrc.sfeduchat.data.local.entity.MessageEntity
import fm.mrc.sfeduchat.data.local.entity.UserEntity
import fm.mrc.sfeduchat.domain.model.Chat
import fm.mrc.sfeduchat.domain.model.DeliveryStatus
import fm.mrc.sfeduchat.domain.model.EncryptedMessage
import fm.mrc.sfeduchat.domain.model.MessageType
import fm.mrc.sfeduchat.domain.model.User

fun UserEntity.toDomain() = User(
    uid = uid,
    username = username,
    publicKeyBase64 = publicKeyBase64,
    avatarPath = avatarPath,
)

fun ChatEntity.toDomain() = Chat(
    id = id,
    participantUid = participantUid,
    participantUsername = participantUsername,
    participantAvatarPath = participantAvatarPath,
    lastMessagePreview = lastMessagePreview,
    lastMessageTimestamp = lastMessageTimestamp,
)

fun MessageEntity.toDomain(
    plainText: String? = null,
    decryptError: String? = null,
    isOutgoing: Boolean? = null,
) = EncryptedMessage(
    id = id,
    chatId = chatId,
    senderUid = senderUid,
    senderUsername = senderUsername,
    senderAvatarPath = senderAvatarPath,
    ciphertextBase64 = ciphertextBase64,
    ivBase64 = ivBase64,
    encryptedAesKeyBase64 = encryptedAesKeyBase64,
    hmacBase64 = hmacBase64,
    timestamp = timestamp,
    isOutgoing = isOutgoing ?: this.isOutgoing,
    deliveryStatus = DeliveryStatus.valueOf(deliveryStatus),
    messageType = MessageType.valueOf(messageType),
    attachmentMimeType = attachmentMimeType,
    plainText = plainText,
    decryptError = decryptError,
)

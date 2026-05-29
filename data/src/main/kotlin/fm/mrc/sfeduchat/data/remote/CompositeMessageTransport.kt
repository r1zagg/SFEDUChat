package fm.mrc.sfeduchat.data.remote

import fm.mrc.sfeduchat.data.remote.dto.RemoteMessageDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
/** Firebase при наличии google-services, иначе локальный relay. */
class CompositeMessageTransport(
    private val firebase: FirebaseMessageTransport,
    private val local: LocalRelayTransport,
) : MessageTransport {

    private val primary: MessageTransport
        get() = if (firebase.isAvailable) firebase else local

    override val isAvailable: Boolean = true

    override suspend fun publishPublicKey(uid: String, publicKeyBase64: String) {
        local.publishPublicKey(uid, publicKeyBase64)
        if (firebase.isAvailable) firebase.publishPublicKey(uid, publicKeyBase64)
    }

    override suspend fun fetchPublicKey(uid: String): String? =
        local.fetchPublicKey(uid)
            ?: if (firebase.isAvailable) firebase.fetchPublicKey(uid) else null

    override suspend fun sendMessage(chatId: String, message: RemoteMessageDto) {
        local.sendMessage(chatId, message)
        if (firebase.isAvailable) firebase.sendMessage(chatId, message)
    }

    override fun observeMessages(chatId: String, recipientUid: String): Flow<List<RemoteMessageDto>> =
        merge(
            local.observeMessages(chatId, recipientUid),
            if (firebase.isAvailable) firebase.observeMessages(chatId, recipientUid)
            else kotlinx.coroutines.flow.emptyFlow(),
        )

    override suspend fun updateDeliveryStatus(chatId: String, messageId: String, status: String) {
        local.updateDeliveryStatus(chatId, messageId, status)
        if (firebase.isAvailable) firebase.updateDeliveryStatus(chatId, messageId, status)
    }
}

package fm.mrc.sfeduchat.data.remote

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import fm.mrc.sfeduchat.data.remote.dto.RemoteMessageDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
class FirebaseMessageTransport : MessageTransport {

    private val database by lazy { FirebaseDatabase.getInstance().reference }

    override val isAvailable: Boolean
        get() = runCatching { FirebaseDatabase.getInstance() }.isSuccess

    override suspend fun publishPublicKey(uid: String, publicKeyBase64: String) {
        database.child("users").child(uid).child("public_key").setValue(publicKeyBase64).await()
    }

    override suspend fun fetchPublicKey(uid: String): String? {
        val snap = database.child("users").child(uid).child("public_key").get().await()
        return snap.getValue(String::class.java)
    }

    override suspend fun sendMessage(chatId: String, message: RemoteMessageDto) {
        database.child("messages").child(chatId).child(message.id).setValue(message).await()
    }

    override fun observeMessages(chatId: String, recipientUid: String): Flow<List<RemoteMessageDto>> =
        callbackFlow {
            val ref = database.child("messages").child(chatId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull { child ->
                        child.getValue(RemoteMessageDto::class.java)?.copy(id = child.key ?: "")
                    }.filter { it.recipientUid == recipientUid || it.recipientUid.isEmpty() }
                    trySend(messages.sortedBy { it.timestamp })
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }

    override suspend fun updateDeliveryStatus(chatId: String, messageId: String, status: String) {
        database.child("messages").child(chatId).child(messageId)
            .child("deliveryStatus").setValue(status).await()
    }
}

package fm.mrc.sfeduchat.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CHAT_LIST = "chat_list"
    const val CHAT_DETAIL =
        "chat/{chatId}/{participantUid}/{participantName}/{participantAvatar}"
    const val KEYS = "keys"
    const val NO_AVATAR = "_"

    fun chatDetail(
        chatId: String,
        participantUid: String,
        participantName: String,
        participantAvatar: String?,
    ): String {
        val name = java.net.URLEncoder.encode(participantName, Charsets.UTF_8.name())
        val avatar = participantAvatar?.let {
            java.net.URLEncoder.encode(it, Charsets.UTF_8.name())
        } ?: NO_AVATAR
        return "chat/$chatId/$participantUid/$name/$avatar"
    }
}

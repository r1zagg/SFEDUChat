package fm.mrc.sfeduchat.di

import fm.mrc.sfeduchat.data.crypto.AesEncryptionManager
import fm.mrc.sfeduchat.data.crypto.HmacSignatureManager
import fm.mrc.sfeduchat.data.crypto.KeystoreManager
import fm.mrc.sfeduchat.data.crypto.MessageCryptoService
import fm.mrc.sfeduchat.data.crypto.RsaEncryptionManager
import fm.mrc.sfeduchat.data.crypto.SessionKeyManager
import fm.mrc.sfeduchat.data.di.DataProviders
import fm.mrc.sfeduchat.data.local.AvatarStorage
import fm.mrc.sfeduchat.data.local.SessionPreferences
import fm.mrc.sfeduchat.data.remote.CompositeMessageTransport
import fm.mrc.sfeduchat.data.remote.FirebaseMessageTransport
import fm.mrc.sfeduchat.data.remote.LocalRelayTransport
import fm.mrc.sfeduchat.data.remote.MessageTransport
import fm.mrc.sfeduchat.data.repository.AuthRepositoryImpl
import fm.mrc.sfeduchat.data.repository.ChatRepositoryImpl
import fm.mrc.sfeduchat.data.repository.KeyRepositoryImpl
import fm.mrc.sfeduchat.data.repository.MessageRepositoryImpl
import fm.mrc.sfeduchat.domain.repository.AuthRepository
import fm.mrc.sfeduchat.domain.repository.ChatRepository
import fm.mrc.sfeduchat.domain.repository.KeyRepository
import fm.mrc.sfeduchat.domain.repository.MessageRepository
import fm.mrc.sfeduchat.domain.usecase.LoginUseCase
import fm.mrc.sfeduchat.domain.usecase.RegisterUseCase
import fm.mrc.sfeduchat.domain.usecase.RegenerateKeysUseCase
import fm.mrc.sfeduchat.domain.usecase.SendMessageUseCase
import fm.mrc.sfeduchat.presentation.SessionViewModel
import fm.mrc.sfeduchat.presentation.auth.AuthViewModel
import fm.mrc.sfeduchat.presentation.chat.ChatViewModel
import fm.mrc.sfeduchat.presentation.chatlist.ChatListViewModel
import fm.mrc.sfeduchat.presentation.keys.KeyManagementViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { AesEncryptionManager() }
    single { RsaEncryptionManager() }
    single { HmacSignatureManager() }
    single { KeystoreManager(get()) }
    single { SessionKeyManager(get(), androidContext()) }
    single { MessageCryptoService(get(), get(), get(), get()) }

    single { DataProviders(androidContext()) }
    single { get<DataProviders>().userDao }
    single { get<DataProviders>().chatDao }
    single { get<DataProviders>().messageDao }
    single { get<DataProviders>().relayMessageDao }
    single { SessionPreferences(androidContext()) }
    single { AvatarStorage(androidContext()) }

    single { FirebaseMessageTransport() }
    single { LocalRelayTransport(get(), get()) }
    single<MessageTransport> { CompositeMessageTransport(get(), get()) }

    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<KeyRepository> { KeyRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single<ChatRepository> { ChatRepositoryImpl(get(), get(), get(), get()) }
    single<MessageRepository> {
        MessageRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }

    factory { RegisterUseCase(get(), get()) }
    factory { LoginUseCase(get(), get()) }
    factory { SendMessageUseCase(get()) }
    factory { RegenerateKeysUseCase(get(), get()) }

    viewModel { SessionViewModel(get()) }
    viewModel { AuthViewModel(get(), get(), get(), get()) }
    viewModel { ChatListViewModel(get(), get()) }
    viewModel { (chatId: String, participantUid: String) ->
        ChatViewModel(chatId, participantUid, get(), get(), get(), get())
    }
    viewModel { KeyManagementViewModel(get(), get()) }
}

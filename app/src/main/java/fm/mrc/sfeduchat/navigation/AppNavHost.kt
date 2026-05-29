package fm.mrc.sfeduchat.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fm.mrc.sfeduchat.presentation.SessionViewModel
import fm.mrc.sfeduchat.presentation.auth.LoginScreen
import fm.mrc.sfeduchat.presentation.auth.RegisterScreen
import fm.mrc.sfeduchat.presentation.chat.ChatDetailScreen
import fm.mrc.sfeduchat.presentation.chatlist.ChatListScreen
import fm.mrc.sfeduchat.presentation.keys.KeyManagementScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavHost(sessionViewModel: SessionViewModel = koinViewModel()) {
    val navController = rememberNavController()
    val currentUser by sessionViewModel.currentUser.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val route = navController.currentDestination?.route
            if (route == Routes.LOGIN || route == Routes.REGISTER) {
                navController.navigate(Routes.CHAT_LIST) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateRegister = { navController.navigate(Routes.REGISTER) },
                onLoggedIn = {
                    navController.navigate(Routes.CHAT_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegistered = {
                    navController.navigate(Routes.CHAT_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onOpenChat = { chatId, uid, name, avatar ->
                    navController.navigate(Routes.chatDetail(chatId, uid, name, avatar))
                },
                onKeys = { navController.navigate(Routes.KEYS) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.CHAT_DETAIL,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("participantUid") { type = NavType.StringType },
                navArgument("participantName") { type = NavType.StringType },
                navArgument("participantAvatar") { type = NavType.StringType },
            ),
        ) { entry ->
            val name = URLDecoder.decode(
                entry.arguments?.getString("participantName").orEmpty(),
                StandardCharsets.UTF_8.toString(),
            )
            val avatarRaw = entry.arguments?.getString("participantAvatar").orEmpty()
            val avatar = if (avatarRaw == Routes.NO_AVATAR) {
                null
            } else {
                URLDecoder.decode(avatarRaw, StandardCharsets.UTF_8.toString())
            }
            val chatId = entry.arguments?.getString("chatId").orEmpty()
            val participantUid = entry.arguments?.getString("participantUid").orEmpty()
            ChatDetailScreen(
                participantName = name,
                participantAvatarPath = avatar,
                onBack = { navController.popBackStack() },
                viewModel = koinViewModel { parametersOf(chatId, participantUid) },
            )
        }
        composable(Routes.KEYS) {
            KeyManagementScreen(onBack = { navController.popBackStack() })
        }
    }
}

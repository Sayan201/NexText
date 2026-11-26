package com.nextext.sms

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nextext.sms.domain.model.MessageCategory
import com.nextext.sms.sms.NotificationHelper
import com.nextext.sms.ui.components.LiquidGlassNavBottomBar
import com.nextext.sms.ui.components.LiquidGlassTabItem
import com.nextext.sms.ui.components.UpdateFlyoutDialog
import com.nextext.sms.ui.screens.ComposeScreen
import com.nextext.sms.ui.screens.FinanceScreen
import com.nextext.sms.ui.screens.InboxScreen
import com.nextext.sms.ui.screens.SearchScreen
import com.nextext.sms.ui.screens.SettingsScreen
import com.nextext.sms.ui.screens.SmartHubScreen
import com.nextext.sms.ui.screens.ThreadScreen
import com.nextext.sms.ui.theme.NexTextTheme
import com.nextext.sms.ui.viewmodel.InboxFilter
import com.nextext.sms.ui.viewmodel.MainViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private data class NotificationNavTarget(
    val threadId: Long,
    val title: String,
    val address: String,
    val nonce: Long = System.nanoTime()
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as NexTextApplication).container)
    }

    private val pendingNotificationTarget = MutableStateFlow<NotificationNavTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialSendTarget = extractSmsTarget(intent)
        handleNotificationIntent(intent)

        setContent {
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            NexTextTheme(
                themeMode = prefs.themeMode,
                pitchBlackAccent = prefs.pitchBlackAccent,
                useMaterial3Design = prefs.useMaterial3Design,
                dynamicColor = prefs.dynamicColor
            ) {
                NexTextApp(
                    viewModel = viewModel,
                    initialSendTarget = initialSendTarget,
                    notificationTargetFlow = pendingNotificationTarget,
                    onConsumeNotificationTarget = { pendingNotificationTarget.value = null },
                    onRequestDefaultSms = { launchDefaultSmsPrompt(it) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val threadId = intent.getLongExtra(NotificationHelper.EXTRA_THREAD_ID, -1L)
        if (threadId > 0L) {
            val title = intent.getStringExtra(NotificationHelper.EXTRA_SENDER_TITLE)
                ?.takeIf { it.isNotBlank() } ?: "Conversation"
            val address = intent.getStringExtra(NotificationHelper.EXTRA_SENDER_ADDRESS).orEmpty()
            pendingNotificationTarget.value = NotificationNavTarget(
                threadId = threadId,
                title = title,
                address = address
            )
            intent.removeExtra(NotificationHelper.EXTRA_THREAD_ID)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemStatusAndSync()
    }

    private fun launchDefaultSmsPrompt(launcher: (Intent) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                launcher(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS))
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            }
            launcher(intent)
        }
    }

    private fun extractSmsTarget(intent: Intent?): Pair<String, String>? {
        if (intent == null) return null
        val action = intent.action
        if (action == Intent.ACTION_SENDTO || action == Intent.ACTION_SEND) {
            val address = intent.data?.schemeSpecificPart?.substringBefore('?')?.trim().orEmpty()
            val body = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            if (address.isNotBlank() || body.isNotBlank()) {
                return address to body
            }
        }
        return null
    }
}

private object Routes {
    const val INBOX = "inbox"
    const val FINANCE = "finance"
    const val SMART_HUB = "smarthub"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val COMPOSE = "compose?address={address}&body={body}"
    const val THREAD = "thread/{threadId}?title={title}&address={address}"

    fun composeRoute(address: String = "", body: String = ""): String =
        "compose?address=${Uri.encode(address)}&body=${Uri.encode(body)}"

    fun threadRoute(threadId: Long, title: String, address: String): String =
        "thread/$threadId?title=${Uri.encode(title)}&address=${Uri.encode(address)}"
}

@Composable
private fun NexTextApp(
    viewModel: MainViewModel,
    initialSendTarget: Pair<String, String>?,
    notificationTargetFlow: StateFlow<NotificationNavTarget?>,
    onConsumeNotificationTarget: () -> Unit,
    onRequestDefaultSms: ((Intent) -> Unit) -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationTarget by notificationTargetFlow.collectAsStateWithLifecycle()

    val threads by viewModel.filteredThreads.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val unreadCounts by viewModel.unreadCounts.collectAsStateWithLifecycle()
    val recentOtp by viewModel.recentOtpMessage.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isDefaultSmsApp by viewModel.isDefaultSmsApp.collectAsStateWithLifecycle()
    val hasPermissions by viewModel.hasPermissions.collectAsStateWithLifecycle()
    val activeSims by viewModel.activeSims.collectAsStateWithLifecycle()
    val selectedSubId by viewModel.selectedSubId.collectAsStateWithLifecycle()
    val financeState by viewModel.financeState.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val transitOrders by viewModel.transitOrders.collectAsStateWithLifecycle()
    val blockedSenders by viewModel.blockedSenders.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val contacts by viewModel.contactSuggestions.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshSystemStatusAndSync()
    }

    val defaultSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemStatusAndSync()
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackup(uri)
    }

    val driveFolderContext = LocalContext.current
    val driveFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.linkDriveFolder(driveFolderContext, uri)
    }

    val requestAllPermissions = {
        val perms = buildList {
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        permissionLauncher.launch(perms)
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            requestAllPermissions()
        }
        if (initialSendTarget != null) {
            navController.navigate(
                Routes.composeRoute(initialSendTarget.first, initialSendTarget.second)
            )
        }
    }

    LaunchedEffect(notificationTarget) {
        notificationTarget?.let { target ->
            navController.navigate(
                Routes.threadRoute(target.threadId, target.title, target.address)
            ) {
                launchSingleTop = true
            }
            onConsumeNotificationTarget()
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val unreadOnly by viewModel.unreadOnly.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = prefs.useMaterial3Design && currentRoute in listOf(Routes.INBOX, Routes.FINANCE, Routes.SMART_HUB)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.INBOX,
                        onClick = {
                            navController.navigate(Routes.INBOX) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.ChatBubble, contentDescription = null) },
                        label = { Text("Messages") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SMART_HUB,
                        onClick = {
                            navController.navigate(Routes.SMART_HUB) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                        label = { Text("Reminders") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.FINANCE,
                        onClick = {
                            navController.navigate(Routes.FINANCE) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                        label = { Text("Finance") }
                    )
                }
            }
        }
    ) { innerPadding ->
        val hazeState = remember { HazeState() }
        val backdropLayer = rememberGraphicsLayer()
        var backdropOriginInRoot by remember { mutableStateOf(Offset.Zero) }
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.INBOX,
                enterTransition = { fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) },
                exitTransition = { fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) },
                popExitTransition = { fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) },
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        backdropOriginInRoot = coords.positionInRoot()
                    }
                    .drawWithContent {
                        backdropLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(backdropLayer)
                    }
                    .haze(state = hazeState)
            ) {
            composable(Routes.INBOX) {
                InboxScreen(
                    threads = threads,
                    selectedFilter = selectedFilter,
                    unreadCounts = unreadCounts,
                    recentOtp = recentOtp,
                    isSyncing = isSyncing,
                    isDefaultSmsApp = isDefaultSmsApp,
                    hasPermissions = hasPermissions,
                    unreadOnly = unreadOnly,
                    useMaterial3Design = prefs.useMaterial3Design,
                    swapMenuBarPositions = prefs.swapMenuBarPositions,
                    onSelectFilter = viewModel::setFilter,
                    onOpenThread = { thread ->
                        val title = thread.contactName?.takeIf { it.isNotBlank() } ?: thread.address
                        navController.navigate(
                            Routes.threadRoute(thread.threadId, title, thread.address)
                        )
                    },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenCompose = { navController.navigate(Routes.composeRoute()) },
                    onSyncClick = { viewModel.syncSms(showFeedback = true) },
                    onToggleUnreadOnly = viewModel::toggleUnreadOnly,
                    onMarkAllAsRead = viewModel::markAllAsRead,
                    onRequestDefaultSms = {
                        onRequestDefaultSms { defaultSmsLauncher.launch(it) }
                    },
                    onRequestPermissions = requestAllPermissions,
                    onArchiveThread = viewModel::setThreadArchived,
                    onDeleteThread = viewModel::deleteThread,
                    onMarkRead = viewModel::markThreadRead,
                    onCopyOtp = { otp -> viewModel.showMessage("OTP $otp copied") },
                    onNavigateToReminders = {
                        navController.navigate(Routes.SMART_HUB) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToFinance = {
                        navController.navigate(Routes.FINANCE) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    showUpdateCapsule = updateState.availableUpdate != null &&
                        prefs.dismissedUpdateVersion == updateState.availableUpdate?.version,
                    onOpenUpdatePopup = viewModel::openUpdatePopup
                )
            }

            composable(Routes.FINANCE) {
                FinanceScreen(
                    state = financeState,
                    useMaterial3Design = prefs.useMaterial3Design,
                    swapMenuBarPositions = prefs.swapMenuBarPositions,
                    unreadMessagesCount = unreadCounts.values.sum(),
                    onNavigateToMessages = {
                        navController.navigate(Routes.INBOX) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToReminders = {
                        navController.navigate(Routes.SMART_HUB) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenThread = { threadId, address ->
                        navController.navigate(Routes.threadRoute(threadId, address, address))
                    }
                )
            }

            composable(Routes.SMART_HUB) {
                SmartHubScreen(
                    reminders = reminders,
                    transitOrders = transitOrders,
                    useMaterial3Design = prefs.useMaterial3Design,
                    swapMenuBarPositions = prefs.swapMenuBarPositions,
                    unreadMessagesCount = unreadCounts.values.sum(),
                    onNavigateToMessages = {
                        navController.navigate(Routes.INBOX) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToFinance = {
                        navController.navigate(Routes.FINANCE) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onToggleReminderDone = viewModel::toggleReminderCompleted,
                    onToggleReminderNotify = viewModel::toggleReminderNotify,
                    onDeleteReminder = viewModel::deleteReminder,
                    onAddReminder = viewModel::addCustomReminder,
                    onOpenThread = { threadId, title ->
                        navController.navigate(Routes.threadRoute(threadId, title, title))
                    },
                    onCopyCode = { code -> viewModel.showMessage("Copied $code") }
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    query = searchQuery,
                    results = searchResults,
                    onQueryChange = viewModel::updateSearchQuery,
                    onBack = { navController.popBackStack() },
                    onOpenResult = { msg ->
                        val title = msg.contactName ?: msg.address
                        navController.navigate(
                            Routes.threadRoute(msg.threadId, title, msg.address)
                        )
                    }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    preferences = prefs,
                    isDefaultSmsApp = isDefaultSmsApp,
                    blockedSenders = blockedSenders,
                    onBack = { navController.popBackStack() },
                    onRequestDefaultSms = {
                        onRequestDefaultSms { defaultSmsLauncher.launch(it) }
                    },
                    onSetThemeMode = viewModel::setThemeMode,
                    onSetPitchBlackAccent = viewModel::setPitchBlackAccent,
                    onSetUseMaterial3Design = viewModel::setUseMaterial3Design,
                    onSetDynamicColor = viewModel::setDynamicColor,
                    onSetAutoDeleteOtps = viewModel::setAutoDeleteOldOtps,
                    onSetMutePromotions = viewModel::setMutePromotions,
                    onSetShowOtpNotification = viewModel::setShowOtpNotification,
                    onSetShowQuickReplies = viewModel::setShowQuickReplies,
                    onSetSwapMenuBarPositions = viewModel::setSwapMenuBarPositions,
                    onSetGlassNavStyle = viewModel::setGlassNavStyle,
                    onSetGroupMessagingMode = viewModel::setGroupMessagingMode,
                    onSetSwipeLeftAction = viewModel::setSwipeLeftAction,
                    onSetSwipeRightAction = viewModel::setSwipeRightAction,
                    onSetAutoBackupFrequency = { freq -> viewModel.setAutoBackupFrequency(context, freq) },
                    onCleanOldOtpsNow = viewModel::cleanOldOtpsNow,
                    onExportBackup = {
                        exportBackupLauncher.launch("nextext-backup-${System.currentTimeMillis()}.json")
                    },
                    onImportBackup = {
                        importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    onBackupToGoogleDrive = { viewModel.backupToDriveNow(context) },
                    onRestoreFromGoogleDrive = { viewModel.restoreFromDrive(context) },
                    onLinkDriveFolder = { driveFolderLauncher.launch(null) },
                    onUnlinkDriveFolder = { viewModel.unlinkDriveFolder(context) },
                    onUnblockSender = viewModel::unblockSender,
                    isCheckingUpdates = updateState.isChecking,
                    onCheckForUpdates = { viewModel.checkForUpdates(userInitiated = true) }
                )
            }

            composable(
                route = Routes.COMPOSE,
                arguments = listOf(
                    navArgument("address") { type = NavType.StringType; defaultValue = "" },
                    navArgument("body") { type = NavType.StringType; defaultValue = "" }
                ),
                enterTransition = {
                    scaleIn(
                        animationSpec = tween(320, easing = FastOutSlowInEasing),
                        initialScale = 0.08f,
                        transformOrigin = TransformOrigin(0.90f, 0.92f)
                    ) + fadeIn(tween(220, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    scaleOut(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        targetScale = 0.08f,
                        transformOrigin = TransformOrigin(0.90f, 0.92f)
                    ) + fadeOut(tween(200, easing = FastOutSlowInEasing))
                }
            ) { entry ->
                val initAddr = entry.arguments?.getString("address").orEmpty()
                val initBody = entry.arguments?.getString("body").orEmpty()
                ComposeScreen(
                    initialAddress = initAddr,
                    initialBody = initBody,
                    contacts = contacts,
                    activeSims = activeSims,
                    selectedSubId = selectedSubId,
                    onSelectSim = viewModel::setSelectedSim,
                    onSearchContacts = viewModel::loadContacts,
                    onBack = { navController.popBackStack() },
                    onSend = { addr, body ->
                        viewModel.sendSms(addr, body) { threadId ->
                            navController.navigate(Routes.threadRoute(threadId, addr, addr)) {
                                popUpTo(Routes.INBOX)
                            }
                        }
                    }
                )
            }

            composable(
                route = Routes.THREAD,
                arguments = listOf(
                    navArgument("threadId") { type = NavType.LongType },
                    navArgument("title") { type = NavType.StringType; defaultValue = "Conversation" },
                    navArgument("address") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val threadId = entry.arguments?.getLong("threadId") ?: -1L
                val title = entry.arguments?.getString("title") ?: "Conversation"
                val address = entry.arguments?.getString("address").orEmpty()
                val messages by viewModel.observeThreadMessages(threadId)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val effectiveAddress = messages.lastOrNull()?.address ?: address
                val isBlocked by viewModel.observeIsBlocked(effectiveAddress)
                    .collectAsStateWithLifecycle(initialValue = false)

                ThreadScreen(
                    threadId = threadId,
                    title = title,
                    address = effectiveAddress,
                    messages = messages,
                    isBlocked = isBlocked,
                    activeSims = activeSims,
                    selectedSubId = selectedSubId,
                    onSelectSim = viewModel::setSelectedSim,
                    onBack = { navController.popBackStack() },
                    onSend = { body ->
                        if (effectiveAddress.isNotBlank()) {
                            viewModel.sendSms(effectiveAddress, body, existingThreadId = threadId)
                        }
                    },
                    onMarkRead = { viewModel.markThreadRead(threadId) },
                    onToggleStar = viewModel::toggleMessageStarred,
                    onDeleteMessage = viewModel::deleteMessage,
                    onOverrideCategory = { cat ->
                        if (effectiveAddress.isNotBlank()) {
                            viewModel.overrideSenderCategory(effectiveAddress, cat)
                        }
                    },
                    onToggleBlock = {
                        if (effectiveAddress.isNotBlank()) {
                            if (isBlocked) {
                                viewModel.unblockSender(
                                    com.nextext.sms.data.repository.SmsRepository.normalizeAddress(effectiveAddress)
                                )
                            } else {
                                viewModel.blockSender(effectiveAddress)
                            }
                        }
                    },
                    onArchiveThread = { viewModel.setThreadArchived(threadId, true) },
                    onDeleteThread = { viewModel.deleteThread(threadId) },
                    onShowMessage = viewModel::showMessage
                )
            }
        }

        AnimatedVisibility(
            visible = !prefs.useMaterial3Design && (
                (!prefs.swapMenuBarPositions && currentRoute in listOf(Routes.INBOX, Routes.FINANCE, Routes.SMART_HUB)) ||
                    (prefs.swapMenuBarPositions && currentRoute == Routes.INBOX)
                ),
            enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) + slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 2 },
            exit = fadeOut(tween(160, easing = FastOutSlowInEasing)) + slideOutVertically(tween(160, easing = FastOutSlowInEasing)) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val isSwapped = prefs.swapMenuBarPositions
            val tabs = if (isSwapped) {
                listOf(
                    LiquidGlassTabItem(
                        label = "Personal",
                        icon = Icons.Default.Person,
                        unreadCount = unreadCounts[MessageCategory.PERSONAL] ?: 0
                    ),
                    LiquidGlassTabItem(
                        label = "Transactions",
                        icon = Icons.Default.CreditCard,
                        unreadCount = unreadCounts[MessageCategory.TRANSACTIONS] ?: 0
                    ),
                    LiquidGlassTabItem(
                        label = "Promotions",
                        icon = Icons.Default.LocalOffer,
                        unreadCount = unreadCounts[MessageCategory.PROMOTIONS] ?: 0
                    )
                )
            } else {
                listOf(
                    LiquidGlassTabItem(
                        label = "Messages",
                        icon = Icons.Default.ChatBubble,
                        unreadCount = unreadCounts.values.sum()
                    ),
                    LiquidGlassTabItem(
                        label = "Reminders",
                        icon = Icons.Default.Notifications
                    ),
                    LiquidGlassTabItem(
                        label = "Finance",
                        icon = Icons.Default.CreditCard
                    )
                )
            }
            val selectedTab = if (isSwapped) {
                when (selectedFilter) {
                    InboxFilter.PERSONAL -> 0
                    InboxFilter.TRANSACTIONS -> 1
                    InboxFilter.PROMOTIONS -> 2
                    else -> 0
                }
            } else {
                when (currentRoute) {
                    Routes.INBOX -> 0
                    Routes.SMART_HUB -> 1
                    Routes.FINANCE -> 2
                    else -> 0
                }
            }
            LiquidGlassNavBottomBar(
                selectedTab = selectedTab,
                tabs = tabs,
                accentColor = MaterialTheme.colorScheme.primary,
                hazeState = hazeState,
                backdropLayer = backdropLayer,
                backdropOriginInRoot = backdropOriginInRoot,
                heightPercent = prefs.glassBarHeightPercent,
                maxWidthPercent = prefs.glassBarMaxWidthPercent,
                buttonSpacingDp = prefs.glassBarButtonSpacingDp,
                opacityPercent = prefs.glassBarOpacityPercent,
                rimBrightnessPercent = prefs.glassBarRimBrightnessPercent,
                onSelectTab = { tab ->
                    if (isSwapped) {
                        val targetFilter = when (tab) {
                            0 -> InboxFilter.PERSONAL
                            1 -> InboxFilter.TRANSACTIONS
                            2 -> InboxFilter.PROMOTIONS
                            else -> InboxFilter.PERSONAL
                        }
                        viewModel.setFilter(targetFilter)
                    } else {
                        val target = when (tab) {
                            0 -> Routes.INBOX
                            1 -> Routes.SMART_HUB
                            2 -> Routes.FINANCE
                            else -> Routes.INBOX
                        }
                        if (navController.currentDestination?.route != target) {
                            navController.navigate(target) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                onComposeClick = { navController.navigate(Routes.composeRoute()) }
            )
        }

        val pendingUpdate = updateState.availableUpdate
        if (updateState.showPopup && pendingUpdate != null) {
            UpdateFlyoutDialog(
                update = pendingUpdate,
                state = updateState,
                onDownloadAndInstall = { viewModel.downloadAndInstallUpdate(context) },
                onCancel = viewModel::cancelUpdatePopup,
                onDontShowAgain = viewModel::dontShowUpdateAgain
            )
        }
        }
    }
}

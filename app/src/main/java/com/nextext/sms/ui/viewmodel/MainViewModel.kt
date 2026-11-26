package com.nextext.sms.ui.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nextext.sms.AppContainer
import com.nextext.sms.data.backup.DriveFolderBackup
import com.nextext.sms.data.local.BlockedSenderEntity
import com.nextext.sms.data.local.MessageEntity
import com.nextext.sms.data.local.ReminderEntity
import com.nextext.sms.data.local.ThreadSummary
import com.nextext.sms.data.local.TransactionEntity
import com.nextext.sms.data.local.TransitOrderEntity
import com.nextext.sms.data.preferences.AutoBackupFrequency
import com.nextext.sms.data.preferences.GroupMessagingMode
import com.nextext.sms.data.preferences.PitchBlackAccent
import com.nextext.sms.data.preferences.SwipeAction
import com.nextext.sms.data.preferences.ThemeMode
import com.nextext.sms.data.preferences.UserPreferences
import com.nextext.sms.worker.AutoBackupWorker
import com.nextext.sms.domain.model.ContactOption
import com.nextext.sms.domain.model.MessageCategory
import com.nextext.sms.domain.model.ReminderKind
import com.nextext.sms.domain.model.SimInfo
import com.nextext.sms.domain.model.SpendCategory
import com.nextext.sms.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class InboxFilter {
    PERSONAL,
    TRANSACTIONS,
    PROMOTIONS,
    SPAM,
    STARRED,
    ARCHIVED
}

data class AccountCardSummary(
    val key: String,
    val institution: String,
    val accountLast4: String?,
    val latestBalance: Double?,
    val currency: String,
    val transactionCount: Int,
    val lastTimestamp: Long
)

data class FinanceDashboardState(
    val monthlyDebitTotal: Double = 0.0,
    val monthlyCreditTotal: Double = 0.0,
    val primaryCurrency: String = "INR",
    val accounts: List<AccountCardSummary> = emptyList(),
    val spendByCategory: List<Pair<SpendCategory, Double>> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList()
)

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.smsRepository
    private val prefsRepo = container.preferencesRepository
    private val backupManager = container.backupManager

    private val _unreadOnly = MutableStateFlow(false)
    val unreadOnly: StateFlow<Boolean> = _unreadOnly.asStateFlow()

    private val _selectedFilter = MutableStateFlow(InboxFilter.PERSONAL)
    val selectedFilter: StateFlow<InboxFilter> = _selectedFilter.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isDefaultSmsApp = MutableStateFlow(repo.isDefaultSmsApp())
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp.asStateFlow()

    private val _hasPermissions = MutableStateFlow(repo.hasSmsPermissions())
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private val _activeSims = MutableStateFlow(repo.getActiveSims())
    val activeSims: StateFlow<List<SimInfo>> = _activeSims.asStateFlow()

    private val _selectedSubId = MutableStateFlow(-1)
    val selectedSubId: StateFlow<Int> = _selectedSubId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MessageEntity>>(emptyList())
    val searchResults: StateFlow<List<MessageEntity>> = _searchResults.asStateFlow()

    private val _contactSuggestions = MutableStateFlow<List<ContactOption>>(emptyList())
    val contactSuggestions: StateFlow<List<ContactOption>> = _contactSuggestions.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserPreferences()
    )

    val allThreads: StateFlow<List<ThreadSummary>> = repo.observeAllThreads().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val filteredThreads: StateFlow<List<ThreadSummary>> = combine(
        allThreads,
        _selectedFilter,
        _unreadOnly
    ) { threads, filter, unreadOnly ->
        val byCat = when (filter) {
            InboxFilter.PERSONAL -> threads.filter { !it.isArchived && it.category == MessageCategory.PERSONAL }
            InboxFilter.TRANSACTIONS -> threads.filter { !it.isArchived && it.category == MessageCategory.TRANSACTIONS }
            InboxFilter.PROMOTIONS -> threads.filter { !it.isArchived && it.category == MessageCategory.PROMOTIONS }
            InboxFilter.SPAM -> threads.filter { !it.isArchived && it.category == MessageCategory.SPAM }
            InboxFilter.STARRED -> threads.filter { it.hasStarred }
            InboxFilter.ARCHIVED -> threads.filter { it.isArchived }
        }
        if (unreadOnly) byCat.filter { it.unreadCount > 0 } else byCat
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCounts: StateFlow<Map<MessageCategory, Int>> = combine(
        repo.observeUnreadCount(MessageCategory.PERSONAL),
        repo.observeUnreadCount(MessageCategory.TRANSACTIONS),
        repo.observeUnreadCount(MessageCategory.PROMOTIONS),
        repo.observeUnreadCount(MessageCategory.SPAM)
    ) { p, t, pr, s ->
        mapOf(
            MessageCategory.PERSONAL to p,
            MessageCategory.TRANSACTIONS to t,
            MessageCategory.PROMOTIONS to pr,
            MessageCategory.SPAM to s
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val recentOtpMessage: StateFlow<MessageEntity?> = repo.observeLatestOtp()
        .map { msg ->
            if (msg != null && System.currentTimeMillis() - msg.timestamp <= 15 * 60_000L) {
                msg
            } else {
                null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val financeState: StateFlow<FinanceDashboardState> = repo.observeAllTransactions()
        .map { list -> computeFinanceDashboard(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceDashboardState())

    val reminders: StateFlow<List<ReminderEntity>> = repo.observeAllReminders().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val transitOrders: StateFlow<List<TransitOrderEntity>> = repo.observeAllTransitOrders().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val blockedSenders: StateFlow<List<BlockedSenderEntity>> = repo.observeBlockedSenders().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        val sims = _activeSims.value
        if (sims.isNotEmpty()) {
            _selectedSubId.value = sims.first().subId
        }
    }

    fun refreshSystemStatusAndSync() {
        _isDefaultSmsApp.value = repo.isDefaultSmsApp()
        _hasPermissions.value = repo.hasSmsPermissions()
        val sims = repo.getActiveSims()
        _activeSims.value = sims
        if (_selectedSubId.value == -1 && sims.isNotEmpty()) {
            _selectedSubId.value = sims.first().subId
        }
        if (_hasPermissions.value) {
            syncSms(showFeedback = false)
        }
    }

    fun syncSms(showFeedback: Boolean = false) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            runCatching { repo.syncSystemSms() }
                .onSuccess { count ->
                    if (showFeedback) {
                        _snackbarMessage.value = if (count > 0) {
                            "Synced $count new messages"
                        } else {
                            "Messages are up to date"
                        }
                    }
                }
                .onFailure { err ->
                    if (showFeedback) {
                        _snackbarMessage.value = "Sync failed: ${err.localizedMessage}"
                    }
                }
            _isSyncing.value = false
        }
    }

    fun setFilter(filter: InboxFilter) {
        _selectedFilter.value = filter
    }

    fun setSelectedSim(subId: Int) {
        _selectedSubId.value = subId
    }

    fun observeThreadMessages(threadId: Long): Flow<List<MessageEntity>> =
        repo.observeThreadMessages(threadId)

    fun observeIsBlocked(address: String): Flow<Boolean> = repo.observeIsBlocked(address)

    fun toggleUnreadOnly() {
        _unreadOnly.value = !_unreadOnly.value
    }

    fun markThreadRead(threadId: Long) {
        viewModelScope.launch { repo.markThreadRead(threadId) }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repo.markAllAsRead()
            _snackbarMessage.value = "All messages marked as read"
        }
    }

    fun setThreadArchived(threadId: Long, archived: Boolean) {
        viewModelScope.launch {
            repo.setThreadArchived(threadId, archived)
            _snackbarMessage.value = if (archived) "Conversation archived" else "Conversation unarchived"
        }
    }

    fun deleteThread(threadId: Long) {
        viewModelScope.launch {
            repo.deleteThread(threadId)
            _snackbarMessage.value = "Conversation deleted"
        }
    }

    fun deleteMessage(messageId: Long, systemSmsId: Long?) {
        viewModelScope.launch {
            repo.deleteMessage(messageId, systemSmsId)
            _snackbarMessage.value = "Message deleted"
        }
    }

    fun toggleMessageStarred(messageId: Long, starred: Boolean) {
        viewModelScope.launch { repo.toggleMessageStarred(messageId, starred) }
    }

    fun overrideSenderCategory(address: String, category: MessageCategory) {
        viewModelScope.launch {
            repo.overrideSenderCategory(address, category)
            _snackbarMessage.value = "Moved to ${category.label}"
        }
    }

    fun blockSender(address: String) {
        viewModelScope.launch {
            repo.blockSender(address)
            _snackbarMessage.value = "$address blocked and moved to Spam"
        }
    }

    fun unblockSender(normalizedAddress: String) {
        viewModelScope.launch {
            repo.unblockSender(normalizedAddress)
            _snackbarMessage.value = "Sender unblocked"
        }
    }

    fun sendSms(
        address: String,
        body: String,
        existingThreadId: Long? = null,
        onSent: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repo.sendSms(
                address = address,
                body = body,
                subId = _selectedSubId.value,
                existingThreadId = existingThreadId
            )
            result.onSuccess { threadId ->
                onSent(threadId)
            }.onFailure { err ->
                _snackbarMessage.value = "Failed to send SMS: ${err.localizedMessage}"
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            _searchResults.value = repo.searchMessages(query)
        }
    }

    fun loadContacts(query: String) {
        viewModelScope.launch {
            _contactSuggestions.value = repo.searchContacts(query)
        }
    }

    fun addCustomReminder(
        title: String,
        description: String,
        amount: Double?,
        dueTimestamp: Long,
        kind: ReminderKind
    ) {
        viewModelScope.launch {
            repo.addCustomReminder(title, description, amount, dueTimestamp, kind)
            _snackbarMessage.value = "Reminder added"
        }
    }

    fun toggleReminderCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch { repo.setReminderCompleted(id, completed) }
    }

    fun toggleReminderNotify(id: Long, enabled: Boolean) {
        viewModelScope.launch { repo.setReminderNotify(id, enabled) }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch { repo.deleteReminder(id) }
    }

    fun cleanOldOtpsNow() {
        viewModelScope.launch {
            val removed = repo.cleanOldOtps(24L)
            _snackbarMessage.value = "Cleaned $removed old OTP messages"
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setPitchBlackAccent(accent: PitchBlackAccent) {
        viewModelScope.launch { prefsRepo.setPitchBlackAccent(accent) }
    }

    fun setUseMaterial3Design(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setUseMaterial3Design(enabled) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDynamicColor(enabled) }
    }

    fun setAutoDeleteOldOtps(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoDeleteOldOtps(enabled) }
    }

    fun setMutePromotions(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setMutePromotionsNotifications(enabled) }
    }

    fun setShowOtpNotification(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setShowOtpCopyNotification(enabled) }
    }

    fun setSwapMenuBarPositions(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setSwapMenuBarPositions(enabled) }
    }

    fun setGroupMessagingMode(mode: GroupMessagingMode) {
        viewModelScope.launch { prefsRepo.setGroupMessagingMode(mode) }
    }

    fun setSwipeLeftAction(action: SwipeAction) {
        viewModelScope.launch { prefsRepo.setSwipeLeftAction(action) }
    }

    fun setSwipeRightAction(action: SwipeAction) {
        viewModelScope.launch { prefsRepo.setSwipeRightAction(action) }
    }

    fun setShowQuickReplies(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setShowQuickReplies(enabled) }
    }

    fun setAutoBackupFrequency(context: android.content.Context, freq: AutoBackupFrequency) {
        viewModelScope.launch {
            prefsRepo.setAutoBackupFrequency(freq)
            AutoBackupWorker.schedule(context, freq)
            _snackbarMessage.value = if (freq == AutoBackupFrequency.OFF) {
                "Auto backup disabled"
            } else {
                "Auto backup scheduled: ${freq.label}"
            }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportToUri(uri)
                .onSuccess { count -> _snackbarMessage.value = "Exported $count messages to backup" }
                .onFailure { err -> _snackbarMessage.value = "Export failed: ${err.localizedMessage}" }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.importFromUri(uri)
                .onSuccess { count -> _snackbarMessage.value = "Restored $count messages from backup" }
                .onFailure { err -> _snackbarMessage.value = "Restore failed: ${err.localizedMessage}" }
        }
    }

    /**
     * Binds the Drive folder picked in the system folder chooser. The folder URI is the
     * account binding: whichever Google account was selected in the picker is the one the
     * Drive app syncs this folder through, and we never see or store an account id.
     */
    fun linkDriveFolder(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            val persisted = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }.isSuccess
            }
            if (!persisted) {
                _snackbarMessage.value = "Could not keep access to that folder"
                return@launch
            }
            val name = withContext(Dispatchers.IO) {
                DriveFolderBackup.queryFolderName(context, uri)
            }
            prefsRepo.setDriveBackupTreeUri(uri.toString())
            prefsRepo.setDriveBackupFolderName(name ?: "Drive folder")
            if (prefsRepo.preferences.first().autoBackupFrequency != AutoBackupFrequency.OFF) {
                AutoBackupWorker.runOnce(context)
            }
            _snackbarMessage.value = "Linked: ${name ?: "Drive folder"}"
        }
    }

    fun unlinkDriveFolder(context: android.content.Context) {
        viewModelScope.launch {
            val stored = prefsRepo.preferences.first().driveBackupTreeUri
            if (stored != null) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            Uri.parse(stored),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                }
            }
            prefsRepo.setDriveBackupTreeUri(null)
            prefsRepo.setDriveBackupFolderName(null)
            _snackbarMessage.value = "Drive folder unlinked"
        }
    }

    fun backupToDriveNow(context: android.content.Context) {
        viewModelScope.launch {
            val tree = prefsRepo.preferences.first().driveBackupTreeUri?.let(Uri::parse)
            if (tree == null) {
                _snackbarMessage.value = "Choose a Drive folder first"
                return@launch
            }
            val bytes = backupManager.exportToBytes().getOrElse { err ->
                _snackbarMessage.value = "Backup failed: ${err.localizedMessage}"
                return@launch
            }
            withContext(Dispatchers.IO) { DriveFolderBackup.write(context, tree, bytes) }
                .onSuccess {
                    prefsRepo.setLastDriveBackupAt(System.currentTimeMillis())
                    _snackbarMessage.value = "Backed up to Drive"
                }
                .onFailure { err ->
                    _snackbarMessage.value =
                        "Drive backup failed: ${err.localizedMessage ?: "re-link the folder"}"
                }
        }
    }

    fun restoreFromDrive(context: android.content.Context) {
        viewModelScope.launch {
            val tree = prefsRepo.preferences.first().driveBackupTreeUri?.let(Uri::parse)
            if (tree == null) {
                _snackbarMessage.value = "Choose a Drive folder first"
                return@launch
            }
            val bytes = withContext(Dispatchers.IO) { DriveFolderBackup.read(context, tree) }
                .getOrElse { err ->
                    _snackbarMessage.value =
                        "Drive restore failed: ${err.localizedMessage ?: "no backup found"}"
                    return@launch
                }
            backupManager.importFromBytes(bytes)
                .onSuccess { count -> _snackbarMessage.value = "Restored $count messages from Drive" }
                .onFailure { err ->
                    _snackbarMessage.value = "Drive restore failed: ${err.localizedMessage}"
                }
        }
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun computeFinanceDashboard(list: List<TransactionEntity>): FinanceDashboardState {
        if (list.isEmpty()) return FinanceDashboardState()

        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val monthly = list.filter { it.timestamp >= monthStart }
        val monthlyDebit = monthly.filter { it.type.isOutflow }.sumOf { it.amount }
        val monthlyCredit = monthly.filter { !it.type.isOutflow }.sumOf { it.amount }
        val primaryCurrency = list.firstOrNull()?.currency ?: "INR"

        val accounts = list
            .groupBy { tx ->
                val inst = tx.institution ?: tx.address
                val acct = tx.accountLast4 ?: "General"
                "$inst|$acct"
            }
            .map { (key, txs) ->
                val sorted = txs.sortedByDescending { it.timestamp }
                val latest = sorted.first()
                AccountCardSummary(
                    key = key,
                    institution = latest.institution ?: latest.address,
                    accountLast4 = latest.accountLast4,
                    latestBalance = sorted.firstOrNull { it.balanceAfter != null }?.balanceAfter,
                    currency = latest.currency,
                    transactionCount = txs.size,
                    lastTimestamp = latest.timestamp
                )
            }
            .sortedByDescending { it.lastTimestamp }

        val spendMap = monthly
            .filter { it.type.isOutflow }
            .groupBy { it.spendCategory }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        return FinanceDashboardState(
            monthlyDebitTotal = monthlyDebit,
            monthlyCreditTotal = monthlyCredit,
            primaryCurrency = primaryCurrency,
            accounts = accounts,
            spendByCategory = spendMap,
            transactions = list
        )
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(container) as T
        }
    }
}

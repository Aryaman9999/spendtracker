package com.aryaman.spendtracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aryaman.spendtracker.SpendTrackerApp
import com.aryaman.spendtracker.widget.SpendWidgetProvider
import com.aryaman.spendtracker.ui.screens.*
import com.aryaman.spendtracker.ui.theme.SpendTrackerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

enum class Screen {
    DASHBOARD,
    TRANSACTIONS,
    SPLITS,
    RULES,
    COLOROS_GUIDE,
    ADD_TEMPLATE
}

enum class PeriodType {
    DAY,
    WEEK,
    MONTH
}

data class PeriodWindow(
    val startTime: Long,
    val endTime: Long,
    val label: String,
    val isCurrent: Boolean
)

class MainActivity : ComponentActivity() {

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions response
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        val app = applicationContext as SpendTrackerApp
        val repository = app.repository

        val hasReviewIntent = intent.getLongExtra("EXTRA_TRANSACTION_ID", -1L) != -1L

        setContent {
            SpendTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(if (hasReviewIntent) Screen.TRANSACTIONS else Screen.DASHBOARD) }
                    var filterPendingInTransactions by remember { mutableStateOf(hasReviewIntent) }
                    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

                    // Reactive Flows from Room DB
                    val transactions by repository.allTransactions.collectAsState(initial = emptyList())
                    val pendingTransactions by repository.pendingTransactions.collectAsState(initial = emptyList())
                    val rules by repository.allRules.collectAsState(initial = emptyList())
                    val categories by repository.allCategories.collectAsState(initial = emptyList())
                    val roommateBalances by repository.roommateBalances.collectAsState(initial = emptyList())
                    val allSplits by repository.allSplits.collectAsState(initial = emptyList())
                    val templates by repository.allTemplates.collectAsState(initial = emptyList())

                    // Time Period State (Week / Month view + Prev/Next navigation)
                    var selectedPeriodType by remember { mutableStateOf(PeriodType.MONTH) }
                    var periodOffset by remember { mutableIntStateOf(0) }

                    val periodWindow = remember(selectedPeriodType, periodOffset) {
                        val cal = Calendar.getInstance()
                        when (selectedPeriodType) {
                            PeriodType.DAY -> {
                                cal.add(Calendar.DAY_OF_YEAR, periodOffset)
                                cal.set(Calendar.HOUR_OF_DAY, 0)
                                cal.set(Calendar.MINUTE, 0)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                val start = cal.timeInMillis
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                val end = cal.timeInMillis - 1
                                cal.timeInMillis = start
                                val isCurrent = (periodOffset == 0)
                                val label = if (isCurrent) "Today" else java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(cal.time)
                                PeriodWindow(start, end, label, isCurrent)
                            }
                            PeriodType.WEEK -> {
                                cal.set(Calendar.HOUR_OF_DAY, 0)
                                cal.set(Calendar.MINUTE, 0)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)

                                val currentDow = cal.get(Calendar.DAY_OF_WEEK)
                                val daysFromMonday = (currentDow - Calendar.MONDAY + 7) % 7
                                cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday + (periodOffset * 7))

                                val start = cal.timeInMillis
                                cal.add(Calendar.DAY_OF_YEAR, 7)
                                val end = cal.timeInMillis - 1

                                val startLabel = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(start))
                                val endLabel = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(end))
                                val label = "$startLabel - $endLabel"
                                val isCurrent = (periodOffset == 0)
                                PeriodWindow(start, end, label, isCurrent)
                            }
                            PeriodType.MONTH -> {
                                cal.add(Calendar.MONTH, periodOffset)
                                cal.set(Calendar.DAY_OF_MONTH, 1)
                                cal.set(Calendar.HOUR_OF_DAY, 0)
                                cal.set(Calendar.MINUTE, 0)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                val start = cal.timeInMillis
                                cal.add(Calendar.MONTH, 1)
                                val end = cal.timeInMillis - 1

                                cal.timeInMillis = start
                                val label = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(cal.time)
                                val isCurrent = (periodOffset == 0)
                                PeriodWindow(start, end, label, isCurrent)
                            }
                        }
                    }

                    val categorySpends by remember(periodWindow.startTime, periodWindow.endTime) {
                        repository.getSpendByCategory(periodWindow.startTime, periodWindow.endTime)
                    }.collectAsState(initial = emptyList())

                    val periodTransactions = remember(transactions, periodWindow.startTime, periodWindow.endTime) {
                        transactions.filter { it.timestampMillis in periodWindow.startTime..periodWindow.endTime }
                    }

                    val scope = rememberCoroutineScope()

                    Scaffold(
                        bottomBar = {
                            if (currentScreen != Screen.COLOROS_GUIDE && currentScreen != Screen.ADD_TEMPLATE) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp
                                ) {
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                if (currentScreen == Screen.DASHBOARD) Icons.Filled.Home else Icons.Outlined.Home,
                                                contentDescription = "Dashboard"
                                            )
                                        },
                                        label = { Text("Home") },
                                        selected = currentScreen == Screen.DASHBOARD,
                                        onClick = { currentScreen = Screen.DASHBOARD }
                                    )
                                    NavigationBarItem(
                                        icon = {
                                            val icon = if (currentScreen == Screen.TRANSACTIONS) Icons.Filled.CreditCard else Icons.Outlined.CreditCard
                                            if (pendingTransactions.isNotEmpty()) {
                                                BadgedBox(
                                                    badge = {
                                                        Badge(
                                                            containerColor = MaterialTheme.colorScheme.error,
                                                            contentColor = MaterialTheme.colorScheme.onError
                                                        ) {
                                                            Text(pendingTransactions.size.toString())
                                                        }
                                                    }
                                                ) {
                                                    Icon(icon, contentDescription = "Transactions")
                                                }
                                            } else {
                                                Icon(icon, contentDescription = "Transactions")
                                            }
                                        },
                                        label = { Text("Spends") },
                                        selected = currentScreen == Screen.TRANSACTIONS,
                                        onClick = {
                                            selectedCategoryFilter = null
                                            filterPendingInTransactions = false
                                            currentScreen = Screen.TRANSACTIONS
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                if (currentScreen == Screen.SPLITS) Icons.Filled.Group else Icons.Outlined.Group,
                                                contentDescription = "Splits"
                                            )
                                        },
                                        label = { Text("Splits") },
                                        selected = currentScreen == Screen.SPLITS,
                                        onClick = { currentScreen = Screen.SPLITS }
                                    )
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                if (currentScreen == Screen.RULES) Icons.Filled.Settings else Icons.Outlined.Settings,
                                                contentDescription = "Rules"
                                            )
                                        },
                                        label = { Text("Rules") },
                                        selected = currentScreen == Screen.RULES,
                                        onClick = { currentScreen = Screen.RULES }
                                    )
                                }
                            }
                        }
                    ) { padding ->
                        Surface(modifier = Modifier.padding(padding)) {
                            when (currentScreen) {
                                Screen.DASHBOARD -> DashboardScreen(
                                    transactions = periodTransactions,
                                    categorySpends = categorySpends,
                                    roommateBalances = roommateBalances,
                                    categories = categories,
                                    pendingCount = pendingTransactions.size,
                                    periodType = selectedPeriodType,
                                    periodLabel = periodWindow.label,
                                    isCurrentPeriod = periodWindow.isCurrent,
                                    canGoForward = periodOffset < 0,
                                    onPeriodTypeChange = {
                                        selectedPeriodType = it
                                        periodOffset = 0
                                    },
                                    onPreviousPeriod = { periodOffset -= 1 },
                                    onNextPeriod = { if (periodOffset < 0) periodOffset += 1 },
                                    onResetToCurrentPeriod = { periodOffset = 0 },
                                    onNavigateToTransactions = { filterPending, catFilter ->
                                        filterPendingInTransactions = filterPending
                                        selectedCategoryFilter = catFilter
                                        currentScreen = Screen.TRANSACTIONS
                                    },
                                    onNavigateToSplits = { currentScreen = Screen.SPLITS },
                                    onUpdateCategoryBudget = { cat, budget ->
                                        scope.launch(Dispatchers.IO) {
                                            repository.updateCategoryBudget(cat, budget)
                                            SpendWidgetProvider.refreshAllWidgets(this@MainActivity)
                                        }
                                    }
                                )

                                Screen.TRANSACTIONS -> TransactionsScreen(
                                    transactions = transactions,
                                    categories = categories,
                                    splits = allSplits,
                                    initialFilterPending = filterPendingInTransactions,
                                    selectedCategoryFilter = selectedCategoryFilter,
                                    onClearCategoryFilter = { selectedCategoryFilter = null },
                                    onCategorize = { id, cat, createRule ->
                                        scope.launch(Dispatchers.IO) {
                                            val txn = transactions.find { it.id == id }
                                            repository.setCategory(
                                                transactionId = id,
                                                category = cat,
                                                createRuleKeyword = if (createRule) txn?.cleanMerchant else null
                                            )
                                            SpendWidgetProvider.refreshAllWidgets(this@MainActivity)
                                        }
                                    },
                                    onSplit = { id, myActualShare, splitsList ->
                                        scope.launch(Dispatchers.IO) {
                                            repository.splitTransaction(id, myActualShare, splitsList)
                                            SpendWidgetProvider.refreshAllWidgets(this@MainActivity)
                                        }
                                    },
                                    onToggleSplit = { id ->
                                        scope.launch(Dispatchers.IO) {
                                            repository.toggleQuickSplit(id)
                                            SpendWidgetProvider.refreshAllWidgets(this@MainActivity)
                                        }
                                    },
                                    onToggleCreditCard = { id ->
                                        scope.launch(Dispatchers.IO) {
                                            repository.toggleCreditCard(id)
                                        }
                                    },
                                    onToggleBudgetExempt = { id ->
                                        scope.launch(Dispatchers.IO) {
                                            repository.toggleBudgetExempt(id)
                                            SpendWidgetProvider.refreshAllWidgets(this@MainActivity)
                                        }
                                    },
                                    onUpdateComment = { id, comment ->
                                        scope.launch(Dispatchers.IO) {
                                            repository.updateTransactionComment(id, comment)
                                        }
                                    },
                                    onDelete = { id ->
                                        scope.launch(Dispatchers.IO) {
                                            repository.deleteTransaction(id)
                                            SpendWidgetProvider.refreshAllWidgets(this@MainActivity)
                                        }
                                    }
                                )

                                Screen.SPLITS -> SplitLedgerScreen(
                                    balances = roommateBalances,
                                    onSettleAll = { roommateName ->
                                        scope.launch(Dispatchers.IO) {
                                            repository.settleAllForRoommate(roommateName)
                                        }
                                    }
                                )

                                Screen.RULES -> RuleManagerScreen(
                                    rules = rules,
                                    categories = categories,
                                    templates = templates,
                                    onAddRule = { rule ->
                                        scope.launch(Dispatchers.IO) {
                                            repository.addRule(rule)
                                        }
                                    },
                                    onDeleteRule = { rule ->
                                        scope.launch(Dispatchers.IO) {
                                            repository.deleteRule(rule)
                                        }
                                    },
                                    onNavigateToAddTemplate = {
                                        currentScreen = Screen.ADD_TEMPLATE
                                    },
                                    onDeleteTemplate = { template ->
                                        scope.launch(Dispatchers.IO) {
                                            repository.deleteTemplate(template)
                                        }
                                    },
                                    onToggleTemplateActive = { id, isActive ->
                                        scope.launch(Dispatchers.IO) {
                                            repository.setTemplateActive(id, isActive)
                                        }
                                    },
                                    onUpdateCategoryBudget = { cat, budget ->
                                        scope.launch(Dispatchers.IO) {
                                            repository.updateCategoryBudget(cat, budget)
                                            SpendWidgetProvider.refreshAllWidgets(this@MainActivity)
                                        }
                                    },
                                    onNavigateToColorOsGuide = {
                                        currentScreen = Screen.COLOROS_GUIDE
                                    }
                                )

                                Screen.COLOROS_GUIDE -> {
                                    BackHandler { currentScreen = Screen.RULES }
                                    ColorOsGuideScreen(
                                        onBack = { currentScreen = Screen.RULES }
                                    )
                                }

                                Screen.ADD_TEMPLATE -> {
                                    BackHandler { currentScreen = Screen.RULES }
                                    CustomTemplateBuilderScreen(
                                        onBack = { currentScreen = Screen.RULES },
                                        onSaveTemplate = { template ->
                                            scope.launch(Dispatchers.IO) {
                                                repository.addTemplate(template)
                                                currentScreen = Screen.RULES
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    override fun onStop() {
        super.onStop()
        SpendWidgetProvider.refreshAllWidgets(this)
    }
}

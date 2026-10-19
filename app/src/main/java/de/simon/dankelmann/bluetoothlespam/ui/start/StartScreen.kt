package de.simon.dankelmann.bluetoothlespam.ui.start

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers.Companion.isBluetooth5Supported
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.FloatingNavBarClearance
import de.simon.dankelmann.bluetoothlespam.ui.theme.LocalExtendedColors
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTopAppBarClearance
import de.simon.dankelmann.bluetoothlespam.ui.theme.TerminalButton
import de.simon.dankelmann.bluetoothlespam.ui.theme.TerminalKeyValue
import de.simon.dankelmann.bluetoothlespam.ui.theme.TerminalPanel
import de.simon.dankelmann.bluetoothlespam.ui.theme.crtScanlines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Route entry point (plan §6 step 4) — owns the business logic `StartFragment` used to own. */
@Composable
fun StartRoute(
    onNavigateToAdvertisement: () -> Unit,
    onNavigateToManageQuickStart: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val viewModel: StartViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var quickStartCollections by remember { mutableStateOf<List<AdvertisementSetCollectionEntity>>(emptyList()) }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            checkBluetoothAdapter(viewModel, activity, null, promptIfAdapterIsDisabled = false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.appVersion.value = getAppVersion(context)
        viewModel.bluetoothSupport.value = getBluetoothSupportText(context)
        // checkDatabase does synchronous Room queries — LaunchedEffect runs on the Compose/main
        // dispatcher by default, so this must hop to IO explicitly (Room asserts against
        // main-thread access). The original Fragment did this via CoroutineScope(Dispatchers.IO).
        withContext(Dispatchers.IO) {
            checkDatabase(viewModel)
            // Loaded once here -- Quick Start is now a manually pinned list (Manage Quick Start
            // screen), not recency-based, so nothing on this screen changes membership and it
            // never needs to reload on ON_RESUME (e.g. every tab switch back to this screen).
            quickStartCollections = AppDatabase.getInstance().advertisementSetCollectionDao().getQuickStartCollections()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkRequiredPermissions(context, viewModel)
                checkBluetoothAdapter(viewModel, activity, enableBluetoothLauncher, promptIfAdapterIsDisabled = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    StartScreen(
        viewModel = viewModel,
        onGrantPermissions = { requestRequiredPermissions(activity) },
        onRecheckPermissions = { checkRequiredPermissions(context, viewModel) },
        onRecheckBluetooth = { checkBluetoothAdapter(viewModel, activity, enableBluetoothLauncher, promptIfAdapterIsDisabled = true) },
        onRecheckDatabase = { scope.launch(Dispatchers.IO) { checkDatabase(viewModel) } },
        quickStartCollections = quickStartCollections,
        onQuickStartItemClicked = { collection ->
            scope.launch(Dispatchers.IO) {
                val database = AppDatabase.getInstance()
                val collectionWithLists = database.advertisementSetCollectionDao().getCollectionWithLists(collection.id)
                    ?: return@launch
                database.advertisementSetCollectionDao().updateLastUsedAt(collection.id, System.currentTimeMillis())

                val listEntities = collectionWithLists.lists
                val domainCollection = DatabaseHelpers.buildAdvertisementSetCollectionSkeletonFromEntity(collectionWithLists)
                    .apply { isLoadingSets = true }

                withContext(Dispatchers.Main) {
                    val app = context.applicationContext as BleSpamApplication
                    app.queueHandler.apply {
                        deactivate(context)
                        setAdvertisementSetCollection(domainCollection)
                    }
                    onNavigateToAdvertisement()

                    // Must outlive this composable (which unmounts the instant we navigate away),
                    // so a plain background Thread rather than this coroutine's own scope.
                    Thread {
                        val setsByList = listEntities.map { DatabaseHelpers.getAllAdvertisementSetsForList(it.id) }
                        Handler(Looper.getMainLooper()).post {
                            setsByList.forEachIndexed { index, sets ->
                                domainCollection.advertisementSetLists[index].advertisementSets = sets.toMutableList()
                            }
                            domainCollection.isLoadingSets = false
                            app.queueHandler.setAdvertisementSetCollection(domainCollection)
                        }
                    }.start()
                }
            }
        },
        onEditQuickStartClicked = onNavigateToManageQuickStart,
    )
}

private fun getAppVersion(context: Context): String? {
    return context.packageManager.getPackageInfo(context.packageName, 0).versionName
}

private fun getBluetoothSupportText(context: Context): String {
    return if (context.isBluetooth5Supported()) "Modern & Legacy" else "Legacy only"
}

// .postValue() (not .value =) — called from both the main thread (permission/bluetooth checks)
// and a background thread (checkDatabase, which must run off-main for its Room queries).
// LiveData.setValue() asserts main-thread; postValue() is safe from anywhere.
private fun addMissingRequirement(viewModel: StartViewModel, missingRequirement: String) {
    val newList = viewModel.missingRequirements.value!!
    if (!newList.contains(missingRequirement)) {
        newList.add(missingRequirement)
    }
    viewModel.missingRequirements.postValue(newList)
}

private fun removeMissingRequirement(viewModel: StartViewModel, missingRequirement: String) {
    val newList = viewModel.missingRequirements.value!!
    newList.remove(missingRequirement)
    viewModel.missingRequirements.postValue(newList)
}

private fun checkDatabase(viewModel: StartViewModel) {
    var result = false
    val database = AppDatabase.getInstance()
    removeMissingRequirement(viewModel, "Database is not initialized")
    if (!database.isSeeding && !database.inTransaction()) {
        removeMissingRequirement(viewModel, "Database is Seeding")
        viewModel.isSeeding.postValue(false)
        val numberOfAdvertisementSetEntities = database.advertisementSetDao().getAll().count()
        if (numberOfAdvertisementSetEntities > 0) {
            removeMissingRequirement(viewModel, "Database is empty")
            result = true
        } else {
            addMissingRequirement(viewModel, "Database is empty")
        }
    } else {
        addMissingRequirement(viewModel, "Database is Seeding")
        viewModel.isSeeding.postValue(true)
    }
    viewModel.databaseIsReady.postValue(result)

    if (!result) {
        // Check again in a few seconds
        Executors.newSingleThreadScheduledExecutor().schedule({
            checkDatabase(viewModel)
        }, 500, TimeUnit.MILLISECONDS)
    }
}

private fun checkBluetoothAdapter(
    viewModel: StartViewModel,
    activity: Activity,
    enableBluetoothLauncher: androidx.activity.result.ActivityResultLauncher<Intent>?,
    promptIfAdapterIsDisabled: Boolean,
) {
    viewModel.bluetoothAdapterIsReady.value = false

    val bluetoothAdapter: BluetoothAdapter? = activity.bluetoothAdapter()
    if (bluetoothAdapter != null) {
        removeMissingRequirement(viewModel, "Bluetooth Adapter not found")
        if (bluetoothAdapter.isEnabled) {
            removeMissingRequirement(viewModel, "Bluetooth is disabled")
            viewModel.bluetoothAdapterIsReady.value = true
        } else {
            addMissingRequirement(viewModel, "Bluetooth is disabled")
            if (promptIfAdapterIsDisabled && enableBluetoothLauncher != null &&
                PermissionCheck.checkPermission(android.Manifest.permission.BLUETOOTH_CONNECT, activity)
            ) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
    } else {
        addMissingRequirement(viewModel, "Bluetooth Adapter not found")
    }
}

private fun checkRequiredPermissions(context: Context, viewModel: StartViewModel) {
    var allGranted = true

    PermissionCheck.getAllRelevantPermissions().forEach { permission ->
        val missingRequirementString = "Permission " + permission.replace("android.permission.", "") + " not granted"
        if (PermissionCheck.checkPermission(permission, context)) {
            removeMissingRequirement(viewModel, missingRequirementString)
        } else {
            allGranted = false
            addMissingRequirement(viewModel, missingRequirementString)
        }
    }
    viewModel.allPermissionsGranted.value = allGranted

    if (!allGranted) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.missing_permissions_title)
            .setMessage(R.string.missing_permissions_text)
            .setPositiveButton(R.string.missing_permissions_grant) { _, _ -> requestRequiredPermissions(context as Activity) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}

private fun requestRequiredPermissions(activity: Activity) {
    PermissionCheck.requestMissingPermissions(PermissionCheck.getAllRelevantPermissions(), activity)
}

// ─────────────────────────────────────────────────────────────────────────────────────────────
// UI — amber-CRT terminal redesign. All business logic stays in StartRoute / the helpers above;
// these composables only present the same state.
// ─────────────────────────────────────────────────────────────────────────────────────────────

@Composable
fun StartScreen(
    viewModel: StartViewModel,
    onGrantPermissions: () -> Unit,
    onRecheckPermissions: () -> Unit,
    onRecheckBluetooth: () -> Unit,
    onRecheckDatabase: () -> Unit,
    quickStartCollections: List<AdvertisementSetCollectionEntity>,
    onQuickStartItemClicked: (AdvertisementSetCollectionEntity) -> Unit,
    onEditQuickStartClicked: () -> Unit,
) {
    val isSeeding by viewModel.isSeeding.observeAsState(false)
    val appVersion by viewModel.appVersion.observeAsState("0.0.0")
    val androidVersion by viewModel.androidVersion.observeAsState("-")
    val sdkVersion by viewModel.sdkVersion.observeAsState("-")
    val bluetoothSupport by viewModel.bluetoothSupport.observeAsState("-")
    val allPermissionsGranted by viewModel.allPermissionsGranted.observeAsState()
    val bluetoothAdapterIsReady by viewModel.bluetoothAdapterIsReady.observeAsState()
    val databaseIsReady by viewModel.databaseIsReady.observeAsState()
    val missingRequirements by viewModel.missingRequirements.observeAsState(mutableListOf())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Clearances keep the first/last panel clear of the floating title bar and nav pill.
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp + SpecterTopAppBarClearance,
            bottom = 16.dp + FloatingNavBarClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BannerPanel(appVersion = appVersion ?: "-") }

        if (allPermissionsGranted == false) {
            item { PermissionRationalePanel(onGrantPermissions = onGrantPermissions) }
        }

        item { AboutPanel() }

        item {
            SystemInfoPanel(
                appVersion = appVersion,
                androidVersion = androidVersion ?: "-",
                sdkVersion = sdkVersion ?: "-",
                bluetoothSupport = bluetoothSupport ?: "-",
                databaseIsReady = databaseIsReady == true,
                isSeeding = isSeeding == true,
            )
        }

        item {
            QuickStartPanel(
                quickStartCollections = quickStartCollections,
                onItemClicked = onQuickStartItemClicked,
                onEditClicked = onEditQuickStartClicked,
            )
        }

        item {
            RequirementsPanel(
                missingRequirements = missingRequirements ?: emptyList(),
                permissionsStatus = requirementStatus(allPermissionsGranted),
                bluetoothStatus = requirementStatus(bluetoothAdapterIsReady),
                databaseStatus = if (isSeeding == true) RequirementStatus.Loading else requirementStatus(databaseIsReady),
                onRecheckPermissions = onRecheckPermissions,
                onRecheckBluetooth = onRecheckBluetooth,
                onRecheckDatabase = onRecheckDatabase,
            )
        }
    }
}

@Composable
private fun BannerPanel(appVersion: String) {
    val cs = MaterialTheme.colorScheme
    TerminalPanel(modifier = Modifier.crtScanlines()) {
        Text(
            text = "BLUETOOTH::LE::SPAM",
            style = MaterialTheme.typography.titleLarge,
            color = cs.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "root@ble-spam:~# ./specter --version $appVersion",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
        Text(
            text = "BLE advertisement toolkit · proof-of-concept",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutPanel() {
    val cs = MaterialTheme.colorScheme
    TerminalPanel(title = "readme") {
        Text(
            text = "Broadcasts Bluetooth Low Energy advertisements that nearby devices may react " +
                "to with pairing pop-ups or setup prompts. Built to study the BLE protocol and to " +
                "test the built-in Spam Detector.",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "[!] Only target devices you own or are authorised to test. Broadcasting at " +
                "strangers' devices may be illegal where you live.",
            style = MaterialTheme.typography.bodySmall,
            color = cs.error,
        )
    }
}

@Composable
private fun PermissionRationalePanel(onGrantPermissions: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    TerminalPanel(title = "permissions required", accent = true) {
        Text(
            text = stringResource(R.string.missing_permissions_text),
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        TerminalButton(text = "> GRANT ACCESS", onClick = onGrantPermissions)
    }
}

@Composable
private fun SystemInfoPanel(
    appVersion: String?,
    androidVersion: String,
    sdkVersion: String,
    bluetoothSupport: String,
    databaseIsReady: Boolean,
    isSeeding: Boolean,
) {
    TerminalPanel(title = "system") {
        TerminalKeyValue("android", androidVersion)
        TerminalKeyValue("sdk", sdkVersion)
        TerminalKeyValue("bluetooth", bluetoothSupport)
        TerminalKeyValue("app", appVersion ?: "-")
        TerminalKeyValue(
            key = "database",
            value = if (isSeeding) "seeding…" else if (databaseIsReady) "ready" else "not ready",
        )
    }
}

@Composable
private fun QuickStartPanel(
    quickStartCollections: List<AdvertisementSetCollectionEntity>,
    onItemClicked: (AdvertisementSetCollectionEntity) -> Unit,
    onEditClicked: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    TerminalPanel(title = "quick_start") {
        if (quickStartCollections.isEmpty()) {
            Text(
                text = "— no pinned groups. tap edit to add some —",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
        } else {
            quickStartCollections.forEach { collection ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClicked(collection) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "> ", style = MaterialTheme.typography.bodyLarge, color = cs.primary)
                    Text(text = collection.title, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TerminalButton(text = "> EDIT", onClick = onEditClicked)
    }
}

@Composable
private fun RequirementsPanel(
    missingRequirements: List<String>,
    permissionsStatus: RequirementStatus,
    bluetoothStatus: RequirementStatus,
    databaseStatus: RequirementStatus,
    onRecheckPermissions: () -> Unit,
    onRecheckBluetooth: () -> Unit,
    onRecheckDatabase: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    TerminalPanel(title = "preflight") {
        StatusRow(label = "permissions", status = permissionsStatus, onClick = onRecheckPermissions)
        StatusRow(label = "bluetooth_adapter", status = bluetoothStatus, onClick = onRecheckBluetooth)
        StatusRow(label = "database", status = databaseStatus, onClick = onRecheckDatabase)

        if (missingRequirements.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            missingRequirements.forEach { requirement ->
                Text(
                    text = "  ! $requirement",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.error,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private enum class RequirementStatus { Ready, NotReady, Loading }

/** null = the check hasn't reported yet. */
private fun requirementStatus(isReady: Boolean?) = when (isReady) {
    true -> RequirementStatus.Ready
    false -> RequirementStatus.NotReady
    null -> RequirementStatus.Loading
}

@Composable
private fun StatusRow(
    label: String,
    status: RequirementStatus,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val extended = LocalExtendedColors.current
    val (tag, tagColor) = when (status) {
        RequirementStatus.Ready -> "[ OK ]" to extended.success
        RequirementStatus.NotReady -> "[FAIL]" to cs.error
        RequirementStatus.Loading -> "[....]" to cs.onSurfaceVariant
    }
    val stateText = when (status) {
        RequirementStatus.Ready -> "OK"
        RequirementStatus.NotReady -> "needs attention"
        RequirementStatus.Loading -> "checking"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$label: $stateText" }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
        Text(text = tag, style = MaterialTheme.typography.labelLarge, color = tagColor)
    }
}

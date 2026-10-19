package de.simon.dankelmann.bluetoothlespam.ui.advertisement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.rememberLottieComposition
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementState
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.getDrawableId
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.LocalExtendedColors
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterShapes
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTopAppBarClearance
import de.simon.dankelmann.bluetoothlespam.ui.theme.StatusBadge
import de.simon.dankelmann.bluetoothlespam.ui.theme.StatusTone
import de.simon.dankelmann.bluetoothlespam.ui.theme.TerminalKeyValue
import de.simon.dankelmann.bluetoothlespam.ui.theme.TerminalPanel
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Core migration of `AdvertisementFragment`'s content (plan §5/§5a) — flattened `LazyColumn`
 * replacing `ExpandableListView` (group headers toggle expanded state, `stateDescription`
 * semantics preserve the TalkBack expand/collapse announcement the old widget gave for free).
 *
 * [revision] has no semantic meaning — [advertisementSetLists] contains plain mutable model
 * objects (`AdvertisementSet.isChecked`/`.currentlyAdvertising`/`.advertisementState`) that the
 * Fragment mutates in place from service callbacks, mirroring the previous
 * `notifyDataSetChanged()` pattern. Bumping [revision] is the only way Compose learns to re-read
 * them.
 */
@Composable
fun AdvertisementScreen(
    isAdvertising: Boolean,
    target: AdvertisementTarget,
    collectionTitle: String,
    collectionSubtitle: String,
    collectionHint: String,
    currentSetTitle: String,
    currentSetSubtitle: String,
    queueMode: AdvertisementQueueMode,
    isLoadingSets: Boolean,
    advertisementSetLists: List<AdvertisementSetList>,
    revision: Int,
    onPlayClicked: () -> Unit,
    onQueueModeSelected: (AdvertisementQueueMode) -> Unit,
    onSetRowClicked: (groupIndex: Int, childIndex: Int, AdvertisementSet) -> Unit,
    onSetCheckedChanged: (AdvertisementSet, Boolean) -> Unit,
    onGroupCheckedChanged: (AdvertisementSetList, Boolean) -> Unit,
    allowCustomSwiftPairNames: Boolean,
    onRenameSwiftPairDevice: (AdvertisementSet, String) -> Unit,
) {
    val expandedGroups = remember(advertisementSetLists) {
        mutableStateMapOf<Int, Boolean>().apply {
            if (advertisementSetLists.size == 1) put(0, true)
        }
    }
    var editingSwiftPairSet by remember { mutableStateOf<AdvertisementSet?>(null) }

    // Surface (not a plain Column) so every Text/Icon below that doesn't set an explicit color
    // gets a real LocalContentColor instead of falling back to black -- MainActivity's root is a
    // bare Box/Column with no Surface of its own, and this screen never established one either,
    // so several rows read as black-on-black in dark mode (same root cause as the Group Editor
    // checkbox-label bug from earlier in this session).
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    // Top padding is SpecterTopAppBarClearance: the title bar floats/blurs on top of content
    // rather than reserving space (see MainActivity), so without this HeaderSection renders
    // underneath it.
    Column(modifier = Modifier.fillMaxSize().padding(top = SpecterTopAppBarClearance)) {
        HeaderSection(
            isAdvertising = isAdvertising,
            target = target,
            collectionTitle = collectionTitle,
            collectionSubtitle = collectionSubtitle,
            collectionHint = collectionHint,
            currentSetTitle = currentSetTitle,
            currentSetSubtitle = currentSetSubtitle,
            queueMode = queueMode,
            armedCount = advertisementSetLists.sumOf { list -> list.advertisementSets.count { it.isChecked } },
            totalCount = advertisementSetLists.sumOf { it.advertisementSets.size },
            onPlayClicked = onPlayClicked,
            onQueueModeSelected = onQueueModeSelected,
        )

        if (isLoadingSets) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                advertisementSetLists.forEachIndexed { groupIndex, list ->
                    // Keys include `revision`: `set`/`list` are plain mutated objects (not Compose
                    // State), and LazyColumn gives each item its own recomposition scope that a
                    // `revision++` bump alone doesn't invalidate -- only a key change forces
                    // Compose to dispose+recreate the row and re-read the mutated field. Without
                    // this, toggling a checkbox silently updates the model but the checkbox glyph
                    // stays stale until the group is collapsed/expanded.
                    item(key = "group-$groupIndex-$revision") {
                        GroupHeaderRow(
                            list = list,
                            expanded = expandedGroups[groupIndex] == true,
                            onToggleExpanded = {
                                expandedGroups[groupIndex] = expandedGroups[groupIndex] != true
                            },
                            onGroupCheckedChanged = { checked -> onGroupCheckedChanged(list, checked) },
                        )
                    }

                    if (expandedGroups[groupIndex] == true) {
                        items(list.advertisementSets, key = { "set-$groupIndex-${it.id}-$revision" }) { set ->
                            val childIndex = list.advertisementSets.indexOf(set)
                            SetRow(
                                set = set,
                                onClick = { onSetRowClicked(groupIndex, childIndex, set) },
                                onCheckedChanged = { checked -> onSetCheckedChanged(set, checked) },
                                showEditButton = allowCustomSwiftPairNames &&
                                    set.type == AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING,
                                onEditClicked = { editingSwiftPairSet = set },
                            )
                        }
                    }
                }
            }
        }
    }
    }

    editingSwiftPairSet?.let { set ->
        RenameSwiftPairDeviceDialog(
            set = set,
            onConfirm = { newName ->
                onRenameSwiftPairDevice(set, newName)
                editingSwiftPairSet = null
            },
            onDismiss = { editingSwiftPairSet = null },
        )
    }
}

@Composable
private fun HeaderSection(
    isAdvertising: Boolean,
    target: AdvertisementTarget,
    collectionTitle: String,
    collectionSubtitle: String,
    collectionHint: String,
    currentSetTitle: String,
    currentSetSubtitle: String,
    queueMode: AdvertisementQueueMode,
    armedCount: Int,
    totalCount: Int,
    onPlayClicked: () -> Unit,
    onQueueModeSelected: (AdvertisementQueueMode) -> Unit,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.advertisingdevices))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isAdvertising,
    )
    val cs = MaterialTheme.colorScheme
    val extended = LocalExtendedColors.current

    Column(modifier = Modifier.padding(16.dp)) {
        TerminalPanel(title = "transmitter", accent = isAdvertising) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
            )

            // Live status line — unmistakable idle/broadcasting readout.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "status", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Text(
                    text = if (isAdvertising) "● BROADCASTING" else "○ IDLE",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isAdvertising) extended.success else cs.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(text = collectionTitle, style = MaterialTheme.typography.titleMedium, color = cs.primary)
            if (collectionSubtitle != "-") {
                Text(text = collectionSubtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            if (collectionHint != "-") {
                Text(text = collectionHint, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))
            // Current set, with the target icon so it's clear what's being broadcast.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(target.getDrawableId()),
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "current",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (currentSetTitle == "-") "—" else currentSetTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface,
                )
            }
            if (currentSetSubtitle != "-") {
                TerminalKeyValue(key = "detail", value = currentSetSubtitle)
            }
            TerminalKeyValue(
                key = "armed",
                value = "$armedCount / $totalCount",
                valueColor = if (armedCount > 0) extended.success else cs.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))
            // Queue mode, labeled so each option is self-explanatory.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "mode:", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                ModeChip(
                    label = "LINEAR",
                    icon = Icons.Filled.Repeat,
                    selected = queueMode == AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LINEAR,
                    onClick = { onQueueModeSelected(AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LINEAR) },
                )
                ModeChip(
                    label = "RANDOM",
                    icon = Icons.Filled.Shuffle,
                    selected = queueMode == AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM,
                    onClick = { onQueueModeSelected(AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM) },
                )
            }

            Spacer(Modifier.height(12.dp))
            // Primary action — big and unambiguous.
            BigActionButton(isAdvertising = isAdvertising, onClick = onPlayClicked)
        }
    }
}

/** Labeled, selectable queue-mode chip. */
@Composable
private fun ModeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = SpecterShapes.small,
        color = if (selected) cs.primary else cs.surfaceContainerHigh,
        contentColor = if (selected) cs.onPrimary else cs.onSurfaceVariant,
        modifier = Modifier.border(1.dp, if (selected) cs.primary else cs.outline, SpecterShapes.small),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Full-width start/stop action; red while broadcasting, amber when idle. */
@Composable
private fun BigActionButton(isAdvertising: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val container = if (isAdvertising) cs.error else cs.primary
    val onContainer = if (isAdvertising) cs.onError else cs.onPrimary
    Surface(
        onClick = onClick,
        shape = SpecterShapes.small,
        color = container,
        contentColor = onContainer,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, container, SpecterShapes.small)
            .semantics { contentDescription = if (isAdvertising) "Stop advertising" else "Start advertising" },
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isAdvertising) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isAdvertising) "STOP BROADCAST" else "START BROADCAST",
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/** Small muted placeholder for a not-yet-available value, standing in for a bare "-" (plan feedback). */
@Composable
private fun PlaceholderBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 40.dp, height = 12.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
    )
}

@Composable
private fun GroupHeaderRow(
    list: AdvertisementSetList,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onGroupCheckedChanged: (Boolean) -> Unit,
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevron")
    val checkedCount = list.advertisementSets.count { it.isChecked }
    val checkedState = when {
        checkedCount == 0 -> ToggleableState.Off
        checkedCount == list.advertisementSets.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val titleColor = if (list.currentlyAdvertising) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded)
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val groupGlyph = when (checkedState) {
            ToggleableState.On -> "[x]"
            ToggleableState.Indeterminate -> "[-]"
            ToggleableState.Off -> "[ ]"
        }
        Text(
            text = groupGlyph,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onGroupCheckedChanged(checkedState != ToggleableState.On) }
                .padding(8.dp)
                .semantics { contentDescription = "Select all in group" },
        )
        Text(
            text = list.title,
            style = MaterialTheme.typography.titleSmall,
            color = titleColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = null,
            modifier = Modifier.rotate(rotation),
        )
    }
}

@Composable
private fun SetRow(
    set: AdvertisementSet,
    onClick: () -> Unit,
    onCheckedChanged: (Boolean) -> Unit,
    showEditButton: Boolean,
    onEditClicked: () -> Unit,
) {
    val extendedColors = LocalExtendedColors.current
    val titleColor = when {
        set.currentlyAdvertising && set.advertisementState == AdvertisementState.ADVERTISEMENT_STATE_SUCCEEDED -> extendedColors.success
        set.currentlyAdvertising && set.advertisementState == AdvertisementState.ADVERTISEMENT_STATE_FAILED -> extendedColors.warning
        set.currentlyAdvertising -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 24.dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (set.isChecked) "[x]" else "[ ]",
            style = MaterialTheme.typography.bodyLarge,
            color = if (set.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .toggleable(value = set.isChecked, role = Role.Checkbox, onValueChange = onCheckedChanged)
                .padding(8.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            Text(text = set.title, style = MaterialTheme.typography.bodyMedium, color = titleColor)
            Text(text = advertisementSetSubtitle(set), style = MaterialTheme.typography.bodySmall)
        }
        if (showEditButton) {
            IconButton(onClick = onEditClicked) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit device name")
            }
        }
    }
}

/** Lets the user set the advertised Swift Pair device name (Allow Custom Swift Pair Names setting). */
@Composable
private fun RenameSwiftPairDeviceDialog(
    set: AdvertisementSet,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // BLE legacy advertising packets top out at 31 bytes and Swift Pair's fixed framing already
    // uses some of that budget, so the name is capped well under the raw limit.
    val maxNameLength = 20
    var name by remember(set) { mutableStateOf(set.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Device Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= maxNameLength) name = it },
                label = { Text("Device name") },
                supportingText = { Text("${name.length}/$maxNameLength") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

internal fun advertisementSetSubtitle(advertisementSet: AdvertisementSet): String {
    val type = when (advertisementSet.type) {
        AdvertisementSetType.ADVERTISEMENT_TYPE_UNDEFINED -> "Undefined"
        AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING -> "Swift Pairing"
        AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE -> "Fast Pairing Device"
        AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP -> "Fast Pairing Phone Setup"
        AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION -> "Fast Pairing Non Production"
        AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG -> "Fast Pairing Debug"
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE -> "New Device Popup"
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE -> "Not your Device Popup"
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG -> "New Airtag Popup"
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS -> "iOS Action Modal"
        AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH -> "iOS 17 Crash"
        AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_WATCH -> "Easy Setup Watch"
        AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_BUDS -> "Easy Setup Buds"
        AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_PLAY -> "Lovespouse Play"
        AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_STOP -> "Lovespouse Stop"
    }

    val range = when (advertisementSet.range) {
        AdvertisementSetRange.ADVERTISEMENTSET_RANGE_CLOSE -> "Close"
        AdvertisementSetRange.ADVERTISEMENTSET_RANGE_MEDIUM -> "Medium"
        AdvertisementSetRange.ADVERTISEMENTSET_RANGE_FAR -> "Far"
        AdvertisementSetRange.ADVERTISEMENTSET_RANGE_UNKNOWN -> "Unknown"
    }

    return "Type: $type, Range: $range"
}

package de.simon.dankelmann.bluetoothlespam.ui.spamDetector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import de.simon.dankelmann.bluetoothlespam.Enums.FlipperDeviceType
import de.simon.dankelmann.bluetoothlespam.Enums.SpamPackageType
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.FloatingNavBarClearance
import de.simon.dankelmann.bluetoothlespam.ui.theme.LocalExtendedColors
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterShapes
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTopAppBarClearance
import de.simon.dankelmann.bluetoothlespam.ui.theme.TerminalKeyValue
import de.simon.dankelmann.bluetoothlespam.ui.theme.TerminalPanel

/**
 * Core migration of `SpamDetectorFragment`'s content (plan §5/§5a/§5b) — single LazyColumn with
 * two named item groups (detected devices, spam packages) replacing the RecyclerView split.
 * Restyled to the amber-CRT terminal look.
 */
@Composable
fun SpamDetectorScreen(
    isDetecting: Boolean,
    flipperDevices: List<FlipperDeviceScanResult>,
    spamPackages: List<SpamPackageScanResult>,
    onToggleClicked: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Top/bottom padding include SpecterTopAppBarClearance/FloatingNavBarClearance so the
        // first/last panel scrolls clear of the floating title bar and nav pill.
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp + SpecterTopAppBarClearance,
            bottom = 16.dp + FloatingNavBarClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeaderPanel(isDetecting = isDetecting, onToggleClicked = onToggleClicked)
        }

        item {
            SectionHeader("detected_devices", flipperDevices.size)
        }
        if (flipperDevices.isEmpty()) {
            item { EmptyListMessage(isDetecting = isDetecting) }
        } else {
            items(flipperDevices, key = { it.address }) { device ->
                FlipperDeviceRow(device)
            }
        }

        item {
            SectionHeader("spam_packages", spamPackages.size)
        }
        if (spamPackages.isEmpty()) {
            item { EmptyListMessage(isDetecting = isDetecting) }
        } else {
            items(spamPackages, key = { it.address }) { spamPackage ->
                SpamPackageRow(spamPackage)
            }
        }
    }
}

@Composable
private fun HeaderPanel(isDetecting: Boolean, onToggleClicked: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.detect))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isDetecting,
    )
    val cs = MaterialTheme.colorScheme
    val extended = LocalExtendedColors.current

    TerminalPanel(title = "detector", accent = isDetecting) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "status", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            Text(
                text = if (isDetecting) "● SCANNING" else "○ IDLE",
                style = MaterialTheme.typography.labelLarge,
                color = if (isDetecting) extended.success else cs.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Detects Flipper Zero and other BLE spamming devices nearby — including spam sent " +
                "by this app.",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))
        ScanToggleButton(isDetecting = isDetecting, onClick = onToggleClicked)
    }
}

/** Full-width start/stop scan action. */
@Composable
private fun ScanToggleButton(isDetecting: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = SpecterShapes.small,
        color = if (isDetecting) cs.surfaceContainerHigh else cs.primary,
        contentColor = if (isDetecting) cs.primary else cs.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cs.primary, SpecterShapes.small)
            .semantics { contentDescription = if (isDetecting) "Stop scanning" else "Start scanning" },
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isDetecting) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isDetecting) "STOP SCAN" else "START SCAN",
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    val cs = MaterialTheme.colorScheme
    Text(
        text = "> ${title.uppercase()} [$count]",
        color = cs.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = cs.surfaceContainerHigh, shape = SpecterShapes.medium)
            .border(1.dp, cs.outline, SpecterShapes.medium)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun EmptyListMessage(isDetecting: Boolean) {
    val cs = MaterialTheme.colorScheme
    TerminalPanel {
        Text(
            text = if (isDetecting) "scanning… no hits yet" else "press START SCAN to begin",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun FlipperDeviceRow(device: FlipperDeviceScanResult) {
    val cs = MaterialTheme.colorScheme
    val deviceTypeLabel = when (device.flipperDeviceType) {
        FlipperDeviceType.FLIPPER_ZERO_WHITE -> "Flipper Zero White"
        FlipperDeviceType.FLIPPER_ZERO_BLACK -> "Flipper Zero Black"
        FlipperDeviceType.FLIPPER_ZERO_TRANSPARENT -> "Flipper Zero Transparent"
        FlipperDeviceType.UNKNOWN -> "Unknown Flipper Zero"
    }

    TerminalPanel {
        Text(
            text = device.deviceName.ifEmpty { deviceTypeLabel },
            style = MaterialTheme.typography.titleSmall,
            color = cs.primary,
        )
        TerminalKeyValue(key = "addr", value = device.address)
        TerminalKeyValue(key = "rssi", value = "${device.rssi} dBm")
        TerminalKeyValue(key = "type", value = deviceTypeLabel)
    }
}

@Composable
private fun SpamPackageRow(spamPackage: SpamPackageScanResult) {
    val cs = MaterialTheme.colorScheme
    val label = when (spamPackage.spamPackageType) {
        SpamPackageType.UNKNOWN -> "Unknown Spam"
        SpamPackageType.FAST_PAIRING -> "Fast Pairing"
        SpamPackageType.CONTINUITY_NEW_AIRTAG -> "Continuity Airtag"
        SpamPackageType.CONTINUITY_NEW_DEVICE -> "Continuity new Device"
        SpamPackageType.CONTINUITY_NOT_YOUR_DEVICE -> "Continuity not your Device"
        SpamPackageType.CONTINUITY_ACTION_MODAL -> "Continuity Action Modal"
        SpamPackageType.CONTINUITY_IOS_17_CRASH -> "Continuity iOS 17 Crash"
        SpamPackageType.SWIFT_PAIRING -> "Swift Pairing"
        SpamPackageType.EASY_SETUP_WATCH -> "Easy Setup Watch"
        SpamPackageType.EASY_SETUP_BUDS -> "Easy Setup Buds"
        SpamPackageType.LOVESPOUSE_PLAY -> "Lovespouse Play"
        SpamPackageType.LOVESPOUSE_STOP -> "Lovespouse Stop"
    }

    TerminalPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(spamPackage.spamPackageType.iconRes()),
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = cs.primary)
        }
        TerminalKeyValue(key = "addr", value = spamPackage.address)
        TerminalKeyValue(key = "rssi", value = "${spamPackage.rssi} dBm")
    }
}

private fun SpamPackageType.iconRes(): Int = when (this) {
    SpamPackageType.UNKNOWN -> R.drawable.bluetooth
    SpamPackageType.FAST_PAIRING -> R.drawable.ic_android
    SpamPackageType.CONTINUITY_NEW_AIRTAG -> R.drawable.apple
    SpamPackageType.CONTINUITY_NEW_DEVICE -> R.drawable.apple
    SpamPackageType.CONTINUITY_NOT_YOUR_DEVICE -> R.drawable.apple
    SpamPackageType.CONTINUITY_ACTION_MODAL -> R.drawable.apple
    SpamPackageType.CONTINUITY_IOS_17_CRASH -> R.drawable.apple
    SpamPackageType.SWIFT_PAIRING -> R.drawable.microsoft
    SpamPackageType.EASY_SETUP_WATCH -> R.drawable.samsung
    SpamPackageType.EASY_SETUP_BUDS -> R.drawable.samsung
    SpamPackageType.LOVESPOUSE_PLAY -> R.drawable.heart
    SpamPackageType.LOVESPOUSE_STOP -> R.drawable.heart
}

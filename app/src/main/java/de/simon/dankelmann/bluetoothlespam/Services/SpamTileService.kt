package de.simon.dankelmann.bluetoothlespam.Services

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.MainActivity
import de.simon.dankelmann.bluetoothlespam.Navigation.SpecterDestinations
import de.simon.dankelmann.bluetoothlespam.R

/**
 * Quick Settings tile to start/stop BLE advertising without opening the app (ease-of-use feature
 * ported in spirit from the ars2nb fork, re-implemented against this fork's
 * [de.simon.dankelmann.bluetoothlespam.Handlers.AdvertisementSetQueueHandler]).
 *
 * It only toggles the collection the user has already loaded and checked — it never changes WHAT
 * gets advertised. With nothing loaded it opens the app on the Advertise screen instead of being a
 * silent no-op.
 */
class SpamTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val queueHandler = (applicationContext as BleSpamApplication).queueHandler

        // Nothing selected to broadcast → open the app rather than silently doing nothing.
        if (!queueHandler.isActive() && !queueHandler.hasCheckedItems()) {
            openAdvertiseScreen()
            return
        }

        queueHandler.toggle(this)
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val active = (applicationContext as BleSpamApplication).queueHandler.isActive()
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        tile.updateTile()
    }

    private fun openAdvertiseScreen() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_DESTINATION, SpecterDestinations.ADVERTISEMENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+ requires a PendingIntent; the Intent overload throws there.
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

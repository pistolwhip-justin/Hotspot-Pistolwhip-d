package com.pistolwhip.hotspot

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class ShareTileService : TileService() {
    override fun onClick() {
        val active = State.share(this)
        val intent = Intent(this, ShareService::class.java).setAction(if (active) ShareService.ACTION_STOP else null)
        if (active) startService(intent) else startForegroundService(intent)
        qsTile?.state = if (active) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }
    override fun onStartListening() { qsTile?.state = if (State.share(this)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE; qsTile?.updateTile() }
}

class ReceiveTileService : TileService() {
    override fun onClick() {
        val active = State.receive(this)
        val intent = Intent(this, ReceiveService::class.java).setAction(if (active) ReceiveService.ACTION_STOP else null)
        if (active) startService(intent) else startForegroundService(intent)
        qsTile?.state = if (active) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }
    override fun onStartListening() { qsTile?.state = if (State.receive(this)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE; qsTile?.updateTile() }
}

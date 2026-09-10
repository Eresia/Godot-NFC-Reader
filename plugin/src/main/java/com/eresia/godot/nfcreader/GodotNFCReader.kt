package com.eresia.godot.nfcreader

import android.Manifest
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

class NfcReader(godotPlugin: GodotNFCReader) : NfcAdapter.ReaderCallback {

	private val godotPlugin : GodotNFCReader

	init {
	    this.godotPlugin = godotPlugin
	}

	override fun onTagDiscovered(tag: Tag?) {
		if(tag != null) {
			godotPlugin.onRead(tag)
		}
		else {
			Log.e(godotPlugin.pluginName, "Tag is null")
		}
	}
}

class GodotNFCReader(godot: Godot) : GodotPlugin(godot) {

	companion object {
		val READ_TAG_SIGNAL = SignalInfo("read_tag_data", String::class.java)
		val READ_COMPLETE_TAG_SIGNAL = SignalInfo("read_complete_tag_data", String::class.java, Array<String>::class.java)
		val WRITE_TAG_SIGNAL = SignalInfo("write_tag_data", String::class.java, Array<String>::class.java)
	}

	override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME

	override fun getPluginSignals(): Set<SignalInfo> {
		return setOf(READ_TAG_SIGNAL, READ_COMPLETE_TAG_SIGNAL, WRITE_TAG_SIGNAL)
	}

	private var nfcAdapter : NfcAdapter? = null
	private var nfcStatus = 0
	private val nfcReader : NfcReader = NfcReader(this)

	@UsedByGodot
	private fun getNFCStatus() : Int
	{
		return nfcStatus
	}

	@UsedByGodot
	private fun enableNFC() {
		val activity = activity ?: return

		val permissionArray = arrayOf<String?>(
			Manifest.permission.NFC
		)

		ActivityCompat.requestPermissions(activity, permissionArray, 0)

		nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
		activateNfc()
	}

	@UsedByGodot
	private fun disableNFC() {
		inactivateNfc()
		nfcAdapter = null
	}

	override fun onMainResume() {
		super.onMainResume()
		activateNfc()
	}

	override fun onMainPause() {
		super.onMainPause()
		inactivateNfc()
	}

	public fun onRead(tag : Tag) {
		var tagIdName : String = "";

		for(byte in tag.id) {
			tagIdName += String.format("%02X", byte);
		}

		emitSignal(READ_TAG_SIGNAL.name, tagIdName)
		logInfo("Read Tag : $tagIdName")
		nfcStatus = 2
	}

	private fun activateNfc()
	{
		if(nfcAdapter == null) {
			nfcStatus = -1
			return
		}

		nfcAdapter!!.enableReaderMode(activity, nfcReader,
			NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS or
					NfcAdapter.FLAG_READER_NFC_A or
					NfcAdapter.FLAG_READER_NFC_B or
					NfcAdapter.FLAG_READER_NFC_F or
					NfcAdapter.FLAG_READER_NFC_V, null)

		nfcStatus = 1
		logInfo("NFC activated")
	}

	private fun inactivateNfc()
	{
		if(nfcAdapter == null) {
			nfcStatus = -1
			return
		}

		nfcAdapter!!.disableReaderMode(activity)

		nfcStatus = 0
		logInfo("NFC inactivated")
	}

	private fun logAndNotifyDebug(data : String) {
		runOnHostThread {
			Toast.makeText(activity, data, Toast.LENGTH_LONG).show()
		}

		logInfo(data)
	}

	private fun logError(data : String) {
		Log.e(pluginName, data)
	}

	private fun logInfo(data : String) {
		Log.i(pluginName, data)
	}
}

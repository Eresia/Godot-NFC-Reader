/*package com.eresia.godotnfc

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

class NfcReader(godotNFC: GodotNFC) : NfcAdapter.ReaderCallback {

	private val godotNFC : GodotNFC

	init {
	    this.godotNFC = godotNFC
	}

	override fun onTagDiscovered(tag: Tag?) {
		if(tag != null) {
			godotNFC.onRead(tag)
		}
		else {
			Log.e(godotNFC.pluginName, "Tag is null")
		}
	}
}

class GodotNFC(godot: Godot) : GodotPlugin(godot) {

	companion object {
		val READ_TAG_SIGNAL = SignalInfo("read_tag_data", String::class.java)
		val READ_COMPLETE_TAG_SIGNAL = SignalInfo("read_complete_tag_data", String::class.java, Array<String>::class.java)
		val WRITE_TAG_SIGNAL = SignalInfo("write_tag_data", String::class.java, Array<String>::class.java)

		private var pluginInstance : GodotNFC? = null

		@JvmStatic
		fun getPluginInstance() : GodotNFC? {
			return pluginInstance
		}
	}

	init {
		if(pluginInstance != null) {
			logError("Double GodotNFC instance")
		}
		else {
			pluginInstance = this
			logInfo("GodotNFC created");
		}
	}

	override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME

	override fun getPluginSignals(): Set<SignalInfo> {
		return setOf(READ_TAG_SIGNAL, READ_COMPLETE_TAG_SIGNAL, WRITE_TAG_SIGNAL)
	}

	private var nfcAdapter : NfcAdapter? = null
	private var pendingIntent : PendingIntent? = null

	private var nfcStatus = 0
	private var usingCompleteMode : Boolean = false

	private val nfcReader : NfcReader = NfcReader(this)
	private var queuedWriteData : ByteArray? = null

	@UsedByGodot
	private fun getNFCStatus() : Int
	{
		return nfcStatus
	}

	@UsedByGodot
	private fun enableNFC(usingCompleteMode : Boolean) {
		this.usingCompleteMode = usingCompleteMode

		nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
		activateNfc()
	}

	@UsedByGodot
	private fun disableNFC() {
		inactivateNfc()
		nfcAdapter = null
	}

	@UsedByGodot
	private fun writeOnNextTag(data : String) {
		queuedWriteData = data.toByteArray(Charsets.UTF_8)
	}

	public fun onCreate(newPendingIntent : PendingIntent){
		pendingIntent = newPendingIntent

		logInfo("Plugin initialized");
	}

	public fun onResume() {
		super.onMainResume()

		if(!usingCompleteMode) {
			activateNfc()
		}
	}

	public fun onPause() {
		super.onMainPause()

		if(!usingCompleteMode) {
			inactivateNfc()
		}
	}

	public fun onRead(tag : Tag) {
		emitSignal(READ_TAG_SIGNAL.name, tag.id.toString())
	}

	public fun onNewIntent(intent : Intent) {
		logInfo("New Intent : " + intent.action)
		//runOnUiThread {
			if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
				val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
				} else {
					@Suppress("DEPRECATION") intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
				}

				if(tag == null)	{
					nfcStatus = -2
					logError("Can't Read Tag")
					return
				}

				if (queuedWriteData == null) {
					var messages: Array<NdefMessage>?

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
						messages = intent.getParcelableArrayExtra(
							NfcAdapter.EXTRA_NDEF_MESSAGES,
							NdefMessage::class.java
						)
					} else {
						val rawMessages: Array<Parcelable>? =
							@Suppress("DEPRECATION") intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

						if (rawMessages == null) {
							messages = null
						} else {
							messages = emptyArray<NdefMessage>()
							for (i in rawMessages.indices) {
								messages += rawMessages[i] as NdefMessage
							}
						}
					}

					if (messages == null) {
						nfcStatus = 2
						Log.v(pluginName, "Read Tag Only")
						emitSignal(READ_COMPLETE_TAG_SIGNAL.name, tag.id.toString(), emptyArray<String>())
						return
					}

					for (i in messages.indices) {
						val ndefMessage: NdefMessage = messages[i]
						val ndefRecords: Array<NdefRecord> = ndefMessage.records

						if (ndefRecords.isEmpty()) {
							nfcStatus = 3
							logError("Read Tag but Can't Read Records")
							emitSignal(READ_COMPLETE_TAG_SIGNAL.name, tag.id.toString(), emptyArray<String>())
							return
						}

						var result: Array<String> = emptyArray<String>()

						for (j in ndefRecords.indices) {
							val record: NdefRecord = ndefRecords[j]
							val payload: ByteArray = record.payload

							result += payload.toString(Charsets.UTF_8)
						}

						emitSignal(READ_COMPLETE_TAG_SIGNAL.name, tag.id.toString(), result)
					}

					Log.v(pluginName, "Read Tag & Records")
					nfcStatus = 4
				} else {

					writeDataOnTag(tag, queuedWriteData!!)
					queuedWriteData = null

					nfcStatus = 3
				}
			} else {
				nfcStatus = 1
			}

			return
		//}
	}

	private fun writeDataOnTag(tag : Tag, data : ByteArray) {

	}

	private fun activateNfc()
	{
		if(nfcAdapter == null) {
			nfcStatus = -1
			logError("nfcAdapter null")
			return
		}

		if(pendingIntent == null) {
			nfcStatus = -5
			logError("plugin not initialized")
			return
		}

		logInfo("NFCAdapter available : " + nfcAdapter!!.isEnabled)

		if(usingCompleteMode) {
			nfcAdapter!!.enableForegroundDispatch(activity, pendingIntent, null, null)
		}
		else {
			nfcAdapter!!.enableReaderMode(activity, nfcReader,
				NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS or
						NfcAdapter.FLAG_READER_NFC_A or
						NfcAdapter.FLAG_READER_NFC_B or
						NfcAdapter.FLAG_READER_NFC_F or
						NfcAdapter.FLAG_READER_NFC_V, null)
		}

		nfcStatus = 1

		logAndNotifyDebug("NFC activated")
	}

	private fun inactivateNfc()
	{
		if(nfcAdapter == null) {
			return
		}

		if(usingCompleteMode) {
			nfcAdapter!!.disableForegroundDispatch(activity)
		}
		else {
			nfcAdapter!!.disableReaderMode(activity)
		}

		nfcStatus = 0

		logAndNotifyDebug("NFC inactivated")
	}

	private fun logAndNotifyDebug(data : String) {
		runOnUiThread {
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
*/
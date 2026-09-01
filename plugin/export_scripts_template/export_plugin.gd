@tool
extends EditorPlugin

# A class member to hold the editor export plugin during its lifecycle.
var export_plugin : AndroidExportPlugin

func _enter_tree():
	# Initialization of the plugin goes here.
	export_plugin = AndroidExportPlugin.new()
	add_export_plugin(export_plugin)


func _exit_tree():
	# Clean-up of the plugin goes here.
	remove_export_plugin(export_plugin)
	export_plugin = null


class AndroidExportPlugin extends EditorExportPlugin:
	var _plugin_name = "GodotNFCReader"

	func _supports_platform(platform):
		if platform is EditorExportPlatformAndroid:
			return true
		return false

	#func _get_android_manifest_activity_element_contents(platform, debug):
		# Add the NFC permission to the AndroidManifest.xml file.
		#var intents = "\n\t\t\t<intent-filter>\n"
		#intents += "\t\t\t\t<action android:name=\"android.nfc.action.NDEF_DISCOVERED\"/>\n"
		#intents += "\t\t\t\t<category android:name=\"android.intent.category.DEFAULT\"/>\n"
		#intents += "\t\t\t</intent-filter>\n\n"
		
		#intents += "\t\t\t<intent-filter>\n"
		#intents += "\t\t\t\t<action android:name=\"android.nfc.action.TAG_DISCOVERED\"/>\n"
		#intents += "\t\t\t\t<category android:name=\"android.intent.category.DEFAULT\"/>\n"
		#intents += "\t\t\t</intent-filter>\n\n"

		#return intents

	func _get_android_manifest_element_contents(platform, debug):
		return "\t<uses-feature android:name=\"android.hardware.nfc\" android:required=\"true\" />"

	func _get_android_libraries(platform, debug):
		if debug:
			return PackedStringArray([_plugin_name + "/bin/debug/" + _plugin_name + "-debug.aar"])
		else:
			return PackedStringArray([_plugin_name + "/bin/release/" + _plugin_name + "-release.aar"])

	func _get_android_dependencies(platform, debug):
		# TODO: Add remote dependices here.
		if debug:
			return PackedStringArray([])
		else:
			return PackedStringArray([])

	func _get_name():
		return _plugin_name

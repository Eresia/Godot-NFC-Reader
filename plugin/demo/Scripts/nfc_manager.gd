extends Node

@export var display_debug: bool = false
@export var status_debug: RichTextLabel = null
@export var data_debug: RichTextLabel = null

var nfc: Object = null

func _ready() -> void:
	print("Launching NFC")
	
	if Engine.has_singleton("GodotNFCReader"):
		nfc = Engine.get_singleton("GodotNFCReader")
	
	if not nfc:
		log_debug("Failed getting NFC singleton")
		return
	
	nfc.enableNFC()
	nfc.read_tag_data.connect(read_tag_data)
	log_status(nfc.getNFCStatus())
	log_debug("NFC Enabled")


func _process(_delta) -> void:
	if(nfc):
		log_status(nfc.getNFCStatus())

func read_tag_data(tag_id: String) -> void:
	var debug_data : String = "Tag : " + tag_id
	log_debug(debug_data)

func log_status(status : int) -> void:
	if(status_debug != null):
		status_debug.text = "Status " + str(status)

func log_debug(data : String) -> void:
	if(display_debug):
		print(data)
		
	if(data_debug != null):
		data_debug.text = data

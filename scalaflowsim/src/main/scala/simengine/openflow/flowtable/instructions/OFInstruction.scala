package scalasim.simengine.openflow.flowtable.instructions

class OFInstruction (private val name : String) {

}

class OFApplyActionIns(name : String) extends OFInstruction(name) {

}

class OFClearActionIns(name : String) extends OFInstruction(name) {

}

class OFWriteActionIns(name : String) extends OFInstruction(name) {

}

class OFWriteMetadataIns(name : String) extends OFInstruction(name) {

}

class OFGotoTable(name : String, tableid : Int = 1) extends OFInstruction(name) {

}
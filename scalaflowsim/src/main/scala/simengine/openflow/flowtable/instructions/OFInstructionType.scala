package simengine.openflow.flowtable.instructions

object OFInstructionType extends Enumeration {
  type OFInstructionType = Value
  val OFApplyAction, OFClearAction, OFGotoTable, OFWriteAction, OFWriteMetadata = Value
}

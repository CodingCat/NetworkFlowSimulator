package simengine.openflow.flowtable.instructions

object OFInstructionFactory {
  def getCounter(instype : OFInstructionType.Value) : OFInstruction = {
    instype match {
      case OFInstructionType.OFApplyAction => new OFApplyActionIns("apply_action_instruction")
      case OFInstructionType.OFClearAction => new OFClearActionIns("clear_action_instruction")
      case OFInstructionType.OFGotoTable => new OFGotoTable("goto_action_instruction")
      case OFInstructionType.OFWriteAction => new OFWriteActionIns("write_action_instruction")
      case OFInstructionType.OFWriteMetadata => new OFWriteMetadataIns("write_metadata_instruction")
    }
  }
}

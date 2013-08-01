package scalasim.test

import org.scalatest.FunSuite
import scalasim.network.component.Pod
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.simengine.SimulationEngine
import scalasim.{SimulationRunner, XmlParser}
import org.openflow.util.{U32, HexString}
import simengine.utils.IPAddressConvertor
import org.openflow.protocol.factory.BasicFactory
import org.openflow.protocol.action.{OFAction, OFActionOutput, OFActionType}
import org.openflow.protocol.{OFMatch, OFFlowMod, OFType}
import java.util

class OpenFlowSuite extends FunSuite {
  test ("routers can be assigned with DPID address correctly") {
    SimulationRunner.reset
    XmlParser.addProperties("scalasim.topology.cell.aggregaterouternum", "1")
    XmlParser.addProperties("scalasim.topology.cell.racknum", "2")
    XmlParser.addProperties("scalasim.topology.cell.racksize", "20")
    XmlParser.addProperties("scalasim.simengine.model", "openflow")
    XmlParser.loadConf("config.xml")
    val cellnet = new Pod(1)
    for (i <- 0 until cellnet.numAggRouters) {
      assert(cellnet.getAggregatRouter(i).getDPID === HexString.toLong("01:01:" +
        cellnet.getAggregatRouter(i).mac_addr(0)))
    }
    for (i <- 0 until cellnet.numRacks) {
      assert(cellnet.getToRRouter(i).getDPID === HexString.toLong("00:01:" +
        cellnet.getToRRouter(i).mac_addr(0)))
    }
  }

  test ("decimal dot ip address can be translated into integer correctly") {
    SimulationRunner.reset
    assert(U32.t(IPAddressConvertor.DecimalStringToInt("192.168.1.1")) === 0xC0A80101)
    assert(U32.t(IPAddressConvertor.DecimalStringToInt("10.4.4.1")) === 0xA040401)
    assert(U32.t(IPAddressConvertor.DecimalStringToInt("255.255.255.255")) === 0xFFFFFFFF)
  }

  test ("the integer can be translated into decimal dot ip address") {
    SimulationRunner.reset
    assert(IPAddressConvertor.IntToDecimalString(0xC0A80101) === "192.168.1.1")
    assert(IPAddressConvertor.IntToDecimalString(0xA040401) === "10.4.4.1")
    assert(IPAddressConvertor.IntToDecimalString(0xFFFFFFFF) === "255.255.255.255")
  }

  test ("when add flow table entry it can schedule entry expire event correctly") {
    SimulationRunner.reset
    val offactory = new BasicFactory
    val table = new OFFlowTable
    val matchfield = new OFMatch
    val outaction  = new OFActionOutput
    val actionlist = new util.ArrayList[OFAction]
    actionlist.add(outaction)
    var flow_mod = offactory.getMessage(OFType.FLOW_MOD).asInstanceOf[OFFlowMod]
    flow_mod.setMatch(matchfield)
    flow_mod.setActions(actionlist)
    flow_mod.setCommand(OFFlowMod.OFPFC_ADD)
    flow_mod.setHardTimeout(500)
    flow_mod.setIdleTimeout(200)
    //first is in 200
    table.addFlowTableEntry(flow_mod)
    //second is in 500
    flow_mod = offactory.getMessage(OFType.FLOW_MOD).asInstanceOf[OFFlowMod]
    flow_mod.setMatch(matchfield)
    flow_mod.setActions(actionlist)
    flow_mod.setCommand(OFFlowMod.OFPFC_ADD)
    flow_mod.setHardTimeout(500)
    flow_mod.setIdleTimeout(0)
    table.addFlowTableEntry(flow_mod)
    //third is in 200
    flow_mod = offactory.getMessage(OFType.FLOW_MOD).asInstanceOf[OFFlowMod]
    flow_mod.setMatch(matchfield)
    flow_mod.setActions(actionlist)
    flow_mod.setCommand(OFFlowMod.OFPFC_ADD)
    flow_mod.setHardTimeout(0)
    flow_mod.setIdleTimeout(200)
    table.addFlowTableEntry(flow_mod)
    //forth is 100
    flow_mod = offactory.getMessage(OFType.FLOW_MOD).asInstanceOf[OFFlowMod]
    flow_mod.setMatch(matchfield)
    flow_mod.setActions(actionlist)
    flow_mod.setCommand(OFFlowMod.OFPFC_ADD)
    flow_mod.setHardTimeout(100)
    flow_mod.setIdleTimeout(200)
    table.addFlowTableEntry(flow_mod)
    assert(SimulationEngine.Events.length === 4)
    assert(SimulationEngine.Events.toList(0).getTimeStamp() === 100)
    assert(SimulationEngine.Events.toList(1).getTimeStamp() === 200)
    assert(SimulationEngine.Events.toList(2).getTimeStamp() === 200)
    assert(SimulationEngine.Events.toList(3).getTimeStamp() === 500)
  }
}

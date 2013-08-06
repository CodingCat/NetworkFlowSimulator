package floodlight

import org.scalatest.FunSuite
import scalasim.network.component._
import scalasim.network.component.builder.{LanBuilder, AddressInstaller}
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import scalasim.network.controlplane.routing.OpenFlowRouting
import scalasim.network.events.StartNewFlowEvent
import scalasim.network.traffic.{CompletedFlow, Flow}
import scalasim.simengine.SimulationEngine
import scalasim.{SimulationRunner, XmlParser}
import org.openflow.util.{U32, HexString}
import simengine.utils.IPAddressConvertor
import org.openflow.protocol.factory.BasicFactory
import org.openflow.protocol.action.{OFAction, OFActionOutput}
import org.openflow.protocol.{OFMatch, OFFlowMod, OFType}
import java.util
import network.controlplane.openflow.flowtable.OFMatchField

class OpenFlowSuite extends FunSuite {
  test ("routers can be assigned with DPID address correctly") {
    SimulationRunner.reset
    XmlParser.addProperties("scalasim.simengine.model", "openflow")
    val aggrouter = new Router(AggregateRouterType)
    val torrouter = new Router(ToRRouterType)
    AddressInstaller.assignIPAddress(aggrouter, "10.1.0.1")
    AddressInstaller.assignIPAddress(torrouter, "10.1.0.2")
    assert(aggrouter.getDPID === HexString.toLong("01:01:" + aggrouter.mac_addr(0)))
    assert(torrouter.getDPID === HexString.toLong("00:01:" + torrouter.mac_addr(0)))
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
    val node = new Router(AggregateRouterType)
    val ofroutingmodule = new OpenFlowRouting(node)
    val offactory = new BasicFactory
    val table = new OFFlowTable(ofroutingmodule)
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
    //sorted
    assert(SimulationEngine.Events.toList(0).getTimeStamp() === 100)
    assert(SimulationEngine.Events.toList(1).getTimeStamp() === 200)
    assert(SimulationEngine.Events.toList(2).getTimeStamp() === 200)
    assert(SimulationEngine.Events.toList(3).getTimeStamp() === 500)
  }

  test ("flow table can match flow entry correctly") {
    SimulationRunner.reset
    val matchfield = new OFMatchField
    val flow = Flow("10.0.0.1", "10.0.0.2", "00:00:00:00:00:11", "00:00:00:00:00:22", size = 1)
    val generatedmatchfield = OFFlowTable.createMatchField(flow, 0)
    //set matchfield
    matchfield.setInputPort(2)
    matchfield.setDataLayerDestination("00:00:00:00:00:22")
    matchfield.setDataLayerSource("00:00:00:00:00:11")
    matchfield.setNetworkDestination(U32.t(IPAddressConvertor.DecimalStringToInt("10.0.0.2")))
    matchfield.setNetworkSource(U32.t(IPAddressConvertor.DecimalStringToInt("10.0.0.1")))
    matchfield.setWildcards(OFMatch.OFPFW_ALL
      & ~OFMatch.OFPFW_DL_VLAN
      & ~OFMatch.OFPFW_DL_SRC
      & ~OFMatch.OFPFW_DL_DST
      & ~OFMatch.OFPFW_NW_SRC_MASK
      & ~OFMatch.OFPFW_NW_DST_MASK)
    matchfield.setDataLayerVirtualLan(0)
    assert(matchfield.matching(generatedmatchfield) === true)
  }

  test ("flow table can match flow entry correctly (with ip mask)") {
    SimulationRunner.reset
    val matchfield = new OFMatchField
    val flow = Flow("10.0.0.1", "10.0.0.2", "00:00:00:00:00:11", "00:00:00:00:00:22", size = 1)
    val generatedmatchfield = OFFlowTable.createMatchField(flow, 0)
    //set matchfield
    matchfield.setInputPort(2)
    matchfield.setDataLayerDestination("00:00:00:00:00:22")
    matchfield.setDataLayerSource("00:00:00:00:00:11")
    matchfield.setNetworkSource(U32.t(IPAddressConvertor.DecimalStringToInt("10.0.0.3")))
    matchfield.setNetworkDestination(U32.t(IPAddressConvertor.DecimalStringToInt("10.0.0.4")))
    matchfield.setWildcards(OFMatch.OFPFW_ALL
      & ~OFMatch.OFPFW_DL_VLAN
      & ~OFMatch.OFPFW_DL_SRC
      & ~OFMatch.OFPFW_DL_DST
      & ~(39 << OFMatch.OFPFW_NW_DST_SHIFT)
      & ~(39 << OFMatch.OFPFW_NW_SRC_SHIFT)
    )
    matchfield.setDataLayerVirtualLan(0)
    assert(matchfield.matching(generatedmatchfield) === true)
  }
}

package scalasim.test

import org.scalatest.FunSuite
import scalasim.network.component.Pod
import scalasim.XmlParser
import org.openflow.util.HexString

class OpenFlowSuite extends FunSuite {
  test ("routers can be assigned with DPID address correctly") {
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
}

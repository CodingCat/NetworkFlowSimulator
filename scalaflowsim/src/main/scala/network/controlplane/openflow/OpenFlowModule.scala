package scalasim.network.controlplane.openflow


import scalasim.network.component._
import scalasim.XmlParser
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ClientBootstrap
import java.net.InetSocketAddress
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.openflow.protocol._
import org.openflow.util.HexString
import scalasim.network.controlplane.openflow.flowtable.OFFlowTable
import java.util

abstract class OpenFlowSwitchStatus

case object OpenFlowSwitchHandShaking extends OpenFlowSwitchStatus
case object OpenFlowSwitchRunning extends OpenFlowSwitchStatus
case object OpenFlowSwitchClosing extends OpenFlowSwitchStatus

class OpenFlowModule (private val router : Router) {

  private val host = XmlParser.getString("scalasim.network.controlplane.openflow.controller.host", "127.0.0.1")
  private val port = XmlParser.getInt("scalasim.network.controlplane.openflow.controller.port", 6633)

  private var config_flags : Short = 0
  private var miss_send_len : Short = 0

  private[openflow] var status : OpenFlowSwitchStatus = OpenFlowSwitchHandShaking

  private[openflow] val flowtables : Array[OFFlowTable] = new Array[OFFlowTable](
    XmlParser.getInt("scalasim.network.controlplane.openflow.tablenum", 1)
  )

  def init() {
    for (i <- 0 until flowtables.length) flowtables(i) = new OFFlowTable
  }

  //build channel to the controller
  def connectToController() {
    try {
      val clientfactory = new NioClientSocketChannelFactory(
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool())
      val clientbootstrap = new ClientBootstrap(clientfactory)
      clientbootstrap.setPipelineFactory(new OpenFlowMsgPipelineFactory(this))
      clientbootstrap.connect(new InetSocketAddress(host, port))
    }
    catch {
      case e : Exception => e.printStackTrace
    }
  }

  def setParameter(f : Short, m : Short) {
    config_flags = f
    miss_send_len = miss_send_len
  }

  def getFlag = config_flags
  def get_miss_send_len = miss_send_len

  def getDPID : Long = {
    val impl_dependent = router.nodetype match {
      case ToRRouterType => "00"
      case AggregateRouterType => "01"
      case CoreRouterType => "02"
    }
    val t = router.ip_addr(0).substring(router.ip_addr(0).indexOf('.') + 1, router.ip_addr(0).size)
    //TODO: implicitly limit the maximum number of pods, improve?
    val podid = HexString.toHexString(Integer.parseInt(t.substring(0, t.indexOf('.'))), 1)
    val order = HexString.toHexString(router.getrid, 4)
    HexString.toLong(impl_dependent + ":" + podid + ":" + order + ":00")
  }


  def getSwitchFeature() = {
    //TODO: specify the switch features
    val ports = new util.ArrayList[OFPhysicalPort]
    //(dpid, buffer, n_tables, capabilities, physical port
    (getDPID, 1000, flowtables.length, 7, router.controlPlane.topoModule.physicalports)
  }

  def setSwitchParameters(configpacket : OFSetConfig) {
    config_flags = configpacket.getFlags
    miss_send_len = configpacket.getMissSendLength
  }

  def getSwitchParameters() : (Short, Short) = {
    (config_flags, miss_send_len)
  }

  def getSwitchDescription = router.ip_addr(0)

  //init
  init
}

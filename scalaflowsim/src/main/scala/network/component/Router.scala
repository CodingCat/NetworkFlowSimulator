package scalasim.network.component

import scalasim.network.controlplane.openflow.OpenFlowModule
import scalasim.XmlParser

class Router (nodetype : NodeType) extends Node(nodetype) {

  private var rid : Int = 0

  private [network] val openflowconnector = {
    if (XmlParser.getString("scalasim.simengine.model", "tcp") == "openflow") {
      new OpenFlowModule(this)
    }
    else {
      null
    }
  }

  def connectTOController() {
    if (openflowconnector != null) openflowconnector.connectToController()
  }

  def setrid (r : Int) { rid = r }
  def getrid = rid

  def getDPID = openflowconnector.getDPID
}

class RouterContainer () extends NodeContainer {
  def create(nodeN : Int, rtype : NodeType) {
    for (i <- 0 until nodeN) {
      nodecontainer += new Router(rtype)
      this(i).setrid(i)
    }
  }

  def create(nodeN : Int) {
    throw new RuntimeException("you have to specify the router type")
  }

  override def apply(i : Int) = nodecontainer(i).asInstanceOf[Router]
}


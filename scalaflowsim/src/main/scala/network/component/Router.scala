package scalasim.network.component

import scalasim.simengine.openflow.OpenFlowConnector
import scalasim.XmlParser

class Router (nodetype : NodeType) extends Node(nodetype) {

  private var rid : Int = 0

  private val openflowconnector = {
    if (XmlParser.getString("scalasim.simengine.model", "tcp") == "openflow") {
      new OpenFlowConnector(this)
    }
    else {
      null
    }
  }

  def connectTOController() {
    if (openflowconnector != null) openflowconnector.initChannel()
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


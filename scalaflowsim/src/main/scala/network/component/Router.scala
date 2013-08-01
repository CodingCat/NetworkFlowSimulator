package scalasim.network.component

import scalasim.network.controlplane.openflow.OpenFlowModule

class Router (nodetype : NodeType) extends Node(nodetype) {

  private var rid : Int = 0

  def connectTOController() {
    if (controlPlane.isInstanceOf[OpenFlowModule]) {
      controlPlane.asInstanceOf[OpenFlowModule].connectToController
    }
  }

  def setrid (r : Int) { rid = r }
  def getrid = rid

  def getDPID : Long = {
    if (! controlPlane.isInstanceOf[OpenFlowModule]) -1
    else controlPlane.asInstanceOf[OpenFlowModule].getDPID
  }
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


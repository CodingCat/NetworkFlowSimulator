package network.device

import scalasim.network.controlplane.openflow.OpenFlowModule
import scala.collection.mutable.ArrayBuffer
import scalasim.network.traffic.Flow
import scala.collection.mutable
import network.device.GlobalDeviceManager

class Router (nodetype : NodeType, globaldevid : Int)
  extends Node(nodetype, globaldevid) {

  private var rid : Int = 0

  def connectTOController() {
    if (controlPlane.isInstanceOf[OpenFlowModule]) {
      controlPlane.asInstanceOf[OpenFlowModule].connectToController()
    }
  }

  def disconnectFromController() {
    if (controlPlane.isInstanceOf[OpenFlowModule])
      controlPlane.asInstanceOf[OpenFlowModule].disconnectFormController()
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
      nodecontainer += new Router(rtype, GlobalDeviceManager.globaldevicecounter)
      GlobalDeviceManager.globaldevicecounter += 1
      this(i).setrid(i)
    }
  }

  def create(nodeN : Int) {
    throw new RuntimeException("you have to specify the router type")
  }

  override def apply(i : Int) = nodecontainer(i).asInstanceOf[Router]
}


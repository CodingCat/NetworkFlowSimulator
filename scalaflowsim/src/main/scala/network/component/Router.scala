package network.component

import network.controlplane.{ControlPlane}

class Router (nodetype : NodeType) extends Node(nodetype) {

}

class RouterContainer () extends NodeContainer {
  def create(nodeN : Int, rtype : NodeType) {
    for (i <- 0 until nodeN) nodecontainer += new Router(rtype)
  }

  def create(nodeN : Int) {
    throw new RuntimeException("you have to specify the router type")
  }
}


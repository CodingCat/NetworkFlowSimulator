package network

import scalasim.network.{NodeContainer, Node}


class Host () extends Node

class HostContainer extends NodeContainer {

  def create(nodeN : Int) = {
    for (i <- 0 until nodeN) nodecontainer += new Host()
  }
}

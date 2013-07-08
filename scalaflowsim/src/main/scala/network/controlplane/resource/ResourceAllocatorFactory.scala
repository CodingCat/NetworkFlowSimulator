package network.controlplane.resource

import network.topo.Node


private [controlplane] object ResourceAllocatorFactory {
  def getResourceAllocator (name : String, node : Node) : ResourceAllocator = name match {
    case "MaxMin" => new MaxMinAllocator(node)
    case _ => throw new Exception("unrecognizable ResourceAllocator type")
  }
}

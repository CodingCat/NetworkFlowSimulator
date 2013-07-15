package network.controlplane.resource

import network.component.Node
import network.controlplane.ControlPlane


private [controlplane] object ResourceAllocatorFactory {
  def getResourceAllocator (name : String, controlPlane : ControlPlane) : ResourceAllocator = name match {
    case "MaxMin" => new MaxMinAllocator(controlPlane)
    case _ => throw new Exception("unrecognizable ResourceAllocator type")
  }
}

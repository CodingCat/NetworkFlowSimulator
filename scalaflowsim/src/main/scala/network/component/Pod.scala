package scalasim.network.component

import scalasim.network.controlplane.openflow.OpenFlowModule
import scalasim.simengine.utils.Logging
import scalasim.XmlParser
import scalasim.network.component.builder.{LanBuilder, AddressInstaller}
;

class Pod (private val cellID : Int,
           private val aggregateRouterNumber : Int = XmlParser.getInt("scalasim.topology.cell.aggregaterouternum", 2),
           private val rackNumber : Int = XmlParser.getInt("scalasim.topology.cell.racknum", 4),
           private val rackSize : Int = XmlParser.getInt("scalasim.topology.cell.racksize", 20))
  extends Logging {

  private val aggContainer = new RouterContainer
  private val torContainer = new RouterContainer
  private val hostsContainer = new HostContainer

  private def buildNetwork() {
    def initOFNetwork {
      def topologypending : Boolean = {
        //checking tor router
        for (i <- 0 until rackNumber) {
          if (!torContainer(i).controlPlane.
            asInstanceOf[OpenFlowModule].topologyHasbeenRecognized())
            return false
        }
        //checking agg router
        for (i <- 0 until numAggRouters) {
          if (!aggContainer(i).controlPlane.
            asInstanceOf[OpenFlowModule].topologyHasbeenRecognized())
            return false
        }
        true
      }
      if (XmlParser.getString("scalasim.simengine.model", "tcp") == "openflow") {
        //aggeregate routers
        for (i <- 0 until aggregateRouterNumber) aggContainer(i).connectTOController()
        //ToR routers
        for (i <- 0 until numRacks) torContainer(i).connectTOController()
        //waiting for controller to process the topology
        while (!topologypending) Thread.sleep(1000)
      }
    }

    def assignIPtoRacks() {
      for (i <- 0 until rackNumber) {
        //assign ip to the TOR router
        AddressInstaller.assignIPAddress(torContainer(i), "10." + cellID + "." + i + ".1")
        //assign ip addresses to the hosts
        AddressInstaller.assignIPAddress(torContainer(i).ip_addr(0),
          2, hostsContainer, i * rackSize, (i + 1) * rackSize - 1)
      }
    }

    def assignIPtoAggLayer() {
      for (i <- 0 until aggregateRouterNumber) {
        AddressInstaller.assignIPAddress(aggContainer(i), "10." + cellID + "." + (rackNumber + i) + ".1")
        AddressInstaller.assignIPAddress(aggContainer(i).ip_addr(0),
          2, torContainer, 0, torContainer.size - 1)
      }
    }

    def buildLanOnAggregate() {
      for (i <- 0 until aggregateRouterNumber) {
        LanBuilder.buildLan(aggContainer(i).asInstanceOf[Router], torContainer, 0, torContainer.size - 1)
      }
    }

    def buildLanOnRack() {
      for (i <- 0 until rackNumber) {
        LanBuilder.buildLan(torContainer(i).asInstanceOf[Router], hostsContainer, i * rackSize,
          (i + 1) * rackSize - 1)
      }
    }

    hostsContainer.create(rackNumber * rackSize)
    torContainer.create(rackNumber, ToRRouterType)
    aggContainer.create(aggregateRouterNumber, AggregateRouterType)

    //main part of buildNetwork
    assignIPtoRacks
    assignIPtoAggLayer
    buildLanOnAggregate
    buildLanOnRack
    initOFNetwork
  }


  def numAggRouters = aggregateRouterNumber
  def numRacks = rackNumber
  def numMachinesPerRack = rackSize

  def getAggregatRouter(idx : Int) : Router = aggContainer(idx)
  def getToRRouter(idx : Int) : Router = torContainer(idx)
  def getHost(rackID : Int, hostID : Int) : Host = hostsContainer(rackID * rackSize + hostID)
  def getAllHostsInPod() : HostContainer = hostsContainer

  buildNetwork()
}




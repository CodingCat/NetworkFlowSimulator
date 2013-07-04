package network.topo

import scalasim.XmlParser
import network.topo.builder.{LanBuilder, IPInstaller}
;

class Cell (private val cellID : Int) {

  private val aggregateRouterNumber = XmlParser.getInt("scalasim.topology.cell.aggregaterouternum", 2)
  private val rackNumber = XmlParser.getInt("scalasim.topology.cell.racknum", 4)
  private val rackSize = XmlParser.getInt("scalasim.topology.cell.racksize", 20)

  private val aggContainer = new RouterContainer()
  private val torContainer = new RouterContainer()
  private val hostsContainer = new HostContainer()

  private def buildNetwork() {
    def assignIPtoRacks() {
      for (i <- 0 until rackNumber) {
        //assign ip to the TOR router
        IPInstaller.assignIPAddress(torContainer(i), "10." + cellID + "." + i + ".1")
        //assign ip addresses to the hosts
        IPInstaller.assignIPAddress(torContainer(i).ip_addr(0),
          2, hostsContainer, i * rackSize, (i + 1) * rackSize - 1)
      }
    }

    def assignIPtoAggLayer() {
      for (i <- 0 until aggregateRouterNumber) {
        IPInstaller.assignIPAddress(aggContainer(i), "10." + cellID + "." + (rackNumber + i) + ".1")
        IPInstaller.assignIPAddress(aggContainer(i).ip_addr(0),
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
        LanBuilder.buildLan(torContainer(i).asInstanceOf[Router], hostsContainer, i * rackSize, (i + 1) * rackSize - 1)
      }
    }
    //main part of buildNetwork
    assignIPtoRacks()
    assignIPtoAggLayer()
    buildLanOnAggregate()
    buildLanOnRack()
  }


  def numAggRouters = aggregateRouterNumber
  def numRacks = rackNumber
  def numMachinesPerRack = rackSize

  def getAggregatRouter(idx : Int) : Router = aggContainer(idx).asInstanceOf[Router]
  def getToRRouter(idx : Int) : Router = torContainer(idx).asInstanceOf[Router]
  def getHost(rackID : Int, hostID : Int) : Host = hostsContainer(rackID * rackSize + hostID).asInstanceOf[Host]

  hostsContainer.create(rackNumber * rackSize)
  torContainer.create(rackNumber)
  aggContainer.create(aggregateRouterNumber)

  buildNetwork()


}

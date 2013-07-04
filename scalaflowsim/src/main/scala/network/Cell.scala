package network

import scalasim.network.{NodeContainer, HostContainer, RouterContainer, Router}
import scalasim.XmlParser
import network.topo.{LanBuilder, IPInstaller}
;

class Cell (private val cellID : Int) {

  private val aggregateRouterNumber = XmlParser.getInt("scalasim.topology.cell.aggregaterouternum", 2)
  private val rackNumber = XmlParser.getInt("scalasim.topology.cell.racknum", 4)
  private val rackSize = XmlParser.getInt("scalasim.topology.cell.racksize", 20)

  private val aggContainer = new RouterContainer()
  private val torContainer = new RouterContainer()
  private val hostsContainer = new HostContainer()

  hostsContainer.create(rackNumber * rackSize)
  torContainer.create(rackNumber)
  aggContainer.create(aggregateRouterNumber)

  buildNetwork()

  private def assignIPtoRacks() {
    for (i <- 1 to rackNumber) {
      //assign ip to the TOR router
      IPInstaller.assignIPAddress(torContainer(i), "10." + cellID + "." + i + ".1")
      //assign ip addresses to the hosts
      IPInstaller.assignIPAddress(torContainer(i).ip_addr(torContainer(i).ip_addr.length - 1),
        2, hostsContainer, (i - 1) * rackSize, i * rackSize - 1)
    }
  }



  private def buildNetwork() {
    def assignIPtoAggLayer() {
      for (i <- 1 to aggregateRouterNumber) {
        aggContainer(i).assignIP("10." + cellID + "." + (rackNumber + i) + ".1")
        IPInstaller.assignIPAddress(aggContainer(i).ip_addr(aggContainer(i).ip_addr.length - 1),
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
        LanBuilder.buildLan(torContainer(i).asInstanceOf[Router], hostsContainer, i * rackSize, (i + 1) * rackSize)
      }
    }

    assignIPtoRacks()
    assignIPtoAggLayer()
    buildLanOnAggregate()
    buildLanOnRack()
  }
}

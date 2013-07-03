package network

import scalasim.network.{NodeContainer, HostContainer, RouterContainer}
import scalasim.XmlParser;

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

  private def buildNetwork() {
    for (i <- 1 to rackNumber) {
      //assign ip to the TOR router
      IPInstaller.assignIPAddress(torContainer(i), "10." + cellID + "." + i + ".1")
      //assign ip addresses to the hosts

    }
  }
}

package network

import scalasim.network.RouterContainer
import scalasim.XmlParser;

class Cell {

  private val aggregateRouterNumber = XmlParser.getInt("scalasim.topology.cell.aggregaterouternum", 2);
  private val rackNumber = XmlParser.getInt("scalasim.topology.cell.racknum", 4);
  private val rackSize = XmlParser.getInt("scalasim.topology.cell.racksize", 20);

  private val torContainer = new RouterContainer();


  buildNetwork();

  private def buildNetwork() {
    torContainer.create(rackNumber)

  }

}

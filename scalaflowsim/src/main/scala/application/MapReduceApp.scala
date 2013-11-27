package application

import network.topology.{Host, HostContainer}
import scala.collection.mutable.{MultiMap, Set, HashMap}
import simengine.utils.XmlParser


class MapReduceApp (servers : HostContainer) extends ServerApp (servers) {

  private val selectedPair = new HashMap[Host, Set[Host]] with MultiMap[Host, Host]//src ip -> dst ip

  private val jobnum = XmlParser.getInt("scalasim.app.mapreduce.jobnum", 10)
  private val arrivalinterval = XmlParser.getInt("scalasim.app.mapreduce.interval", 10)
  private val maxmappernum = XmlParser.getInt("scalasim.app.mapreduce.maxmapnum", 20)
  private val maxreducernum = XmlParser.getInt("scalasim.app.mapreduce.maxreducenum", 20)

  def run() {
    for (i <- 0 until jobnum) {

    }
  }

  def reset() {

  }
}

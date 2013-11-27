package application

import network.topology.{Host, HostContainer}
import scala.collection.mutable.{HashSet, MultiMap, Set, HashMap}
import simengine.utils.XmlParser
import network.events.StartNewFlowEvent
import network.traffic.Flow
import simengine.SimulationEngine
import scala.util.Random


class MapReduceApp (servers : HostContainer) extends ServerApp (servers) {


  private val jobnum = XmlParser.getInt("scalasim.app.mapreduce.jobnum", 10)
  private val arrivalinterval = XmlParser.getDouble("scalasim.app.mapreduce.interval", 10)
  private val maxmappernum = XmlParser.getInt("scalasim.app.mapreduce.maxmapnum", 20)
  private val maxreducernum = XmlParser.getInt("scalasim.app.mapreduce.maxreducenum", 20)
  private val flowsize = XmlParser.getInt("scalasim.app.mapreduce.flowsize", 200)


  def generateJob(startTime : Double) {
    var selectedMapperIndices = List[Int]()
    var selectedReducerIndices = List[Int]()

    def selectMapperServers() = {
      val mapperNum = Random.nextInt(maxmappernum)
      for (i <- 0 until mapperNum) {
        var idx = Random.nextInt(servers.size())
        while (selectedMapperIndices.contains(idx) ||
          selectedReducerIndices.contains(idx)) {
          idx = Random.nextInt(maxmappernum)
        }
        selectedMapperIndices = idx :: selectedMapperIndices
      }
    }

    def selectReducerServers() {
      val reducerNum = Random.nextInt(maxreducernum)
      for (i <- 0 until reducerNum) {
        var idx = Random.nextInt(servers.size())
        while (selectedMapperIndices.contains(idx) ||
          selectedReducerIndices.contains(idx)) {
          idx = Random.nextInt(maxreducernum)
        }
        selectedReducerIndices = idx :: selectedReducerIndices
      }
    }

    selectMapperServers()
    selectReducerServers()

    for (i <- 0 to selectedMapperIndices.length;
         j <- 0 until selectedReducerIndices.length) {
      val newflowevent = new StartNewFlowEvent(
        Flow(servers(selectedMapperIndices(i)).ip_addr(0),
          servers(selectedReducerIndices(j)).ip_addr(0),
          servers(selectedMapperIndices(i)).mac_addr(0),
          servers(selectedReducerIndices(j)).mac_addr(0),
          appDataSize = flowsize),
        servers(i),
        startTime)
      SimulationEngine.addEvent(newflowevent)
    }
  }

  def run() {
    for (i <- 0 until jobnum)
      generateJob(SimulationEngine.currentTime + i * arrivalinterval)
  }

  def reset() {

  }

}

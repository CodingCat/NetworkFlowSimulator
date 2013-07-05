package scalasim.application

import network.topo.HostContainer

abstract class ServerApp(protected val servers : HostContainer) {
  def run()
}

object ServerApp {
  def apply(appName : String, servers : HostContainer)  = {
    appName match {
      case "Spark" => new MapReduceApp(servers)
    }
  }
}





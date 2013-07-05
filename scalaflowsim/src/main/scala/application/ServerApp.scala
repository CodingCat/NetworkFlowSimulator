package scalasim.application

import network.topo.HostContainer
import application.PermuMatrixApp

abstract class ServerApp(protected val servers : HostContainer) {
  def run()
}

object ServerApp {
  def apply(appName : String, servers : HostContainer)  = {
    appName match {
      case "PermuMatrixApp" => new PermuMatrixApp(servers)
    }
  }
}





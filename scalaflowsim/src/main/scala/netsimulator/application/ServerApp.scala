package scalasim.application

import scalasim.network.component.HostContainer
import application.PermuMatrixApp

abstract class ServerApp(protected val servers : HostContainer) {
  def run()
  def reset()
}

object ServerApp {
  def apply(appName : String, servers : HostContainer)  = {
    appName match {
      case "PermuMatrixApp" => new PermuMatrixApp(servers)
    }
  }
}





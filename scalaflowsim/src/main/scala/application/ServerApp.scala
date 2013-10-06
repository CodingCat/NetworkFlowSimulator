package application

import network.device.HostContainer

abstract class ServerApp(protected val servers : HostContainer) {
  def run()
  def reset()
}

object ServerApp {
  def apply(appName : String, servers : HostContainer)  = {
    appName match {
      case "PermuMatrixApp" => new PermuMatrixApp(servers)
      case "OnOffApp" => new OnOffApp(servers)
    }
  }
}





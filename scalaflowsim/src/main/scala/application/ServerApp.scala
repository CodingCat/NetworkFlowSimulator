package application

import network.device.HostContainer

abstract class ServerApp(protected val servers : HostContainer) {
  def run()
  def reset()
}

object ServerApp {
  def apply(appName : String, servers : HostContainer) = {//, args: AnyVal)  = {
    appName match {
      case "PermuMatrixApp" => new PermuMatrixApp(servers)
     // case "OnOffApp" => new OnOffApp(servers, args(0).asInstanceOf[Double], args(1).asInstanceOf[Double])
    }
  }
}





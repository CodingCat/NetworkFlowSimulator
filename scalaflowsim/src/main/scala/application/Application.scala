package scalasim.application


;

abstract class Application {
  def run();
}

object Application {
  def apply(appName : String)  = {
    appName match {
      case "Spark" => new SparkApp()
    }
  }
}





package scalasim.application


import scala.collection.mutable.ListBuffer
import scalasim.XmlParser
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





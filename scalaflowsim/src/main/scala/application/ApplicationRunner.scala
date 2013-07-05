package scalasim.application

import scala.collection.mutable.ListBuffer
import scalasim.XmlParser
import network.topo.HostContainer

object ApplicationRunner {

  private val resourcePool : HostContainer = new HostContainer

  private val apps = new ListBuffer[ServerApp];

  def installApplication() {
    val appNames:String = XmlParser.getString("scalasim.application.names", "MapReduceApp");
    val namesStr = appNames.split(',');
    for (name <- namesStr) apps += ServerApp(name, resourcePool);
  }

  installApplication()

  def run() = for (app <- apps) app.run()

}

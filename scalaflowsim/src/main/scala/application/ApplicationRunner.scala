package scalasim.application

import scala.collection.mutable.ListBuffer
import scalasim.XmlParser

object ApplicationRunner {
  private val apps = new ListBuffer[Application];

  def installApplication() {
    val appNames:String = XmlParser.getString("scalasim.application.names", "SparkApp");
    val namesStr = appNames.split(',');
    for (name <- namesStr) apps += Application(name);
  }

  installApplication()

  def run() = for (app <- apps) app.run()

}

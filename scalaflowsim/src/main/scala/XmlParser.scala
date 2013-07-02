package scalasim;

import scala.xml.XML;
import scala.collection.mutable.HashMap;

object XmlParser {

  private val properties = new HashMap[String, String]();

  def loadConf(confPath:String) = {
    val xmldata = XML.loadFile(confPath);
    for (ele <- xmldata \\ "property" ) {
      properties += ((ele \\ "name").text -> (ele \\ "value").text);
    }
  }

  def getString(key:String, defaultValue:String) : String = {
    properties.getOrElse(key, defaultValue);
  }

  //TODO: can I do better?
  def getInt(key:String, defaultValue:Int) : Int = {
    properties.getOrElse(key, defaultValue.toString).toInt;
  }
}

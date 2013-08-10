package simengine.ofconnector

import java.io.{InputStreamReader, BufferedReader}
import java.net.URL

object FloodlightConnector {
  def readUrl(urlString : String,  expectedStatus : String) : Boolean = {
    val url = new URL(urlString)
    val reader = new BufferedReader(new InputStreamReader(url.openStream()))
    try {
      val buffer : StringBuffer = new StringBuffer()
      val chars = new Array[Char](1024)
      var read = 0
      read = reader.read(chars)
      while (read != -1) {
        buffer.append(chars, 0, read)
        read = reader.read(chars)
      }
      val jsonString = buffer.toString()
      while (jsonString != expectedStatus) {
        println(jsonString)
      }
    } finally {
      if (reader != null)
        reader.close()
    }
    true
  }
}

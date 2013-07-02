package network

import scalasim.network.NodeContainer
import scala.Predef.String


object IPInstaller {


  def assignIPAddress (ipbase : String,
                       startAddress : Int,
                       nodes : NodeContainer,
                       startIdx : Int,
                       endIdx : Int) {
    val ip_prefix : String =  ipbase.substring(0, ipbase.lastIndexOf('.') + 1)
    for (i <- 1 to 10) System.out.println("111")
  }
}

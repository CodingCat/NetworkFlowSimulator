package scalasim.network.util

object AddressNameConvertor {
  def IPStringtoBytes(ipString : String) = {
    val ipsegs = ipString.split('.')
    val ret = new Array[Byte](ipsegs.length)
    for (i <- 0 until ipsegs.length) {
      ret(i) = ipsegs(i).toInt.toByte
    }
    ret
  }
}

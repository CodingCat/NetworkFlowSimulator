package network.data

object FlowRateOrdering extends Ordering[Flow] {
  def compare(a : Flow, b : Flow) = {
    val r1 = {if (a.status == NewStartFlow) a.getTempRate else a.Rate}
    val r2 = {if (b.status == NewStartFlow) b.getTempRate else b.Rate}
    r1 > r2 match {
      case true => 1
      case false => r1 == r2 match {
        case true => 0
        case false => -1
      }
    }
  }
}
package network.component


class Link (val end_from : Node,
             val end_to : Node,
            private[network] val bandwidth : Double) {

  override def toString = "link-" + end_from.ip_addr(0) + "-" + end_to.ip_addr(0)
}

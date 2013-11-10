package network.topology.builder

import network.topology.{GlobalDeviceManager, Link, HostContainer, RouterContainer}


class FatTreeBuilder (private val podnum: Int = 4,
                      private val linkspeed: Double = 1.0) {

  val pods = 0 until podnum
  val core_sws_idx = 1 until (podnum / 2  + 1)
  val agg_sws_idx = podnum / 2 until podnum
  val edge_sws_idx = 0 until (podnum / 2)
  val hosts_idx = 2 until (podnum / 2 + 2)


  def buildNetwork(coreRouters: RouterContainer, aggRouters : RouterContainer,
                   edgeRouters : RouterContainer,
                   hosts: HostContainer) {
    for (pod_idx <- pods) {
      //connect the eage to each aggregate switch and make the connection within the rack
      for (edge_idx <- edge_sws_idx) {
        edgeRouters(edge_idx).id_gen(pod_idx, edge_idx, 1)
        for (host_idx <- hosts_idx) {
          hosts(host_idx - 2).id_gen(pod_idx, edge_idx, host_idx)
          val newlink = new Link(hosts(host_idx - 2), edgeRouters(edge_idx), linkspeed)
          hosts(host_idx - 2).interfacesManager.registerOutgoingLink(newlink)
          edgeRouters(edge_idx).interfacesManager.registerIncomeLink(newlink)
          GlobalDeviceManager.addNewNode(hosts(host_idx - 2).ip_addr(0), hosts(host_idx - 2))
        }
        GlobalDeviceManager.addNewNode(edgeRouters(edge_idx).ip_addr(0), edgeRouters(edge_idx))

        for (agg_idx <- agg_sws_idx) {
          aggRouters(agg_idx - podnum / 2).id_gen(pod_idx, agg_idx, 1)
          val newlink = new Link(edgeRouters(edge_idx), aggRouters(agg_idx - podnum / 2), linkspeed)
          aggRouters(agg_idx - podnum / 2).interfacesManager.registerIncomeLink(newlink)
          edgeRouters(edge_idx).interfacesManager.registerOutgoingLink(newlink)
          GlobalDeviceManager.addNewNode(aggRouters(agg_idx - podnum / 2).ip_addr(0),
            aggRouters(agg_idx - podnum / 2))
        }
      }

      for (agg_idx <- agg_sws_idx) {
        val c_index = agg_idx - podnum / 2 + 1
        for (core_idx <- core_sws_idx) {
          coreRouters(core_idx - 1).id_gen(pod_idx, c_index, 1)
          val newlink = new Link(coreRouters(core_idx), aggRouters(agg_idx - podnum / 2),
            linkspeed)
          aggRouters(agg_idx - podnum / 2).interfacesManager.registerOutgoingLink(newlink)
          coreRouters(core_idx - 1).interfacesManager.registerOutgoingLink(newlink)
          GlobalDeviceManager.addNewNode(coreRouters(core_idx - 1).ip_addr(0),
            coreRouters(core_idx - 1))

        }
      }
    }
  }
}

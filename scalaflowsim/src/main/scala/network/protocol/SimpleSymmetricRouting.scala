package network.protocol

import network.topo._
import network.data.Flow
import network.topo.ToRRouter
import scala.util.hashing.Hashing


class SimpleSymmetricRouting (router : Router) extends RoutingProtocol (router) {
  def nextNode(flow: Flow): Node = {

    def getCellID(flow : Flow) : String = {
      val t : String = flow.DstIP.substring(flow.DstIP.indexOf('.') + 1, flow.DstIP.size)
      t.substring(0, t.indexOf('.'))
    }

    def selectRandomOutlink(flow : Flow) : Link = {
      val selectidx = Math.max(flow.DstIP.hashCode(), flow.DstIP.hashCode() * -1) % router.outlink.size
      var i = 0
      var l : Link = null
      for (l <- router.outlink) {
        if (i == selectidx) break
        i = i + 1
      }
      l
    }

    var nexthopNode : Node = null
    var outlink : Link = null
    val dstRange = flow.DstIP.substring(0, flow.DstIP.lastIndexOf('.') + 1) + ".1"
    val localRange = router.ip_addr(0).substring(0, router.ip_addr(0).lastIndexOf('.') + 1) + ".1"
    val dstCellID = getCellID(flow)

    nexthopNode = router.routertype match {
      case ToRRouter => {
        if (dstRange == localRange) {
          if (router.inLinks.contains(flow.DstIP)) {
            //in the same lan
            outlink = router.inLinks.get(flow.DstIP).get
            nexthopNode = outlink.end_from
          }
          else {
            //send through arbitrary outlinks to aggregate layer
            outlink =
          }
        }
      }
    }
   /* try {
      if (routertype == null) throw new Exception("unindicated router type");

      NFSNode nexthopNode = null;
      NFSLink outgoingPath = null;
      String dstCrange = flow.dstipString.substring(0, flow.dstipString.lastIndexOf(".")) + ".0";
      String localCrange = this.ipaddress.substring(0, this.ipaddress.lastIndexOf(".")) + ".0";
      //get the building tag
      //get the later 3 segment
      String dstlater3seg = flow.dstipString.substring(flow.dstipString.indexOf(".") + 1,
        flow.dstipString.length());
      String dstbuildingTag = dstlater3seg.substring(0, dstlater3seg.indexOf("."));
      if (routertype.equals(RouterType.Edge)) {
        if (dstCrange.equals(localCrange))
        {
          //in the same lan
          if (lanLinks.containsKey(flow.dstipString)) {
            outgoingPath = lanLinks.get(flow.dstipString);
            nexthopNode = outgoingPath.src;
          }
        }
        else{
          //send through arbitrary outlinks to distribution layer
          outgoingPath = flowscheduler.schedule(flow);
          nexthopNode = outgoingPath.dst;
        }
      }
      else {
        if (routertype.equals(RouterType.Distribution)) {
          String locallater3seg = this.ipaddress.substring(this.ipaddress.indexOf(".") + 1,
            this.ipaddress.length());
          String localbuildingTag = locallater3seg.substring(0, locallater3seg.indexOf("."));
          if (dstbuildingTag.equals(localbuildingTag)) {
            //local query
            if (lanLinks.containsKey(dstCrange)) {
              outgoingPath = lanLinks.get(dstCrange);
              nexthopNode = outgoingPath.src;
            }
          }
          else {
            //send through arbitrary link to the core
            outgoingPath = flowscheduler.schedule(flow);
            nexthopNode = outgoingPath.dst;
          }
        }
        else {
          //Must be core
          //local query
          ArrayList<NFSLink> potentialLinks = new ArrayList<NFSLink>();
          for (String link: lanLinks.keySet()) {
            String linklater3seg = link.substring(link.indexOf(".") + 1, link.length());
            String linkbuildingTag = linklater3seg.substring(0, linklater3seg.indexOf("."));
            if (linkbuildingTag.equals(dstbuildingTag)) {
              potentialLinks.add(lanLinks.get(link));
            }
          }
          if (potentialLinks.size() != 0) {
            int selectedIdx = flow.dstipString.hashCode() % potentialLinks.size();
            selectedIdx = selectedIdx > 0 ? selectedIdx : -selectedIdx;
            outgoingPath = potentialLinks.get(selectedIdx);
            nexthopNode = outgoingPath.src;
          }
          else {
            //send out
            outgoingPath = (NFSLink) outLinks.get(0);
            nexthopNode = outgoingPath.dst;
          }
        }
      }
      if (nexthopNode == null) {
        String pathStr = "";
        for (int i = 0; i < flow.getPaths().size(); i++) {
          pathStr += (flow.getPaths().get(i).getName() + " ");
        }
        throw new Exception("could not find ip: " + flow.dstipString +
          " Router Type:" + this.routertype.toString() + " Router IP:" +
          this.ipaddress + " " + pathStr);
      }
      //update involved the objects
      outgoingPath.addRunningFlow(flow);
      flow.addPath(outgoingPath);
      return nexthopNode;
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }  */
  }
}

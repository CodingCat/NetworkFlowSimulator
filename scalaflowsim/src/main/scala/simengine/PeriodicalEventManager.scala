package simengine

/**
 * this class is used to merge the virtual time and wall-clock time
 * mainly used to periodically report counters to controller
 *
 * the basic idea is, this class inserts period events (virtual time) and
 * indicate which types of counter are reported, then the routers send statistical
 * messages to the controller,
 */
object PeriodicalEventManager {

  def run(startT: Int, endT: Int) {

  }
}

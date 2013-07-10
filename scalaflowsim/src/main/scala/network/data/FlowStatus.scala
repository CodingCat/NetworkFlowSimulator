package network.data


abstract class FlowStatus

case object NewStartFlow extends FlowStatus
case object RunningFlow extends FlowStatus
case object ChangingRateFlow extends FlowStatus
case object CompletedFlow extends  FlowStatus
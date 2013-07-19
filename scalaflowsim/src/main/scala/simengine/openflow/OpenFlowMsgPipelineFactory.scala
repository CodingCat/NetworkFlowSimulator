package simengine.openflow

import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory}

class OpenFlowMsgPipelineFactory extends ChannelPipelineFactory {
  def getPipeline: ChannelPipeline = {
    val p = Channels.pipeline()
    p.addLast("msg decoder", new OpenFlowMsgDecoder)
    p.addLast("msg handler", new OpenFlowChannelHandler)
    p
  }
}

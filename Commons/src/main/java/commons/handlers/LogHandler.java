package commons.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class LogHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)  {
        System.out.println("LogHandler принял это - " + msg.toString());
        ctx.fireChannelRead(msg);
    }

}

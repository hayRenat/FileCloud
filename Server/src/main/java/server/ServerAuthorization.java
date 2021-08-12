package server;

import commons.AuthMessage;
import commons.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ServerAuthorization extends SimpleChannelInboundHandler<Message> {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println("ServerAuthorization is active");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("ServerAuthorization is close");
    }


    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        System.out.println("Читаю сообщение на хендлере Авторизации");
        if (msg instanceof AuthMessage) {
            String username = SQLHandler.sendAuth(((AuthMessage) msg).getLogin(), ((AuthMessage) msg).getPassword());
            if (username != null) {
                if (!((AuthMessage) msg).getAutorized()){
                    if (username.equals(((AuthMessage) msg).getLogin())) {
                        ((AuthMessage) msg).setAutorized(true);
                        ctx.pipeline().remove(this);
                        ctx.pipeline().addLast(new ServerHandler(((AuthMessage) msg).getLogin()));
                        ctx.writeAndFlush(msg);
                    } else {
                        ((AuthMessage) msg).setAutorized(false);
                        ctx.writeAndFlush(msg).sync();
                    }
                    } else ctx.fireChannelRead(msg);
            } else {
                ((AuthMessage) msg).setAutorized(false);
                ctx.writeAndFlush(msg).sync();
            }
        }
    }
}


package server;

import commons.AuthMessage;
import commons.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ServerAuthorization extends SimpleChannelInboundHandler<Message> {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println("server.ServerAuthorization is active");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("server.ServerAuthorization is close");
    }


    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        System.out.println("Читаю сообщение на хендлере Авторизации");
        if (msg instanceof AuthMessage) {
            System.out.println("1 шаг");//временно
            String username = SQLHandler.sendAuth(((AuthMessage) msg).getLogin(), ((AuthMessage) msg).getPassword());
            System.out.println("БД ВЕРНУЛ это - " + username);//временно
            System.out.println("2 шаг");//временно
            if (username != null) {
                    if (username.equals(((AuthMessage) msg).getLogin())) {
                        System.out.println("Верный логин/пароль");//временно
                        ((AuthMessage) msg).setAutorized(true);
                        System.out.println(msg);
                        ((AuthMessage) msg).getLogin();
                        ctx.pipeline().remove(this);
                        ctx.pipeline().addLast(new ServerHandler());
                        ctx.writeAndFlush(msg);
                        System.out.println(ctx);
                        System.out.println(ctx.channel().isRegistered());
//                        System.out.println(ctx.channel);
                    } else {
                        ((AuthMessage) msg).setAutorized(false);
                        ctx.writeAndFlush(msg).sync();
                        System.out.println("Не верный логин/пароль с БД");//временно
                    }
            } else {
                ((AuthMessage) msg).setAutorized(false);
                ctx.writeAndFlush(msg).sync();
                System.out.println("Не верный логин/пароль с БД");//временно
            }
        }    else {
            ctx.fireChannelRead(msg);
            System.out.println("Принял - " + msg.getClass());
        }
    }
}


package client;

import commons.AuthMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

public class ClientHandler extends SimpleChannelInboundHandler {

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("Сработал ClientHandler");
        System.out.println("Принял это сообщение - " + msg);
        if (msg instanceof AuthMessage){
            if (((AuthMessage) msg).getAutorized()){
                System.out.println("Пользователь авторизован");//временно
                Main.controller.setAuthorized();
                Main.controller.updatingTheClientFileList();
                System.out.println(ctx);
//                ctx.channel().flush();
//                System.out.println(ctx);
//                msg = new java.commons.SyncronizedClientFromServer(Main.controller.getFileList());
//                ctx.writeAndFlush(msg);
            } else {
                System.out.println("Пользователь не авторизован!");
                Main.controller.setAlertmsg("Не верный логин/пароль. Введите данные снова");
                ReferenceCountUtil.release(msg);
            }
        }
    }
}


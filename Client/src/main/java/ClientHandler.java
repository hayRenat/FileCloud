import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.SocketAddress;

public class ClientHandler extends SimpleChannelInboundHandler<String> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        System.out.println("Сработал ClientHandler");
        System.out.println("Принял это сообщение - " + msg);
        if (msg.startsWith("/auth")){
            String[] subStrings = msg.split(" ", 2);
            String authorization = subStrings[1];
            if (authorization.equals("OK")){
                Controller.setAuthorized();
                System.out.println("Пользователь авторизован");//временно
                ctx.flush();
            } else {
                System.out.println("Пользователь не авторизован!");
                ctx.close();
            }
        }
    }
}

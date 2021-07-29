import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ServerAuthorization extends SimpleChannelInboundHandler<String> {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println("Client is active");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("Client is close");
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg.startsWith("/auth")) {
            String[] subStrings = msg.split(" ", 3);
            System.out.println("1 шаг");//временно
                String password = SQLHandler.sendAuth(subStrings[1], subStrings[2]);
            System.out.println("БД ВЕРНУЛ это - " + password);//временно
                System.out.println("2 шаг");//временно
                if (password.equals(subStrings[2])){
                    System.out.println("Верный логин/пароль");//временно
                    msg = "/auth OK";
                    System.out.println(msg);
                    ctx.writeAndFlush(msg);
                }else {
                    msg = "/auth OFF";
                    ctx.writeAndFlush(msg).sync();
                    System.out.println("Не верный логин/пароль с БД");//временно
                }
 //         ctx.pipeline().remove(this);
 //         ctx.pipeline().addLast(new ServerHandler());
            }
        }
    }

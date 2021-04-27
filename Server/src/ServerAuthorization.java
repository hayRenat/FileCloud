import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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
            System.out.println("1 шаг");
                String nickFromDB = SQLHandler.getNickByLoginAndPassword(subStrings[1], subStrings[2]);
                System.out.println("2 шаг");
                if (nickFromDB != null) {
                    ctx.pipeline().remove(this);
                    ctx.pipeline().addLast(new ServerHandler());
                    System.out.println("3 шаг");
                    ctx.writeAndFlush("authok");
                }
            }
        }
    }

package server;

import commons.handlers.JsonDecoder;
import commons.handlers.JsonEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

public class Server {
    private final int port;

    public static void main(String[] args) throws InterruptedException {
        new Server(9000).start();
    }

    public Server(int port) {
        this.port = port;
        SQLHandler.connect();
    }
    private void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.AUTO_READ, true)
                    .childHandler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel channel) {
                            channel.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4),
                                    new LengthFieldPrepender(4),
                                    new ByteArrayDecoder(),
                                    new ByteArrayEncoder(),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
//                                    new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.weakCachingConcurrentResolver(null)), // Декодировщик с максимальной размером обьекта
//                                    new ObjectEncoder(), //Кодировщик, который сериализует объект Java в файл ByteBuf.
//                                    new LineBasedFrameDecoder(256),
//                                    new StringDecoder(),
//                                    new StringEncoder(),
                                    new ServerAuthorization() // хендлер авторизации
                            );}
                    });
            ChannelFuture future = serverBootstrap.bind(port).sync();
            System.out.println("server.Server UP");
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


}

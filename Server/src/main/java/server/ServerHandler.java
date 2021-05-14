package server;

import commons.FileInfo;
import commons.SyncronizedClientFromServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServerHandler extends SimpleChannelInboundHandler{
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("server.ServerHandler active");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("server.ServerHandler inActive");
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("server.ServerHandler принял сообщение"); //временно
        System.out.println(msg);
        if (msg instanceof SyncronizedClientFromServer) {
            List<FileInfo> serverList = new ArrayList<>(updateClientListFromServer(Paths.get("C:\\Cloud\\server.Server\\")));
            System.out.println(serverList.size());
            List<FileInfo> clientList = ((SyncronizedClientFromServer) msg).getClientList();
            System.out.println("Файлов на сервере - " + serverList.size() + ", Файлов на клиенте" + clientList.size());
        }
    }

    private List<FileInfo> updateClientListFromServer(Path path) {
        //Обновление списка файлов клиента на сервере
        List<FileInfo> list = new ArrayList<>();
        try {
            list = Files.walk(path)
                    .map(FileInfo::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }
}

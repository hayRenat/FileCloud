package server;

import commons.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServerHandler extends SimpleChannelInboundHandler<Message> {

    private String defaultPath = "C:\\Cloud\\Server\\";
    private final String clientLogin;
    private final static int BUFFER_SIZE = 1024 * 512;

    public ServerHandler(String clientLogin) {
        this.clientLogin = clientLogin;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("ServerHandler active");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("ServerHandler inActive");
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message msg){
        if (msg instanceof SyncronizedClientFromServer) {
            List<FileInfo> serverList = new ArrayList<>(updateClientListFromServer(Paths.get(defaultPath + clientLogin)));
            List<FileInfo> clientList = ((SyncronizedClientFromServer) msg).getFileInfoList();
            System.out.println("Файлов на сервере - " + serverList.size() + ", Файлов на клиенте - " + clientList.size());
            ctx.writeAndFlush(new SyncronizedClientFromServer(serverList));

        }
        if (msg instanceof FileMessage) {
            FileMessage fileMessage = (FileMessage) msg;
            readAndWriteFile(fileMessage);
        }
        if (msg instanceof DownloadFileMessage){
            List<FileInfo> serverList = new ArrayList<>(updateClientListFromServer(Paths.get(defaultPath + clientLogin)));
            for (FileInfo sl: serverList){
                readAndUploadFile(ctx, sl);
            }
        }
    }

    private void readAndWriteFile (FileMessage fileMessage) {
        File file = new File(defaultPath + fileMessage.getPath());
        if (fileMessage.getFileType() == FileInfo.FileType.DIRECTORY) {
            file.mkdir();
        } else if (fileMessage.getEndPos() == 0) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    System.out.println("Ошибка при создании пустого файла - " + e);
                }
            } else {
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) { //rw - читает и записывает - качаю на сервер
                    byte[] bytes = fileMessage.getBytes();
                    randomAccessFile.seek(fileMessage.getStarPos());
                    randomAccessFile.write(bytes);
                } catch (IOException e) {
                    System.out.println("Ошибка при загрузке файла на сервер - " + e);
                }
            }
            System.out.println("Загрузка файла завершена");
        }

    private List<FileInfo> updateClientListFromServer(Path path) {
        //Обновление списка файлов пользователя на сервере
        List<FileInfo> list = new ArrayList<>();
        if (!path.toFile().exists()){
            path.toFile().mkdir();
        }
            try {
                list = Files.walk(path)
                        .map(FileInfo::new)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                System.out.println("Ошибка в updateClientListFromServer - " + e);
            }
        return list;
    }

    private void readAndUploadFile(ChannelHandlerContext ctx, FileInfo fileInfo) {
        if (fileInfo.getType() == FileInfo.FileType.DIRECTORY || fileInfo.getSize()==0){
            FileMessage fileMessage = new FileMessage(fileInfo);
            ctx.writeAndFlush(fileMessage);
        } else {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(Paths.get(fileInfo.getPathFile()).toFile(), "r")) { //  r- read - копирую на сервер
                long length = fileInfo.getSize();
                long pointer = randomAccessFile.getFilePointer();
                long available = length - pointer;
                while (available > 0) {
                    byte[] bytes;
                    if (available > BUFFER_SIZE) {
                        bytes = new byte[BUFFER_SIZE];
                    } else {
                        bytes = new byte[((int) available)];
                    }
                    randomAccessFile.read(bytes);
                    FileMessage fileMessage = new FileMessage(fileInfo);
                    fileMessage.setStarPos(pointer);
                    fileMessage.setBytes(bytes);
                    while (true) {
                        if (ctx.channel().isWritable()) {
                            ctx.writeAndFlush(fileMessage);
                            break;
                        } else {
                            Thread.sleep(10);
                            System.out.println("Pause break");
                            break;
                        }
                    }
                    pointer = randomAccessFile.getFilePointer();
                    available = length - pointer;
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Ошибка при записи файла - " + e);
            }
        }
    }

    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    public String getClientLogin() {
        return clientLogin;
    }
}

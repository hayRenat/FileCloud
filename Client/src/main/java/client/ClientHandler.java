package client;

import commons.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class ClientHandler extends SimpleChannelInboundHandler<Message> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        System.out.println("Сработал ClientHandler");
        System.out.println("Принял это сообщение - " + msg);
        if (msg instanceof AuthMessage) {
            if (((AuthMessage) msg).getAutorized()) {
                Main.controller.setAuthorized(((AuthMessage) msg).getLogin());
                Main.controller.updatingTheClientFileList();
                System.out.println(msg);
                ctx.flush();
            } else {
                System.out.println("Пользователь не авторизован!");
                Main.controller.setAlertmsg("Не верный логин/пароль. Введите данные снова");
                ReferenceCountUtil.release(msg);
            }
        }
        if (msg instanceof SyncronizedClientFromServer) {
            List<FileInfo> serverList = ((SyncronizedClientFromServer) msg).getFileInfoList();
            List<FileInfo> clientList = Main.controller.getFileList();
            String result = "Файлов на сервере - " + serverList.size() + "\n" + "Файлов на клиенте - " + clientList.size() + "\n";
            Main.controller.setSyncDialog(result);
        }
        if (msg instanceof FileMessage) {
            FileMessage fileMessage = (FileMessage) msg;
            readAndWriteFile(fileMessage);
        }
    }

    private void readAndWriteFile(FileMessage fileMessage) {
        String defaultPath = Main.controller.getDefaultPath();
        File file = new File(defaultPath + fileMessage.getPath());
        if (fileMessage.getFileType() == FileInfo.FileType.DIRECTORY) {
            file.mkdir();
        }
        else if (fileMessage.getEndPos() == 0) {
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
}


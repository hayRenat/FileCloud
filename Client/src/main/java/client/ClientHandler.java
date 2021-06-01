package client;

import commons.AuthMessage;
import commons.FileInfo;
import commons.FileMessage;
import commons.SyncronizedClientFromServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.List;

public class ClientHandler extends SimpleChannelInboundHandler {

    long seekposfile = 0;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("Сработал ClientHandler");
        System.out.println("Принял это сообщение - " + msg);
        if (msg instanceof AuthMessage){
            if (((AuthMessage) msg).getAutorized()){
                System.out.println("Пользователь авторизован");//временно
                Main.controller.setAuthorized(((AuthMessage) msg).getLogin());
                Main.controller.updatingTheClientFileList();
                System.out.println(msg.toString());
                ctx.flush();
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
        if (msg instanceof SyncronizedClientFromServer){
            List<FileInfo> serverList = ((SyncronizedClientFromServer) msg).getFileInfoList();
            List<FileInfo> clientList = Main.controller.getFileList();
            SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String result = "Файлов на сервере - " + serverList.size() + "\n" + "Файлов на клиенте - " + clientList.size() + "\n";
            Main.controller.setSyncDialog(result);
//            if (serverList.size()!= clientList.size()){
//                for (FileInfo sl : serverList) {
//                    for (FileInfo cl : clientList) {
//                        if (sl.getFilename().equals(cl.getFilename())&&sl.getSize()!=cl.getSize()){
//                            if (sl.getSize()>cl.getSize())
//                            result.concat("Файл с наименованием - " + sl.getFilename() + "на сервере, имеет больший размер чем на клиента, на - "  + (sl.getSize()-cl.getSize()) + " байт");
//                            else result.concat("Файл с наименованием - " + cl.getFilename() + "на сервере, имеет больший размер чем на клиента, на - "  + (cl.getSize() - sl.getSize()) + " байт");
//                        }
//                    }
//                }
//            }
        }
        if (msg instanceof FileMessage){
            FileMessage fileMessage = (FileMessage) msg;
            readAndWriteFile(fileMessage);
        }
    }
    private void readAndWriteFile (FileMessage fileMessage) {
        String defaultPath = Main.controller.getDefaultPath();
        File file = new File(defaultPath + fileMessage.getPath());
        System.out.println(defaultPath + fileMessage.getPath());
        if (fileMessage.getFileType() == FileInfo.FileType.DIRECTORY) {
            System.out.println("Создание папки");
            file.mkdir();
        } else if (fileMessage.getEndPos() == 0) {
            try {
                System.out.println("Создание пустого файла");
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("Ошибка при создании пустого файла - " + e);
            }
        } else {
            if (file.exists()||file.lastModified()>fileMessage.getLastModifed()){
                file.delete();
            }
            System.out.println(file.getName()); //временно
            System.out.println("Проверка существования файла - " + file.exists());//временные проверки
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) { //rw - читает и записывает - качаю на сервер
                System.out.println("Проверка существования файла после рандома - " + file.exists());//временные проверки
                byte[] bytes = fileMessage.getBytes();
                if (fileMessage.getEndPos() == 0) {
                    System.out.println("Файл пустой");
                    randomAccessFile.seek(seekposfile);
                    randomAccessFile.write(bytes);
                    seekposfile += bytes.length;
                    System.out.println("seekposfile - " + seekposfile);
                } else {
                    if (file.length() != fileMessage.getEndPos()) {
                        System.out.println("Файл существет");
                        randomAccessFile.seek(seekposfile);
                        randomAccessFile.write(bytes);
                        seekposfile += bytes.length;
                        if (seekposfile == fileMessage.getEndPos()) {
                            seekposfile = 0;
                        }
                        System.out.println("seekposfile - " + seekposfile);
                    }
                }
//        randomAccessFile.close();
            } catch (IOException e) {
                System.out.println("Ошибка при загрузке файла на сервер - " + e);;
            }
        }
        System.out.println("Загрузка файла завершена");
    }
}


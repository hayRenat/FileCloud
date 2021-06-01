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
    private String clentLogin;
    private final static int BUFFER_SIZE = 1024 * 512;

    public ServerHandler(String clentLogin) {
        this.clentLogin = clentLogin;
    }

    public void setClentLogin(String clentLogin) {
        this.clentLogin = clentLogin;
    }

    long seekposfile = 0;

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
    public void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        System.out.println("ServerHandler принял сообщение"); //временно
        System.out.println(msg); // временно
        if (msg instanceof SyncronizedClientFromServer) {
            List<FileInfo> serverList = new ArrayList<>(updateClientListFromServer(Paths.get(defaultPath + clentLogin)));
            List<FileInfo> clientList = ((SyncronizedClientFromServer) msg).getFileInfoList();
            System.out.println("Файлов на сервере - " + serverList.size() + ", Файлов на клиенте - " + clientList.size());
            ctx.writeAndFlush(new SyncronizedClientFromServer(serverList));
//            for (FileInfo x : serverList) {
//                System.out.println(x.getFilename());
//                System.out.println(x.getPathFile());
//            }
////            serverList.remove(0);
////            clientList.remove(0);
//            if (serverList.size() == 0) {
//                // на сервере нет файлов клиента
//
//                ctx.writeAndFlush(msg);
//            }
//            HashMap<FileInfo, String> differences = new HashMap<>();

//            SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            String result = "Дата последней синфхранизации файлов - " + (dtf.format(serverList.get(0).getLastModified())) + "\n"+ "Файлов на сервере - " + serverList.size() + ", Файлов на клиенте - " + clientList.size() + "\n";
//            if (serverList.size()!= clientList.size()){
//                for (FileInfo sl : serverList) {
//                    for (FileInfo cl : clientList) {
//                        if (sl.getFilename().equals(cl.getFilename())&&sl.getSize()!=cl.getSize()){
//                            if (sl.getSize()>cl.getSize())
//                            result.concat("Файл с наименованием - " + sl.getFilename() + "на сервере, имеет больший размер чем на клиента, на - "  + (sl.getSize()-cl.getSize()) + " байт");
//                            else result.concat("Файл с наименованием - " + cl.getFilename() + "на сервере, имеет больший размер чем на клиента, на - "  + (cl.getSize() - sl.getSize()) + " байт");
//                        } if
//                    }
//                }
//            }
        }
        if (msg instanceof FileMessage) {
//            System.out.println("ServerHandler принял сообщение - FileMessage"); //временно
            FileMessage fileMessage = (FileMessage) msg;
//            File file = new File(path + fileMessage.getFile_md5());
////            System.out.println("Проверка существования файла - " + file.exists());//временные проверки
//            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw"); //rw - читает и записывает - качаю на сервер
////            System.out.println("Проверка существования файла после рандома - " + file.exists());//временные проверки
//            byte[] bytes = fileMessage.getBytes();
//            if (file.length() == 0) {
//                System.out.println("Файл пустой");
//                randomAccessFile.seek(seekposfile);
//                randomAccessFile.write(bytes);
//                seekposfile += bytes.length;
//                System.out.println("seekposfile - " + seekposfile);
//            } else {
//                if (file.length() != fileMessage.getEndPos()) {
//                    System.out.println("Файл существет");
//                    randomAccessFile.seek(seekposfile);
//                    randomAccessFile.write(bytes);
//                    seekposfile += bytes.length;
//                    if (seekposfile == fileMessage.getEndPos()) seekposfile = 0;
//                    System.out.println("seekposfile - " + seekposfile);
//                }
//            }
//            System.out.println("Загрузка файла завершена");
//            randomAccessFile.close();
            readAndWriteFile(fileMessage);
        }
        if (msg instanceof DownloadFileMessage){
            List<FileInfo> serverList = new ArrayList<>(updateClientListFromServer(Paths.get(defaultPath + clentLogin)));
            for (FileInfo sl: serverList){
                readAndUploadFile(ctx, sl);
            }
        }
    }

    private void readAndWriteFile (FileMessage fileMessage) {

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
            System.out.println("Директория или пустой файл");
            FileMessage fileMessage = new FileMessage(fileInfo);
            System.out.println(fileMessage.toString());
            ctx.writeAndFlush(fileMessage);
        } else {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(Paths.get(fileInfo.getPathFile()).toFile(), "r")) { //  r- read - копирую на сервер
                long lenght = fileInfo.getSize();
                long pointer = randomAccessFile.getFilePointer();
                long available = lenght - pointer;
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
                        }
                    }
                    pointer = randomAccessFile.getFilePointer();
                    available = lenght - pointer;

                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Ошибка при записи файла - " + e);;
            }
        }
    }
}

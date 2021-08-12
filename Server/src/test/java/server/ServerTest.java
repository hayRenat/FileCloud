package server;


import commons.DownloadFileMessage;
import commons.FileInfo;
import commons.FileMessage;
import commons.SyncronizedClientFromServer;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ServerTest {
    private EmbeddedChannel channel;
    private ServerHandler serverHandler;
    private final static int BUFFER_SIZE = 1024 * 512;

    @Before
    public void setUp() {
        serverHandler = new ServerHandler("test");
        channel = new EmbeddedChannel(
                serverHandler);
    }

    @Ignore
    @Test
    public void SyncronizedClientFromServer() { //синхронизация файлов на клиенте и сервере
        serverHandler.setDefaultPath("src/test/resources/Test1/Cloud/");
        List<FileInfo> clientlist = updateList(Paths.get("src/test/resources/Test1/ClientFile/" + serverHandler.getClientLogin()));

        SyncronizedClientFromServer sync1 = new SyncronizedClientFromServer(clientlist);
        channel.writeInbound(sync1);

        SyncronizedClientFromServer sync2 = channel.readOutbound();

        Assert.assertTrue(sync1.getFileInfoList().size()<sync2.getFileInfoList().size());

    }

    @Test
    public void readAndUploadFile(){ //загрузка файла с сервера на клиент
        serverHandler.setDefaultPath("src/test/resources/Test2/Cloud/");
        DownloadFileMessage downloadFileMessage = new DownloadFileMessage();
        //отправка запроса на загрузку с сервера
        channel.writeInbound(downloadFileMessage);
        //приём файлов на клиенте
        readAndWriteFile(channel.readOutbound());
        readAndWriteFile(channel.readOutbound());

        File file = new File("src/test/resources/Test2/ClientFile/test/Java1.docx");
        Assert.assertTrue(file.exists());
    }

    @Test
    public void readAndWriteFile() throws IOException { //загрузка файла с клиента на сервер
        serverHandler.setDefaultPath("src/test/resources/Test3/Cloud/test/");
        FileInfo fileInfo = new FileInfo(Path.of("src/test/resources/OldCalculator.exe"));

        readAndUploadFile(fileInfo);

        channel.readInbound();

        File file = new File("src/test/resources/Test3/Cloud/test/OldCalculator.exe");
        Assert.assertTrue(file.exists());


    }

    private List<FileInfo> updateList(Path path) {
        List<FileInfo> list = null;
        try {
            list = Files.walk(path)
                    .map(FileInfo::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println(e);
        }
        return list;
    }

    private void readAndUploadFile(FileInfo fileInfo) throws IOException {
        if (fileInfo.getType() == FileInfo.FileType.DIRECTORY || fileInfo.getSize()==0){
            FileMessage fileMessage = new FileMessage(fileInfo);
            channel.writeInbound(fileMessage);
        } else {
            RandomAccessFile randomAccessFile = new RandomAccessFile(Paths.get(fileInfo.getPathFile()).toFile(), "r"); //  r- read - копирую на сервер
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
                    channel.writeInbound(fileMessage);
                    pointer = randomAccessFile.getFilePointer();
                    available = lenght - pointer;
                }
            }
        }

    private void readAndWriteFile(FileMessage fileMessage) {
        String defaultPath = "src/test/resources/Test2/ClientFile/";
        String pathFile = pathToFileClient(fileMessage.getPath());
        File file = new File(defaultPath + pathFile);
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
        System.out.println("Загрузка файла завершена - test");
    }
    private String pathToFileClient(String path){
        String[] userPath = path.split("\\\\", 3);
        String userPathToFile = userPath[2];
        return userPathToFile;
    }
    }

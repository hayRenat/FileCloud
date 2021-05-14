package client;

import commons.*;
import commons.handlers.JsonDecoder;
import commons.handlers.JsonEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    private Channel channel;
    private BooleanProperty connected = new SimpleBooleanProperty(false);
    private Boolean authorized;
    private List<FileInfo> fileList;

    public List<FileInfo> getFileList() {
        return fileList;
    }

    @FXML
    TextField loginField;
    @FXML
    HBox authPanel, workedplace;
    @FXML
    PasswordField passField;
    @FXML
    ProgressIndicator piStatus;
    @FXML
    Label lblStatus;
    @FXML
    TableView<FileInfo> filesTable;



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        authorized = false;
        start();
    }

    public void start() {
        Task<Channel> task = new Task<Channel>() {
            @Override
            protected Channel call() throws Exception {

                //сообщение для прогрессбара
                updateMessage("Устанавливается соеденение");
                updateProgress(0.1d, 1.0d);

                NioEventLoopGroup group = new NioEventLoopGroup();
                Bootstrap bootstrap = new Bootstrap()
                        .group(group)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(
                                        new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4),
                                        new LengthFieldPrepender(4),
                                        new ByteArrayDecoder(),
                                        new ByteArrayEncoder(),
                                        new JsonDecoder(),
                                        new JsonEncoder(),
//                                        new ObjectEncoder(), //Кодировщик, который сериализует объект Java в файл ByteBuf.
//                                        new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.weakCachingConcurrentResolver(null)), // Декодировщик с максимальной размером обьекта
                                        new ClientHandler());
                            }
                        });
                //временное сообщение для меня
                System.out.println("Client started");

                ChannelFuture channelFuture = bootstrap.connect("localhost", 9000);
                channel = channelFuture.channel();

                //сообщение для прогрессбара
                updateMessage("Соединение установлено");
                updateProgress(1.0d, 1.0d);

                channelFuture.sync();
                return channel;
            }

            @Override
            protected void succeeded() {
                //временное сообщение для меня
                System.out.println("succeeded в старте сработал");

                channel = getValue();
                connected.set(true);
            }

            @Override
            protected void failed() {
                connected.set(false);
                //временное сообщение для меня
                System.out.println("failed сработал");
            }
        };
        lblStatus.textProperty().bind(task.messageProperty());
        piStatus.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
    }

    public void sendAuth() {
        // нажатие кнопки для авторизации
        System.out.println("пошла sendAuth");//временно
//        start();
        if (!connected.get())
            return;

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                channel.writeAndFlush(new AuthMessage(loginField.getText(),passField.getText()));
                System.out.println(channel);
                return null;
            }
            @Override // временная попытка
            protected void succeeded(){
                System.out.println("Succeeded в аутентификации");
            }

            @Override
            protected void failed() {
                connected.set(false);
                System.out.println("failed сработал");
            }
        };
        lblStatus.textProperty().bind(task.messageProperty());
        piStatus.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
    }

    public void setAuthorized() {
        authorized = true;
        System.out.println("Авторизация - " + authorized.toString());
        Platform.runLater(()->{
            authPanel.setVisible(false);
            workedplace.setVisible(true);
            filesTable.setVisible(true);
            setDesktopClient();
        });
    }

    private void setDesktopClient() {
        if (authorized) {
            //Заполняем таблицу для отображения файлов
            //Столбец тип файла
            TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>("Тип");
            fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
            fileTypeColumn.setPrefWidth(40);

            //столбец имя файла
            TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
            filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
            filenameColumn.setPrefWidth(240);

            //столбец размер файла
            TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
            fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
            fileSizeColumn.setCellFactory(column -> {
                return new TableCell<FileInfo, Long>() {
                    @Override
                    protected void updateItem(Long item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                            setStyle("");
                        } else {
                            String text = String.format("%,d bytes", item);
                            if (item == -1L) {
                                text = "[DIR]";
                            }
                            setText(text);
                        }
                    }
                };
            });
            fileSizeColumn.setPrefWidth(120);

            //столбец Дата изменения
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
            fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
            fileDateColumn.setPrefWidth(120);

            //добаляем столбцы в таблицу
            filesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
            //сортировка по типу файла (папка/файл)
            filesTable.getSortOrder().add(fileTypeColumn);
            //патч папки с файлами
            updateListWindow(Paths.get("C:\\Cloud\\Client"));
        } else System.out.println("Не удалось поменять рабочий стол");
    }

    public void updatingTheClientFileList() {
        fileList = updateList(Paths.get("C:\\Cloud\\Client\\"));
        System.out.println("Количество записей в Листе - " + fileList.size());
//            int subFolders = 0;
//        List<java.commons.FileInfo> subf = new ArrayList<>();
//        for (java.commons.FileInfo x : fileList) {
//            if (x.getType() == java.commons.FileInfo.FileType.DIRECTORY) {
//                List<java.commons.FileInfo> buff;
//                try {
//                    buff = Files.list(Paths.get("C:\\Cloud\\Client" + "\\" + x.getFilename())).map(java.commons.FileInfo::new).collect(Collectors.toList());
//                    System.out.println(buff.size());
//                    subf.addAll(buff);
//                    buff.clear();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        fileList.addAll(subf);
//        subf = null;
//            List subFoldersList = new ArrayList();
//            System.out.println(subFolders);
//            for (int i = 0; i < subFolders; i++) {
//                subFoldersList.add(i, new ArrayList<java.commons.FileInfo>());
//            }
        // кусок кода на кнопку (заносим в лист файлы подпапок)
//            fileList.stream()
//                    .filter(x -> x.getType() == java.commons.FileInfo.FileType.DIRECTORY)
//                    .forEach(x -> updateSubFolders(fileList, Paths.get("C:\\Cloud\\Client"), x));
//        List<java.commons.FileInfo> filetest = new ArrayList<>();
//        filetest = updateList(filetest,Paths.get("C:\\Cloud\\Client"));
//        System.out.println(filetest.size());

        //Временная проверка наполнения  списка файлов
        for (FileInfo x : fileList) {
            System.out.println(x.getFilename());
            System.out.println(x.getPathFile());
        }
//        System.out.println("Количество записей в Листе - " + fileList.size());
    }

    private List<FileInfo> updateList(Path path){
        //Обновление списка файлов на Клиенте
        List<FileInfo> list = null;
        try {
            list = Files.walk(path)
                    .map(FileInfo::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            setAlertmsg("Не удалось обновить список файлов по пути - " + path);
        }
        return list;
    }

    private void updateListWindow(Path path) {
        //обновление таблицы файлов UI
        try {
            filesTable.getItems().clear();
            filesTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            filesTable.sort();
        } catch (IOException e) {
            //выскакивающее окно с ошибкой
            Alert alert = new Alert(Alert.AlertType.WARNING, "По какой-то причине не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

//    private void updateList(Path path){
//        try {
//            fileList = Files.list(path).map(java.commons.FileInfo::new).collect(Collectors.toList());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    private void updateSubFolders(List<java.commons.FileInfo> fileList, Path path, java.commons.FileInfo fileInfo) {
//        try {
//            String pathsubfolder = path + "\\" + fileInfo.getFilename();
//        Files.list(Path.of(pathsubfolder)).map(java.commons.FileInfo::new).forEach(fileList::add);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void setAlertmsg(String alertmsg) {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.ERROR, alertmsg, ButtonType.OK);
            alert.showAndWait();
        });
    }

    public void syncronizedClientFromServer(){
        System.out.println("запуск syncronizedClientFromServer");
        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                channel.writeAndFlush(new SyncronizedClientFromServer(fileList));
                System.out.println(channel.read());
                return null;
            }
            @Override // временная попытка
            protected void succeeded(){
                System.out.println("Succeeded syncronizedClientFromServer");
            }

            @Override
            protected void failed() {
                connected.set(false);
                System.out.println("failed syncronizedClientFromServer");
            }
        };
        new Thread(task).start();
    }
}

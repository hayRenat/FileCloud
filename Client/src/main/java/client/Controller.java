package client;

import commons.*;
import commons.handlers.JsonDecoder;
import commons.handlers.JsonEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    private final static int BUFFER_SIZE = 1024 * 512;
    private Channel channel;
    private BooleanProperty connected = new SimpleBooleanProperty(false);
    private Boolean authorized;
    private List<FileInfo> fileList;
    private String login;
//    private RandomAccessFile randomAccessFile;
    private String defaultPath = "C:\\Cloud\\Client\\";

    public String getDefaultPath() {
        return defaultPath;
    }

    public List<FileInfo> getFileList() {
        return fileList;
    }

    @FXML
    TextField loginField, pathField;
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
//        while (!authorized){
//            authDialog();
//        }
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
                        .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024 * 5, 1024 * 1024 * 10))
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
//                                        new LogOUTbandHandler(),
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
                ChannelFuture f = channel.writeAndFlush(new AuthMessage(loginField.getText(), passField.getText()));
                f.sync();
                System.out.println(channel);
                return null;
            }

            @Override // временная попытка
            protected void succeeded() {
                System.out.println("Succeeded в аутентификации");
            }

            @Override
            protected void failed() {
                connected.set(false);
                System.out.println("failed сработал");
            }
        };
//        lblStatus.textProperty().bind(task.messageProperty());
//        piStatus.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
    }

    public void setAuthorized(String clientLogin) {
        authorized = true;
        login = clientLogin;
        System.out.println("Авторизация - " + authorized.toString()); //Авторизация - true/false
        Platform.runLater(() -> {
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
            SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
            fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(dtf.format(param.getValue().getLastModified())));
            fileDateColumn.setPrefWidth(120);

            //добаляем столбцы в таблицу
            filesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
            //сортировка по типу файла (папка/файл)
            filesTable.getSortOrder().add(fileTypeColumn);
            //патч папки с файлами
            updateListWindow(Paths.get(defaultPath + login));

            filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    if (mouseEvent.getClickCount() == 2){
                        Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
                        if (Files.isDirectory(path)) {
                            updateListWindow(path);
                        }
                    }
                }
            });

        } else System.out.println("Не удалось поменять рабочий стол");
    }

    public void updatingTheClientFileList() {
        fileList = updateList(Paths.get(defaultPath + login));
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

    private List<FileInfo> updateList(Path path) {
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
            pathField.setText(path.normalize().toAbsolutePath().toString());
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
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, alertmsg, ButtonType.OK);
            alert.showAndWait();
        });
    }

    public void setSyncDialog(String infomsg){
        Platform.runLater(()->{
            System.out.println("setSyncDialog");
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            ButtonType buttonTypeCopy = new ButtonType("Копировать на сервер");
            ButtonType buttonTypeDownload = new ButtonType("Копировать с сервера");
            ButtonType buttonTypeCancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(buttonTypeCopy, buttonTypeDownload, buttonTypeCancel);
            alert.setTitle("Пожалуйста, выберите действие");
            alert.setHeaderText("Ниже приведена информация о различиях...");
            alert.setContentText(infomsg);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == buttonTypeCopy){
                copyFileToServer();
            }else if (result.get() == buttonTypeDownload){
                copyFileToClient();
            }
        });
    }

    public void syncronizedClientFromServer() {
        System.out.println("запуск syncronizedClientFromServer");
        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() {
                fileList = updateList(Paths.get(defaultPath + login));
                updateListWindow(Paths.get(defaultPath + login));
                ChannelFuture f = channel.writeAndFlush(new SyncronizedClientFromServer(fileList));
                return null;
            }

            @Override // временная попытка
            protected void succeeded() {
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

    public void copyFileToServer() {
        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
//                System.out.println("Кнопка - копировать на сервер");//временно
//                List<FileMessage> fileMessage = new ArrayList<>();
//                for (int i = 0; i < fileList.size(); i++) {
//                    fileMessage.add(new FileMessage());
//                }
                for (FileInfo fl: fileList){
                    readAndUploadFile(fl);
                }
//                FileInfo fileInfo = new FileInfo(Paths.get("C:\\Cloud\\Client\\openjdk-8u41-b04-windows-i586-14_jan_2020.zip"));

//                randomAccessFile = new RandomAccessFile(fileMessage.getFile(), "r"); //  r- read - копирую на сервер
//                long lenght = fileMessage.getFile().length();
//                long pointer = randomAccessFile.getFilePointer();
//                long available = lenght - pointer;
//                while (available > 0) {
//                    byte[] bytes;
//                    if (available > BUFFER_SIZE) {
//                        bytes = new byte[BUFFER_SIZE];
//                    } else {
//                        bytes = new byte[((int) available)];
//                    }
//                    randomAccessFile.read(bytes);
//                    FileMessage fileMessage1 = new FileMessage();
//                    fileMessage1.setFile_md5(fileMessage.getFile_md5());
//                    fileMessage1.setStarPos(pointer);
//                    fileMessage1.setBytes(bytes);
//                    while (true) {
//                        if (channel.isWritable()) {
//                            channel.writeAndFlush(fileMessage1);
//                            break;
//                        } else {
//                            Thread.sleep(10);
//                        }
//                    }
//                    pointer = randomAccessFile.getFilePointer();
//                    available = lenght - pointer;
//                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void readAndUploadFile(FileInfo fileInfo) {
        if (fileInfo.getType() == FileInfo.FileType.DIRECTORY || fileInfo.getSize()==0){
            System.out.println("Директория или пустой файл");
            FileMessage fileMessage = new FileMessage(fileInfo);
            System.out.println(fileMessage.toString());
            channel.writeAndFlush(fileMessage);
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
                        if (channel.isWritable()) {
                            channel.writeAndFlush(fileMessage);
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

    public void btnUP(ActionEvent actionEvent){
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null){
            updateListWindow(upperPath);
        }
    }

    @FXML
    private void copyFileToClient() {
        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() {
                channel.writeAndFlush(new DownloadFileMessage());
                return null;
            }
        };
        new Thread(task).start();
    }

//    public void authDialog(){
//        // Create the custom dialog.
//        Dialog<Pair<String, String>> dialog = new Dialog<>();
//        dialog.setTitle("Авторизация");
//        dialog.setHeaderText("Пожалуйста, введите логин и пароль");
//
//// Set the icon (must be included in the project).
////        dialog.setGraphic(new ImageView(this.getClass().getResource("login.png").toString()));
//
//// Set the button types.
//        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
//        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
//
//// Create the username and password labels and fields.
//        GridPane grid = new GridPane();
//        grid.setHgap(10);
//        grid.setVgap(10);
//        grid.setPadding(new Insets(20, 150, 10, 10));
//
//        TextField username = new TextField();
//        username.setPromptText("Username");
//        PasswordField password = new PasswordField();
//        password.setPromptText("Password");
//
//        grid.add(new Label("Username:"), 0, 0);
//        grid.add(username, 1, 0);
//        grid.add(new Label("Password:"), 0, 1);
//        grid.add(password, 1, 1);
//
//// Enable/Disable login button depending on whether a username was entered.
//        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
//        loginButton.setDisable(true);
//
//// Do some validation (using the Java 8 lambda syntax).
//        username.textProperty().addListener((observable, oldValue, newValue) -> {
//            loginButton.setDisable(newValue.trim().isEmpty());
//        });
//
//        dialog.getDialogPane().setContent(grid);
//
//// Request focus on the username field by default.
//        Platform.runLater(() -> username.requestFocus());
//
//// Convert the result to a username-password-pair when the login button is clicked.
//        dialog.setResultConverter(dialogButton -> {
//            if (dialogButton == loginButtonType) {
//                return new Pair<>(username.getText(), password.getText());
//            }
//            return null;
//        });
//
//        Optional<Pair<String, String>> result = dialog.showAndWait();
//
//        result.ifPresent(usernamePassword -> System.out.println("Username=" + usernamePassword.getKey() + ", Password=" + usernamePassword.getValue()));
//        Task<Void> task = new Task<Void>() {
//            @Override
//            protected Void call() {
//                result.ifPresent(usernamePassword -> channel.writeAndFlush(new AuthMessage(usernamePassword.getKey(), usernamePassword.getValue())));
//                return null;
//            }
//    };
//        new Thread(task).start();
//}
}

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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
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
import java.lang.*;

public class Controller implements Initializable {

    private final static int BUFFER_SIZE = 1024 * 512;
    private Channel channel;
    private final BooleanProperty connected = new SimpleBooleanProperty(false);
    private Boolean authorized;
    private List<FileInfo> fileList;
    private String login;
    private final String defaultPath = "C:\\Cloud\\Client\\";

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
    }

    public void start() {
        Task<Channel> task = new Task<>() {
            @Override
            protected Channel call() throws Exception {

                //?????????????????? ?????? ????????????????????????
                updateMessage("?????????????????????????????? ????????????????????");
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
                                        new ClientHandler());
                            }
                        });

                ChannelFuture channelFuture = bootstrap.connect("localhost", 9000);
                channel = channelFuture.channel();

                //?????????????????? ?????? ????????????????????????
                updateMessage("???????????????????? ??????????????????????");
                updateProgress(1.0d, 1.0d);

                channelFuture.sync();
                return channel;
            }

            @Override
            protected void succeeded() {

                channel = getValue();
                connected.set(true);
            }

            @Override
            protected void failed() {
                connected.set(false);
            }
        };
        lblStatus.textProperty().bind(task.messageProperty());
        piStatus.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
    }

    public void sendAuth() {
        // ?????????????? ???????????? ?????? ??????????????????????
        if (!connected.get())
            return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ChannelFuture f = channel.writeAndFlush(new AuthMessage(loginField.getText(), passField.getText()));
                f.sync();
                System.out.println(channel);
                return null;
            }

            @Override // ?????????????????? ??????????????
            protected void succeeded() {
                System.out.println("Succeeded ?? ????????????????????????????");
            }

            @Override
            protected void failed() {
                connected.set(false);
                System.out.println("failed ????????????????");
            }
        };
        new Thread(task).start();
    }

    public void setAuthorized(String clientLogin) {
        authorized = true;
        login = clientLogin;
        Platform.runLater(() -> {
            authPanel.setVisible(false);
            workedplace.setVisible(true);
            filesTable.setVisible(true);
            setDesktopClient();
        });
    }

    private void setDesktopClient() {
        if (authorized) {
            //?????????????????? ?????????????? ?????? ?????????????????????? ????????????
            //?????????????? ?????? ??????????
            TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>("??????");
            fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
            fileTypeColumn.setPrefWidth(40);

            //?????????????? ?????? ??????????
            TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("??????");
            filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
            filenameColumn.setPrefWidth(240);

            //?????????????? ???????????? ??????????
            TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("????????????");
            fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
            fileSizeColumn.setCellFactory(column -> new TableCell<>() {
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
            });
            fileSizeColumn.setPrefWidth(120);

            //?????????????? ???????? ??????????????????
            SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("???????? ??????????????????");
            fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(dtf.format(param.getValue().getLastModified())));
            fileDateColumn.setPrefWidth(120);

            //???????????????? ?????????????? ?? ??????????????
            filesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
            //???????????????????? ???? ???????? ?????????? (??????????/????????)
            filesTable.getSortOrder().add(fileTypeColumn);
            //???????? ?????????? ?? ??????????????
            updateListWindow(Paths.get(defaultPath + login));

            filesTable.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getClickCount() == 2){
                    Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
                    if (Files.isDirectory(path)) {
                        updateListWindow(path);
                    }
                }
            });

        } else System.out.println("???? ?????????????? ???????????????? ?????????????? ????????");
    }

    public void updatingTheClientFileList() {
        fileList = updateList(Paths.get(defaultPath + login));
    }

    private List<FileInfo> updateList(Path path) {
        //???????????????????? ???????????? ???????????? ???? ??????????????
        List<FileInfo> list = null;
        try {
            list = Files.walk(path)
                    .map(FileInfo::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            setAlertmsg("???? ?????????????? ???????????????? ???????????? ???????????? ???? ???????? - " + path);
        }
        return list;
    }

    private void updateListWindow(Path path) {
        //???????????????????? ?????????????? ???????????? UI
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            filesTable.getItems().clear();
            filesTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            filesTable.sort();
        } catch (IOException e) {
            //?????????????????????????? ???????? ?? ??????????????
            Alert alert = new Alert(Alert.AlertType.WARNING, "???? ??????????-???? ?????????????? ???? ?????????????? ???????????????? ???????????? ????????????", ButtonType.OK);
            alert.showAndWait();
        }
    }

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
            ButtonType buttonTypeCopy = new ButtonType("???????????????????? ???? ????????????");
            ButtonType buttonTypeDownload = new ButtonType("???????????????????? ?? ??????????????");
            ButtonType buttonTypeCancel = new ButtonType("????????????", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(buttonTypeCopy, buttonTypeDownload, buttonTypeCancel);
            alert.setTitle("????????????????????, ???????????????? ????????????????");
            alert.setHeaderText("???????? ?????????????????? ???????????????????? ?? ??????????????????...");
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
        System.out.println("???????????? syncronizedClientFromServer");
        Task<Void> task = new Task<>() {

            @Override
            protected Void call() {
                fileList = updateList(Paths.get(defaultPath + login));
                updateListWindow(Paths.get(defaultPath + login));
                channel.writeAndFlush(new SyncronizedClientFromServer(fileList));
                return null;
            }

            @Override // ?????????????????? ??????????????
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
        Task<Void> task = new Task<>() {

            @Override
            protected Void call() {
                for (FileInfo fl : fileList) {
                    readAndUploadFile(fl);
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void readAndUploadFile(FileInfo fileInfo) {
        if (fileInfo.getType() == FileInfo.FileType.DIRECTORY || fileInfo.getSize()==0){
            FileMessage fileMessage = new FileMessage(fileInfo);
            channel.writeAndFlush(fileMessage);
        } else {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(Paths.get(fileInfo.getPathFile()).toFile(), "r")) { //  r- read - ?????????????? ???? ????????????
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
                System.out.println("???????????? ?????? ???????????? ?????????? - " + e);
            }
        }
    }

    public void btnUP(){
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null){
            updateListWindow(upperPath);
        }
    }

    @FXML
    private void copyFileToClient() {
        Task<Void> task = new Task<>() {

            @Override
            protected Void call() {
                channel.writeAndFlush(new DownloadFileMessage());
                return null;
            }
        };
        new Thread(task).start();
    }
}

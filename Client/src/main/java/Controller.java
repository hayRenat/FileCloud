import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private ChannelFuture channelFuture;
    private static Channel channel;
    private BooleanProperty connected = new SimpleBooleanProperty(false);
    private static Boolean authorized;

    @FXML
    TextField loginField;
    @FXML
    VBox mainBox;
    @FXML
    HBox authPanel;
    @FXML
    PasswordField passField;
    @FXML
    ProgressIndicator piStatus;
    @FXML
    Label lblStatus;

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
                                        new StringEncoder(),
                                        new StringDecoder(),
                                        new ClientHandler());
                            }
                        });
                //временное сообщение для меня
                System.out.println("Client started");

                ChannelFuture channelFuture = bootstrap.connect("localhost", 9000);
                Channel channel = channelFuture.channel();

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
                System.out.println("посылаю данные о " + loginField.getText() + " " + passField.getText());
                ChannelFuture f = channel.writeAndFlush("/auth " + loginField.getText() + " " + passField.getText() + "\n").sync();
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
        //здесь мне нужна какая то пауза пока authorized не будет true
//        while ()
        setWorkDesktop();
    }

    public static void setAuthorized() {
        authorized = true;
        System.out.println("Авторизация - " + authorized.toString());

    }

    void setWorkDesktop() {
        if (authorized) {
            authPanel.setVisible(false);
            authPanel.setManaged(false);
        } else System.out.println("Не удалось поменять рабочий стол");
    }
}

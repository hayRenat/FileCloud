package client;

import commons.FileMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

//основа для хендлера по закачке файлов на сервер

public class FileUploadClientHandler extends ChannelInboundHandlerAdapter {
    private int byteRead;
    private volatile int start = 0;
    private volatile int lastLength = 0;
    public RandomAccessFile randomAccessFile;
    private FileMessage fileMessage;

    public FileUploadClientHandler(FileMessage ef) {
        if (ef.getFile().exists()) {
            if (!ef.getFile().isFile()) {
                System.out.println("Not a file :" + ef.getFile());
                return;
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    public void channelActive(ChannelHandlerContext ctx) {
        try {
            randomAccessFile = new RandomAccessFile(fileMessage.getFile(),
                    "r");
            randomAccessFile.seek(fileMessage.getStarPos());
            // lastLength = (int) randomAccessFile.length() / 10;
            lastLength = 1024 * 10;
            byte[] bytes = new byte[lastLength];
            if ((byteRead = randomAccessFile.read(bytes)) != -1) {
                fileMessage.setEndPos(byteRead);
                fileMessage.setBytes(bytes);
                ctx.writeAndFlush(fileMessage); // отправкао сообщения на сервер, содержание сообщения, объект java.commons.FileUploadFile содержит: файл, имя файла, начальную позицию, массив байтов файла, конечную позицию
            } else {
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof Integer) {
            start = (Integer) msg;
            if (start != -1) {
                randomAccessFile = new RandomAccessFile(fileMessage.getFile(), "r");
                randomAccessFile.seek(start);
                int a = (int) (randomAccessFile.length() - start);
                int b = (int) (randomAccessFile.length() / 1024 * 2);
                if (a < lastLength) {
                    lastLength = a;
                }
                byte[] bytes = new byte[lastLength];
                if ((byteRead = randomAccessFile.read(bytes)) != -1 && (randomAccessFile.length() - start) > 0) {
                    fileMessage.setEndPos(byteRead);
                    fileMessage.setBytes(bytes);
                    try {
                        ctx.writeAndFlush(fileMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    randomAccessFile.close();
                    ctx.close();
                }
            }
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


}

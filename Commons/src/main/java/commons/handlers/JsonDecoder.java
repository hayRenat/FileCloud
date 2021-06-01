package commons.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.io.IOException;
import java.util.List;

public class JsonDecoder extends MessageToMessageDecoder<byte[]> {
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, byte[] bytes, List<Object> list){
        try {
            Message message = null;
            message = OBJECT_MAPPER.readValue(bytes, Message.class);
            list.add(message);
        } catch (IOException e) {
            System.out.println(e);
        }

    }
}

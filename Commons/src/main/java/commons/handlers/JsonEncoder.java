package commons.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import commons.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class JsonEncoder extends MessageToMessageEncoder<Message> {
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Message message, List<Object> list) {
        byte[] bytes = new byte[0];
        try {
            bytes = OBJECT_MAPPER.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            System.out.println("Ошибка ENCODER - " + e);;
        }
        list.add(bytes);
        System.out.println("Кодирование завершенно"); // временно
    }
}

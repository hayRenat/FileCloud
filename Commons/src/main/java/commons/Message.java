package commons;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "id"
)
@JsonSubTypes(
        value = {
                @JsonSubTypes.Type(value = AuthMessage.class, name = "AuthMessage"),
                @JsonSubTypes.Type(value = FileInfo.class, name = "FileInfo"),
                @JsonSubTypes.Type(value = FileMessage.class, name = "FileUploadFile"),
                @JsonSubTypes.Type(value = SyncronizedClientFromServer.class, name = "SyncronizedClientFromServer"),
                @JsonSubTypes.Type(value = DownloadFileMessage.class, name = "DownloadFileMessage")
        }
)

public abstract class Message {
}

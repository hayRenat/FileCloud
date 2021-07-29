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
                @JsonSubTypes.Type(value = FileUploadFile.class, name = "FileUploadFile"),
                @JsonSubTypes.Type(value = SyncronizedClientFromServer.class, name = "SyncronizedClientFromServer")
        }
)

public abstract class Message {
}

package commons;

import java.util.List;

public class SyncronizedClientFromServer extends Message {
    private List<FileInfo> clientList;

    public SyncronizedClientFromServer() {
    }

    public List<FileInfo> getClientList() {
        return clientList;
    }

    public SyncronizedClientFromServer(List<FileInfo> clientList) {
        this.clientList = clientList;
    }
}

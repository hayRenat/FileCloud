package commons;

import java.util.List;

public class SyncronizedClientFromServer extends Message {
    private List<FileInfo> fileInfoList;

    public List<FileInfo> getFileInfoList() {
        return fileInfoList;
    }

    public SyncronizedClientFromServer(List<FileInfo> fileInfoList) {
        this.fileInfoList = fileInfoList;
    }

    public SyncronizedClientFromServer() {
        this.fileInfoList = null;
    }
}

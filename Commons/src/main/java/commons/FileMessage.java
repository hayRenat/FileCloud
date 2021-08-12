package commons;

import java.io.File;
import java.nio.file.Paths;

//класс для передачи закачки файла в хендлере

public class FileMessage extends Message{

    private static final long serialVersionUID = 1L;
    private File file;// Файл
    private String fileName;// наименование + расширение)
    private long starPos;// Начальная позиция для seek
    private byte[] bytes;// массив байтов
    private long endPos;// Конечная позиция для seek
    private String path; //путь к файлу без учёта дефолтного пути
    private FileInfo.FileType fileType; // тип файла (папка/файл)
    private long lastModifed; //время изменения файла


    public FileInfo.FileType getFileType() {
        return fileType;
    }

    public void setStarPos(long starPos) {
        this.starPos = starPos;
    }

    public long getStarPos() {
        return starPos;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public long getEndPos() {
        return endPos;
    }


    public String getPath() {
        return path;
    }

    public long getLastModifed() {
        return lastModifed;
    }



    public FileMessage() {
    }

    public FileMessage (FileInfo fileInfo) {
        this.file = Paths.get(fileInfo.getPathFile()).toFile();
        this.fileName = fileInfo.getFilename();
        this.endPos = fileInfo.getSize();
        this.fileType = fileInfo.getType();
        this.path = pathToFileClient(fileInfo.getPathFile());
        this.lastModifed = fileInfo.getLastModified();
    }

    private String pathToFileClient(String path){
        String[] userPath = path.split("\\\\", 4);
        String userPathToFile = userPath[3];
        return userPathToFile;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
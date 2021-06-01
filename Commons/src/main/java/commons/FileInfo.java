package commons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileInfo extends Message {
    public enum FileType {
        FILE("FILE"), DIRECTORY("DIR");

        private String name;

        public String getName() {
            return name;
        }

        FileType(String name) {
            this.name = name;
        }
    }

    private String filename;
    private FileType type;
    private long size;
    private long lastModified;
    //все изменения Дат в программе связаны с ошибкой энкодера - com.fasterxml.jackson.databind.exc.InvalidDefinitionException: Java 8 тип даты/времени `java.time.LocalDateTime` не поддерживается по умолчанию: добавьте модуль "com.fasterxml.jackson.datatype:jackson-datatype-jsr310", Добавление модуля ситуацию не меняет
    private String pathFile;

    public String getPathFile() {
        return pathFile;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public FileInfo(Path path) {
        try {
            this.filename = path.getFileName().toString();
            this.size = Files.size(path);
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE;
            if (this.type == FileType.DIRECTORY) {
                this.size = -1L;
            }
            this.lastModified = Files.getLastModifiedTime(path).toMillis();
            this.pathFile = path.toFile().getPath();
        } catch (IOException e) {
            throw new RuntimeException("Не смог создать FileInfo по пути к файлу");
        }
    }

    public FileInfo() {
    }

    public FileInfo(String filename, long size, long lastModified, String pathFile) {
        this.filename = filename;
        this.size = size;
        this.lastModified = lastModified;
        this.pathFile = pathFile;
    }
}

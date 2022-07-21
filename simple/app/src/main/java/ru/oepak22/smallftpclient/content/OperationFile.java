package ru.oepak22.smallftpclient.content;

// класс файла, преобразованнго для выполнения над ним операций
public class OperationFile {

    private long mSize;                                                                 // размер файла
    private String mName;                                                               // имя файла
    private String mSourcePath;                                                         // исходный путь к файлу
    private String mDestinationPath;                                                    // результирующий путь к файлу
    private boolean mDirectory;                                                         // файл является директорией

    // конструктор
    public OperationFile() {}

    // GET и SET имени файла
    public String getName() { return mName; }
    public void setName(String name) { mName = name; }

    // GET и SET размера файла
    public long getSize() { return mSize; }
    public void setSize(long size) { mSize = size; }

    // GET и SET исходного пути
    public String getSourcePath() { return mSourcePath; }
    public void setSourcePath(String path) { mSourcePath = path; }

    // GET и SET результирующего пути
    public String getDestinationPath() { return mDestinationPath; }
    public void setDestinationPath(String path) { mDestinationPath = path; }

    // является ли файл каталогом
    public boolean isDirectory() { return mDirectory; }
    public void setDirectory() { mDirectory = !mDirectory; }
}

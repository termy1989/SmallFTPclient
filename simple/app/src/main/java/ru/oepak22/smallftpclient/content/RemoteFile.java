package ru.oepak22.smallftpclient.content;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

// класс файла/каталога FTP
public class RemoteFile extends RealmObject {

    private String mName;                                                                   // имя файла
    private long mSize;                                                                     // размер файла
    private String mLastModified;                                                           // дата последнего изменения
    private boolean mSelect;                                                                // файл выбран
    private boolean mNavigator;                                                             // файл является ярлыком для перехода в родительский каталог
    private boolean mDirectory;                                                             // файл является каталогом

    // пустой конструктор для Realm
    public RemoteFile() {}

    // установка файла как навигационного
    public boolean isNavigator() { return mNavigator; }
    public void setNavigator() { mNavigator = !mNavigator; }

    // выбор файла/каталога
    public boolean isSelected() { return mSelect; }
    public void setSelected() { mSelect = !mSelect; }

    // проверка, является ли файл каталогом
    public boolean isDirectory() { return mDirectory; }
    public void setDirectory() { mDirectory = !mDirectory; }

    // GET и SET имени файла
    public String getName() { return mName; }
    public void setName(String name) { mName = name; }

    // GET и SET размера файла
    public long getSize() { return mSize; }
    public void setSize(long size) { mSize = size; }

    // GET и SET даты последнего изменения
    public String getLastModified() { return mLastModified; }
    public void setLastModified(String lastModified) { mLastModified = lastModified; }
}

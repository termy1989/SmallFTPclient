package ru.oepak22.smallftpclient.content;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

// класс локального файла/каталога
public class LocalFile extends RealmObject {

    private String mPath;                                                                   // полный путь к файлу
    private String mName;                                                                   // имя файла
    private long mSize;                                                                     // размер файла
    private String mLastModified;                                                           // дата последнего изменения
    private boolean mSelect;                                                                // файл выбран
    private boolean mNavigator;                                                             // файл является ярлыком для перехода в родительский каталог
    private boolean mDirectory;                                                             // файл является каталогом

    // конструктор для инициализации
    @SuppressLint("SimpleDateFormat")
    public LocalFile(@NonNull String pathname) {
        mPath = pathname;
        mName = (new File(mPath)).getName();
        mDirectory = (new File(mPath)).isDirectory();
        if (!mDirectory) mSize = (new File(mPath)).length();
        mLastModified = new SimpleDateFormat("dd MMM yyyy, HH:mm")
                                        .format(new Date((new File(mPath))
                                                            .lastModified()));
        mSelect = false;
        mNavigator = false;
    }

    // пустой конструктор для Realm
    public LocalFile() {}

    // установка файла как навигационного
    public boolean isNavigator() { return mNavigator; }
    public void setNavigator() { mNavigator = !mNavigator; }

    // выбор файла/каталога
    public boolean isSelected() { return mSelect; }
    public void setSelected() { mSelect = !mSelect; }

    // проверка, является ли файл каталогом
    public boolean isDirectory() { return mDirectory; }
    public void setDirectory() { mDirectory = !mDirectory; }

    // получение/установка имени файла
    public String getName() { return mName; }
    public void setName(String name) { mName = name; }

    // GET и SET размера файла
    public long getSize() { return mSize; }
    public void setSize(long size) { mSize = size; }

    // GET и SET даты последнего изменения
    public String getLastModified() { return mLastModified; }
    public void setLastModified(String lastModified) { mLastModified = lastModified; }

    // GET и SET полного пути до файла
    public String getPath() { return mPath; }
    public void setPath(String path) { mPath = path; }
}

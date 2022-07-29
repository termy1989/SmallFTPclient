package ru.oepak22.smallftpclient.screen.local;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.oepak22.smallftpclient.content.LocalFile;
import ru.oepak22.smallftpclient.data.FilesService;
import ru.oepak22.smallftpclient.utils.TextUtils;

// класс презентера для работы с содержимым локального каталога
public class LocalStoragePresenter {

    private final FilesService mService;                                                                // интерфейс операций с файлами
    private LocalStorageView mView;                                                                     // интерфейс презентера
    private Context mContext;
    private String mCurrentDir;                                                                         // путь до текущей директории
    private List<LocalFile> mCachedList;                                                                // список файлов для восстановления после поворота

    // конструктор
    public LocalStoragePresenter(@NonNull LocalStorageView view, FilesService filesService,
                                                                Context context, String path) {
        mView = view;
        mService = filesService;
        mContext = context;
        mCurrentDir = path;
        mCachedList = null;
    }

    // обнуление презентера
    public void close() {
        mView = null;
        mContext = null;
    }

    // обновление содержимого каталога
    public void refresh(String path) {

        mCurrentDir = path;

        // восстановление списка
        if (mCachedList != null) {
            mView.showFiles(mCachedList);
            mCachedList = null;
        }

        // переход в домашнюю директорию в случае ошибки с SD
        else if (mService.isRoot(mContext, mCurrentDir)
                                && mService.isSDcard(path)
                                && !(new File(path).exists()))
            refresh("/storage/emulated/0");

        // обычное обновление
        else if (mService.isRoot(mContext, mCurrentDir)
                                || (new File(path).exists())) {
            mCachedList = mService.getListFiles(mCurrentDir);
            showFiles();
        }

        // переход на каталог выше
        else toParentDir();
    }

    // получение текущего и родительского каталогов
    public String getCurrentDir() { return mCurrentDir; }

    // кэширование списка для отображения после поворота экрана
    public void saveCache(List<LocalFile> cachedList) { mCachedList = cachedList; }

    // обработчик короткого нажатия - переход в выбранную директорию
    public void onItemClick(@NonNull LocalFile dir) {
        if (dir.isNavigator()) toParentDir();
        else if (dir.isDirectory()) refresh(dir.getPath());
    }

    // отображение содержимого текущего каталога на экране
    private void showFiles() {

        if (!mService.isRoot(mContext, mCurrentDir)) {
            LocalFile localFile = new LocalFile(mCurrentDir);
            localFile.setNavigator();
            localFile.setName(". .");

            // проверка, является ли текущий каталог пустым
            if (mCachedList == null)
                mCachedList = new ArrayList<>(Collections.singletonList(localFile));
            else
                mCachedList.add(0, localFile);
        }

        // вывод результатата (список, либо пустота) на экран
        if (mCachedList == null) mView.showEmpty();
        else {

            // сортировка (сначала каталоги, потом файлы)
            Collections.sort(mCachedList, (file1, file2) -> {
                boolean b1 = file1.isDirectory();
                boolean b2 = file2.isDirectory();
                return (b1 != b2) ? (b1) ? -1 : 1 : 0;
            });

            mView.showFiles(mCachedList);
            mCachedList = null;
        }
    }

    // переход в родительскую директорию
    public void toParentDir() {
        refresh(TextUtils.splitBySlash(mCurrentDir));
    }

    // получение выделенных элементов списка
    public List<LocalFile> getSelectedItems(@NonNull List<LocalFile> files) {
        List<LocalFile> list = new ArrayList<>();
        for (LocalFile file : files) {
            if (file.isSelected())
                list.add(file);
        }
        return list;
    }

    // снять/установить выделение всех элементов
    public List<LocalFile> selectItems(@NonNull List<LocalFile> files, boolean select) {
        List<LocalFile> list = new ArrayList<>();
        for (LocalFile file : files) {
            if (!file.isNavigator()) {
                if (select && !file.isSelected()) file.setSelected();
                else if (!select && file.isSelected()) file.setSelected();
            }
            list.add(file);
        }
        return list;
    }

    // создание нового каталога
    public boolean createNewFolder(String name) {

        return mService.mkdir(mContext, new File(mCurrentDir, name));
    }

    // переименование элемента
    public boolean renameFile(String name, @NonNull LocalFile file) {

        return mService.rename(mContext, new File(file.getPath()), new File(mCurrentDir, name));

    }
}
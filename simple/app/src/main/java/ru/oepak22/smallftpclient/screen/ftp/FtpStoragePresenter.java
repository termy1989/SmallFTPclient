package ru.oepak22.smallftpclient.screen.ftp;

import android.annotation.SuppressLint;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.oepak22.smallftpclient.data.FtpProvider;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.content.RemoteFile;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

// класс презентера для работы с содержимым хранилища FTP
public class FtpStoragePresenter {

    private FtpStorageView mView;                                                                       // интерфейс презентера
    private Subscription mSubscription;                                                                 // реактивный наблюдатель
    private String mCurrentDir;                                                                         // текущая директория
    private FTPClient mClient;                                                                          // клиент FTP
    private FtpServer mServer;                                                                          // сервер FTP
    private List<RemoteFile> mCachedList;                                                               // список файлов для восстановления после поворота

    // конструктор
    public FtpStoragePresenter(FtpStorageView view, FtpServer server, FTPClient client) {
        mView = view;
        mCachedList = null;
        mServer = server;
        if (client == null) mClient = new FTPClient();
        else mClient = client;
    }

    // обновление содержимого каталога
    public void refresh() {

        // подключение к серверу, если оно отсутствует
        if (!FtpProvider.provideServiceFTP().isActive(mClient)) {

            mSubscription = FtpProvider.provideServiceFTP()
                    .connect(mClient, mServer, mCurrentDir)
                    .doOnSubscribe(mView::showLoading)
                    .doAfterTerminate(mView::hideLoading)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(status -> {
                                if (status.equals("Success")) {

                                    // восстановление списка после поворота экрана
                                    if (mCachedList != null && !mCachedList.isEmpty()) {
                                        mView.showFiles(mCachedList);
                                        mCachedList = null;
                                    }

                                    // обновление списка
                                    else refreshConnected();//changeDir(currentPath);
                                }
                                else mView.showError(status);
                            },
                            throwable -> mView.showError(throwable.getMessage()));
        }

        // обновление списка
        else refreshConnected();
    }

    // получение содержимого текущей рабочей директории при установленном соединении
    protected void refreshConnected() {

        // определение источников получения пути к текущей директории и списка файлов в ней
        Observable<String> path = FtpProvider.provideServiceFTP()
                                                .getWorkingDirectory(mClient);
        Observable<List<FTPFile>> files = FtpProvider.provideServiceFTP()
                                                        .getFiles(mClient);

        // одновременное получение пути к текущей директории и списка файлов в ней
        mSubscription = Observable.zip(files, path, this::createStorageList)
                .doOnSubscribe(mView::showLoading)
                .doAfterTerminate(mView::hideLoading)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                            if (list != null && !list.isEmpty())
                                mView.showFiles(list);
                            else mView.showEmpty();
                        },
                        throwable -> mView.showError(throwable.getMessage()));
    }

    // формирование списка содержимого текущей рабочей директории
    @SuppressLint("SimpleDateFormat")
    protected List<RemoteFile> createStorageList(List<FTPFile> files, String path) {

        List<RemoteFile> remoteFiles = new ArrayList<>();
        mCurrentDir = path;

        // формирование ".." для перехода в предыдущую директорию,
        // если текущая директория не корневая
        if (!isRoot(path)) {
            RemoteFile remoteFile = new RemoteFile();
            remoteFile.setName(". .");
            remoteFile.setNavigator();
            remoteFile.setDirectory();
            remoteFiles.add(remoteFile);
        }

        // формирование списка содержимого текущего каталога
        if (files != null && !files.isEmpty()) {
            for (FTPFile file : files) {

                // имя файла
                RemoteFile remoteFile = new RemoteFile();
                remoteFile.setName(file.getName());

                if (!remoteFile.getName().equals(".")
                        && !remoteFile.getName().equals("..")) {

                    // аттрибуты файла
                    remoteFile.setLastModified(new SimpleDateFormat("dd MMM yyyy, HH:mm")
                                                                        .format(file.getTimestamp()
                                                                                        .getTime()));

                    // флаг каталога или размер файла
                    if (file.isDirectory())
                        remoteFile.setDirectory();
                    else remoteFile.setSize(file.getSize());

                    // добавление файла в список
                    remoteFiles.add(remoteFile);
                }
            }

            // сортировка (сначала каталоги, потом файлы)
            Collections.sort(remoteFiles, (file1, file2) -> {
                boolean b1 = file1.isDirectory();
                boolean b2 = file2.isDirectory();
                return (b1 != b2) ? (b1) ? -1 : 1 : 0;
            });
        }

        return remoteFiles;
    }

    // обработчик короткого нажатия - переход в выбранную директорию
    public void onItemClick(@NonNull RemoteFile dir) {

        // переход в предыдущую директорию
        if (dir.isNavigator()) {

            // подключение к серверу, если оно отсутствует
            if (!FtpProvider.provideServiceFTP().isActive(mClient)) {

                mSubscription = FtpProvider.provideServiceFTP()
                        .connect(mClient, mServer, mCurrentDir)
                        .doOnSubscribe(mView::showLoading)
                        .doAfterTerminate(mView::hideLoading)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(status -> {
                                    if (status.equals("Success")) toParentDir();
                                    else mView.showError(status);
                                },
                                throwable -> mView.showError(throwable.getMessage()));
            }

            // переход в родительскую директорию
            else toParentDir();
        }

        // переход в указанную директорию
        else
        {
            // подключение к серверу, если оно отсутствует
            if (!FtpProvider.provideServiceFTP().isActive(mClient)) {

                mSubscription = FtpProvider.provideServiceFTP()
                        .connect(mClient, mServer, mCurrentDir)
                        .doOnSubscribe(mView::showLoading)
                        .doAfterTerminate(mView::hideLoading)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(status -> {
                                    if (status.equals("Success")) changeDir(dir.getName());
                                    else mView.showError(status);
                                },
                                throwable -> mView.showError(throwable.getMessage()));
            }

            // переход в выбранную директорию
            else changeDir(dir.getName());
        }
    }

    // переход в выбранную директорию
    protected void changeDir(String name) {

        mSubscription = FtpProvider.provideServiceFTP().changeDirectory(mClient, name)
                .doOnSubscribe(mView::showLoading)
                .doAfterTerminate(mView::hideLoading)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> refresh(),
                        throwable -> mView.showError(throwable.getMessage()));
    }

    // переход в родительскую директорию
    protected void toParentDir() {

        mSubscription = FtpProvider.provideServiceFTP().parentDirectory(mClient)
                .doOnSubscribe(mView::showLoading)
                .doAfterTerminate(mView::hideLoading)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                            if (result) refresh();

                            // переход в корневую директорию в случае ошибки
                            else {
                                mCurrentDir = mServer.getPath();
                                rootDir();
                            }
                        },
                        throwable -> mView.showError(throwable.getMessage()));
    }

    // переход в корневую директорию
    protected void rootDir() {

        mSubscription = FtpProvider.provideServiceFTP()
                .setWorkingDirectory(mClient, mServer.getPath())
                .doOnSubscribe(mView::showLoading)
                .doAfterTerminate(mView::hideLoading)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {
                            if (status) refreshConnected();
                            else {
                                mClient = null;
                                mView.showError("Could not open root directory!");
                            }
                        },
                        throwable -> mView.showError(throwable.getMessage()));

    }

    // восстановление из кэша после поворота экрана
    public void saveCache(List<RemoteFile> cachedList) {
        mCachedList = cachedList;
    }

    // GET и SET клиента FTP
    public FTPClient getFtpClient() { return mClient; }
    public void setFtpClient(FTPClient client) {
        if (client == null) mClient = new FTPClient();
        else mClient = client;
    }

    // получение парамметров сервера
    public FtpServer getServer() { return mServer; }

    // GET и SET пути к текущей директории
    public String getCurrentDir() { return mCurrentDir; }
    public void setCurrentDir(String path) { mCurrentDir = path; }

    // проверка на выход за корневую директорию
    public boolean isRoot(@NonNull String path) { return path.equals(mServer.getPath()); }

    // постановка презентера на паузу
    public void pause() {

        // отписка от наблюдения
        if (mSubscription != null)
            mSubscription.unsubscribe();
    }

    // уничтожение презентера
    public void close() {
        mView = null;
        mServer = null;
        mClient = null;
    }

    // получение выделенных элементов списка
    public List<RemoteFile> getSelectedItems(@NonNull List<RemoteFile> files) {
        List<RemoteFile> list = new ArrayList<>();
        for (RemoteFile file : files) {
            if (file.isSelected())
                list.add(file);
        }
        return list;
    }

    // снять/установить выделение всех элементов
    public List<RemoteFile> selectItems(@NonNull List<RemoteFile> files, boolean select) {
        List<RemoteFile> list = new ArrayList<>();
        for (RemoteFile file : files) {
            if (!file.isNavigator()) {
                if (select && !file.isSelected()) file.setSelected();
                else if (!select && file.isSelected()) file.setSelected();
            }
            list.add(file);
        }
        return list;
    }

    // создание нового каталога
    public void createNewFolder(@NonNull String name) {

        // подключение к серверу, если оно отсутствует
        if (!FtpProvider.provideServiceFTP().isActive(mClient)) {

            mSubscription = FtpProvider.provideServiceFTP()
                    .connect(mClient, mServer, mCurrentDir)
                    .doOnSubscribe(mView::showLoading)
                    .doAfterTerminate(mView::hideLoading)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(status -> {
                                if (status.equals("Success")) newFolder(name);
                                else mView.showError(status);
                            },
                            throwable -> mView.showError(throwable.getMessage()));
        }

        // создание нового каталога
        else newFolder(name);
    }

    protected void newFolder(@NonNull String name) {

        if (!name.equals(".") && !name.equals("..") && !name.equals("*")) {
            mSubscription = FtpProvider.provideServiceFTP()
                    .createDirectory(mClient, name)
                    .doOnSubscribe(mView::showLoading)
                    .doAfterTerminate(mView::hideLoading)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(status -> {
                                if (status) {
                                    Toast.makeText(((Fragment) mView).requireActivity(),
                                                                    "Successfully created",
                                                                                Toast.LENGTH_SHORT)
                                                                                            .show();
                                    refresh();
                                } else mView.showError("Could not create directory!");
                            },
                            throwable -> mView.showError(throwable.getMessage()));
        }
        else mView.showError("Could not create directory!");
    }

    // переименование выбранного элемента
    public void renameFile(String oldName, String newName) {

        // подключение к серверу, если оно отсутствует
        if (!FtpProvider.provideServiceFTP().isActive(mClient)) {

            mSubscription = FtpProvider.provideServiceFTP()
                    .connect(mClient, mServer, mCurrentDir)
                    .doOnSubscribe(mView::showLoading)
                    .doAfterTerminate(mView::hideLoading)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(status -> {
                                if (status.equals("Success")) newName(oldName, newName);
                                else mView.showError(status);
                            },
                            throwable -> mView.showError(throwable.getMessage()));
        }

        // смена имени
        else newName(oldName, newName);
    }

    protected void newName(String oldName, String newName) {

        mSubscription = FtpProvider.provideServiceFTP()
                .renameFile(mClient, oldName, newName)
                .doOnSubscribe(mView::showLoading)
                .doAfterTerminate(mView::hideLoading)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {
                            if (status) {
                                Toast.makeText(((Fragment) mView).requireActivity(),
                                                                "Successfully renamed",
                                                                            Toast.LENGTH_SHORT)
                                                                                        .show();
                                refresh();
                            } else mView.showError("Could not rename item!");
                        },
                        throwable -> mView.showError(throwable.getMessage()));
    }
}
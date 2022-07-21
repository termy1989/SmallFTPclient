package ru.oepak22.smallftpclient.tasks.management.delete;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.content.OperationFile;
import ru.oepak22.smallftpclient.tasks.management.CopyMoveDeleteView;

// класс презентера для удаления файлов FTP
public class DeleteFtpPresenter {

    CopyMoveDeleteView mView;                                                                           // интерфейс удаления
    Context mContext;                                                                                   // основной контекст приложения
    FtpServer mServer;                                                                                  // параметры сервера
    List<OperationFile> mStartDeleteList;                                                               // начальный список удаляемых файлов
    List<String> mFilesList;                                                                            // итоговый список удаляемых файлов
    List<String> mFoldersList;

    // конструктор
    public DeleteFtpPresenter(CopyMoveDeleteView view, Context context,
                                        FtpServer server, List<OperationFile> list) {
        mView = view;
        mContext = context;
        mServer = server;
        mStartDeleteList = list;
    }

    // завершение работы презентера
    public void close() {
        mView = null;
        mContext = null;
        mStartDeleteList.clear();
        mFilesList.clear();
        mFoldersList.clear();
    }

    // удаление файлов - построение структуры
    public void delete() {

        // инициализация массивов
        int counter = 0;
        mFilesList = new ArrayList<>();
        mFoldersList = new ArrayList<>();

        // инициализация клиента FTP
        FTPClient client = new FTPClient();

        try {

            // соединение с сервером
            client.connect(mServer.getAddress(), mServer.getPort());

            if (FTPReply.isPositiveCompletion(client.getReplyCode())) {

                // авторизация на сервере
                if (client.login(mServer.getUsername(), mServer.getPassword())) {

                    // установка пассивного режима
                    client.enterLocalPassiveMode();

                    // обработка начального списка удаляемых файлов и каталогов
                    for (OperationFile file : mStartDeleteList) {

                        // составление списка папок
                        if (file.isDirectory())
                            createStructureForDelete(client, file.getDestinationPath());

                        // составление списка файлов
                        else {
                            mFilesList.add(file.getDestinationPath());
                            mView.showMessage("prepare (" + mFoldersList.size()
                                                                + " folders, " + mFilesList.size()
                                                                                        + " files)");
                        }
                    }

                    // удаление файлов
                    for (String file : mFilesList) {

                        if (!((AsyncTask) mView).isCancelled()) {

                            // удаление файла
                            boolean deleted = client.deleteFile(file);
                            if (deleted) {
                                Log.d("DeleteTask", "Delete the file: " + file);
                                counter++;
                            }
                            else Log.d("DeleteTask", "Can't delete the file: " + file);

                            // обновление уведомления о прогрессе
                            String[] name = file.split("/");
                            int progress = (int) ((double) (counter * 100)
                                                        / (double) (mFilesList.size()
                                                                        + mFoldersList.size()));

                            mView.showDeleteMoveProgress(counter, progress, name[name.length - 1],
                                                        mFilesList.size() + mFoldersList.size());
                        }
                    }

                    // удаление каталогов
                    for (String dir : mFoldersList) {

                        if (!((AsyncTask) mView).isCancelled()) {

                            // удаление каталога
                            boolean removed = client.removeDirectory(dir);
                            if (removed) {
                                Log.d("DeleteTask", "Delete the directory: " + dir);
                                counter++;
                            } else Log.d("DeleteTask", "Can't delete the directory: " + dir);

                            // обновление уведомления о прогрессе
                            String[] name = dir.split("/");
                            int progress = (int) ((double) (counter * 100)
                                                        / (double) (mFilesList.size()
                                                                        + mFoldersList.size()));


                            mView.showDeleteMoveProgress(counter, progress, name[name.length - 1],
                                                        mFilesList.size() + mFoldersList.size());
                        }
                    }

                    // отключение от сервера
                    client.disconnect();

                    // отправка сообщения о завершении задачи
                    mView.showCompleted(counter, mFilesList.size() + mFoldersList.size());
                }

                // ошибка авторизации
                else {
                    if (client.isConnected()) client.disconnect();
                    mView.showError("Login failed!");
                }
            }

            // ошибка соединения
            else mView.showError("Connection failed!");
        }
        catch (IOException e) {
            mView.showError(e.getMessage());
        }
    }

    // рекурсивное формирование структуры удаления
    public void createStructureForDelete(@NonNull FTPClient client,
                                                        String parentDir) throws IOException {

        // определение очередного содержимого каталога
        FTPFile[] files = client.listFiles(parentDir);
        if (files != null && files.length != 0) {
            for (FTPFile file : files) {
                if (!((AsyncTask) mView).isCancelled()) {
                    if (file.isDirectory()) createStructureForDelete(client, parentDir + "/"
                                                                                    + file.getName());
                    else {
                        mFilesList.add(parentDir + "/" + file.getName());
                        mView.showMessage("prepare (" + mFoldersList.size()
                                                            + " folders, " + mFilesList.size()
                                                                                    + " files)");
                    }
                }
            }
        }

        // сообщение о добавлении очередного элемента в список на удаление
        mView.showMessage("prepare (" + mFoldersList.size()
                                            + " folders, " + mFilesList.size()
                                                                    + " files)");

        // добавление каталога в список на удаление
        mFoldersList.add(parentDir);
    }
}

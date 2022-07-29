package ru.oepak22.smallftpclient.tasks.download;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.content.OperationFile;
import ru.oepak22.smallftpclient.data.FilesService;
import ru.oepak22.smallftpclient.utils.TextUtils;

// класс презентера для скачивания файлов
public class DownloadPresenter {

    private final FilesService mService;                                                                        // интерфейс операций с файлами
    private DownloadView mView;                                                                                 // интерфейс загрузки
    private Context mContext;                                                                                   // основной контекст приложения
    private final FtpServer mServer;                                                                            // параметры сервера
    private final List<OperationFile> mStartDownloadList;                                                       // начальный список скачиваемых файлов
    private List<OperationFile> mResultDownloadList;                                                            // результирующий список скачиваемых файлов
    private List<String> mCreateFoldersList;                                                                    // список папок для создания
    private long mTotalSize;                                                                                    // суммарный объем скачиваемой информации
    private final long mAvailableSpace;                                                                         // доступное место для скачивания
    private long mDownloadSize;                                                                                 // суммарный объем уже скачанной информации
    private int mFileCounter;                                                                                   // счетчик скачанных файлов

    // конструктор
    public DownloadPresenter(DownloadView view, FilesService filesService, Context context,
                                        FtpServer server, List<OperationFile> list, long space) {
        mView = view;
        mService = filesService;
        mContext = context;
        mServer = server;
        mStartDownloadList = list;
        mAvailableSpace = space;
    }

    // завершение работы презентера
    public void close() {
        mView = null;
        mContext = null;
        mStartDownloadList.clear();
        mResultDownloadList.clear();
        mCreateFoldersList.clear();
    }

    // скачивание файлов - построение структуры
    public void download() {

        // инициализация массивов
        mFileCounter = 0;
        mDownloadSize = 0;
        mTotalSize = 0;
        mResultDownloadList = new ArrayList<>();
        mCreateFoldersList = new ArrayList<>();

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

                    // формирование нового списка элементов для скачивания
                    for (OperationFile item : mStartDownloadList) {

                        // добавление папки в список на созданиеи и рекурсия по остальным папкам
                        if (item.isDirectory()) {
                            mCreateFoldersList.add(item.getDestinationPath());
                            mView.showMessage("prepare (" + mCreateFoldersList.size()
                                                    + " folders, " + mResultDownloadList.size()
                                                                                    + " files)");
                            createDirectoryTree(client, item.getSourcePath(),
                                                            item.getDestinationPath());
                        }

                        // добавление файла в список на скачивание
                        else {
                            mTotalSize += item.getSize();
                            mResultDownloadList.add(item);
                            mView.showMessage("prepare (" + mCreateFoldersList.size()
                                                    + " folders, " + mResultDownloadList.size()
                                                                                    + " files)");
                        }
                    }

                    // отключение от сервера
                    client.disconnect();

                    // проверка на нехватку места для скачивания
                    if (mTotalSize > mAvailableSpace)
                        mView.showError("The total size of files is more than available space. Required "
                                                        + TextUtils.bytesToString(mTotalSize - mAvailableSpace));

                    // переход к созданию папок для скачивания
                    else createDownloadFolders();
                }

                // ошибка авторизации
                else {
                    if (client.isConnected())
                        client.disconnect();
                    mView.showError("Login failed!");
                }
            }

            // ошибка соединения
            else mView.showError("Connection failed!");
        }
        catch (IOException e) {
            Log.d("DownloadTask", e.getMessage());
            mView.showError(e.getMessage());
        }
    }

    // формирование списка файлов и создание директорий
    private void createDirectoryTree(@NonNull FTPClient ftpClient,
                                     String remotePath, String localPath) throws IOException {

        // получение содержимого очередной директории
        FTPFile[] remoteFiles = ftpClient.listFiles(remotePath);

        // обработка списка файлов и каталогов очередной директории
        if (remoteFiles != null && remoteFiles.length > 0 && !((AsyncTask) mView).isCancelled()) {
            for (FTPFile remoteFile : remoteFiles) {
                if (!remoteFile.getName().equals(".")
                        && !remoteFile.getName().equals("..")
                        && !((AsyncTask) mView).isCancelled()) {

                    // формирование новых путей
                    String remoteFilePath = remotePath + "/" + remoteFile.getName();
                    String localFilePath = localPath + "/" + remoteFile.getName();

                    // добавление каталога в список на создание, переход в следующий каталог
                    if (remoteFile.isDirectory()) {
                        mCreateFoldersList.add(localFilePath);
                        mView.showMessage("prepare (" + mCreateFoldersList.size()
                                                    + " folders, " + mResultDownloadList.size()
                                                                                    + " files)");
                        createDirectoryTree(ftpClient, remoteFilePath, localFilePath);
                    }

                    // добавление файла в список на скачивание
                    else {
                        OperationFile downloadFile = new OperationFile();
                        downloadFile.setName(remoteFile.getName());
                        downloadFile.setDestinationPath(localFilePath);
                        downloadFile.setSourcePath(remoteFilePath);
                        downloadFile.setSize(remoteFile.getSize());
                        mResultDownloadList.add(downloadFile);
                        mView.showMessage("prepare (" + mCreateFoldersList.size()
                                                + " folders, " + mResultDownloadList.size()
                                                                                + " files)");
                        mTotalSize += downloadFile.getSize();
                    }
                }
            }
        }
    }

    // создание папок для скачивания
    private void createDownloadFolders() {

        // создание папок по списку (если нужно создать)
        if (mCreateFoldersList != null && !mCreateFoldersList.isEmpty()) {
            for (String path : mCreateFoldersList) {
                File newDir = new File(path);
                if (!newDir.exists() && !((AsyncTask) mView).isCancelled()) {
                    if (mService.mkdir(mContext, newDir)) {
                        Log.d("DownloadTask", "Create: " + path);
                        mView.showMessage("Create folder: " + path);
                    }
                    else {
                        Log.d("DownloadTask", "Can't create: " + path);
                        mView.showMessage("Can't create: " + path);
                    }
                }
            }
        }

        // переход к скачиванию файлов
        downloadAllFiles();
    }

    // скачивание всех файлов по списку
    private void downloadAllFiles() {

        boolean status = true;
        if (!mResultDownloadList.isEmpty()) {
            for (OperationFile file : mResultDownloadList) {

                if (!((AsyncTask) mView).isCancelled()) {
                    String downloading = downloadSingleFile(file);
                    if (!downloading.equals("Success")) {
                        mView.showError(downloading);
                        status = false;
                        break;
                    }
                }
            }
        }
        if (status) mView.showCompleted(mFileCounter, mResultDownloadList.size());
    }

    // скачивание одного файла
    private String downloadSingleFile(OperationFile file) {

        // инициализация клиента FTP
        FTPClient client = new FTPClient();

        // установка тайм-аутов
        client.setDataTimeout(10000);
        client.setConnectTimeout(10000);
        client.setDefaultTimeout(10000);

        try {

            // соединение с сервером FTP
            Log.d("DownloadTask", "Connect to FTP server...");
            client.connect(mServer.getAddress(), mServer.getPort());

            // авторизация на сервере FTP
            Log.d("DownloadTask", "Login on FTP server...");
            if (client.login(mServer.getUsername(), mServer.getPassword())) {

                // установка пассивного режима
                client.enterLocalPassiveMode();
                Log.d("DownloadTask", "Enter FTP passive mode");
                if (client.setFileType(FTP.BINARY_FILE_TYPE))
                    Log.d("DownloadTask", "Enter FTP binary file type");

                // инициализация потока чтения файла на сервере
                InputStream inputStream = client.retrieveFileStream(file.getSourcePath());
                Log.d("DownloadTask", "Open input stream for file - " + file.getName());

                if (inputStream != null && client.getReplyCode() != 550) {

                    // подготовка выходного файла и его удаление, если он уже есть
                    File downloadFile = new File(file.getDestinationPath());

                    // инициализация потока записи (файл для скачивания)
                    OutputStream outputStream;

                    mService.delete(mContext, downloadFile);
                    outputStream = mService.getOutputStream(mContext, downloadFile);

                    Log.d("DownloadTask", "Open output stream for file - " + file.getDestinationPath());

                    // чтение/запись из потока в поток
                    byte[] data = new byte[8192];
                    long singleDownload = 0;
                    int count;
                    while ((count = inputStream.read(data)) != -1
                                        && !((AsyncTask) mView).isCancelled()
                                        && downloadFile.exists()) {

                        outputStream.write(data, 0, count);
                        singleDownload += count;
                        mDownloadSize += count;

                        int progressCommon = (int) ((double) (mDownloadSize * 100)
                                                                / (double) mTotalSize);
                        int progressSingle = (int) ((double) (singleDownload * 100)
                                                                / (double) file.getSize());

                        mView.showProgress(mFileCounter, mResultDownloadList.size(),
                                                progressCommon, progressSingle, mDownloadSize,
                                                                    mTotalSize, file.getName());
                    }

                    // закрытие потоков
                    outputStream.close();
                    Log.d("DownloadTask", "Close outputStream for file - " + file.getName());
                    inputStream.close();
                    Log.d("DownloadTask", "Close inputStream for file - " + file.getName());

                    // файл удален в процессе скачивания
                    if (!downloadFile.exists()) mTotalSize -= file.getSize();

                    // увеличение числа скачанных файлов
                    else mFileCounter++;
                }
            }

            // ошибка авторизации
            else {
                Log.d("DownloadTask", "Login incorrect");
                return "Login incorrect";
            }

            // отключение от сервера
            client.disconnect();
            Log.d("DownloadTask", "Disconnect");
        }
        catch (IOException ex) {
            Log.d("DownloadTask", ex.getMessage());
            return ex.getMessage();
        }

        return "Success";
    }
}

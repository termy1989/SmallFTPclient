package ru.oepak22.smallftpclient.tasks.upload;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
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

// класс презентера для загрузки файлов
public class UploadPresenter {

    private final FilesService mService;                                                                // интерфейс операций с файлами
    private UploadView mView;                                                                           // интерфейс загрузки
    private Context mContext;                                                                           // основной контекст приложения
    private final FtpServer mServer;                                                                    // параметры сервера
    private final List<OperationFile> mStartUploadList;                                                 // начальный список загружаемых файлов
    private List<OperationFile> mResultUploadList;                                                      // результирующий список загружаемых файлов
    private List<String> mCreateFoldersList;                                                            // список папок для создания
    private long mTotalSize;                                                                            // суммарный объем загружаемой информации
    private long mUploadSize;                                                                           // суммарный объем уже загруженной информации
    private int mFileCounter;                                                                           // счетчик загруженных файлов

    // конструктор
    public UploadPresenter(UploadView view, FilesService filesService,
                            Context context, FtpServer server, List<OperationFile> list) {
        mView = view;
        mService = filesService;
        mContext = context;
        mServer = server;
        mStartUploadList = list;
    }

    // завершение работы презентера
    public void close() {
        mView = null;
        mContext = null;
        mStartUploadList.clear();
        mResultUploadList.clear();
        mCreateFoldersList.clear();
    }

    // загрузка файлов - построение структуры
    public void upload() {

        // инициализация массивов
        mFileCounter = 0;
        mUploadSize = 0;
        mTotalSize = 0;
        mResultUploadList = new ArrayList<>();
        mCreateFoldersList = new ArrayList<>();

        // формирование нового списка элементов для загрузки
        for (OperationFile item : mStartUploadList) {

            // добавление папки в список на создание и рекурсия по остальным папкам
            if (item.isDirectory()) {
                mCreateFoldersList.add(item.getDestinationPath());
                createDirectoryTree(item.getDestinationPath(), item.getSourcePath());
            }

            // добавление файла в список на загрузку
            else {
                mTotalSize += item.getSize();
                mResultUploadList.add(item);
            }
            mView.showMessage("prepare (" + mCreateFoldersList.size()
                                                + " folders, " + mResultUploadList.size()
                                                                                + " files)");
        }

        // переход к созданию папок для загрузки
        createUploadFolders();
    }

    // формирование списка файлов и создание директорий
    private void createDirectoryTree(String remotePath, String localPath) {

        // получение содержимого очередной директории
        File[] localFiles = new File(localPath).listFiles();

        // обработка списка файлов и каталогов очередной директории
        if (localFiles != null && localFiles.length > 0 && !((AsyncTask) mView).isCancelled()) {
            for (File localFile : localFiles) {
                if (!localFile.getName().equals(".")
                        && !localFile.getName().equals("..")
                        && !((AsyncTask) mView).isCancelled()) {

                    // формирование новых путей
                    String remoteFilePath = remotePath + "/" + localFile.getName();
                    String localFilePath = localPath + "/" + localFile.getName();

                    // добавление каталога в список на создание, переход в следующий каталог
                    if (localFile.isDirectory()) {
                        mCreateFoldersList.add(remoteFilePath);
                        mView.showMessage("prepare (" + mCreateFoldersList.size()
                                                    + " folders, " + mResultUploadList.size()
                                                                                    + " files)");
                        createDirectoryTree(remoteFilePath, localFilePath);
                    }

                    // добавление файла в список на загрузку
                    else {
                        OperationFile uploadFile = new OperationFile();
                        uploadFile.setName(localFile.getName());
                        uploadFile.setSourcePath(localFilePath);
                        uploadFile.setDestinationPath(remoteFilePath);
                        uploadFile.setSize(localFile.length());
                        mResultUploadList.add(uploadFile);
                        mView.showMessage("prepare (" + mCreateFoldersList.size()
                                                    + " folders, " + mResultUploadList.size()
                                                                                    + " files)");
                        mTotalSize += uploadFile.getSize();
                    }
                }
            }
        }
    }

    // создание папок на сервере, в соответствии с инфраструктурой
    private void createUploadFolders() {

        boolean status = true;

        // создание папок по списку (если нужно создать)
        if (mCreateFoldersList != null && !mCreateFoldersList.isEmpty()) {

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

                        // обработка списка каталогов, которые необходимо создать
                        for (String path : mCreateFoldersList) {

                            client.changeWorkingDirectory(path);
                            int returnCode = client.getReplyCode();
                            if (returnCode == 550 && !((AsyncTask) mView).isCancelled()) {
                                boolean created = client.makeDirectory(path);
                                if (created) {
                                    Log.d("UploadTask", "Create folder: " + path);
                                    mView.showMessage("Create folder: " + path);
                                }
                                else {
                                    Log.d("UploadTask", "Can't create folder: " + path);
                                    mView.showMessage("Can't create folder: " + path);
                                }
                            }
                        }

                        // отключение от сервера
                        client.disconnect();
                    }

                    // ошибка авторизации
                    else {
                        if (client.isConnected()) {
                            status = false;
                            client.disconnect();
                        }
                        mView.showError("Login failed");
                    }
                }
                else {
                    status = false;
                    mView.showError("Connection error");
                }
            } catch (IOException e) {
                status = false;
                Log.v("UploadTask", "Error: " + e.getMessage());
                mView.showError(e.getMessage());
            }
        }

        // загрузка файлов
        if (status) uploadAllFiles();
    }

    // загрузка файлов по списку
    private void uploadAllFiles() {

        boolean status = true;

        if (!mResultUploadList.isEmpty()) {
            for (OperationFile file : mResultUploadList) {

                if (!((AsyncTask) mView).isCancelled()) {
                    String uploading = uploadSingleFile(file);
                    if (!uploading.equals("Success")) {
                        mView.showError(uploading);
                        status = false;
                        break;
                    }
                }
            }
        }
        if (status) mView.showCompleted(mFileCounter, mResultUploadList.size());
    }

    // загрузка одного файла
    private String uploadSingleFile(OperationFile file) {

        // инициализация клиента FTP
        FTPClient client = new FTPClient();

        // установка тайм-аутов
        client.setDataTimeout(10000);
        client.setConnectTimeout(10000);
        client.setDefaultTimeout(10000);

        try {

            // соединение с сервером FTP
            Log.d("UploadTask", "Connect to FTP server...");
            client.connect(mServer.getAddress(), mServer.getPort());

            // авторизация на сервере FTP
            Log.d("UploadTask", "Login on FTP server...");
            if (client.login(mServer.getUsername(), mServer.getPassword())) {

                // установка пассивного режима
                client.enterLocalPassiveMode();
                Log.d("UploadTask", "Enter FTP passive mode");
                if (client.setFileType(FTP.BINARY_FILE_TYPE))
                    Log.d("UploadTask", "Enter FTP binary file type");

                // проверка на наличие загружаемого файла
                File uploadFile = new File(file.getSourcePath());
                if (uploadFile.exists()) {

                    // предварительное удаление файла с сервера, если уже есть
                    client.deleteFile(file.getDestinationPath());

                    // инициализация потока записи файла на сервер
                    OutputStream outputStream = client.storeFileStream(file.getDestinationPath());
                    Log.d("UploadTask", "Open output stream for file - "
                                                        + file.getDestinationPath());

                    if (outputStream != null) {

                        // инициализация потока чтения исходного файла
                        InputStream inputStream = mService.getInputStream(mContext, uploadFile);

                        if (inputStream != null) {

                            Log.d("UploadTask", "Open input stream for file - "
                                                                    + file.getSourcePath());

                            // чтение/запись из потока в поток
                            byte[] data = new byte[8192];
                            long singleDownload = 0;
                            int count;
                            while ((count = inputStream.read(data)) != -1
                                        && !((AsyncTask) mView).isCancelled()) {

                                outputStream.write(data, 0, count);
                                singleDownload += count;
                                mUploadSize += count;

                                int progressCommon = (int) ((double) (mUploadSize * 100)
                                                                    / (double) mTotalSize);
                                int progressSingle = (int) ((double) (singleDownload * 100)
                                                                    / (double) file.getSize());

                                mView.showProgress(mFileCounter, mResultUploadList.size(),
                                                    progressCommon, progressSingle, mUploadSize,
                                                                        mTotalSize, file.getName());
                            }

                            inputStream.close();
                            Log.d("UploadTask", "Close inputStream for file - "
                                                                            + file.getName());
                        }

                        // закрытие потоков
                        outputStream.close();
                        Log.d("UploadTask", "Close outputStream for file - "
                                                                        + file.getName());

                        // увеличение числа закачанных файлов
                        mFileCounter++;
                    }
                }
            }

            // ошибка авторизации
            else {
                Log.d("UploadTask", "Login incorrect");
                return "Login incorrect";
            }

            // отключение от сервера
            client.disconnect();
            Log.d("UploadTask", "Disconnect");
        }
        catch (IOException ex) {
            Log.d("UploadTask", ex.getMessage());
            return ex.getMessage();
        }

        return "Success";
    }

}

package ru.oepak22.smallftpclient.tasks;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import ru.oepak22.smallftpclient.content.Define;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.content.LocalFile;
import ru.oepak22.smallftpclient.content.OperationFile;
import ru.oepak22.smallftpclient.content.RemoteFile;
import ru.oepak22.smallftpclient.screen.main.MainActivity;
import ru.oepak22.smallftpclient.tasks.download.DownloadTask;
import ru.oepak22.smallftpclient.tasks.management.CopyMoveDeleteTask;
import ru.oepak22.smallftpclient.tasks.upload.UploadTask;

// класс фрагмента для работы с асинхронными процессами
public class TasksFragment extends Fragment {

    private DownloadTask mDownloadTask;                                                                 // задача для скачивания файлов
    private UploadTask mUploadTask;                                                                     // задача для загрузки файлов
    private CopyMoveDeleteTask mCopyMoveDeleteTask;                                                     // задача для удаления, копирования и перемещения файлов

    // создание фрагмента
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);                                                                        // удержание выполняемых задач в памяти
    }

    // запуск скачивания файлов
    public void startDownload(FtpServer server, @NonNull List<RemoteFile> remoteFiles,
                                        String localPath, String remotePath, long space) {

        mDownloadTask = new DownloadTask();                                                             // инициализация экземпляра задачи
        List<OperationFile> operationFiles = new ArrayList<>();                                         // инициализация списка файлов для скачивания

        // формирование списка скачиваемых файлов
        for (RemoteFile file : remoteFiles) {

            OperationFile downloadFile = new OperationFile();                                           // инициализация скачиваемого файла
            downloadFile.setName(file.getName());                                                       // установка имя файла
            downloadFile.setDestinationPath(localPath + "/" + file.getName());                          // установка директории скачивания

            // установка исходного пути файла
            if (remotePath.equals("/")) downloadFile.setSourcePath("/" + file.getName());
            else downloadFile.setSourcePath(remotePath + "/" + file.getName());

            // установка размера файла или флага директории
            if (file.isDirectory()) downloadFile.setDirectory();
            else downloadFile.setSize(file.getSize());

            operationFiles.add(downloadFile);                                                           // добавление файла в список
        }

        // запуск процесса скачивания
        mDownloadTask.start((MainActivity) requireActivity(), server,
                                                        operationFiles, space);
    }

    // прерывание скачивания файлов
    public void stopDownload() {
        mDownloadTask.cancel(true);
    }

    // проверка активности задачи скачивания файлов
    public boolean isDownloadRunning() {

        if (mDownloadTask == null) return false;
        else {
            String stat = mDownloadTask.getStatus().toString();
            if (stat.equals("PENDING") || stat.equals("FINISHED")) return false;
            else return !mDownloadTask.isCancelled();
        }
    }

    // запуск загрузки файлов
    public void startUpload(FtpServer server, @NonNull List<LocalFile> localFiles,
                                                String localPath, String remotePath) {

        mUploadTask = new UploadTask();                                                                 // инициализация экземпляра задачи
        List<OperationFile> operationFiles = new ArrayList<>();                                         // инициализация списка файлов для загрузки

        // формирование списка загружаемых файлов
        for (LocalFile file : localFiles) {

            OperationFile uploadFile = new OperationFile();                                             // инициализация загружаемого файла
            uploadFile.setName(file.getName());                                                         // установка имя файла
            uploadFile.setSourcePath(localPath + "/" + file.getName());                                 // установка исходного пути файла

            // установка директории загрузки
            if (remotePath.equals("/")) uploadFile.setDestinationPath("/" + file.getName());
            else uploadFile.setDestinationPath(remotePath + "/" + file.getName());

            // установка размера файла или флага директории
            if (file.isDirectory()) uploadFile.setDirectory();
            else uploadFile.setSize(file.getSize());

            operationFiles.add(uploadFile);                                                             // добавление файла в список
        }

        // запуск процесса загрузки
        mUploadTask.start((MainActivity) requireActivity(), server, operationFiles);
    }

    // прерывание загрузки файлов
    public void stopUpload() {
        mUploadTask.cancel(true);
    }

    // проверка активности задачи загрузки файлов
    public boolean isUploadRunning() {

        if (mUploadTask == null) return false;
        else {
            String stat = mUploadTask.getStatus().toString();
            if (stat.equals("PENDING") || stat.equals("FINISHED")) return false;
            else return !mUploadTask.isCancelled();
        }
    }

    // запуск удаления выбранных файлов
    public void startDelete(FtpServer server, List<LocalFile> localFiles,
                                List<RemoteFile> remoteFiles, String sourcePath,
                                                                String destinationPath) {

        mCopyMoveDeleteTask = new CopyMoveDeleteTask();                                                 // инициализация экземпляра задачи
        List<OperationFile> operationFiles = new ArrayList<>();                                         // инициализация списка файлов для скачивания

        // работа с файлами на сервере
        if (server != null) {

            // формирование списка удаляемых файлов
            for (RemoteFile file : remoteFiles)
                operationFiles.add(initOperationFiles(file.getName(), sourcePath, destinationPath,
                                                                file.isDirectory(), file.getSize()));
        }

        // работа с файлами в локальном хранилище
        else {

            // формирование списка удаляемых файлов
            for (LocalFile file : localFiles)
                operationFiles.add(initOperationFiles(file.getName(), sourcePath, destinationPath,
                                                                file.isDirectory(), file.getSize()));
        }

        // запуск задачи
        mCopyMoveDeleteTask.start((MainActivity) requireActivity(), server,
                                                    operationFiles, 0, Define.DELETE_TASK);

    }

    // запуск перемещения выбранных файлов
    public void startMove(FtpServer server, List<LocalFile> localFiles,
                            List<RemoteFile> remoteFiles, String sourcePath,
                                            String destinationPath, long space) {

        mCopyMoveDeleteTask = new CopyMoveDeleteTask();                                                 // инициализация экземпляра задачи
        List<OperationFile> operationFiles = new ArrayList<>();                                         // инициализация списка файлов для скачивания

        // работа с файлами на сервере
        if (server != null) {

            // формирование списка удаляемых файлов
            for (RemoteFile file : remoteFiles)
                operationFiles.add(initOperationFiles(file.getName(), sourcePath, destinationPath,
                                                                file.isDirectory(), file.getSize()));
        }

        // работа с файлами в локальном хранилище
        else {

            // формирование списка удаляемых файлов
            for (LocalFile file : localFiles)
                operationFiles.add(initOperationFiles(file.getName(), sourcePath, destinationPath,
                                                                file.isDirectory(), file.getSize()));
        }

        // запуск задачи
        mCopyMoveDeleteTask.start((MainActivity) requireActivity(), server,
                                                    operationFiles, space, Define.MOVE_TASK);
    }

    // запуск копирования выбранных файлов
    public void startCopy(FtpServer server, List<LocalFile> localFiles,
                            List<RemoteFile> remoteFiles, String sourcePath,
                                            String destinationPath, long space) {

        mCopyMoveDeleteTask = new CopyMoveDeleteTask();                                                 // инициализация экземпляра задачи
        List<OperationFile> operationFiles = new ArrayList<>();                                         // инициализация списка файлов для скачивания

        // работа с файлами на сервере
        if (server != null) {

            // формирование списка удаляемых файлов
            for (RemoteFile file : remoteFiles)
                operationFiles.add(initOperationFiles(file.getName(), sourcePath, destinationPath,
                                                                file.isDirectory(), file.getSize()));
        }

        // работа с файлами в локальном хранилище
        else {

            // формирование списка удаляемых файлов
            for (LocalFile file : localFiles)
                operationFiles.add(initOperationFiles(file.getName(), sourcePath, destinationPath,
                                                                file.isDirectory(), file.getSize()));
        }

        // запуск задачи
        mCopyMoveDeleteTask.start((MainActivity) requireActivity(), server,
                                                    operationFiles, space, Define.COPY_TASK);
    }

    // прерывание операции с файлами
    public void stopFileOperation() {
        mCopyMoveDeleteTask.cancel(true);
    }

    // проверка активности операции с файлами
    public boolean isFileOperationRunning() {

        if (mCopyMoveDeleteTask == null) return false;
        else {
            String stat = mCopyMoveDeleteTask.getStatus().toString();
            if (stat.equals("PENDING") || stat.equals("FINISHED")) return false;
            else return !mCopyMoveDeleteTask.isCancelled();
        }
    }

    // формирование файла для исполнения операций удаления, перемещения и копирования
    @NonNull
    private OperationFile initOperationFiles(String fileName, String sourcePath,
                                                String destinationPath, boolean isDirectory,
                                                                                    long size) {

        OperationFile operationFile = new OperationFile();                                              // инициализация файла
        operationFile.setName(fileName);                                                                // установка имя файла

        // установка исходной директории
        if (sourcePath != null) {
            if (sourcePath.equals("/"))
                operationFile.setSourcePath("/" + fileName);
            else
                operationFile.setSourcePath(sourcePath + "/" + fileName);
        }

        // установка целевой директории
        if (destinationPath != null) {
            if (destinationPath.equals("/"))
                operationFile.setDestinationPath("/" + fileName);
            else
                operationFile.setDestinationPath(destinationPath + "/" + fileName);
        }

        // установка размера файла или флага директории
        if (isDirectory) operationFile.setDirectory();
        else operationFile.setSize(size);
        return operationFile;
    }
}
package ru.oepak22.smallftpclient.tasks.management.copy;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import ru.oepak22.smallftpclient.content.OperationFile;
import ru.oepak22.smallftpclient.data.FilesService;
import ru.oepak22.smallftpclient.tasks.management.CopyMoveDeleteView;
import ru.oepak22.smallftpclient.utils.TextUtils;

// класс презентера для копирования локальных файлов
public class CopyLocalPresenter {

    private final FilesService mService;                                                                // интерфейс операций с файлами
    private CopyMoveDeleteView mView;                                                                   // интерфейс копирования
    private Context mContext;                                                                           // основной контекст приложения
    private final List<OperationFile> mStartCopyList;                                                   // начальный список копируемых файлов
    private List<OperationFile> mResultCopyList;                                                        // результирующий список копируемых файлов
    private List<String> mCreateFoldersList;                                                            // список папок для создания
    private long mTotalSize;                                                                            // суммарный объем копируемой информации
    private long mCopySize;                                                                             // суммарный объем уже скопированной информации
    private int mFileCounter;                                                                           // счетчик скопированных файлов
    private final long mAvailableSpace;                                                                 // доступное пространство для копирования

    // конструктор
    public CopyLocalPresenter(CopyMoveDeleteView view, FilesService filesService,
                                Context context, List<OperationFile> list, long space) {
        mView = view;
        mService = filesService;
        mContext = context;
        mStartCopyList = list;
        mAvailableSpace = space;
    }

    // завершение работы презентера
    public void close() {
        mView = null;
        mContext = null;
        mStartCopyList.clear();
        mResultCopyList.clear();
        mCreateFoldersList.clear();
    }

    // копирование файлов - построение структуры
    public void copy() {

        // инициализация массивов
        mFileCounter = 0;
        mCopySize = 0;
        mTotalSize = 0;
        mResultCopyList = new ArrayList<>();
        mCreateFoldersList = new ArrayList<>();

        // формирование нового списка элементов для копирования
        for (OperationFile item : mStartCopyList) {

            // проверка на существование элемента в целевом месте
            item.setDestinationPath(mService.changeOfExisting(item.getDestinationPath()));

            // добавление папки в список на копирование и рекурсия проход по остальным папкам
            if (item.isDirectory() && !((AsyncTask) mView).isCancelled()) {
                mCreateFoldersList.add(item.getDestinationPath());
                createDirectoryTree(item.getDestinationPath(), item.getSourcePath());
            }

            // добавление файла в список на копирование
            else if (!((AsyncTask) mView).isCancelled()) {
                mTotalSize += item.getSize();
                mResultCopyList.add(item);
            }
        }

        // проверка на нехватку места
        if (mTotalSize > mAvailableSpace)
            mView.showError("The total size of files is more than available space. Required "
                                            + TextUtils.bytesToString(mTotalSize - mAvailableSpace));

        // переход к созданию каталогов
        else createFolders();
    }

    // формирование структуры копирования
    private void createDirectoryTree(String destinationPath, String sourcePath) {

        // получение содержимого очередной директории
        File[] localFiles = new File(sourcePath).listFiles();

        // обработка списка файлов и каталогов очередной директории
        if (localFiles != null && localFiles.length > 0 && !((AsyncTask) mView).isCancelled()) {
            for (File localFile : localFiles) {
                if (!localFile.getName().equals(".")
                        && !localFile.getName().equals("..")
                        && !((AsyncTask) mView).isCancelled()) {

                    // формирование новых путей (проверка на существование по месту назначения)
                    String destinationFilePath = mService.changeOfExisting(destinationPath
                                                                            + "/" + localFile.getName());
                    String sourcePathFilePath = sourcePath + "/" + localFile.getName();

                    // добавление каталога в список на создание, переход в следующий каталог
                    if (localFile.isDirectory()) {
                        mCreateFoldersList.add(destinationFilePath);
                        createDirectoryTree(destinationFilePath, sourcePathFilePath);
                    }

                    // добавление файла в список на копирование
                    else {
                        OperationFile copyFile = new OperationFile();
                        copyFile.setName(localFile.getName());
                        copyFile.setSourcePath(sourcePathFilePath);
                        copyFile.setDestinationPath(destinationFilePath);
                        copyFile.setSize(localFile.length());
                        mResultCopyList.add(copyFile);
                        mTotalSize += copyFile.getSize();
                    }
                }
            }
        }
    }

    // создание папок в соответствии с инфраструктурой
    private void createFolders() {

        if (mCreateFoldersList != null && !mCreateFoldersList.isEmpty()) {
            for (String path : mCreateFoldersList) {
                File newDir = new File(path);
                if (!newDir.exists() && !((AsyncTask) mView).isCancelled()) {
                    if (mService.mkdir(mContext, newDir)) {
                        Log.d("CopyTask", "Create folder: " + path);
                        mView.showMessage("Create folder: " + path);
                    }
                    else {
                        Log.d("CopyTask", "Can't create folder: " + path);
                        mView.showMessage("Can't create folder: " + path);
                    }
                }
            }
        }

        // переход к копированию файлов
        copyAllFiles();
    }

    // копирование файлов по списку
    private void copyAllFiles() {

        boolean status = true;
        if (!mResultCopyList.isEmpty()) {
            for (OperationFile file : mResultCopyList) {
                if (!((AsyncTask) mView).isCancelled()) {
                    String copying = copySingleFile(file);
                    if (!copying.equals("Success")) {
                        mView.showError(copying);
                        status = false;
                        break;
                    }
                }
            }
        }

        // завершающее уведомление
        if (status) mView.showCompleted(mFileCounter, mResultCopyList.size());
    }

    // копирование одного файла
    private String copySingleFile(@NonNull OperationFile file) {

        // инициализация исходного и копируемого файлов
        File inputFile = new File(file.getSourcePath());
        File outputFile = new File(file.getDestinationPath());

        try {

            // проверка на наличие исходного файла
            if (inputFile.exists()) {

                // инициализация потоков
                InputStream inputStream = mService.getInputStream(mContext, inputFile);
                Log.d("CopyTask", "Open input stream for file - " + file.getSourcePath());

                OutputStream outputStream = mService.getOutputStream(mContext, outputFile);
                Log.d("CopyTask", "Open output stream for file - " + file.getDestinationPath());

                if (inputStream != null && outputStream != null) {

                    // чтение/запись из потока в поток
                    byte[] data = new byte[8192];
                    long singleDownload = 0;
                    int count;
                    while ((count = inputStream.read(data)) != -1
                            && !((AsyncTask) mView).isCancelled()) {

                        outputStream.write(data, 0, count);
                        singleDownload += count;
                        mCopySize += count;

                        int progressCommon = (int) ((double) (mCopySize * 100)
                                                            / (double) mTotalSize);
                        int progressSingle = (int) ((double) (singleDownload * 100)
                                                            / (double) file.getSize());

                        mView.showCopyMoveProgress(mFileCounter, mResultCopyList.size(),
                                                    progressCommon, progressSingle, mCopySize,
                                                                    mTotalSize, file.getName());
                    }

                    // закрытие потоков
                    outputStream.close();
                    Log.d("CopyTask", "Close outputStream for file - " + file.getName());
                    inputStream.close();
                    Log.d("CopyTask", "Close inputStream for file - " + file.getName());

                    // файл удален в процессе копирования
                    if (!outputFile.exists()) mTotalSize -= file.getSize();

                    // увеличение числа скопированных файлов
                    else mFileCounter++;
                }
            }
        }
        catch (IOException ex) {
            Log.d("CopyTask", ex.getMessage());
            return ex.getMessage();
        }

        return "Success";
    }
}

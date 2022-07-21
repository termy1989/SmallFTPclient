package ru.oepak22.smallftpclient.tasks.management.move;

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
import ru.oepak22.smallftpclient.data.FilesProvider;
import ru.oepak22.smallftpclient.tasks.management.CopyMoveDeleteView;
import ru.oepak22.smallftpclient.utils.TextUtils;

// класс презентера для перемещения локальных файлов
public class MoveLocalPresenter {

    private CopyMoveDeleteView mView;                                                                   // интерфейс перемещения
    private Context mContext;                                                                           // основной контекст приложения
    private final List<OperationFile> mStartMoveList;                                                   // начальный список перемещаемых файлов
    private List<OperationFile> mResultMoveList;                                                        // результирующий список перемещаемых файлов
    private List<String> mCreateFoldersList;                                                            // список папок для создания
    private List<String> mDeleteFoldersList;                                                            // список папок для удаления
    private long mTotalSize;                                                                            // суммарный объем перемещаемой информации
    private long mMoveSize;                                                                             // суммарный объем уже перемещенной информации
    private int mFileCounter;                                                                           // счетчик перемещенных файлов
    private final long mAvailableSpace;                                                                 // доступное пространство для перемещения

    // конструктор
    public MoveLocalPresenter(CopyMoveDeleteView view, Context context,
                                        List<OperationFile> list, long space) {

        mView = view;
        mContext = context;
        mStartMoveList = list;
        mAvailableSpace = space;
    }

    // завершение работы презентера
    public void close() {
        mView = null;
        mContext = null;
        mStartMoveList.clear();
        mResultMoveList.clear();
        mCreateFoldersList.clear();
        mDeleteFoldersList.clear();
    }

    // перемещение файлов - формирование структуры
    public void move() {

        // инициализация массивов
        mFileCounter = 0;
        mMoveSize = 0;
        mTotalSize = 0;
        mResultMoveList = new ArrayList<>();
        mCreateFoldersList = new ArrayList<>();
        mDeleteFoldersList = new ArrayList<>();

        // перемещение в зависимости от направления
        if (FilesProvider.provideFilesService()
                            .isSDcard(mStartMoveList.get(0).getSourcePath())
                && FilesProvider.provideFilesService()
                                .isSDcard(mStartMoveList.get(0).getDestinationPath()))
            moveFilesSimple();
        else if (!FilesProvider.provideFilesService()
                                .isSDcard(mStartMoveList.get(0).getSourcePath())
                    && !FilesProvider.provideFilesService()
                                        .isSDcard(mStartMoveList.get(0).getDestinationPath()))
            moveFilesSimple();
        else moveFiles();

    }

    // простое перемещение
    private void moveFilesSimple() {

        // перемещение элементов по списку
        for (OperationFile item : mStartMoveList) {

            if (!((AsyncTask) mView).isCancelled()) {

                // перемещение путем переименования
                boolean rename = FilesProvider.provideFilesService()
                                                .rename(mContext, new File(item.getSourcePath()),
                                                                new File(item.getDestinationPath()));

                if (rename) mFileCounter++;

                // уведомление о прогрессе
                int progress = (int) ((double) (mFileCounter * 100)
                                        / (double) mStartMoveList.size());
                mView.showDeleteMoveProgress(mFileCounter, progress, item.getName(),
                                                                mStartMoveList.size());
            }
        }

        // завершающее уведомление
        mView.showCompleted(mFileCounter, mStartMoveList.size());
    }

    // перемещение между внутренним хранилищем и SD
    private void moveFiles() {

        // формирование нового списка элементов для копирования
        for (OperationFile item : mStartMoveList) {

            // проверка на существование элемента в целевом месте
            item.setDestinationPath(FilesProvider.provideFilesService()
                                                    .changeOfExisting(item.getDestinationPath()));

            // добавление папки в список на копирование и рекурсия проход по остальным папкам
            if (item.isDirectory() && !((AsyncTask) mView).isCancelled()) {
                mCreateFoldersList.add(item.getDestinationPath());
                createDirectoryTree(item.getDestinationPath(), item.getSourcePath());
            }

            // добавление файла в список на копирование
            else if (!((AsyncTask) mView).isCancelled()) {
                mTotalSize += item.getSize();
                mResultMoveList.add(item);
            }
        }

        // проверка на нехватку места
        if (mTotalSize > mAvailableSpace)
            mView.showError("The total size of files is more than available space. Required "
                                            + TextUtils.bytesToString(mTotalSize - mAvailableSpace));

        // переход к созданию каталогов
        else createFolders();
    }

    // формирование структуры перемещение
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
                    String destinationFilePath = FilesProvider.provideFilesService()
                                                                .changeOfExisting(destinationPath
                                                                            + "/" + localFile.getName());
                    String sourcePathFilePath = sourcePath + "/" + localFile.getName();

                    // добавление каталога в список на создание, переход в следующий каталог
                    if (localFile.isDirectory()) {
                        mCreateFoldersList.add(destinationFilePath);
                        createDirectoryTree(destinationFilePath, sourcePathFilePath);
                    }

                    // добавление файла в список на копирование
                    else {
                        OperationFile moveFile = new OperationFile();
                        moveFile.setName(localFile.getName());
                        moveFile.setSourcePath(sourcePathFilePath);
                        moveFile.setDestinationPath(destinationFilePath);
                        moveFile.setSize(localFile.length());
                        mResultMoveList.add(moveFile);
                        mTotalSize += moveFile.getSize();
                    }
                }
            }
        }

        // добавление папки в список на удаление
        mDeleteFoldersList.add(sourcePath);
    }

    // создание папок в соответствии с инфраструктурой
    private void createFolders() {

        if (mCreateFoldersList != null && !mCreateFoldersList.isEmpty()) {
            for (String path : mCreateFoldersList) {
                File newDir = new File(path);
                if (!newDir.exists() && !((AsyncTask) mView).isCancelled()) {
                    if (FilesProvider.provideFilesService().mkdir(mContext, newDir)) {
                        Log.d("MoveTask", "Create folder: " + path);
                        mView.showMessage("Create folder: " + path);
                    }
                    else {
                        Log.d("MoveTask", "Can't create folder: " + path);
                        mView.showMessage("Can't create folder: " + path);
                    }
                }
            }
        }

        // переход к копированию файлов
        moveAllFiles();
    }

    // перемещение файлов по списку
    private void moveAllFiles() {

        boolean status = true;

        // перемещение
        if (!mResultMoveList.isEmpty()) {
            for (OperationFile file : mResultMoveList) {
                if (!((AsyncTask) mView).isCancelled()) {
                    String moving = moveSingleFile(file);
                    if (!moving.equals("Success")) {
                        mView.showError(moving);
                        status = false;
                        break;
                    }
                }
            }
        }

        // удаление папок
        for (String path : mDeleteFoldersList) FilesProvider.provideFilesService()
                                                                .delete(mContext, new File(path));

        // завершающее уведомление
        if (status) mView.showCompleted(mFileCounter, mResultMoveList.size());
    }

    // перемещение одного файла
    private String moveSingleFile(@NonNull OperationFile file) {

        // инициализация исходного и перемещаемого файлов
        File inputFile = new File(file.getSourcePath());
        File outputFile = new File(file.getDestinationPath());

        try {

            // проверка на наличие исходного файла
            if (inputFile.exists()) {

                // инициализация потоков
                InputStream inputStream = FilesProvider.provideFilesService()
                                                        .getInputStream(mContext, inputFile);
                Log.d("MoveTask", "Open input stream for file - " + file.getSourcePath());

                OutputStream outputStream = FilesProvider.provideFilesService()
                                                            .getOutputStream(mContext, outputFile);
                Log.d("MoveTask", "Open output stream for file - " + file.getDestinationPath());

                if (inputStream != null && outputStream != null) {

                    // чтение/запись из потока в поток
                    byte[] data = new byte[8192];
                    long singleDownload = 0;
                    int count;
                    while ((count = inputStream.read(data)) != -1
                            && !((AsyncTask) mView).isCancelled()) {

                        outputStream.write(data, 0, count);
                        singleDownload += count;
                        mMoveSize += count;

                        int progressCommon = (int) ((double) (mMoveSize * 100)
                                / (double) mTotalSize);
                        int progressSingle = (int) ((double) (singleDownload * 100)
                                / (double) file.getSize());

                        mView.showCopyMoveProgress(mFileCounter, mResultMoveList.size(),
                                                    progressCommon, progressSingle, mMoveSize,
                                                                    mTotalSize, file.getName());
                    }

                    // закрытие потоков
                    outputStream.close();
                    Log.d("MoveTask", "Close outputStream for file - " + file.getName());
                    inputStream.close();
                    Log.d("MoveTask", "Close inputStream for file - " + file.getName());

                    // файл удален в процессе перемещения
                    if (!outputFile.exists()) mTotalSize -= file.getSize();

                    // увеличение числа перемещенных файлов
                    else mFileCounter++;

                    // удаление файла
                    FilesProvider.provideFilesService().delete(mContext, inputFile);
                }
            }
        }
        catch (IOException ex) {
            Log.d("MoveTask", ex.getMessage());
            return ex.getMessage();
        }

        return "Success";
    }
}

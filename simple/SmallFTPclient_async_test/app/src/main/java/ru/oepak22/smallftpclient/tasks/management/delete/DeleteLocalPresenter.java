package ru.oepak22.smallftpclient.tasks.management.delete;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.oepak22.smallftpclient.content.OperationFile;
import ru.oepak22.smallftpclient.data.FilesProvider;
import ru.oepak22.smallftpclient.tasks.management.CopyMoveDeleteView;

// класс презентера для удаления локальных файлов
public class DeleteLocalPresenter {

    CopyMoveDeleteView mView;                                                                           // интерфейс удаления
    Context mContext;                                                                                   // основной контекст приложения
    List<OperationFile> mStartDeleteList;                                                               // начальный список удаляемых файлов
    List<String> mFilesList;                                                                            // итоговый список удаляемых файлов
    private int mFileCounter;                                                                           // счетчик скачанных файлов

    // конструктор
    public DeleteLocalPresenter(CopyMoveDeleteView view, Context context,
                                                            List<OperationFile> list) {
        mView = view;
        mContext = context;
        mStartDeleteList = list;
    }

    // завершение работы презентера
    public void close() {
        mView = null;
        mContext = null;
        mStartDeleteList.clear();
        mFilesList.clear();
    }

    // удаление файлов - построение структуры
    public void delete() {

        // инициализация массивов
        mFileCounter = 0;
        mFilesList = new ArrayList<>();

        // обработка списка выбранных элементов
        for (OperationFile file : mStartDeleteList)
            createStructureForDelete(new File(file.getSourcePath()));

        // удаление всех найденных элементов
        if (!((AsyncTask) mView).isCancelled()) deleteFiles();
    }

    // рекурсивное формирование структуры удаления
    private void createStructureForDelete(@NonNull File item) {

        // рекурсивное прохождение всех каталогов
        File[] files = item.listFiles();
        if (files != null && files.length != 0) {
            for (File file : files) {
                if (!((AsyncTask) mView).isCancelled()) {
                    mView.showMessage("prepare (" + mFilesList.size() + " files)");
                    if (file.isDirectory()) createStructureForDelete(file);
                    else mFilesList.add(file.getPath());
                }
            }
        }

        // добавление каталога в список на удаление
        mFilesList.add(item.getPath());
    }

    // удаление файлов из внутреннего хранилища
    public void deleteFiles() {

        for (String item : mFilesList) {

            if (!((AsyncTask) mView).isCancelled()) {

                // удаление элемента
                File file = new File(item);
                if (FilesProvider.provideFilesService().delete(mContext, file))
                    mFileCounter++;

                // обновление уведомления о прогрессе
                int progress = (int) ((double) (mFileCounter * 100) / (double) mFilesList.size());
                mView.showDeleteMoveProgress(mFileCounter, progress,
                                                file.getName(), mFilesList.size());
            }
        }

        // отправка сообщения о завершении задачи
        mView.showCompleted(mFileCounter, mFilesList.size());
    }
}

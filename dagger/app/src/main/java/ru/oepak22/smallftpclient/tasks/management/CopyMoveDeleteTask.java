package ru.oepak22.smallftpclient.tasks.management;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.inject.Inject;

import ru.oepak22.smallftpclient.MainApp;
import ru.oepak22.smallftpclient.content.Define;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.content.OperationFile;
import ru.oepak22.smallftpclient.data.FilesService;
import ru.oepak22.smallftpclient.screen.main.MainActivity;
import ru.oepak22.smallftpclient.tasks.management.copy.CopyFtpPresenter;
import ru.oepak22.smallftpclient.tasks.management.copy.CopyLocalPresenter;
import ru.oepak22.smallftpclient.tasks.management.delete.DeleteFtpPresenter;
import ru.oepak22.smallftpclient.tasks.management.delete.DeleteLocalPresenter;
import ru.oepak22.smallftpclient.tasks.management.move.MoveFtpPresenter;
import ru.oepak22.smallftpclient.tasks.management.move.MoveLocalPresenter;
import ru.oepak22.smallftpclient.utils.TextUtils;

// класс асинхронной задачи удаления, перемещения и копирования файлов
public class CopyMoveDeleteTask extends AsyncTask<Void, Void, Void> implements CopyMoveDeleteView {

    private WeakReference<MainActivity> weakReference;                                                          // "лёгкая" ссылка на основную активность

    private CopyLocalPresenter mCopyLocalPresenter;                                                             // презентер копирования локальных файлов
    private CopyFtpPresenter mCopyFtpPresenter;                                                                 // презентер копирования файлов FTP
    private MoveLocalPresenter mMoveLocalPresenter;                                                             // презентер перемещения локальных файлов
    private MoveFtpPresenter mMoveFtpPresenter;                                                                 // презентер перемещения файлов FTP
    private DeleteLocalPresenter mDeleteLocalPresenter;                                                         // презентер удаления локальных файлов
    private DeleteFtpPresenter mDeleteFtpPresenter;                                                             // презентер удаления файлов FTP

    private int mOperationId;                                                                                   // идентификатор операции

    private NotificationCompat.Builder notificationBuilder;                                                     // уведомление Notification.Builder
    private NotificationManager notificationManager;                                                            // обработчик уведомлений

    @Inject FilesService mService;                                                                              // внедрение зависимости - интерфейс операций с файлами

    // подготовка асинхронной задачи к запуску
    public void start(MainActivity activity, FtpServer server,
                        @NonNull List<OperationFile> operationFiles,
                                                    long space, int id) {

        // инициализация презентера (в соответствии с задачей)
        this.weakReference = new WeakReference<>(activity);
        mOperationId = id;

        // внедрение зависимостей
        MainApp.getAppComponent().injectCopyMoveDeleteTask(this);

        // определение операции
        switch (mOperationId) {
            case Define.COPY_TASK:
                if (server != null)
                    mCopyFtpPresenter = new CopyFtpPresenter(this, server, operationFiles);
                else
                    mCopyLocalPresenter = new CopyLocalPresenter(this, mService,
                                                                        this.weakReference.get()
                                                                                .getApplicationContext(),
                                                                                    operationFiles, space);
                break;
            case Define.MOVE_TASK:
                if (server != null)
                    mMoveFtpPresenter = new MoveFtpPresenter(this, server, operationFiles);
                else
                    mMoveLocalPresenter = new MoveLocalPresenter(this, mService,
                                                                    this.weakReference.get()
                                                                            .getApplicationContext(),
                                                                                operationFiles, space);
                break;
            case Define.DELETE_TASK:
                if (server != null)
                    mDeleteFtpPresenter = new DeleteFtpPresenter(this,
                                                                        this.weakReference.get()
                                                                                .getApplicationContext(),
                                                                                    server, operationFiles);
                else
                    mDeleteLocalPresenter = new DeleteLocalPresenter(this, mService,
                                                                            this.weakReference.get()
                                                                                    .getApplicationContext(),
                                                                                                operationFiles);
                break;
        }

        // запуск асинхронной задачи
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // выполнение задачи в фоне
    @Override
    protected Void doInBackground(Void... voids) {

        notificationInit();

        // копирование файлов FTP
        if (mCopyFtpPresenter != null) {
            mCopyFtpPresenter.copy();
            mCopyFtpPresenter.close();
        }

        // копирование локальных файлов
        else if (mCopyLocalPresenter != null) {
            mCopyLocalPresenter.copy();
            mCopyLocalPresenter.close();
        }

        // перемещение файлов FTP
        else if (mMoveFtpPresenter != null) {
            mMoveFtpPresenter.move();
            mMoveFtpPresenter.close();
        }

        // перемещение локальных файлов
        else if (mMoveLocalPresenter != null) {
            mMoveLocalPresenter.move();
            mMoveLocalPresenter.close();
        }

        // удаление файлов FTP
        else if (mDeleteFtpPresenter != null) {
            mDeleteFtpPresenter.delete();
            mDeleteFtpPresenter.close();
        }

        // удаление локальных файлов
        else if (mDeleteLocalPresenter != null) {
            mDeleteLocalPresenter.delete();
            mDeleteLocalPresenter.close();
        }

        return null;
    }

    // задача отменена
    @Override
    protected void onCancelled() {
        notificationManager.cancel(4);
        notificationManager.cancel(5);
    }

    // инициализация уведомления
    @SuppressLint("UnspecifiedImmutableFlag")
    private void notificationInit() {

        // инициализация менеджера уведомлений
        notificationManager = (NotificationManager) this.weakReference.get()
                                                        .getSystemService(Context.NOTIFICATION_SERVICE);

        // инициализация канала уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Define.SERVICE_OTHER,
                                                                                "FTP files operations task",
                                                                                NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("Task for Removing, Copying and Moving files");
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager = this.weakReference.get().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        // закрытие уже открытых уведомлений
        notificationManager.cancel(4);
        notificationManager.cancel(5);

        // построение и вывод уведомления
        String title;
        switch (mOperationId) {

            case Define.COPY_TASK: title = "Init copying files..."; break;
            case Define.MOVE_TASK: title = "Init moving files..."; break;
            default: title = "Init removing files..."; break;
        }

        notificationBuilder = new NotificationCompat.Builder(this.weakReference.get()
                                                                    .getApplicationContext(),
                                                                        Define.SERVICE_OTHER)
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentTitle(TextUtils.boldText(title))
                .setContentText("preparing")
                .setDefaults(Notification.DEFAULT_SOUND)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .addAction(0, "Cancel",
                                    PendingIntent.getBroadcast(weakReference.get(),
                                                                Define.OTHER_CANCEL,
                                                                new Intent(Define.SERVICE_OTHER),
                                                                                            0));

        notificationManager.notify(4, notificationBuilder.build());
    }

    // уведомления в процессе подготовки операции
    @Override
    public void showMessage(String msg) {
        notificationBuilder.setSilent(true);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(msg));
        notificationManager.notify(4, notificationBuilder.build());
    }

    // уведомление о прогрессе при копировании/перемещении
    @Override
    public void showCopyMoveProgress(int counter, int count, int commonPercent,
                                            int singlePercent, long currentBytes,
                                                            long size, String name) {

        String title;
        if (mOperationId == Define.MOVE_TASK) title = "Move ";
        else title = "Copy ";

        title += commonPercent + "% - "
                    + TextUtils.bytesToString(currentBytes)
                    + " / "
                    + TextUtils.bytesToString(size);

        String text = (counter + 1) + "/" + count + " - " + name + " (" + singlePercent + "%)";

        notificationBuilder.setSilent(true);
        notificationBuilder.setContentTitle(TextUtils.boldText(title));
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        notificationBuilder.setProgress(100, commonPercent, false);
        notificationManager.notify(4, notificationBuilder.build());
    }

    // уведомление о прогрессе при удалении/перемещении
    @Override
    public void showDeleteMoveProgress(int counter, int percent, String name, int total) {

        String title;
        if (mOperationId == Define.MOVE_TASK) title = "Move ";
        else title = "Delete ";

        title += counter + " / " + total;

        notificationBuilder.setSilent(true);
        notificationBuilder.setContentTitle(TextUtils.boldText(title));
        notificationBuilder.setStyle(new NotificationCompat
                                            .BigTextStyle().bigText(name + " - " + counter));
        notificationBuilder.setProgress(100, percent, false);
        notificationManager.notify(4, notificationBuilder.build());
    }

    // уведомление о завершении задачи
    @Override
    public void showCompleted(int completed, int total) {

        notificationManager.cancel(4);
        notificationManager.cancel(5);

        notificationBuilder = new NotificationCompat.Builder(this.weakReference.get()
                                                                .getApplicationContext(),
                                                                    Define.SERVICE_OTHER);

        if (mOperationId == Define.DELETE_TASK)
            notificationBuilder.setSmallIcon(android.R.drawable.ic_menu_delete);
        else
            notificationBuilder.setSmallIcon(android.R.drawable.ic_menu_save);

        switch (mOperationId) {
            case Define.COPY_TASK:
                notificationBuilder.setContentTitle(TextUtils.boldText("Copying completed"));
                break;
            case Define.MOVE_TASK:
                notificationBuilder.setContentTitle(TextUtils.boldText("Moving completed"));
                break;
            case Define.DELETE_TASK:
                notificationBuilder.setContentTitle(TextUtils.boldText("Removing completed"));
                break;
        }

        notificationBuilder.setContentText(completed + "/" + total + " - " + completed + " Files");

        notificationBuilder.setOngoing(false);
        notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
        notificationBuilder.setProgress(100, 100, false);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        notificationBuilder.setAutoCancel(false);
        notificationBuilder.clearActions();
        notificationManager.notify(5, notificationBuilder.build());
    }

    // уведомление об ошибке
    @Override
    public void showError(String msg) {

        notificationManager.cancel(4);
        notificationManager.cancel(5);

        notificationBuilder = new NotificationCompat.Builder(this.weakReference.get()
                                                                    .getApplicationContext(),
                                                                        Define.SERVICE_OTHER);

        notificationBuilder.setSmallIcon(android.R.drawable.stat_notify_error);

        switch (mOperationId) {
            case Define.COPY_TASK:
                notificationBuilder.setContentTitle(TextUtils.boldText("Copying error"));
                break;
            case Define.MOVE_TASK:
                notificationBuilder.setContentTitle(TextUtils.boldText("Moving error"));
                break;
            case Define.DELETE_TASK:
                notificationBuilder.setContentTitle(TextUtils.boldText("Removing error"));
                break;
        }

        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(msg));
        notificationBuilder.setOngoing(false);
        notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
        notificationBuilder.setProgress(100, 100, false);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        notificationBuilder.setAutoCancel(false);
        notificationBuilder.clearActions();
        notificationManager.notify(5, notificationBuilder.build());
    }
}

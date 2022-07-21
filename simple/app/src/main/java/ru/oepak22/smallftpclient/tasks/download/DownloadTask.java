package ru.oepak22.smallftpclient.tasks.download;

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

import ru.oepak22.smallftpclient.content.Define;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.content.OperationFile;
import ru.oepak22.smallftpclient.screen.main.MainActivity;
import ru.oepak22.smallftpclient.utils.TextUtils;

// класс асинхронной операции скачивания файлов с сервера
public class DownloadTask extends AsyncTask<Void, Void, Void> implements DownloadView {

    private WeakReference<MainActivity> weakReference;                                              // "лёгкая" ссылка на основную активность
    private DownloadPresenter mPresenter;                                                           // презентер скачивания

    private NotificationCompat.Builder notificationBuilder;                                         // уведомление Notification.Builder
    private NotificationManager notificationManager;                                                // обработчик уведомлений

    // подготовка асинхронной задачи к запуску
    public void start(MainActivity activity, FtpServer server,
                              @NonNull List<OperationFile> operationFiles,
                              long space) {

        // инициализация презентера
        this.weakReference = new WeakReference<>(activity);
        mPresenter = new DownloadPresenter(this,
                                            this.weakReference.get().getApplicationContext(),
                                                                server, operationFiles, space);

        // запуск асинхронной задачи
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // выполнение задачи в фоне
    @Override
    protected Void doInBackground(Void... voids) {

        // уведомление о начале скачивания
        notificationInit();

        // запуск скачивания
        mPresenter.download();

        // завершение скачивания
        mPresenter.close();
        return null;
    }

    // задача отменена
    @Override
    protected void onCancelled() {
        notificationManager.cancel(0);
        notificationManager.cancel(1);
    }

    // инициализация уведомления
    @SuppressLint("UnspecifiedImmutableFlag")
    private void notificationInit() {

        // инициализация менеджера уведомлений
        notificationManager = (NotificationManager) this.weakReference.get()
                                                        .getSystemService(Context.NOTIFICATION_SERVICE);

        // инициализация канала уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Define.SERVICE_DOWNLOAD,
                                                                                "FTP Download Task",
                                                                                NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("Task for downloading files from FTP");
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager = this.weakReference.get().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        // закрытие уже открытых уведомлений
        notificationManager.cancel(0);
        notificationManager.cancel(1);

        // построение и вывод уведомления
        notificationBuilder = new NotificationCompat.Builder(this.weakReference.get()
                                                                    .getApplicationContext(),
                                                                        Define.SERVICE_DOWNLOAD)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(TextUtils.boldText("Init download transfer..."))
                .setContentText("preparing")
                .setDefaults(Notification.DEFAULT_SOUND)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .addAction(0, "Cancel",
                                    PendingIntent.getBroadcast(weakReference.get(),
                                                                Define.DOWNLOAD_CANCEL,
                                                                new Intent(Define.SERVICE_DOWNLOAD),
                                                                                            0));

        notificationManager.notify(0, notificationBuilder.build());
    }

    // уведомление о процессе скачивания
    @Override
    public void showProgress(int counter, int count, int commonPercent, int singlePercent,
                                                    long currentBytes, long size, String name) {
        String title = "Download " + commonPercent + "% - "
                                    + TextUtils.bytesToString(currentBytes)
                                    + " / "
                                    + TextUtils.bytesToString(size);

        String text = (counter + 1) + "/" + count + " - " + name + " (" + singlePercent + "%)";

        notificationBuilder.setSilent(true);
        notificationBuilder.setContentTitle(TextUtils.boldText(title));
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        notificationBuilder.setProgress(100, commonPercent, false);
        notificationManager.notify(0, notificationBuilder.build());
    }

    // уведомления в процессе подготовки скачивания
    @Override
    public void showMessage(String msg) {
        notificationBuilder.setSilent(true);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(msg));
        notificationManager.notify(0, notificationBuilder.build());
    }

    // завершающее уведомление
    @Override
    public void showCompleted(int downloaded, int total) {

        notificationManager.cancel(0);
        notificationManager.cancel(1);

        notificationBuilder = new NotificationCompat.Builder(this.weakReference.get()
                                                                    .getApplicationContext(),
                                                                        Define.SERVICE_DOWNLOAD);

        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        notificationBuilder.setContentTitle(TextUtils.boldText("Download completed"));
        notificationBuilder.setContentText(downloaded + "/" + total + " - " + downloaded + " Files");

        notificationBuilder.setOngoing(false);
        notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
        notificationBuilder.setProgress(100, 100, false);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        notificationBuilder.setAutoCancel(false);
        notificationBuilder.clearActions();
        notificationManager.notify(1, notificationBuilder.build());
    }

    // уведомление об ошибке
    @Override
    public void showError(String msg) {

        notificationManager.cancel(0);
        notificationManager.cancel(1);

        notificationBuilder = new NotificationCompat.Builder(this.weakReference.get()
                                                                    .getApplicationContext(),
                                                                        Define.SERVICE_DOWNLOAD);

        notificationBuilder.setSmallIcon(android.R.drawable.stat_notify_error);
        notificationBuilder.setContentTitle(TextUtils.boldText("Download error"));
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(msg));

        notificationBuilder.setOngoing(false);
        notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
        notificationBuilder.setProgress(100, 100, false);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        notificationBuilder.setAutoCancel(false);
        notificationBuilder.clearActions();
        notificationManager.notify(1, notificationBuilder.build());
    }
}
package ru.oepak22.smallftpclient.tasks.upload;

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

// класс асинхронной операции загрузки файлов на сервер
public class UploadTask extends AsyncTask<Void, Void, Void> implements UploadView {

    private WeakReference<MainActivity> weakReference;                                              // "лёгкая" ссылка на основную активность
    private UploadPresenter mPresenter;

    private NotificationCompat.Builder notificationBuilder;                                         // уведомление Notification.Builder
    private NotificationManager notificationManager;                                                // обработчик уведомлений

    // подготовка асинхронной задачи к запуску
    public void start(MainActivity activity, FtpServer server,
                                @NonNull List<OperationFile> operationFiles) {

        // инициализация презентера
        this.weakReference = new WeakReference<>(activity);
        mPresenter = new UploadPresenter(this,
                                            this.weakReference.get().getApplicationContext(),
                                                                        server, operationFiles);

        // запуск асинхронной задачи
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // выполнение задачи в фоне
    @Override
    protected Void doInBackground(Void... voids) {

        // уведомление о начале загрузки
        notificationInit();

        // запуск загрузки
        mPresenter.upload();

        // завершение загрузки
        mPresenter.close();
        return null;
    }

    // задача отменена
    @Override
    protected void onCancelled() {
        notificationManager.cancel(2);
        notificationManager.cancel(3);
    }

    // инициализация уведомления
    @SuppressLint("UnspecifiedImmutableFlag")
    private void notificationInit() {

        // инициализация менеджера уведомлений
        notificationManager = (NotificationManager) this.weakReference.get()
                                                        .getSystemService(Context.NOTIFICATION_SERVICE);

        // инициализация канала уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Define.SERVICE_UPLOAD,
                                                                                "FTP Upload Task",
                                                                                NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("Task for uploading files to FTP");
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager = this.weakReference.get().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        // закрытие уже открытых уведомлений
        notificationManager.cancel(2);
        notificationManager.cancel(3);

        // построение и вывод уведомления
        notificationBuilder = new NotificationCompat.Builder(this.weakReference.get()
                                                                    .getApplicationContext(),
                                                                        Define.SERVICE_UPLOAD)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle(TextUtils.boldText("Init upload transfer..."))
                .setContentText("preparing")
                .setDefaults(Notification.DEFAULT_SOUND)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .addAction(0, "Cancel",
                                    PendingIntent.getBroadcast(weakReference.get(),
                                                                Define.UPLOAD_CANCEL,
                                                                new Intent(Define.SERVICE_UPLOAD),
                                                                                            0));

        notificationManager.notify(2, notificationBuilder.build());
    }

    // уведомление о процессе загрузки
    @Override
    public void showProgress(int counter, int count, int commonPercent, int singlePercent,
                                                    long currentBytes, long size, String name) {
        String title = "Upload " + commonPercent + "% - "
                                    + TextUtils.bytesToString(currentBytes)
                                    + " / "
                                    + TextUtils.bytesToString(size);

        String text = (counter + 1) + "/" + count + " - " + name + " (" + singlePercent + "%)";

        notificationBuilder.setSilent(true);
        notificationBuilder.setContentTitle(TextUtils.boldText(title));
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        notificationBuilder.setProgress(100, commonPercent, false);
        notificationManager.notify(2, notificationBuilder.build());
    }

    // уведомления в процессе подготовки загрузки
    @Override
    public void showMessage(String msg) {
        notificationBuilder.setSilent(true);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(msg));
        notificationManager.notify(2, notificationBuilder.build());
    }

    // завершающее уведомление
    @Override
    public void showCompleted(int uploaded, int total) {

        notificationManager.cancel(2);
        notificationManager.cancel(3);

        notificationBuilder = new NotificationCompat.Builder(this.weakReference.get()
                                                                    .getApplicationContext(),
                                                                        Define.SERVICE_UPLOAD);

        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_upload_done);
        notificationBuilder.setContentTitle(TextUtils.boldText("Upload completed"));
        notificationBuilder.setContentText(uploaded + "/" + total + " - " + uploaded + " Files");

        notificationBuilder.setOngoing(false);
        notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
        notificationBuilder.setProgress(100, 100, false);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        notificationBuilder.setAutoCancel(false);
        notificationBuilder.clearActions();
        notificationManager.notify(3, notificationBuilder.build());
    }

    // уведомление об ошибке
    @Override
    public void showError(String msg) {

        notificationManager.cancel(2);
        notificationManager.cancel(3);

        notificationBuilder = new NotificationCompat.Builder(this.weakReference.get()
                                                                    .getApplicationContext(),
                                                                        Define.SERVICE_UPLOAD);

        notificationBuilder.setSmallIcon(android.R.drawable.stat_notify_error);
        notificationBuilder.setContentTitle(TextUtils.boldText("Upload error"));
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(msg));

        notificationBuilder.setOngoing(false);
        notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
        notificationBuilder.setProgress(100, 100, false);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        notificationBuilder.setAutoCancel(false);
        notificationBuilder.clearActions();
        notificationManager.notify(3, notificationBuilder.build());
    }
}

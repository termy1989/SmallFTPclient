package ru.oepak22.smallftpclient.tasks.download;

// интерфейс для презентера скачивания
public interface DownloadView {
    void showCompleted(int downloaded, int total);
    void showProgress(int counter, int count, int commonPercent, int singlePercent,
                                            long currentBytes, long size, String name);
    void showMessage(String msg);
    void showError(String msg);
}

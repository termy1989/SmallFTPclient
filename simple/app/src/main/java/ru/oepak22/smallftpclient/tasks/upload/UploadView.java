package ru.oepak22.smallftpclient.tasks.upload;

// интерфейс для презентера загрузки
public interface UploadView {
    void showCompleted(int uploaded, int total);
    void showProgress(int counter, int count, int commonPercent, int singlePercent,
                                            long currentBytes, long size, String name);
    void showMessage(String msg);
    void showError(String msg);
}

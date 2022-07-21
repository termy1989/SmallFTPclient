package ru.oepak22.smallftpclient.tasks.management;

// интерфейс для презентеров операций с файлами
public interface CopyMoveDeleteView {
    void showCompleted(int completed, int total);
    void showCopyMoveProgress(int counter, int count, int commonPercent, int singlePercent,
                                                    long currentBytes, long size, String name);
    void showDeleteMoveProgress(int counter, int percent, String name, int total);
    void showMessage(String msg);
    void showError(String msg);
}

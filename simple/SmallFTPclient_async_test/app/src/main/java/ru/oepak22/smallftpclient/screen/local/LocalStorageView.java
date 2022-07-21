package ru.oepak22.smallftpclient.screen.local;

import androidx.annotation.NonNull;

import java.util.List;

import ru.oepak22.smallftpclient.content.LocalFile;

// интерфейс для презентера содержимого текущего каталога
public interface LocalStorageView {
    void showFiles(@NonNull List<LocalFile> files);             // вывод содержимого на экран
    void showEmpty();                                           // вывод пустого пространства на экран
}

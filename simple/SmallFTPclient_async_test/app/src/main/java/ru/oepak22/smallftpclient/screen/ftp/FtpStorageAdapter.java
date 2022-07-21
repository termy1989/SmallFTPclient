package ru.oepak22.smallftpclient.screen.ftp;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Locale;

import ru.oepak22.smallftpclient.R;
import ru.oepak22.smallftpclient.content.RemoteFile;
import ru.oepak22.smallftpclient.widget.BaseAdapter;

// класс адаптера для списка файлов/каталогов FTP
public class FtpStorageAdapter extends BaseAdapter<FtpStorageHolder, RemoteFile> {

    // флаг - список формируется для диалогового окна
    private final boolean isDialog;

    // конструктор
    public FtpStorageAdapter(@NonNull List<RemoteFile> items, boolean isDialog) {
        super(items);
        this.isDialog = isDialog;
    }

    // инициализация отображателя элементов списка
    @NonNull
    @Override
    public FtpStorageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FtpStorageHolder(LayoutInflater.from(parent.getContext())
                                                        .inflate(R.layout.recycler_file_item,
                                                                    parent, false),
                                                                                this.isDialog);
    }

    // оформление каждого элемента списка
    @Override
    public void onBindViewHolder(@NonNull FtpStorageHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        RemoteFile file = getItem(position);
        holder.bind(file);
    }

    // фильтр для поиска
    public void filter(String charText) {
        charText = charText.toLowerCase(Locale.getDefault());
        clear();
        if (charText.length() == 0) recover();
        else {
            for (RemoteFile wp : getValuesOld()) {
                if (wp.getName().toLowerCase(Locale.getDefault())
                                                .contains(charText))
                    add(wp);
            }
        }
    }
}

package ru.oepak22.smallftpclient.screen.local;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.oepak22.smallftpclient.R;
import ru.oepak22.smallftpclient.content.LocalFile;
import ru.oepak22.smallftpclient.widget.BaseAdapter;

// класс адаптера для списка локальных файлов/каталогов
public class LocalStorageAdapter extends BaseAdapter<LocalStorageHolder, LocalFile> {

    // флаг - список формируется для диалогового окна
    private final boolean isDialog;

    // конструктор
    public LocalStorageAdapter(@NonNull List<LocalFile> items, boolean isDialog) {
        super(items);
        this.isDialog = isDialog;
    }

    // инициализация отображателя элементов списка
    @NonNull
    @Override
    public LocalStorageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LocalStorageHolder(LayoutInflater.from(parent.getContext())
                                                        .inflate(R.layout.recycler_file_item,
                                                                    parent, false),
                                                                                this.isDialog);
    }

    // оформление каждого элемента списка
    @Override
    public void onBindViewHolder(@NonNull LocalStorageHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        LocalFile file = getItem(position);
        holder.bind(file);
    }

    // фильтр для поиска
    public void filter(String charText) {
        charText = charText.toLowerCase(Locale.getDefault());
        clear();
        if (charText.length() == 0) recover();
        else {
            for (LocalFile wp : getValuesOld()) {
                if (wp.getName()
                        .toLowerCase(Locale.getDefault())
                                        .contains(charText))
                    add(wp);
            }
        }
    }
}
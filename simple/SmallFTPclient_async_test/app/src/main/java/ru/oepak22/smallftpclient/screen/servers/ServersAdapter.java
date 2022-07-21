package ru.oepak22.smallftpclient.screen.servers;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.List;

import ru.oepak22.smallftpclient.R;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.widget.BaseAdapter;

// класс адаптера для списка серверов
public class ServersAdapter extends BaseAdapter<ServersHolder, FtpServer> {

    // флаг - список формируется для диалогового окна
    private final boolean isDialog;

    // конструтор
    public ServersAdapter(@NonNull List<FtpServer> items, boolean isDialog) {
        super(items);
        this.isDialog = isDialog;
    }

    // инициализация отображателя элементов списка
    @NonNull
    @Override
    public ServersHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ServersHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_server_item, parent, false), this.isDialog);
    }

    // оформление каждого элемента списка
    @Override
    public void onBindViewHolder(@NonNull ServersHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        FtpServer server = getItem(position);
        holder.bind(server);
    }
}

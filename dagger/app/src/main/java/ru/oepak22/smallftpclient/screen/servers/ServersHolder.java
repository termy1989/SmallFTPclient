package ru.oepak22.smallftpclient.screen.servers;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ru.oepak22.smallftpclient.R;
import ru.oepak22.smallftpclient.content.FtpServer;

// класс отображателя элемента списка
public class ServersHolder extends RecyclerView.ViewHolder {

    // составляющие элемента списка
    private final TextView textViewName;
    private final TextView textViewAddress;
    private final CheckBox checkBox;
    private final ImageView imageView;

    // флаг - список формируется для диалогового окна
    private final boolean isDialog;

    // конструктор
    public ServersHolder(@NonNull View view, boolean isDialog) {
        super(view);

        // установка флага
        this.isDialog = isDialog;

        // определение компонентов элемента списка
        textViewName = view.findViewById(R.id.server_name_text_view);
        textViewAddress = view.findViewById(R.id.server_address_text_view);
        checkBox = view.findViewById(R.id.server_check_box);
        imageView = view.findViewById(R.id.server_icon_view);
    }

    // оформление каждого элемента списка
    @SuppressLint("SetTextI18n")
    public void bind(@NonNull FtpServer server) {

        // установка имени сервера
        textViewName.setText(server.getName());

        // установка адреса и порта сервера
        textViewAddress.setText(server.getAddress() + ":" + server.getPort());

        // в диалоговом окне элемент нельзя пометить как выбранный
        if (this.isDialog) {
            checkBox.setVisibility(View.GONE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) imageView.getLayoutParams();
            params.setMarginStart(15);
            imageView.setLayoutParams(params);
        }

        // checkbox для выбора элемента
        else {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setChecked(server.isSelected());
            checkBox.setOnClickListener(v -> server.setSelected());
        }
    }

}

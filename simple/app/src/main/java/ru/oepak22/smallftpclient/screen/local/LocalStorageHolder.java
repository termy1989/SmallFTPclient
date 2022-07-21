package ru.oepak22.smallftpclient.screen.local;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;

import ru.oepak22.smallftpclient.R;
import ru.oepak22.smallftpclient.content.LocalFile;
import ru.oepak22.smallftpclient.utils.TextUtils;

// класс отображателя элемента списка
public class LocalStorageHolder extends RecyclerView.ViewHolder {

    // составляющие элемента списка
    private final TextView textViewName;
    private final TextView textViewAttributes;
    private final ImageView imageView;
    private final CheckBox checkBox;

    // флаг - список формируется для диалогового окна
    private final boolean isDialog;

    // конструктор
    public LocalStorageHolder(View view, boolean isDialog) {
        super(view);

        // установка флага
        this.isDialog = isDialog;

        // определение компонентов элемента списка
        textViewName = view.findViewById(R.id.file_name_text_view);
        textViewAttributes = view.findViewById(R.id.file_attr_text_view);
        imageView = view.findViewById(R.id.file_icon_view);
        checkBox = view.findViewById(R.id.file_check_box);
    }

    // оформление каждого элемента списка
    @SuppressLint("SetTextI18n")
    public void bind(@NonNull LocalFile file) {

        // установка неполного имени файла/каталога
        textViewName.setText(file.getName());

        // установка ярлыка для перехода в родительский каталог
        if (file.isNavigator()) {
            checkBox.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
            textViewAttributes.setText("");
        }

        // установка ярлыков файла/каталога
        else {

            imageView.setVisibility(View.VISIBLE);

            if (file.isDirectory()) {
                imageView.setImageResource(R.drawable.ic_baseline_folder_24);
                textViewAttributes.setText(file.getLastModified());
            }
            else {
                imageView.setImageResource(R.drawable.ic_baseline_file_24);
                textViewAttributes.setText(file.getLastModified() +
                                            " (" + TextUtils.bytesToString(file.getSize()) + ")");
            }

            // checkbox для выбора файла/каталога
            if (this.isDialog) {
                checkBox.setVisibility(View.GONE);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) imageView.getLayoutParams();
                params.setMarginStart(15);
                imageView.setLayoutParams(params);
            }
            else {
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(file.isSelected());
                checkBox.setOnClickListener(v -> file.setSelected());
            }
        }
    }
}
package ru.oepak22.smallftpclient.utils;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;

// класс для работы с текстом
public final class TextUtils {

    // конструктор
    private TextUtils() {
    }

    // проверка на пустой текст
    public static boolean isEmpty(@Nullable CharSequence text) {
        return text == null || text.length() == 0;
    }

    // перевод значения памяти в текст
    @NonNull
    public static String bytesToString(long bytes) {

        String[] symbols = new String[] {"B", "Kb", "Mb", "Gb", "Tb", "Pb", "Eb"};
        long scale = 1L;
        for (String symbol : symbols) {
            if (bytes < (scale * 1024L))
                return String.format("%s %s", new DecimalFormat("#.##")
                                .format((double) bytes / scale), symbol);
            scale *= 1024L;
        }
        return "-1 B";
    }

    // жирный текст
    @NonNull
    public static Spannable boldText(String text) {

        Spannable sb = new SpannableString(text);
        sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, text.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    // парсинг пути файла с удалением концовки (получение родительского каталога)
    @NonNull
    public static String splitBySlash(@NonNull String text) {
        String[] separator = text.split("/");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < separator.length - 1; i++) {
            result.append(separator[i]);
            if (i != separator.length - 2) result.append("/");
        }
        if (result.length() == 0) result = new StringBuilder("/");
        return result.toString();
    }
}

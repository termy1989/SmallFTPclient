package ru.oepak22.smallftpclient.screen.servers;

import android.app.Dialog;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import ru.oepak22.smallftpclient.MainApp;
import ru.oepak22.smallftpclient.R;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.data.FtpService;
import ru.oepak22.smallftpclient.data.RealmService;
import ru.oepak22.smallftpclient.screen.main.MainActivity;
import ru.oepak22.smallftpclient.widget.BaseAdapter;
import ru.oepak22.smallftpclient.widget.DividerItemDecoration;
import ru.oepak22.smallftpclient.widget.EmptyRecyclerView;

// класс диалого выбора сервера для загрузки файлов
public class ListServersDialog extends DialogFragment implements ServersView,
                                        BaseAdapter.OnItemClickListener<FtpServer> {

    private ServersAdapter mAdapter;                                                                    // адаптер списка серверов
    private ServersPresenter mPresenter;                                                                // презентер списка серверов
    @Inject RealmService mRealmService;                                                                 // внедрение зависимости - интерфейс операций Realm
    @Inject FtpService mFtpService;                                                                     // внедрение зависимости - интерфейс операций FTP

    // создание фрагмента
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // внедрение зависимостей
        MainApp.getAppComponent().injectListServersDialog(this);

        // инициализация адаптера
        mPresenter = new ServersPresenter(this, mRealmService, mFtpService);
    }

    // формирование диалогового окна с кнопкой
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // добавление кнопки
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.message_upload_path)
                //.setIcon(R.drawable.ic_textview_path)
                .setCancelable(true)
                .setNegativeButton(R.string.message_cancel, (dialog, whichButton) -> dialog.dismiss());


        // инициализация разметки
        View view = requireActivity().getLayoutInflater()
                                        .inflate(R.layout.activity_servers, null);

        // сокрытие тулбара
        Toolbar toolbar = view.findViewById(R.id.toolbar_servers);
        toolbar.setVisibility(View.GONE);

        // сокрытие кнопок
        view.findViewById(R.id.fab_add).setVisibility(View.GONE);
        view.findViewById(R.id.fab_del).setVisibility(View.GONE);

        // компоненты фрагмента
        EmptyRecyclerView mRecyclerView = view.findViewById(R.id.server_name_recycler_view);
        View mEmptyView = view.findViewById(R.id.noservers_textview);

        // настройка списка
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(requireActivity()));
        mRecyclerView.setEmptyView(mEmptyView);

        // инициализация адаптера списка
        mAdapter = new ServersAdapter(new ArrayList<>(), true);
        mAdapter.attachToRecyclerView(mRecyclerView);
        mAdapter.setOnItemClickListener(this);

        // добавление разметки в диалог и вывод диалога на экран
        onViewCreated(view, null);
        dialogBuilder.setView(view);
        return dialogBuilder.create();
    }

    // возобновление работы фрагмента
    @Override
    public void onResume() {
        super.onResume();
        if (mPresenter != null)
            mPresenter.refresh();
    }

    // вывод списка серверов из базы
    @Override
    public void showServers(@NonNull List<FtpServer> servers) {
        mAdapter.changeDataSet(servers);
    }

    // выбор сервера из списка
    @Override
    public void onItemClick(@NonNull FtpServer item) {
        ((MainActivity) requireActivity()).openUploadDialog(item.getName());
        dismiss();
    }

    // ошибка
    @Override
    public void showMessage(String msg, boolean isError) {
        dismiss();
        ((MainActivity) requireActivity()).showMessage(msg, true);
    }

    // пустое пространство (при отсутствии серверов для выбора)
    @Override
    public void showEmpty() {
        mAdapter.clear();
    }

    // настройка размеров диалогового окна
    private void setDialogSize() {

        // сохранение переменных доступа для окна и пустой точки
        Window window = Objects.requireNonNull(getDialog()).getWindow();
        Point size = new Point();

        // сохранение размеров экрана в size
        Display display = window.getWindowManager().getDefaultDisplay();
        display.getSize(size);

        // установка ширины диалога, составляющей 75% ширины экрана;
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, (int) (size.y * 0.75));
        window.setGravity(Gravity.CENTER);
    }

    @Override
    public void showLoading() {}

    @Override
    public void hideLoading() {}

    @Override
    public void showFoundedServer(@NonNull FtpServer server) {}

    @Override
    public void connectResult(String stat) {}

    @Override
    public void disconnectResult(boolean stat) {}

    @Override
    public void taskComplete(String msg) {}
}

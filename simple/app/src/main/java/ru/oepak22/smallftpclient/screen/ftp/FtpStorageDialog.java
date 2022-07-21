package ru.oepak22.smallftpclient.screen.ftp;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.apache.commons.net.ftp.FTPClient;

import java.util.ArrayList;
import java.util.List;

import ru.oepak22.smallftpclient.R;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.content.RemoteFile;
import ru.oepak22.smallftpclient.screen.main.MainActivity;
import ru.oepak22.smallftpclient.widget.BaseAdapter;
import ru.oepak22.smallftpclient.widget.DividerItemDecoration;
import ru.oepak22.smallftpclient.widget.EmptyRecyclerView;

// класс диалогового фрагмента выбора папки на сервере FTP
public class FtpStorageDialog extends DialogFragment implements FtpStorageView,
                                        BaseAdapter.OnItemClickListener<RemoteFile> {

    private FtpServer mServer;                                                                          // сервер FTP
    private FtpStoragePresenter mPresenter;                                                             // презентер хранилища FTP
    private FtpStorageAdapter mAdapter;                                                                 // адаптер списка
    private TextView mEmptyView;                                                                        // текст вместо списка
    private TextView pathTextView;                                                                      // текст с путем к текущей рабочей директории
    private EmptyRecyclerView mRecyclerView;                                                            // область списка
    private int mOperationID;                                                                           // идентификатор операции - загрузка/копирование/перемещение
    private boolean isClicked;                                                                          // флаг нажатия кнопки

    // создание фрагмента
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // флаг нажатия кнопки обнуляется
        isClicked = false;
    }

    // формирование диалогового окна с кнопкой
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // добавление кнопки
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.message_folder)
                //.setIcon(R.drawable.ic_textview_path)
                .setCancelable(true)
                .setPositiveButton(R.string.message_ok,
                        (dialog, whichButton) -> ((MainActivity) requireActivity())
                                                                    .uploadCopyMoveFiles(mServer,
                                                                        mPresenter.getCurrentDir(),
                                                                                    mOperationID))
                .setNegativeButton(R.string.message_cancel, (dialog, which) -> dialog.dismiss());


        // инициализация разметки
        View view = requireActivity().getLayoutInflater()
                                        .inflate(R.layout.fragment_ftp_storage, null);

        view.findViewById(R.id.fab_download).setVisibility(View.GONE);
        view.findViewById(R.id.remote_swipe_container).setEnabled(false);

        // компоненты фрагмента
        mRecyclerView = view.findViewById(R.id.ftp_name_recycler_view);
        pathTextView = view.findViewById(R.id.remote_path_text_view);
        mEmptyView = view.findViewById(R.id.noftp_textview);

        // настройка списка
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(requireActivity()));
        mRecyclerView.setEmptyView(mEmptyView);

        // инициализация адаптера списка
        mAdapter = new FtpStorageAdapter(new ArrayList<>(), true);
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

    // уничтожение фрагмента
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPresenter != null)
            mPresenter.close();
        if (mAdapter != null)
            mAdapter.destroy();
    }

    // инициализация обработчика соединения перед открытием фрагмента
    public void init(FtpServer server, FTPClient client, int op) {

        // установка идентификатора операции
        mOperationID = op;

        // установка параметров сервера
        mServer = server;

        // инициализация презентера
        mPresenter = new FtpStoragePresenter(this, mServer, client);
    }

    // открытие индикатора загрузки
    @Override
    public void showLoading() {
        mRecyclerView.setEnabled(false);
    }

    // закрытие индикатора загрузки
    @Override
    public void hideLoading() {
        mRecyclerView.setEnabled(true);
    }

    // вывод содержимого текущего каталога на сервере
    @Override
    public void showFiles(@NonNull List<RemoteFile> list) {

        showPath(mPresenter.getCurrentDir());
        List<RemoteFile> dirs = new ArrayList<>();
        for (RemoteFile file : list) {
            if (file.isDirectory() || file.isNavigator())
                dirs.add(file);
        }
        mAdapter.changeDataSet(dirs);
        mEmptyView.setText("");
        isClicked = false;
    }

    // отображение текущего пути (строка над списком)
    public void showPath(String path) {

        if (mServer.getPath().equals("/")) pathTextView.setText(path);
        else {
            if (path.equals(mServer.getPath())) pathTextView.setText("/");
            else pathTextView.setText(path.substring(mServer.getPath().length()));
        }
    }

    // обработка нажатия на элемент списка
    @Override
    public void onItemClick(@NonNull RemoteFile item) {
        if (!isClicked) {
            isClicked = true;
            mPresenter.onItemClick(item);
        }
    }

    // в случае ошибки - закрытие текущего диалога и вывод сообщения
    @Override
    public void showError(String msg) {
        dismiss();
        ((MainActivity) requireActivity()).showMessage(msg, true);
    }

    // каталог пустой, переход выше невозможен
    @Override
    public void showEmpty() {
        showPath(mPresenter.getCurrentDir());
        mEmptyView.setText(R.string.no_files_found);
        mAdapter.clear();
        isClicked = false;
    }
}

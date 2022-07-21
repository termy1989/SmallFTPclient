package ru.oepak22.smallftpclient.screen.ftp;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.apache.commons.net.ftp.FTPClient;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import ru.oepak22.smallftpclient.R;
import ru.oepak22.smallftpclient.content.Define;
import ru.oepak22.smallftpclient.content.FtpServer;
import ru.oepak22.smallftpclient.content.RemoteFile;
import ru.oepak22.smallftpclient.general.LoadingDialog;
import ru.oepak22.smallftpclient.general.LoadingView;
import ru.oepak22.smallftpclient.screen.main.MainActivity;
import ru.oepak22.smallftpclient.widget.BaseAdapter;
import ru.oepak22.smallftpclient.widget.DividerItemDecoration;
import ru.oepak22.smallftpclient.widget.EmptyRecyclerView;

// класс фрагмента для работы с хранилищем FTP
public class FtpStorageFragment extends Fragment implements FtpStorageView,
                                    BaseAdapter.OnItemClickListener<RemoteFile> {

    private FtpServer mServer;                                                                              // сервер FTP
    private Realm mRealm;                                                                                   // база данных
    private FtpStorageAdapter mAdapter;                                                                     // адаптер списка
    private FtpStoragePresenter mPresenter;                                                                 // презентер хранилища FTP
    private LoadingView mLoadingView;                                                                       // диалог загрузки
    private TextView mEmptyView;                                                                            // текст вместо списка
    private TextView pathTextView;                                                                          // текст с путем к текущей рабочей директории
    private FloatingActionButton fab;                                                                       // кнопка скачивания
    private SwipeRefreshLayout mSwipeRefreshLayout;                                                         // обновлялка
    private boolean isClicked;                                                                              // флаг нажатия кнопки

    // создание фрагмента
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // добавление меню
        setHasOptionsMenu(true);

        // инициализация базы данных
        mRealm = Realm.getDefaultInstance();

        // флаг нажатия кнопки обнуляется
        isClicked = false;

        // восстановление фрагмента после поворота
        if (savedInstanceState != null) {

            // восстановление параметров сервера
            mServer = new FtpServer();
            mServer.setName(savedInstanceState.getString("SERVER_NAME"));
            mServer.setAddress(savedInstanceState.getString("SERVER_ADDRESS"));
            mServer.setPort(savedInstanceState.getInt("SERVER_PORT"));
            mServer.setPath(savedInstanceState.getString("SERVER_PATH"));
            mServer.setAnon(savedInstanceState.getBoolean("SERVER_ANON"));
            if (!mServer.isAnon()) {
                mServer.setUsername(savedInstanceState.getString("SERVER_USERNAME"));
                mServer.setPassword(savedInstanceState.getString("SERVER_PASSWORD"));
            }

            // инициализация презентера
            mPresenter = new FtpStoragePresenter(this, mServer, null);

            // восстановление списка файлов и пути к текущей рабочей директории
            mPresenter.setCurrentDir(savedInstanceState.getString("SAVED_DIR"));
            mPresenter.saveCache(mRealm.copyFromRealm(mRealm.where(RemoteFile.class).findAll()));
        }

        // переопределение кнопки "Назад"
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                // переход к предыдущей директории, если есть куда переходить
                if (!pathTextView.getText().toString().equals("/")
                        && mEmptyView.getText().toString().isEmpty()) {
                    RemoteFile dir = new RemoteFile();
                    dir.setName("..");
                    dir.setDirectory();
                    dir.setNavigator();
                    onItemClick(dir);
                }

                // закрытие фрагмента
                else ((MainActivity) requireActivity()).startLocalRoot();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    // отрисовка фрагмента
    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                                                Bundle savedInstanceState) {

        // инициализация разметки
        View view = inflater.inflate(R.layout.fragment_ftp_storage, null);

        // инициализация кнопки скачивания
        fab = view.findViewById(R.id.fab_download);
        fab.setOnClickListener(view2 -> {
            List<RemoteFile> selectedItems = getSelectedItems();
            if (selectedItems.isEmpty())
                ((MainActivity) requireActivity())
                        .showMessage("Please, select at least one item for downloading",
                                                                                    false);
            else ((MainActivity) requireActivity()).checkDownloadRunning();
        });

        // инициализация обновлялки
        mSwipeRefreshLayout = view.findViewById(R.id.remote_swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mPresenter.refresh();
            new Handler().postDelayed(() -> mSwipeRefreshLayout.setRefreshing(false), 500);
        });
        mSwipeRefreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN);

        // компоненты фрагмента
        EmptyRecyclerView mRecyclerView = view.findViewById(R.id.ftp_name_recycler_view);
        pathTextView = view.findViewById(R.id.remote_path_text_view);
        mEmptyView = view.findViewById(R.id.noftp_textview);
        mEmptyView.setClickable(false);
        mEmptyView.setOnClickListener(view1 -> {
            if (!isClicked) {
                mPresenter = new FtpStoragePresenter(FtpStorageFragment.this, mServer, null);
                mPresenter.refresh();
                isClicked = true;
            }
        });

        // настройка списка
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(requireActivity()));
        mRecyclerView.setEmptyView(mEmptyView);

        // инициализация диалога загрузки
        mLoadingView = LoadingDialog.view(requireActivity().getSupportFragmentManager());

        // инициализация адаптера списка
        mAdapter = new FtpStorageAdapter(new ArrayList<>(), false);
        mAdapter.attachToRecyclerView(mRecyclerView);
        mAdapter.setOnItemClickListener(this);

        return view;
    }

    // возобновление работы фрагмента
    @Override
    public void onResume() {
        super.onResume();
        if (mPresenter != null)
            mPresenter.refresh();
    }

    // постановка фрагмента на паузу
    @Override
    public void onPause() {
        super.onPause();
        if (mPresenter != null)
            mPresenter.pause();
    }

    // уничтожение фрагмента
    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
        if (mPresenter != null)
            mPresenter.close();
        if (mAdapter != null)
            mAdapter.destroy();
        if (fab != null)
            fab.setOnClickListener(null);
        mEmptyView.setOnClickListener(null);
    }

    // инициализация главного меню
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.ftp_fragment_menu, menu);

        // инициализация поисковика
        MenuItem menuItem = menu.findItem(R.id.action_ftp_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQueryHint("Type here to search");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                mAdapter.filter(s);
                return false;
            }
        });
    }

    // обработчик главного меню
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        // выбранные файлы
        List<RemoteFile> selectedItems;

        // кнопки главного меню
        switch (item.getItemId()) {

            // кнопка "Select all"
            case R.id.action_ftp_select_all:
                mAdapter.changeDataSet(mPresenter.selectItems(mAdapter.getValues(), true));
                break;

            // кнопка "Clear all"
            case R.id.action_ftp_clear_all:
                mAdapter.changeDataSet(mPresenter.selectItems(mAdapter.getValues(), false));
                break;

            // кнопка "New folder"
            case R.id.action_ftp_new_folder:
                ((MainActivity) requireActivity()).openEditDialog("", Define.FTP_DIR_CREATE);
                break;

            // кнопка "Rename"
            case R.id.action_ftp_rename:
                selectedItems = getSelectedItems();
                if (selectedItems.size() != 1)
                    ((MainActivity) requireActivity())
                            .showMessage("Please, select one item for rename", false);
                else
                    ((MainActivity) requireActivity())
                                        .openEditDialog(selectedItems.get(0).getName(),
                                                                        Define.FTP_FILE_RENAME);
                break;

            // кнопка "Delete"
            case R.id.action_ftp_delete:
                selectedItems = getSelectedItems();
                if (selectedItems == null || selectedItems.isEmpty())
                    ((MainActivity) requireActivity())
                            .showMessage("Please, select at least one item for deleting",
                                                                                    false);
                else
                    ((MainActivity) requireActivity()).deleteFiles(mPresenter.getServer(),
                                                                                null,
                                                                                getCurrentPath(),
                                                                                null,
                                                                                    selectedItems);
                break;

            // кнопка "Move"
            case R.id.action_ftp_move:
                selectedItems = getSelectedItems();
                if (selectedItems.isEmpty())
                    ((MainActivity) requireActivity())
                            .showMessage("Please, select at least one item for moving",
                                                                                    false);
                else ((MainActivity) requireActivity())
                                        .checkCopyMoveRunning(mPresenter.getFtpClient(),
                                                                    mPresenter.getServer(),
                                                                            Define.MOVE_TASK);
                break;

            // кнопка "Copy"
            case R.id.action_ftp_copy:
                selectedItems = getSelectedItems();
                if (selectedItems.isEmpty())
                    ((MainActivity) requireActivity())
                            .showMessage("Please, select at least one item for copying",
                                                                                    false);
                else ((MainActivity) requireActivity())
                                        .checkCopyMoveRunning(mPresenter.getFtpClient(),
                                                                    mPresenter.getServer(),
                                                                            Define.COPY_TASK);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    // сохранение данных перед поворотом экрана
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("SAVED_DIR", mPresenter.getCurrentDir());
        mAdapter.recover();
        mRealm.beginTransaction();
        mRealm.delete(RemoteFile.class);
        mRealm.insert(mAdapter.getValues());
        mRealm.commitTransaction();
        outState.putString("SERVER_NAME", mServer.getName());
        outState.putString("SERVER_ADDRESS", mServer.getAddress());
        outState.putInt("SERVER_PORT", mServer.getPort());
        outState.putString("SERVER_PATH", mServer.getPath());
        outState.putBoolean("SERVER_ANON", mServer.isAnon());
        if (!mServer.isAnon()) {
            outState.putString("SERVER_USERNAME", mServer.getUsername());
            outState.putString("SERVER_PASSWORD", mServer.getPassword());
        }
    }

    // открытие индикатора загрузки
    @Override
    public void showLoading() {
        mLoadingView.showLoading();
    }

    // закрытие индикатора загрузки
    @Override
    public void hideLoading() {
        mLoadingView.hideLoading();
    }

    // короткое нажатие по элементу списка
    @Override
    public void onItemClick(@NonNull RemoteFile item) {
        if (!isClicked && item.isDirectory()) {
            isClicked = true;
            mPresenter.onItemClick(item);
        }
    }

    // вывод содержимого текущего каталога на сервере
    @Override
    public void showFiles(@NonNull List<RemoteFile> files) {
        showPath(mPresenter.getCurrentDir());
        mAdapter.changeDataSet(files);
        isClicked = false;
        mEmptyView.setText("");
        mEmptyView.setClickable(false);
    }

    // вывод сообщения об ошибке
    @SuppressLint("SetTextI18n")
    @Override
    public void showError(String msg) {

        // отображение пути до текущей рабочей директории
        showPath(mPresenter.getCurrentDir());

        // отображение ошибки, если соединение потеряно
        if (mPresenter.getFtpClient() == null || !mPresenter.getFtpClient().isConnected()) {
            mEmptyView.setText("CONNECTION FAILED" + "\n\n" + "(click here to reconnect)");
            mEmptyView.setClickable(true);
            mAdapter.clear();
        }

        // вывод сообщения об ошибке
        ((MainActivity) requireActivity()).showMessage(msg, true);

        // обнуление клиента FTP
        mPresenter.setFtpClient(null);

        // сброс флага нажатия
        isClicked = false;
    }

    // вывод пустого пространства
    @Override
    public void showEmpty() {
        showPath(mPresenter.getCurrentDir());
        mEmptyView.setText(R.string.no_files_found);
        mEmptyView.setClickable(false);
        mAdapter.clear();
        isClicked = false;
    }

    // инициализация обработчика соединения перед открытием фрагмента
    public void init(FtpServer server, FTPClient client) {

        // установка параметров сервера
        mServer = server;

        // инициализация презентера
        mPresenter = new FtpStoragePresenter(this, mServer, client);
    }

    // отображение текущего пути (строка над списком)
    public void showPath(String path) {

        if (mServer.getPath().equals("/")) pathTextView.setText(path);
        else {
            if (path.equals(mServer.getPath())) pathTextView.setText("/");
            else pathTextView.setText(path.substring(mServer.getPath().length()));
        }
    }

    // получение пути к текущему каталогу
    public String getCurrentPath() {
        return mPresenter.getCurrentDir();
    }

    // получение параметров сервера
    public FtpServer getServer() {
        return mPresenter.getServer();
    }

    // получение выделенных элементов списка
    public List<RemoteFile> getSelectedItems() {
        return mPresenter.getSelectedItems(mAdapter.getValues());
    }

    // создание нового каталога
    public void createNewFolder(String name) {
        mPresenter.createNewFolder(name);
    }

    // переименование элемента
    public void renameFile(String name) {
        mPresenter.renameFile(getSelectedItems().get(0).getName(), name);
    }
}
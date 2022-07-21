package ru.oepak22.smallftpclient.screen;

import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import ru.oepak22.smallftpclient.content.LocalFile;
import ru.oepak22.smallftpclient.data.FilesProvider;
import ru.oepak22.smallftpclient.screen.local.LocalStoragePresenter;
import ru.oepak22.smallftpclient.screen.local.LocalStorageView;
import ru.oepak22.smallftpclient.test.TestFilesServices;

// тестирование презентера работы с локальным хранилищем
@RunWith(JUnit4.class)
public class LocalPresenterTest {

    private LocalStoragePresenter mPresenter;
    private LocalStorageView mLocalStorageView;
    private String mCurrentDir = "/storage/emulated/0/Download";
    private Context mContext;
    private List<LocalFile> mCachedList;

    // инициализация объектов
    @Before
    public void setUp() throws Exception {
        mLocalStorageView = Mockito.mock(LocalStorageView.class);
        mContext = Mockito.mock(Context.class);
        mPresenter = new LocalStoragePresenter(mLocalStorageView, mContext, mCurrentDir);
        mCachedList = new ArrayList<>();
    }

    // проверка на то, что презентер корректно создается и инициализируется
    @Test
    public void testCreated() throws Exception {
        assertNotNull(mPresenter);
    }

    // проверка на отсутствие каких-либо действий
    @Test
    public void testNoActionsWithView() throws Exception {
        Mockito.verifyNoMoreInteractions(mLocalStorageView);
    }

    // тестирование закрытие презентера
    @Test
    public void testClose() throws Exception {
        mPresenter.close();
    }

    // тестирование получения текущего каталога
    @Test
    public void testGetCurrentDir() throws Exception {
        mPresenter.getCurrentDir();
    }

    // тестирование сохранения списка файлов перед поворотом
    @Test
    public void testSaveCache() throws Exception {
        mPresenter.saveCache(mCachedList);
    }

    // тестирование создания каталога
    @Test
    public void testCreateFolder() throws Exception {
        TestFilesProvider testFilesProvider = new TestFilesProvider("/storage/emulated/0/Download",
                                                                                false, false);
        FilesProvider.setFilesService(testFilesProvider);
        mPresenter.createNewFolder("new");
    }

    // тестирование переименования элемента
    @Test
    public void testRename() throws Exception {
        LocalFile file = new LocalFile("/storage/emulated/0/Download/file.txt");
        mCurrentDir = "/storage/emulated/0/Download/";
        TestFilesProvider testFilesProvider = new TestFilesProvider("/storage/emulated/0/Download/",
                                                                                false, false);
        FilesProvider.setFilesService(testFilesProvider);
        mPresenter.renameFile("new file", file);
    }

    // тестирование перехода в родительский каталог
    @Test
    public void testToParentDir() throws Exception {
        mCurrentDir = "/storage/emulation/0/Download";
        TestFilesProvider testFilesProvider = new TestFilesProvider(null, false,
                                                                                false);
        FilesProvider.setFilesService(testFilesProvider);
        mPresenter.toParentDir();
    }

    // тестирование нажатия на ".."
    @Test
    public void testClickNavigator() throws Exception {

        LocalFile localFile = new LocalFile("/localfile");
        localFile.setNavigator();
        mPresenter.onItemClick(localFile);
        mPresenter.toParentDir();
    }

    // тестирование нажатия на каталог
    @Test
    public void testClickDirectory() throws Exception {
        LocalFile localFile = new LocalFile("/localfile");
        localFile.setDirectory();
        mPresenter.onItemClick(localFile);
        mPresenter.refresh("/localfile");
    }

    // тестирование получения выбранных элементов
    @Test
    public void testGetSelectedItems() throws Exception {

        List<LocalFile> list = new ArrayList<>();
        LocalFile file = new LocalFile(". .");
        file.setNavigator();
        list.add(file);
        LocalFile file2 = new LocalFile("/folder");
        file2.setDirectory();
        list.add(file2);
        LocalFile file3 = new LocalFile("/file");
        file3.setSelected();
        list.add(file3);

        mPresenter.getSelectedItems(list);
    }

    // тыстирование выделения элементов
    @Test
    public void testSelectItems() throws Exception {
        List<LocalFile> list = new ArrayList<>();
        LocalFile file = new LocalFile(". .");
        file.setNavigator();
        list.add(file);
        LocalFile file2 = new LocalFile("/folder");
        file2.setDirectory();
        list.add(file2);
        LocalFile file3 = new LocalFile("/file");
        file3.setSelected();
        list.add(file3);

        mPresenter.selectItems(list, true);
    }

    // тестирование вывода списка файлов - восстановление после поворота
    @Test
    public void testRefreshFiles1() throws Exception {
        List<LocalFile> list = new ArrayList<>();
        LocalFile file = new LocalFile(". .");
        file.setNavigator();
        list.add(file);
        LocalFile file2 = new LocalFile("/path/folder");
        file2.setDirectory();
        list.add(file2);
        LocalFile file3 = new LocalFile("/path/file");
        file3.setSelected();
        list.add(file3);

        mPresenter.saveCache(list);
        mPresenter.refresh("/path");
    }

    // тестирование вывода списка файлов - корень хранилища
    @Test
    public void testRefreshFiles2() throws Exception {

        TestFilesProvider testFilesProvider = new TestFilesProvider("/storage/SD-01/Download",
                                                                            true, false);
        FilesProvider.setFilesService(testFilesProvider);
        mPresenter.saveCache(null);
        mPresenter.refresh("/storage/SD-01/Download");
    }

    // тестирование вывода списка файлов - пусто, не корень
    @Test
    public void testRefreshFiles3() throws Exception {

        TestFilesProvider testFilesProvider = new TestFilesProvider("/storage/SD-01/Download",
                                                                            false, true);
        FilesProvider.setFilesService(testFilesProvider);
        mPresenter.saveCache(null);
        mPresenter.refresh("/storage/SD-01/Download");
    }

    // завершение
    @After
    public void tearDown() throws Exception {
        FilesProvider.setFilesService(null);
    }

    // переопределенный класс для тестирования FilesProvider
    private static class TestFilesProvider extends TestFilesServices {

        private final String sdPath;
        private final boolean isRoot;
        private final boolean isEmpty;

        // конструктор
        public TestFilesProvider(String sdPath, boolean isRoot, boolean isEmpty) {
            this.sdPath = sdPath;
            this.isRoot = isRoot;
            this.isEmpty = isEmpty;
        }

        // получение пути к SD-карте
        @Override
        public String getSDcard() {
            return this.sdPath;
        }

        // проверка на нахождение в SD-карте
        @Override
        public boolean isSDcard(@NonNull String path) {
            return !path.startsWith("/storage/emulated/0");
        }

        // проверка на нахождение в корне хранилища
        @Override
        public boolean isRoot(Context context, String path) {
            return this.isRoot;
        }

        // получение содержимого указанного каталога
        @Nullable
        @Override
        public List<LocalFile> getListFiles(String dir) {
            List<LocalFile> list = new ArrayList<>();
            LocalFile file2 = new LocalFile("/storage/SD-01/Download/path/folder");
            file2.setDirectory();
            list.add(file2);
            LocalFile file3 = new LocalFile("/storage/SD-01/Download/path/file");
            file3.setSelected();
            list.add(file3);

            if (isEmpty) return null;
            return list;
        }
    }
}

package ru.oepak22.smallftpclient.data;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.oepak22.smallftpclient.content.LocalFile;

// класс операций с файлами локального хранилища
public class FilesOperations implements FilesService {

    // проверка на наличие SD-карты
    @Override
    public String getSDcard() {
        String removableStoragePath = "";
        File[] fileList = new File("/storage/").listFiles();

        for (File file : Objects.requireNonNull(fileList))
        {
            if (!file.getAbsolutePath().equalsIgnoreCase(Environment
                    .getExternalStorageDirectory()
                    .getAbsolutePath())
                    && file.isDirectory()
                    && file.canRead())
                removableStoragePath = file.getAbsolutePath();

        }

        if (removableStoragePath.length() == 0) return null;
        else return removableStoragePath;
    }

    // проверка на нахождение внутри SD-карты
    @Override
    public boolean isSDcard(String path) {

        String sd = getSDcard();
        if (sd == null) return false;
        else return path.startsWith(sd);
    }

    // проверка на нахождение в корневом каталоге SD или внутреннего хранилища
    @Override
    public boolean isRoot(Context context, String path) {

        // SD
        if (PreferenceManager.getDefaultSharedPreferences(context).contains("SD-root")) {
            if (path.equals(PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("SD-root", null)))
                return true;
        }

        // внутреннее хранилище
        return path.equals("/storage/emulated/0");
    }

    // вычисление доступного и общего объема хранилища
    @Override
    public Pair<Long, Long> getSpace(String path) {

        // доступное пространство
        long availableSpace = new StatFs(path).getAvailableBlocksLong()
                * (new StatFs(path).getBlockSizeLong());

        // общее пространство
        long totalSpace = new StatFs(path).getTotalBytes();

        return new Pair<>(availableSpace, totalSpace);
    }

    // переименование файла при копировании/перемещении
    // (если с таким именем уже существует)
    @Override
    public String changeOfExisting(String path) {
        int i = 0;
        String newPath = path;
        while (new File(newPath).exists()) {
            i++;
            newPath = path + " (" + i + ")";
        }
        return newPath;
    }

    // получение содержимого выбранного каталога
    @Override
    public List<LocalFile> getListFiles(String dir) {
        File[] filesAndFolders = (new File(dir)).listFiles();

        // каталог пустой
        if (filesAndFolders == null
                || filesAndFolders.length == 0) return null;

        // составление списка содержимого каталога
        else {
            List<LocalFile> list = new ArrayList<>();
            for (File file: filesAndFolders)
                list.add(new LocalFile(file.getPath()));
            return list;
        }
    }

    // получение файла в формате Document (для работы с SD)
    @Override
    public DocumentFile getDocumentFile(Context context, File file, boolean isDirectory) {

        // получение сохраненного пути к корневому каталогу
        String baseFolder = PreferenceManager.getDefaultSharedPreferences(context)
                                                    .getString("SD-root", null);

        boolean originalDirectory = false;
        if (baseFolder == null) return null;

        // получение пути к указанному файлу без учета корневого каталога
        String relativePath = null;
        try {

            String fullPath = file.getCanonicalPath();
            if (!baseFolder.equals(fullPath)) {
                relativePath = fullPath.substring(baseFolder.length() + 1);
            }
            else originalDirectory = true;

        }
        catch (IOException e) {
            return null;
        }
        catch (Exception f) {
            originalDirectory = true;
        }

        // получение сохраненного пути к корневому каталогу в формате Tree
        String root = PreferenceManager.getDefaultSharedPreferences(context)
                                                .getString("SD-uri", null);
        Uri treeUri = null;
        if (root != null) treeUri = Uri.parse(root);
        if (treeUri == null) return null;

        // парсинг пути к указанному файлу через document tree
        DocumentFile document = DocumentFile.fromTreeUri(context, treeUri);
        if (originalDirectory) return document;
        String[] parts = relativePath.split("/");
        for (int i = 0; i < parts.length; i++) {

            DocumentFile nextDocument = null;
            if (document != null) {
                nextDocument = document.findFile(parts[i]);
                if (nextDocument == null) {
                    if ((i < parts.length - 1) || isDirectory)
                        nextDocument = document.createDirectory(parts[i]);
                    else
                        nextDocument = document.createFile("image", parts[i]);
                }
            }
            document = nextDocument;
        }

        return document;
    }

    // создание каталога
    @Override
    public boolean mkdir(Context context, @NonNull File dir) {
        boolean res = dir.mkdirs();
        if (!res) {
            if (isSDcard(dir.getPath())) {
                DocumentFile documentFile = getDocumentFile(context, dir, true);
                res = documentFile != null && documentFile.canWrite();
            }
        }
        return res;
    }

    // удаление элемента
    @Override
    public boolean delete(Context context, @NonNull File file) {

        boolean ret = file.delete();
        if (!ret && isSDcard(file.getPath())) {
            DocumentFile f = getDocumentFile(context, file, file.isDirectory());
            if (f != null) {
                ret = f.delete();
            }
        }
        return ret;
    }

    // переименование элемента
    @Override
    public boolean rename(Context context, @NonNull File src, File dest) {

        boolean res = src.renameTo(dest);

        if (!res && isSDcard(dest.getPath())) {

            DocumentFile srcDoc;
            if (isSDcard(src.getPath()))
                srcDoc = getDocumentFile(context, src, false);
            else
                srcDoc = DocumentFile.fromFile(src);

            DocumentFile destDoc = getDocumentFile(context, dest.getParentFile(), true);
            if (srcDoc != null && destDoc != null) {
                try {
                    if (Objects.requireNonNull(src.getParent()).equals(dest.getParent()))
                        res = srcDoc.renameTo(dest.getName());
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        /*Uri renameSrcUri = DocumentsContract.renameDocument(context.getContentResolver(),
                                srcDoc.getUri(), dest.getName());*/
                        res = DocumentsContract.moveDocument(context.getContentResolver(),
                                srcDoc.getUri(),//renameSrcUri,
                                Objects.requireNonNull(srcDoc.getParentFile()).getUri(),
                                destDoc.getUri()) != null;
                    }
                } catch (Exception ex) {
                    Log.v("FileUtils", ex.getMessage());
                }
            }
        }

        return res;
    }

    // получение потока записи
    @Override
    public OutputStream getOutputStream(Context context, @NonNull File destFile)
                                                                throws FileNotFoundException {

        OutputStream out = null;
        if (/*!canWrite(destFile) &&*/ isSDcard(destFile.getPath())) {
            DocumentFile file = getDocumentFile(context, destFile, false);
            if (file != null && file.canWrite())
                out = context.getContentResolver().openOutputStream(file.getUri());
        }
        else out = new BufferedOutputStream(new FileOutputStream(destFile));

        return out;
    }

    // получение потока чтения
    @Override
    public InputStream getInputStream(Context context, @NonNull File destFile)
                                                        throws FileNotFoundException {
        InputStream in = null;

        if (/*!canWrite(destFile) &&*/ isSDcard(destFile.getPath())) {
            DocumentFile file = getDocumentFile(context, destFile, false);
            if (file != null && file.canWrite())
                in = context.getContentResolver().openInputStream(file.getUri());
        }
        else in = new FileInputStream(destFile);

        return in;
    }
}

package ru.oepak22.smallftpclient.test;

import android.content.Context;

import androidx.core.util.Pair;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import ru.oepak22.smallftpclient.content.LocalFile;
import ru.oepak22.smallftpclient.data.FilesService;

public class TestFilesServices implements FilesService {

    @Override
    public String getSDcard() {
        return null;
    }

    @Override
    public boolean isSDcard(String path) {
        return false;
    }

    @Override
    public boolean isRoot(Context context, String path) {
        return false;
    }

    @Override
    public Pair<Long, Long> getSpace(String path) {
        return null;
    }

    @Override
    public String changeOfExisting(String path) {
        return null;
    }

    @Override
    public List<LocalFile> getListFiles(String dir) {
        return null;
    }

    @Override
    public DocumentFile getDocumentFile(Context context, File file, boolean isDirectory) {
        return null;
    }

    @Override
    public boolean mkdir(Context context, File dir) {
        return false;
    }

    @Override
    public boolean delete(Context context, File file) {
        return false;
    }

    @Override
    public boolean rename(Context context, File src, File dest) {
        return false;
    }

    @Override
    public OutputStream getOutputStream(Context context, File destFile) throws IOException {
        return null;
    }

    @Override
    public InputStream getInputStream(Context context, File destFile) throws FileNotFoundException {
        return null;
    }
}

package ru.familycinema.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;

/** Instrumentation-only provider; standalone process deliberately needs no Kotlin runtime. */
public final class TestMediaProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "video/mp4"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        boolean second = uri.getPath().contains("second");
        File file = new File(getContext().getCacheDir(), second ? "owned-short-fixture.mp4" : "owned-fixture.mp4");
        if (!file.exists()) {
            try (InputStream src = getContext().getAssets().open(second ? "short-evening.mp4" : "quiet-evening.mp4"); OutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192]; int n;
                while ((n=src.read(buffer))>=0) out.write(buffer,0,n);
            } catch(IOException e) { throw new FileNotFoundException("Owned fixture unavailable"); }
        }
        return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o) { return null; }
    @Override public Uri insert(Uri u,ContentValues v) { return null; }
    @Override public int delete(Uri u,String s,String[] a) { return 0; }
    @Override public int update(Uri u,ContentValues v,String s,String[] a) { return 0; }
}

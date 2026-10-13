package com.secure.trust_vault.util;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * DocumentVaultManager
 * Manages secure internal storage for documents, scans, and attachments.
 */
public class DocumentVaultManager {

    private static final String DOCUMENTS_DIR = "documents";

    public static File getDocumentsFolder(Context context) {
        File dir = new File(context.getFilesDir(), DOCUMENTS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File createScanFile(Context context) {
        File folder = getDocumentsFolder(context);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(folder, "SCAN_" + timestamp + ".jpg");
    }

    public static String copyUriToVault(Context context, Uri sourceUri, String originalName) {
        try {
            File folder = getDocumentsFolder(context);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String safeName = (originalName != null && !originalName.isEmpty()) ? originalName : "DOC_" + timestamp;
            File destFile = new File(folder, "ATTACH_" + timestamp + "_" + safeName);

            try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
                 FileOutputStream out = new FileOutputStream(destFile)) {
                if (in == null) return null;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            return destFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean deleteFile(String path) {
        if (path == null) return false;
        try {
            File file = new File(path);
            if (file.exists()) {
                return file.delete();
            }
        } catch (Exception ignored) {}
        return false;
    }
}

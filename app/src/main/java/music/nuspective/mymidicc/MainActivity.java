package music.nuspective.mymidicc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://appassets.androidplatform.net/web/index.html";
    private static final String APP_HOST = "appassets.androidplatform.net";
    private static final String UPDATE_BASE =
            "https://raw.githubusercontent.com/dennis1579/MyMidiCC./main/web/";
    private static final int MAX_MANIFEST = 64 * 1024;
    private static final int MAX_FILE = 2 * 1024 * 1024;
    private static final int MAX_ALL_FILES = 12 * 1024 * 1024;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private File currentSite;
    private WebView browser;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        currentSite = new File(getFilesDir(), "web-current");
        try {
            installBundledSiteIfNeeded();
        } catch (IOException | JSONException e) {
            TextView error = new TextView(this);
            error.setPadding(32, 48, 32, 48);
            error.setText("MyMidiCC: Die lokale Startseite konnte nicht geladen werden.\n"
                    + "Bitte die App neu installieren.");
            setContentView(error);
            return;
        }

        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/web/",
                        new WebViewAssetLoader.InternalStoragePathHandler(this, currentSite))
                .build();

        browser = new WebView(this);
        WebSettings settings = browser.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptThirdPartyCookies(browser, false);

        browser.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                               WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("https".equalsIgnoreCase(uri.getScheme()) &&
                        APP_HOST.equalsIgnoreCase(uri.getHost()) &&
                        uri.getPath() != null && uri.getPath().startsWith("/web/")) {
                    return false;
                }
                if (request.isForMainFrame()) {
                    String scheme = uri.getScheme();
                    if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        } catch (Exception ignored) {
                            // No external browser available.
                        }
                    }
                    return true; // Never navigate the in-app window outside local content.
                }
                return false; // Subresources can use normal HTTPS when online.
            }
        });
        setContentView(browser);
        browser.loadUrl(APP_URL);
        worker.execute(this::checkAndInstallWebUpdate);
    }

    private static class Entry {
        final String path;
        final String sha;
        Entry(String path, String sha) {
            this.path = path;
            this.sha = sha;
        }
    }

    private static List<Entry> validateManifest(String manifest) throws JSONException, IOException {
        JSONObject root = new JSONObject(manifest);
        if (root.getInt("schema") != 1) throw new IOException("Unknown manifest format");
        JSONArray items = root.getJSONArray("files");
        if (items.length() == 0 || items.length() > 256) throw new IOException("Invalid file count");
        List<Entry> entries = new ArrayList<>();
        boolean hasIndex = false;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String path = item.getString("path");
            String sha = item.getString("sha256").toLowerCase(Locale.ROOT);
            if (!path.matches("[A-Za-z0-9_.\\-/]+") ||
                    !sha.matches("[0-9a-f]{64}") ||
                    path.startsWith("/") || path.startsWith(".") ||
                    path.contains("//")) {
                throw new IOException("Invalid web file entry");
            }
            for (String segment : path.split("/")) {
                if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                    throw new IOException("Path traversal rejected");
                }
            }
            for (Entry previous : entries) {
                if (previous.path.equals(path)) throw new IOException("Duplicate web path");
            }
            if (path.equals("index.html")) hasIndex = true;
            entries.add(new Entry(path, sha));
        }
        if (!hasIndex) throw new IOException("index.html missing");
        return entries;
    }

    private void installBundledSiteIfNeeded() throws IOException, JSONException {
        if (new File(currentSite, "index.html").isFile() &&
                new File(currentSite, ".manifest.json").isFile()) {
            return;
        }
        String manifest;
        try (InputStream input = getAssets().open("manifest.json")) {
            manifest = new String(readLimited(input, MAX_MANIFEST), StandardCharsets.UTF_8);
        }
        List<Entry> files = validateManifest(manifest);
        deleteTree(currentSite);
        if (!currentSite.mkdirs() && !currentSite.isDirectory()) {
            throw new IOException("Cannot prepare local web directory");
        }
        int total = 0;
        for (Entry entry : files) {
            byte[] bytes;
            try (InputStream input = getAssets().open(entry.path)) {
                bytes = readLimited(input, MAX_FILE);
            }
            total += bytes.length;
            if (total > MAX_ALL_FILES || !sha256(bytes).equals(entry.sha)) {
                throw new IOException("Bundled web file invalid: " + entry.path);
            }
            writeFile(new File(currentSite, entry.path), bytes);
        }
        writeFile(new File(currentSite, ".manifest.json"), manifest.getBytes(StandardCharsets.UTF_8));
    }

    private void checkAndInstallWebUpdate() {
        File stage = new File(getFilesDir(), "web-stage");
        try {
            String manifest = new String(download("manifest.json", MAX_MANIFEST),
                    StandardCharsets.UTF_8);
            String installed = new String(readLimited(
                    new FileInputStream(new File(currentSite, ".manifest.json")), MAX_MANIFEST),
                    StandardCharsets.UTF_8);
            if (manifest.equals(installed)) return;

            List<Entry> files = validateManifest(manifest);
            deleteTree(stage);
            if (!stage.mkdirs()) throw new IOException("Cannot create download directory");

            int total = 0;
            for (Entry entry : files) {
                byte[] data = download(entry.path, MAX_FILE);
                total += data.length;
                if (total > MAX_ALL_FILES || !sha256(data).equals(entry.sha)) {
                    throw new IOException("Web content checksum mismatch: " + entry.path);
                }
                writeFile(new File(stage, entry.path), data);
            }
            writeFile(new File(stage, ".manifest.json"),
                    manifest.getBytes(StandardCharsets.UTF_8));
            activateDownload(stage);
            runOnUiThread(() -> {
                if (browser != null) {
                    browser.reload();
                    Toast.makeText(this, "MyMidiCC wurde aktualisiert", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ignored) {
            // Offline, 404, partial download or invalid manifest: keep the last good site.
        } finally {
            deleteTree(stage);
        }
    }

    private void activateDownload(File stage) throws IOException {
        File backup = new File(getFilesDir(), "web-backup");
        deleteTree(backup);
        if (!currentSite.renameTo(backup)) throw new IOException("Cannot back up current site");
        if (!stage.renameTo(currentSite)) {
            if (!backup.renameTo(currentSite)) {
                throw new IOException("Cannot restore previous site");
            }
            throw new IOException("Cannot activate web update");
        }
        deleteTree(backup);
    }

    private static byte[] download(String relativePath, int limit) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(UPDATE_BASE + relativePath)
                .openConnection();
        conn.setConnectTimeout(7000);
        conn.setReadTimeout(12000);
        conn.setUseCaches(false);
        conn.setRequestProperty("Cache-Control", "no-cache");
        try {
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Update unavailable");
            }
            try (InputStream input = conn.getInputStream()) {
                return readLimited(input, limit);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static byte[] readLimited(InputStream input, int limit) throws IOException {
        try (InputStream resource = input;
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int count;
            while ((count = resource.read(buf)) != -1) {
                if (out.size() > limit - count) throw new IOException("File too large");
                out.write(buf, 0, count);
            }
            return out.toByteArray();
        }
    }

    private static String sha256(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                result.append(Character.forDigit((b >>> 4) & 0x0f, 16));
                result.append(Character.forDigit(b & 0x0f, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeFile(File target, byte[] data) throws IOException {
        File parent = target.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            throw new IOException("Cannot create web directory");
        }
        try (FileOutputStream out = new FileOutputStream(target)) {
            out.write(data);
        }
    }

    private static void deleteTree(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        if (file.exists()) file.delete();
    }

    @Override
    public void onBackPressed() {
        if (browser != null && browser.canGoBack()) {
            browser.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        if (browser != null) {
            browser.destroy();
            browser = null;
        }
        super.onDestroy();
    }
}

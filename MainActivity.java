package app.bollywood.studio;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * Bollywood Studio: a thin Android shell around assets/index.html.
 * - serves the page from https://appassets.androidplatform.net/assets/
 * - lets the page open the phone's file picker (gaana, photos)
 * - lets the page save the finished video into Downloads/BollywoodStudio
 */
public class MainActivity extends Activity {

    private static final int REQ_FILE = 4711;
    private static final String HOST = "appassets.androidplatform.net";

    private WebView web;
    private ValueCallback<Uri[]> filePathCallback;

    // state of the file currently being saved through the bridge
    private final Object saveLock = new Object();
    private OutputStream saveStream;
    private Uri saveUri;
    private String saveName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.parseColor("#14061C"));
        getWindow().setNavigationBarColor(Color.parseColor("#14061C"));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#14061C"));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (HOST.equals(u.getHost())) {
                    String p = u.getPath();
                    // the page probes /api/health to detect a server; in the app there is none
                    if (p != null && p.startsWith("/assets/api/")) {
                        return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found",
                                new HashMap<String, String>(), new ByteArrayInputStream(new byte[0]));
                    }
                    return loader.shouldInterceptRequest(u);
                }
                return null;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (HOST.equals(u.getHost())) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {
                    // no app can open this link
                }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        if (savedInstanceState == null || web.restoreState(savedInstanceState) == null) {
            web.loadUrl("https://" + HOST + "/assets/index.html");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(
                        WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                filePathCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        abortSave();
        if (web != null) {
            web.destroy();
        }
        super.onDestroy();
    }

    private void abortSave() {
        synchronized (saveLock) {
            try {
                if (saveStream != null) {
                    saveStream.close();
                }
            } catch (Exception ignored) {
                // nothing to do
            }
            try {
                if (saveUri != null) {
                    getContentResolver().delete(saveUri, null, null);
                }
            } catch (Exception ignored) {
                // nothing to do
            }
            saveStream = null;
            saveUri = null;
            saveName = null;
        }
    }

    /** Called from JavaScript as window.AndroidBridge.* */
    private class Bridge {

        @JavascriptInterface
        public boolean begin(String name, String mime) {
            abortSave();
            try {
                String safe = (name == null || name.trim().isEmpty()) ? "bollywood-video.mp4"
                        : name.replaceAll("[^A-Za-z0-9._ -]", "_");
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, safe);
                v.put(MediaStore.MediaColumns.MIME_TYPE, (mime == null || mime.isEmpty()) ? "video/mp4" : mime);
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BollywoodStudio");
                v.put(MediaStore.MediaColumns.IS_PENDING, 1);
                ContentResolver r = getContentResolver();
                Uri uri = r.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) {
                    return false;
                }
                OutputStream os = r.openOutputStream(uri);
                if (os == null) {
                    r.delete(uri, null, null);
                    return false;
                }
                synchronized (saveLock) {
                    saveStream = os;
                    saveUri = uri;
                    saveName = safe;
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public boolean chunk(String base64) {
            synchronized (saveLock) {
                if (saveStream == null) {
                    return false;
                }
                try {
                    saveStream.write(Base64.decode(base64, Base64.DEFAULT));
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        }

        @JavascriptInterface
        public String end() {
            synchronized (saveLock) {
                if (saveStream == null || saveUri == null) {
                    return "";
                }
                try {
                    saveStream.flush();
                    saveStream.close();
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    getContentResolver().update(saveUri, v, null, null);
                    String shown = "Downloads/BollywoodStudio/" + saveName;
                    saveStream = null;
                    saveUri = null;
                    saveName = null;
                    return shown;
                } catch (Exception e) {
                    return "";
                }
            }
        }

        @JavascriptInterface
        public void abort() {
            abortSave();
        }
    }
}

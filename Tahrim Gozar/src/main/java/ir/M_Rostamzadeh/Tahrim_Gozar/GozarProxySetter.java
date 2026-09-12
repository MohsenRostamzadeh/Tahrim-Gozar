package ir.M_Rostamzadeh.Tahrim_Gozar;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.ERROR_DENIED_PERMISSION_CODE;
import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.PROXY_OK;

/**
 * Internal class that handles automatic proxy selection and switching based on connection errors.
 * Uses WeakReference to prevent WebView memory leaks and a shared thread pool for background work.
 * 
 * کلاس داخلی مدیریت و انتخاب خودکار پروکسی بر اساس خطاهای اتصال.
 * جهت جلوگیری از نشت حافظه (Memory Leak) از WeakReference برای ویو استفاده شده و تردها از ThreadPool بهره می‌برند.
 */
class GozarProxySetter {

    private String link;
    private WeakReference<WebView> webViewRef;
    private int errorCode;
    private boolean haveErrorCode;
    private final Handler handler;

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "TahrimGozar-ProxySetter");
        t.setDaemon(true);
        return t;
    });

    @SuppressLint("StaticFieldLeak")
    private static volatile GozarProxySetter gozarProxySetterNormal = null;
    @SuppressLint("StaticFieldLeak")
    private static volatile GozarProxySetter gozarProxySetterError = null;

    protected static synchronized GozarProxySetter getInstance(String link) {
        if (gozarProxySetterNormal == null) {
            gozarProxySetterNormal = new GozarProxySetter(link);
        } else {
            gozarProxySetterNormal.link = link;
            gozarProxySetterNormal.webViewRef = null;
            gozarProxySetterNormal.haveErrorCode = false;
        }
        return gozarProxySetterNormal;
    }

    protected static synchronized GozarProxySetter getInstance(String link, WebView webView, int errorCode) {
        if (gozarProxySetterError == null) {
            gozarProxySetterError = new GozarProxySetter(link, webView, errorCode);
        } else {
            gozarProxySetterError.link = link;
            gozarProxySetterError.webViewRef = webView != null ? new WeakReference<>(webView) : null;
            gozarProxySetterError.errorCode = errorCode;
            gozarProxySetterError.haveErrorCode = true;
        }
        return gozarProxySetterError;
    }

    /** Backward-compatible overload for GozarWebView */
    protected static synchronized GozarProxySetter getInstance(String link, GozarWebView webView, int errorCode) {
        return getInstance(link, (WebView) webView, errorCode);
    }

    GozarProxySetter(String link) {
        handler = new Handler(Looper.getMainLooper());
        this.link = link;
        this.haveErrorCode = false;
        this.webViewRef = null;
    }

    GozarProxySetter(String link, WebView webView, int errorCode) {
        handler = new Handler(Looper.getMainLooper());
        this.link = link;
        this.webViewRef = webView != null ? new WeakReference<>(webView) : null;
        this.errorCode = errorCode;
        haveErrorCode = true;
    }

    /**
     * Background work — determines best proxy based on connection response.
     * Uses ProxyProvider instead of hardcoded proxy constants.
     */
    protected Integer doInBackground() {
        if (link == null) return Utils.getInstance().choseBestProxyForLink(link);

        int code = haveErrorCode ? errorCode : Utils.getInstance().getLinkResponse(link);
        switch (code) {
            case Constants.ERROR_TUNNEL_CODE:
                TahrimGozar.getInstance().removeProxy();
                break;
            case ERROR_DENIED_PERMISSION_CODE:
                // Use ProxyProvider to get available proxies instead of hardcoded constants
                ProxyProvider provider = TahrimGozar.getInstance().getProxyProvider();
                if (provider != null) {
                    List<ProxyInfo> proxies = provider.getAvailableProxies();
                    if (!proxies.isEmpty()) {
                        TahrimGozar.getInstance().setCustomProxy(proxies.get(0));
                    }
                }
                break;
        }
        return PROXY_OK;
    }

    /**
     * Execute works asynchronously or on main thread.
     * 
     * @param runnable For webView works, if you do not have work, pass null
     * @param onMainThread If you want run method on main thread, pass true, otherwise pass false
     */
    protected void execute(Runnable runnable, boolean onMainThread) {
        Runnable works = () -> {
            if (doInBackground() == PROXY_OK) {
                if (webViewRef != null && haveErrorCode) {
                    WebView wv = webViewRef.get();
                    if (wv != null) {
                        handler.post(wv::reload);
                    }
                }
                if (runnable != null) handler.post(runnable);
                Utils.getInstance().showLog("Proxy automatic setup finished.", Log.DEBUG);
            } else {
                Utils.getInstance().showLog("Fail to set proxy for your link.", Log.DEBUG);
            }
        };
        if (onMainThread) works.run();
        else EXECUTOR.execute(works);
    }
}
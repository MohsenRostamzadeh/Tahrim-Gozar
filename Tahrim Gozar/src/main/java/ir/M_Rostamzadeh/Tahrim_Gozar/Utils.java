package ir.M_Rostamzadeh.Tahrim_Gozar;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.APP_NAME;
import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.PROXY_FAIL;
import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.PROXY_OK;
import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.isDebugMode;

class Utils {

    private static volatile Utils utils;

    /**Get singleton instance (thread-safe, double-checked locking)*/
    public static Utils getInstance() {
        if (utils == null) {
            synchronized (Utils.class) {
                if (utils == null) utils = new Utils();
            }
        }
        return utils;
    }

    /** Check internet connection
     * @return if device is connected to internet, return true else return false*/
    protected boolean isOnline() {
        Context context = TahrimGozar.getInstance().context;
        if (context == null) return false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork == null) return false;
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                return networkCapabilities != null && (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
            } else {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnectedOrConnecting();
            }
        }
        return false;
    }

    /**Set system-level proxy properties for non-WebView HTTP clients (OkHttp, Volley, HttpURLConnection).
     * Supports HTTP, HTTPS, SOCKS4, and SOCKS5 proxy types.
     * @param proxyInfo Proxy information to set*/
    protected void setSystemProxy(ProxyInfo proxyInfo) {
        if (proxyInfo == null || proxyInfo.getPort() == 0) {
            clearSystemProxy();
            return;
        }

        switch (proxyInfo.getType()) {
            case SOCKS4:
            case SOCKS5:
                System.setProperty("socksProxyHost", proxyInfo.getHost());
                System.setProperty("socksProxyPort", String.valueOf(proxyInfo.getPort()));
                // Clear HTTP/HTTPS proxy to avoid conflicts
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
                System.clearProperty("https.proxyHost");
                System.clearProperty("https.proxyPort");
                showLog("System SOCKS proxy set: " + proxyInfo, Log.DEBUG);
                break;
            default: // HTTP, HTTPS
                System.setProperty("http.proxyHost", proxyInfo.getHost());
                System.setProperty("http.proxyPort", String.valueOf(proxyInfo.getPort()));
                System.setProperty("https.proxyHost", proxyInfo.getHost());
                System.setProperty("https.proxyPort", String.valueOf(proxyInfo.getPort()));
                // Clear SOCKS proxy to avoid conflicts
                System.clearProperty("socksProxyHost");
                System.clearProperty("socksProxyPort");
                showLog("System HTTP proxy set: " + proxyInfo, Log.DEBUG);
                break;
        }
    }

    /**Clear all system-level proxy properties*/
    protected void clearSystemProxy() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        showLog("System proxy cleared", Log.DEBUG);
    }

    /**Set proxy for WebView using official ProxyController API (androidx.webkit).
     * Falls back to system properties if ProxyController is not supported.
     * @param proxyInfo Proxy information to set
     * @param onSuccess Callback when proxy is set successfully, can be null
     * @param onFailure Callback when proxy setting fails, can be null*/
    protected void setWebViewProxy(ProxyInfo proxyInfo, Runnable onSuccess, Runnable onFailure) {
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                String proxyRule = proxyInfo.getHost() + ":" + proxyInfo.getPort();
                // Use scheme prefix for non-HTTP proxy types
                if (proxyInfo.getType() == ProxyInfo.ProxyType.SOCKS5) {
                    proxyRule = "socks5://" + proxyRule;
                } else if (proxyInfo.getType() == ProxyInfo.ProxyType.SOCKS4) {
                    proxyRule = "socks4://" + proxyRule;
                } else if (proxyInfo.getType() == ProxyInfo.ProxyType.HTTPS) {
                    proxyRule = "https://" + proxyRule;
                }

                ProxyConfig proxyConfig = new ProxyConfig.Builder()
                        .addProxyRule(proxyRule)
                        .addDirect() // Fallback to direct connection
                        .build();

                ProxyController.getInstance().setProxyOverride(
                        proxyConfig,
                        Runnable::run,
                        () -> {
                            showLog("WebView proxy set via ProxyController: " + proxyInfo, Log.DEBUG);
                            if (onSuccess != null) onSuccess.run();
                        }
                );
            } else {
                showLog("ProxyController not supported, using system properties only", Log.WARN);
                setSystemProxy(proxyInfo);
                if (onSuccess != null) onSuccess.run();
            }
        } catch (Exception e) {
            showLog("Failed to set WebView proxy: " + e.getMessage(), Log.ERROR);
            if (onFailure != null) onFailure.run();
        }
    }

    /**Clear WebView proxy override
     * @param onComplete Callback when proxy is cleared, can be null*/
    protected void clearWebViewProxy(Runnable onComplete) {
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyController.getInstance().clearProxyOverride(
                        Runnable::run,
                        () -> {
                            showLog("WebView proxy cleared via ProxyController", Log.DEBUG);
                            if (onComplete != null) onComplete.run();
                        }
                );
            } else {
                if (onComplete != null) onComplete.run();
            }
        } catch (Exception e) {
            showLog("Failed to clear WebView proxy: " + e.getMessage(), Log.ERROR);
            if (onComplete != null) onComplete.run();
        }
    }

    /**Show log for debugging library
     * @param s Log message
     * @param type Log type <br/>
     * for example Log.ERROR or Log.INFO and...*/
    protected void showLog(String s, int type) {
        if (!isDebugMode) return;
        switch (type) {
            case Log.ERROR:
                Log.e(APP_NAME + "_", s);
                break;
            case Log.INFO:
                Log.i(APP_NAME + "_", s);
                break;
            case Log.WARN:
                Log.w(APP_NAME + "_", s);
                break;
            case Log.DEBUG:
                Log.d(APP_NAME + "_", s);
                break;
            case Log.VERBOSE:
                Log.v(APP_NAME + "_", s);
                break;
        }
    }

    /**Get link connectivity response code with retry support.
     * @param link Web URL <br/> Make sure your link contains 'http' or 'https',
     *             otherwise this method does not work
     * @return Response code*/
    public int getLinkResponse(String link) {
        return getLinkResponse(link, 1);
    }

    /**Internal method with retry count
     * @param link Web URL
     * @param retriesLeft Number of retries remaining
     * @return Response code*/
    private int getLinkResponse(String link, int retriesLeft) {
        int responseCode = 10;
        if (!isOnline()) return responseCode;
        String linkAddress = (link == null ? Constants.Sanctions_link : link);
        try {
            URL url = new URL(linkAddress);
            if (linkAddress.contains(Constants.https)) {
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.connect();
                responseCode = conn.getResponseCode();
                conn.disconnect();
            } else {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.connect();
                responseCode = conn.getResponseCode();
                conn.disconnect();
            }
        } catch (java.net.SocketTimeoutException e) {
            showLog("Connection timeout: " + e.getMessage(), Log.WARN);
            responseCode = Constants.ERROR_TIME_OUT_CODE;
        } catch (javax.net.ssl.SSLException e) {
            showLog("SSL/Tunnel error: " + e.getMessage(), Log.WARN);
            responseCode = Constants.ERROR_TUNNEL_CODE;
        } catch (Exception e) {
            showLog(e.toString(), Log.ERROR);
        }
        if (responseCode == Constants.ERROR_TIME_OUT_CODE && retriesLeft > 0) {
            return getLinkResponse(link, retriesLeft - 1);
        }
        return responseCode;
    }

    /**Check proxy connectivity using ProxyProvider
     * @param proxy Proxy to test, null to test without proxy
     * @param link Site link
     * @return Response code*/
    protected int checkProxy(ProxyInfo proxy, String link) {
        try {
            if (proxy == null) {
                TahrimGozar.getInstance().removeProxy();
            } else {
                TahrimGozar.getInstance().setCustomProxy(proxy);
            }
        } catch (Exception e) {
            showLog("Error checking proxy: " + e.getMessage(), Log.ERROR);
        }
        return getLinkResponse(link);
    }

    /**Choose best proxy for link from available proxies
     * @param link Website link
     * @return Link proxy status (PROXY_OK or PROXY_FAIL)*/
    protected Integer choseBestProxyForLink(String link) {
        ProxyProvider provider = TahrimGozar.getInstance().getProxyProvider();
        if (provider == null) return PROXY_FAIL;

        for (ProxyInfo proxy : provider.getAvailableProxies()) {
            int responseCode = checkProxy(proxy, link);
            if (responseCode != Constants.ERROR_DENIED_PERMISSION_CODE
                    && responseCode != Constants.ERROR_TUNNEL_CODE) {
                return PROXY_OK;
            }
        }
        return PROXY_FAIL;
    }
}
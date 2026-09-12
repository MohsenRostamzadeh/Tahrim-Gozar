package ir.M_Rostamzadeh.Tahrim_Gozar.tunnel;

import android.content.Context;
import android.util.Log;

import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Intelligent in-app traffic router (Java Sockets, HttpURLConnection, OkHttp, and WebView)
 * without requiring device-wide system VPN permissions.
 * هدایت‌کننده هوشمند ترافیک کل اپلیکیشن (Java Sockets, HttpURLConnection, OkHttp و WebView)
 * بدون درگیر کردن VPN سیستم‌عامل.
 */
public class InAppTrafficRouter {

    private static final String TAG = "InAppTrafficRouter";

    private static final java.util.concurrent.Executor ROUTER_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TahrimGozar-TrafficRouter");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean isRouted = new AtomicBoolean(false);
    private ProxySelector originalProxySelector;
    private Authenticator originalAuthenticator;

    private SmartRoutingRules routingRules;

    public void setRoutingRules(SmartRoutingRules routingRules) {
        this.routingRules = routingRules;
    }

    public SmartRoutingRules getRoutingRules() {
        return routingRules;
    }

    /**
     * Routes application traffic to the given host and port.
     * هدایت ترافیک اپلیکیشن به هاست و پورت مشخص.
     */
    public synchronized void applyRouting(Context context,
                                         String host,
                                         int port,
                                         Proxy.Type proxyType,
                                         String username,
                                         String password,
                                         boolean routeWebView,
                                         boolean routeGlobalHttp) {
        applyRouting(context, host, port, proxyType, username, password, routeWebView, routeGlobalHttp, port, proxyType);
    }

    /**
     * هدایت ترافیک اپلیکیشن به هاست و پورت مشخص با امکان تعیین پورت مجزا برای وب‌ویو
     */
    public synchronized void applyRouting(Context context,
                                         String host,
                                         int port,
                                         Proxy.Type proxyType,
                                         String username,
                                         String password,
                                         boolean routeWebView,
                                         boolean routeGlobalHttp,
                                         int webViewPort,
                                         Proxy.Type webViewProxyType) {
        if (isRouted.get()) {
            resetRouting(context);
        }

        // ۱. تنظیم شبکه سراسری جاوا و کتابخانه‌های HTTP
        if (routeGlobalHttp) {
            originalProxySelector = ProxySelector.getDefault();
            final Proxy appProxy = new Proxy(proxyType != null ? proxyType : Proxy.Type.HTTP,
                    new InetSocketAddress(host, port));
            final SmartRoutingRules currentRules = this.routingRules;

            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    if (uri == null) return Collections.singletonList(appProxy);
                    String h = uri.getHost();
                    if (h != null) {
                        if (currentRules != null && currentRules.shouldBypass(h)) {
                            return Collections.singletonList(Proxy.NO_PROXY);
                        }
                        if (h.equalsIgnoreCase("localhost") || h.startsWith("127.")) {
                            return Collections.singletonList(Proxy.NO_PROXY);
                        }
                    }
                    return Collections.singletonList(appProxy);
                }

                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                    Log.w(TAG, "Connection failed through in-app proxy: " + sa, ioe);
                }
            });

            // تنظیم System Properties برای اتصال‌های سازگار با سیستم
            if (proxyType == Proxy.Type.SOCKS) {
                System.setProperty("socksProxyHost", host);
                System.setProperty("socksProxyPort", String.valueOf(port));
            } else {
                System.setProperty("http.proxyHost", host);
                System.setProperty("http.proxyPort", String.valueOf(port));
                System.setProperty("https.proxyHost", host);
                System.setProperty("https.proxyPort", String.valueOf(port));
            }

            // تنظیم احراز هویت در صورت نیاز
            if (username != null && !username.isEmpty()) {
                final String user = username;
                final char[] pass = (password != null) ? password.toCharArray() : new char[0];
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestorType() == RequestorType.PROXY) {
                            return new PasswordAuthentication(user, pass);
                        }
                        return super.getPasswordAuthentication();
                    }
                });
            }
        }

        // ۲. تنظیم پروکسی وب‌ویوهای اندروید
        if (routeWebView) {
            try {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                    int targetWvPort = webViewPort > 0 ? webViewPort : port;
                    Proxy.Type targetWvType = webViewProxyType != null ? webViewProxyType : proxyType;
                    String scheme = (targetWvType == Proxy.Type.SOCKS) ? "socks://" : "http://";
                    ProxyConfig.Builder proxyConfigBuilder = new ProxyConfig.Builder()
                            .addProxyRule(scheme + host + ":" + targetWvPort)
                            .addBypassRule("<local>")
                            .addBypassRule("*.local");

                    if (routingRules != null && routingRules.isEnabled()) {
                        if (routingRules.isBypassIranTraffic()) {
                            proxyConfigBuilder.addBypassRule("*.ir");
                        }
                        for (String b : routingRules.getBypassDomains()) {
                            proxyConfigBuilder.addBypassRule(b);
                        }
                    }

                    ProxyConfig proxyConfig = proxyConfigBuilder.build();

                    ProxyController.getInstance().setProxyOverride(proxyConfig,
                            ROUTER_EXECUTOR,
                            () -> Log.i(TAG, "WebView proxy override applied to " + scheme + host + ":" + targetWvPort));
                }
            } catch (Throwable t) {
                Log.w(TAG, "Could not set WebView ProxyOverride", t);
            }
        }

        isRouted.set(true);
    }

    /**
     * بازگردانی تمام تنظیمات شبکه به حالت طبیعی اولیه اپلیکیشن
     */
    public synchronized void resetRouting(Context context) {
        if (!isRouted.get()) {
            return;
        }

        // ۱. ریست ProxySelector
        if (originalProxySelector != null) {
            ProxySelector.setDefault(originalProxySelector);
            originalProxySelector = null;
        }

        // ۲. پاکسازی System Properties
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");

        // ۳. ریست Authenticator
        if (originalAuthenticator != null) {
            Authenticator.setDefault(originalAuthenticator);
            originalAuthenticator = null;
        }

        // ۴. پاکسازی وب‌ویو
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyController.getInstance().clearProxyOverride(
                        ROUTER_EXECUTOR,
                        () -> Log.i(TAG, "WebView proxy override cleared."));
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not clear WebView ProxyOverride", t);
        }

        isRouted.set(false);
    }

    public boolean isRouted() {
        return isRouted.get();
    }
}

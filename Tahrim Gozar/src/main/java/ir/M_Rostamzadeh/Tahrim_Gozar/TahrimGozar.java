package ir.M_Rostamzadeh.Tahrim_Gozar;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ir.M_Rostamzadeh.Tahrim_Gozar.config.ConfigType;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.UniversalConfigParser;
import ir.M_Rostamzadeh.Tahrim_Gozar.dns.TahrimDns;
import ir.M_Rostamzadeh.Tahrim_Gozar.traffic.TrafficStatsListener;
import ir.M_Rostamzadeh.Tahrim_Gozar.traffic.TrafficStatsTracker;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.SmartRoutingRules;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.StandardProxyEngine;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.SubscriptionHealthListener;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.SubscriptionManager;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.TunnelCallback;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.TunnelEngine;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.TunnelHealthMonitor;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.TunnelSession;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.XrayBridgeEngine;

import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.useInWebView;

/**
 * Main entry point for Tahrim Gozar library.
 * نقطه ورود اصلی کتابخانه تحریم‌گذر.
 * <p>
 * Provides in-app anti-censorship and anti-sanction tunneling with support for DNS presets,
 * standard proxies, V2Ray/Xray protocols, multi-config subscriptions, sticky connections,
 * and 20-second periodic health monitoring with smart auto-failover.
 * <br>
 * پشتیبانی کامل از کدهای دی‌ان‌اس ضدتحریم، پروکسی‌های استاندارد، پروتکل‌های V2Ray/Xray،
 * سابسکرپشن‌های چندکانفیگه، استراتژی Sticky Connection و پایش مداوم ۲۰ ثانیه‌ای همراه با Auto-Failover.
 * </p>
 */
public class TahrimGozar {

    private static final String TAG = "TahrimGozar";
    private static volatile TahrimGozar tahrimGozar;

    protected Context context;
    protected boolean isUseProxy = false;
    private ProxyInfo currentProxy;
    private ProxyProvider proxyProvider;
    private TahrimGozarListener listener;

    // In-app tunneling engines and monitors / موتورها و ناظرهای تونلینگ درون‌برنامه‌ای
    private TunnelConfig currentTunnelConfig;
    private TunnelEngine activeEngine;
    private TunnelSession activeSession;
    private final StandardProxyEngine standardProxyEngine = new StandardProxyEngine();
    private final XrayBridgeEngine xrayBridgeEngine = new XrayBridgeEngine();
    private final SubscriptionManager subscriptionManager = new SubscriptionManager();
    private final TunnelHealthMonitor healthMonitor = new TunnelHealthMonitor(subscriptionManager);
    private SubscriptionHealthListener subscriptionHealthListener;

    // Advanced roadmap features / امکانات پیشرفته نقشه راه
    private final TrafficStatsTracker trafficStatsTracker = new TrafficStatsTracker();
    private final SmartRoutingRules smartRoutingRules = new SmartRoutingRules();
    private boolean dnsRaceEnabled = false;

    /**
     * Gets the thread-safe singleton instance.
     * دریافت نمونه Singleton کلاس (Thread-Safe).
     *
     * @return {@link TahrimGozar} instance
     */
    public static TahrimGozar getInstance() {
        if (tahrimGozar == null) {
            synchronized (TahrimGozar.class) {
                if (tahrimGozar == null) {
                    tahrimGozar = new TahrimGozar();
                }
            }
        }
        return tahrimGozar;
    }

    /**
     * Fluent builder entry point for concise and modern setup.
     * سازنده فلوئنت برای راه‌اندازی سریع و مدرن.
     *
     * @param context Android context / کانتکست برنامه
     * @return {@link TunnelBuilder}
     */
    public static TunnelBuilder with(@NonNull Context context) {
        getInstance().init(context);
        return new TunnelBuilder(getInstance());
    }

    /**
     * Initializes the library with default configuration.
     * مقداردهی اولیه کتابخانه با ارائه‌دهنده پیش‌فرض.
     *
     * @param context Application context / کانتکست برنامه
     */
    public void init(@NonNull Context context) {
        this.context = context.getApplicationContext();
        if (this.proxyProvider == null) {
            this.proxyProvider = new DefaultProxyProvider();
        }
    }

    /**
     * Initializes the library with debug logging option.
     * مقداردهی اولیه کتابخانه به همراه تعیین وضعیت حالت دیباگ.
     */
    public void init(@NonNull Context context, boolean isDebugMode) {
        init(context);
        setIsDebugMode(isDebugMode);
    }

    /**
     * Initializes the library with a custom proxy provider.
     * مقداردهی اولیه با ارائه‌دهنده پروکسی سفارشی.
     */
    public void init(@NonNull Context context, @NonNull ProxyProvider provider) {
        this.context = context.getApplicationContext();
        this.proxyProvider = provider;
    }

    /**
     * Initializes the library with a custom proxy provider and debug logging option.
     * مقداردهی اولیه با ارائه‌دهنده پروکسی سفارشی و لاگ دیباگ.
     */
    public void init(@NonNull Context context, @NonNull ProxyProvider provider, boolean isDebugMode) {
        init(context, provider);
        setIsDebugMode(isDebugMode);
    }

    // =========================================================================
    // Modern In-App Tunneling & Subscriptions / تونلینگ درون‌برنامه‌ای و سابسکرپشن
    // =========================================================================

    /**
     * Sets the tunnel configuration from any raw string (DNS preset, VLESS, VMess, SOCKS5, HTTP, etc.).
     * تنظیم کانفیگ تونل از روی هر نوع رشته ورودی (کد DNS، لینک VLESS، VMess، SOCKS5 و ...).
     *
     * @param rawConfig Configuration string / رشته کانفیگ
     * @return this instance
     */
    public TahrimGozar setTunnelConfig(@NonNull String rawConfig) {
        this.currentTunnelConfig = UniversalConfigParser.parse(rawConfig);
        return this;
    }

    /**
     * Sets a structured {@link TunnelConfig} instance directly.
     * تنظیم مستقیم شیء ساختاریافته {@link TunnelConfig}.
     *
     * @param config Parsed configuration / کانفیگ ساختاریافته
     * @return this instance
     */
    public TahrimGozar setTunnelConfig(@NonNull TunnelConfig config) {
        this.currentTunnelConfig = config;
        return this;
    }

    /**
     * Sets a V2Ray/Xray subscription URL or raw multi-config content.
     * تنظیم لینک یا متن چندخطی سابسکرپشن ویتوری/اکس‌ری.
     *
     * @param subscriptionUrlOrContent Subscription HTTP/HTTPS URL or multi-line content / آدرس یا محتوا
     * @return this instance
     */
    public TahrimGozar setSubscription(@NonNull String subscriptionUrlOrContent) {
        this.currentTunnelConfig = new TunnelConfig.Builder(ConfigType.SUBSCRIPTION)
                .rawConfig(subscriptionUrlOrContent)
                .name("Subscription")
                .build();
        return this;
    }

    /**
     * Enables or disables automatic failover to healthy alternatives when ping drops.
     * فعال یا غیرفعال‌سازی سوییچ خودکار به سرور سالم در صورت افت پینگ (Auto-Failover).
     *
     * @param enable True to enable auto failover / فعال‌سازی سوییچ خودکار
     * @return this instance
     */
    public TahrimGozar enableAutoFailover(boolean enable) {
        this.subscriptionManager.setAutoFailoverEnabled(enable);
        return this;
    }

    /**
     * Sets the health check interval (default: 20 seconds).
     * تنظیم بازه زمانی پایش دوره‌ای سلامت اتصال (پیش‌فرض ۲۰ ثانیه).
     *
     * @param interval Time value / مقدار زمان
     * @param unit Time unit / واحد زمان
     * @return this instance
     */
    public TahrimGozar setHealthCheckInterval(long interval, TimeUnit unit) {
        long ms = unit.toMillis(interval);
        this.subscriptionManager.setHealthCheckIntervalMs(ms);
        this.healthMonitor.setIntervalMs(ms);
        return this;
    }

    /**
     * Sets the target HTTP/HTTPS URL for health checking probes (default: gstatic generate_204).
     * تنظیم آدرس پیش‌فرض یا اختصاصی جهت تست سلامت اتصال در پایش دوره‌ای.
     *
     * @param url Target HTTP/HTTPS probe URL / آدرس مورد نظر
     * @return this instance
     */
    public TahrimGozar setHealthCheckUrl(String url) {
        this.subscriptionManager.setHealthCheckUrl(url);
        this.healthMonitor.setHealthCheckUrl(url);
        return this;
    }

    /**
     * Sets the listener for subscription health events and automatic failover.
     * تنظیم شنونده رخدادهای پایش سلامت و Failover سابسکرپشن.
     *
     * @param listener Health & failover listener / شنونده رخدادها
     * @return this instance
     */
    public TahrimGozar setSubscriptionHealthListener(@Nullable SubscriptionHealthListener listener) {
        this.subscriptionHealthListener = listener;
        this.healthMonitor.setListener(listener);
        return this;
    }

    /**
     * Gets the active health monitor instance.
     * دریافت نمونه ناظر سلامت اتصال.
     */
    public TunnelHealthMonitor getHealthMonitor() {
        return healthMonitor;
    }

    /**
     * Gets the active subscription manager instance.
     * دریافت نمونه مدیریت سابسکرپشن.
     */
    public SubscriptionManager getSubscriptionManager() {
        return subscriptionManager;
    }

    /**
     * Starts the in-app tunnel using the configured node or subscription.
     * شروع تونل درون‌برنامه‌ای با کانفیگ یا سابسکرپشن تنظیم‌شده.
     *
     * @param callback Connection callback / کال‌بک وضعیت اتصال
     */
    public void startTunnel(@Nullable TunnelCallback callback) {
        if (context == null) {
            throw new IllegalStateException("init(context) must be called before starting tunnel.");
        }
        if (currentTunnelConfig == null) {
            throw new IllegalStateException("setTunnelConfig(...) or setSubscription(...) must be called before starting tunnel.");
        }

        stopTunnel();

        if (currentTunnelConfig.getType() == ConfigType.SUBSCRIPTION) {
            String raw = currentTunnelConfig.getRawConfig();
            SubscriptionManager.SubscriptionCallback subCallback = new SubscriptionManager.SubscriptionCallback() {
                @Override
                public void onSuccess(List<SubscriptionManager.ConfigWithPing> configs, TunnelConfig selected) {
                    currentTunnelConfig = selected;
                    startEngineForConfig(selected, new TunnelCallback() {
                        @Override
                        public void onConnected(TunnelSession session) {
                            // Start periodic health monitor (20s default) with sticky connection strategy
                            // شروع ناظر سلامت دوره‌ای (پیش‌فرض ۲۰ ثانیه‌ای) با استراتژی Sticky
                            healthMonitor.startMonitoring(selected, session.getLocalPort(), subscriptionHealthListener);
                            if (callback != null) callback.onConnected(session);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (callback != null) callback.onError(throwable);
                        }

                        @Override
                        public void onDisconnected() {
                            healthMonitor.stopMonitoring();
                            if (callback != null) callback.onDisconnected();
                        }
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                    if (callback != null) callback.onError(throwable);
                }
            };

            if (raw.startsWith("http://") || raw.startsWith("https://")) {
                subscriptionManager.fetchAndTest(raw, subCallback);
            } else {
                subscriptionManager.parseAndTest(raw, subCallback);
            }
            return;
        }

        startEngineForConfig(currentTunnelConfig, new TunnelCallback() {
            @Override
            public void onConnected(TunnelSession session) {
                if (subscriptionHealthListener != null || subscriptionManager.isAutoFailoverEnabled()) {
                    healthMonitor.startMonitoring(currentTunnelConfig, session.getLocalPort(), subscriptionHealthListener);
                }
                if (callback != null) callback.onConnected(session);
            }

            @Override
            public void onError(Throwable throwable) {
                if (callback != null) callback.onError(throwable);
            }

            @Override
            public void onDisconnected() {
                healthMonitor.stopMonitoring();
                if (callback != null) callback.onDisconnected();
            }
        });
    }

    /**
     * Performs a seamless in-place engine switch to an alternative node during auto-failover.
     * سوییچ سریع به کانفیگ جایگزین هنگام رخداد Auto-Failover.
     *
     * @param newConfig Replacement configuration / کانفیگ جایگزین
     * @param callback Callback / کال‌بک اتصال
     */
    public void switchActiveTunnel(@NonNull TunnelConfig newConfig, @Nullable TunnelCallback callback) {
        Log.i(TAG, "Switching active tunnel engine to new config: " + newConfig.getName());
        if (activeEngine != null) {
            activeEngine.stop();
            activeEngine = null;
        }

        this.currentTunnelConfig = newConfig;
        startEngineForConfig(newConfig, new TunnelCallback() {
            @Override
            public void onConnected(TunnelSession session) {
                activeSession = session;
                isUseProxy = true;
                healthMonitor.updateActiveSession(newConfig, session.getLocalPort());
                if (callback != null) callback.onConnected(session);
            }

            @Override
            public void onError(Throwable throwable) {
                if (callback != null) callback.onError(throwable);
            }

            @Override
            public void onDisconnected() {
                if (callback != null) callback.onDisconnected();
            }
        });
    }

    private void startEngineForConfig(TunnelConfig config, @Nullable TunnelCallback callback) {
        try {
            if (config.isV2RayConfig()) {
                activeEngine = xrayBridgeEngine;
            } else {
                activeEngine = standardProxyEngine;
            }

            activeEngine.start(context, config, new TunnelCallback() {
                @Override
                public void onConnected(TunnelSession session) {
                    activeSession = session;
                    isUseProxy = true;
                    if (callback != null) {
                        callback.onConnected(session);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    isUseProxy = false;
                    activeSession = null;
                    if (callback != null) {
                        callback.onError(throwable);
                    }
                }

                @Override
                public void onDisconnected() {
                    isUseProxy = false;
                    activeSession = null;
                    if (callback != null) {
                        callback.onDisconnected();
                    }
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start tunnel engine", t);
            if (callback != null) {
                callback.onError(t);
            }
        }
    }

    /**
     * Stops the in-app tunnel and restores default network routing.
     * قطع کامل تونل درون‌برنامه‌ای و متوقف‌سازی ناظر سلامت.
     */
    public void stopTunnel() {
        healthMonitor.stopMonitoring();
        if (activeEngine != null) {
            activeEngine.stop();
            activeEngine = null;
        }
        activeSession = null;
        isUseProxy = false;
    }

    /**
     * Checks if the in-app tunnel is currently active.
     * بررسی فعال بودن تونل درون‌برنامه‌ای.
     */
    public boolean isTunnelActive() {
        return activeSession != null && activeSession.isActive() &&
                activeEngine != null && activeEngine.isRunning();
    }

    /**
     * Gets the active tunnel session.
     * دریافت نشست فعال فعلی تونل.
     */
    @Nullable
    public TunnelSession getActiveSession() {
        return activeSession;
    }

    /**
     * Gets the current active tunnel configuration.
     * دریافت کانفیگ فعال فعلی.
     */
    @Nullable
    public TunnelConfig getCurrentTunnelConfig() {
        return currentTunnelConfig;
    }

    /**
     * Gets a custom DNS resolver instance for use in third-party networking libraries.
     * دریافت رزولور دی‌ان‌اس اختصاصی برای استفاده در کتابخانه‌های دلخواه.
     */
    public TahrimDns getDns() {
        TahrimDns dns;
        if (currentTunnelConfig != null && currentTunnelConfig.isDnsConfig()) {
            dns = new TahrimDns(currentTunnelConfig.getDnsServers(), currentTunnelConfig.getDohUrl());
        } else {
            dns = TahrimDns.shecan();
        }
        dns.setRaceEnabled(dnsRaceEnabled);
        return dns;
    }

    // =========================================================================
    // Real-Time Traffic & Speed Metering / پایش زنده ترافیک و سرعت
    // =========================================================================

    /**
     * Enables or disables real-time traffic and speed metering.
     * فعال یا غیرفعال‌سازی سیستم پایش زنده مصرف ترافیک و سرعت لحظه‌ای.
     *
     * @param enable True to enable / وضعیت فعال یا غیرفعال
     * @return this instance
     */
    public TahrimGozar enableTrafficStats(boolean enable) {
        trafficStatsTracker.setEnabled(enable);
        return this;
    }

    /**
     * Checks if traffic metering is currently enabled.
     * بررسی وضعیت فعال بودن سیستم پایش ترافیک.
     */
    public boolean isTrafficStatsEnabled() {
        return trafficStatsTracker.isEnabled();
    }

    /**
     * Sets the real-time traffic statistics listener.
     * تنظیم شنونده زنده رخدادهای ترافیک و سرعت لحظه‌ای (هر ثانیه یک‌بار).
     *
     * @param listener Traffic stats listener / شنونده آمار ترافیک
     * @return this instance
     */
    public TahrimGozar setTrafficStatsListener(@Nullable TrafficStatsListener listener) {
        trafficStatsTracker.setListener(listener);
        return this;
    }

    /**
     * Gets the traffic statistics tracker instance.
     * دریافت نمونه سیستم ثبت آمار ترافیک.
     */
    public TrafficStatsTracker getTrafficStatsTracker() {
        return trafficStatsTracker;
    }

    // =========================================================================
    // Split Tunneling & Smart Routing / تونلینگ هوشمند و تفکیک ترافیک
    // =========================================================================

    /**
     * Enables or disables split tunneling.
     * فعال یا غیرفعال‌سازی قابلیت تفکیک ترافیک (Split Tunneling).
     *
     * @param enable True to enable / فعال یا غیرفعال
     * @return this instance
     */
    public TahrimGozar enableSplitTunneling(boolean enable) {
        smartRoutingRules.setEnabled(enable);
        return this;
    }

    /**
     * Checks if split tunneling is currently enabled.
     * بررسی فعال بودن قابلیت تفکیک ترافیک.
     */
    public boolean isSplitTunnelingEnabled() {
        return smartRoutingRules.isEnabled();
    }

    /**
     * Enables or disables automatic domestic country traffic bypass.
     * فعال یا غیرفعال‌سازی عبور مستقیم ترافیک دامنه‌های ملی/داخلی کشورها.
     *
     * @param bypass True to bypass domestic traffic / عبور مستقیم ترافیک داخلی
     * @return this instance
     */
    public TahrimGozar setBypassDomesticTraffic(boolean bypass) {
        smartRoutingRules.setBypassDomesticTraffic(bypass);
        return this;
    }

    /**
     * Checks if domestic traffic bypass is active.
     * بررسی فعال بودن عبور مستقیم ترافیک دامنه‌های ملی/داخلی.
     */
    public boolean isBypassDomesticTraffic() {
        return smartRoutingRules.isBypassDomesticTraffic();
    }

    /**
     * Adds a domestic top-level domain (TLD) for automatic direct bypass (e.g. ".ir", ".ru", ".cn").
     * افزودن پسوند دامنه ملی جهت عبور مستقیم ترافیک داخلی.
     *
     * @param tld Domain extension (e.g. ".ir", "ru", ".cn")
     * @return this instance
     */
    public TahrimGozar addDomesticTld(@NonNull String tld) {
        smartRoutingRules.addDomesticTld(tld);
        return this;
    }

    /**
     * Sets the active domestic TLDs list.
     * جایگزینی کامل لیست پسوندهای ملی داخلی.
     */
    public TahrimGozar setDomesticTlds(@NonNull java.util.Collection<String> tlds) {
        smartRoutingRules.setDomesticTlds(tlds);
        return this;
    }

    /**
     * Enables or disables automatic domestic (.ir) traffic bypass (backward-compatible alias).
     * فعال یا غیرفعال‌سازی عبور مستقیم ترافیک سایت‌ها و سرورهای ایرانی (.ir) از اینترنت معمولی.
     *
     * @param bypass True to bypass domestic Iranian traffic / عبور مستقیم ترافیک داخلی
     * @return this instance
     */
    public TahrimGozar setBypassIranTraffic(boolean bypass) {
        return setBypassDomesticTraffic(bypass);
    }

    /**
     * Checks if domestic Iranian traffic bypass is active (backward-compatible alias).
     * بررسی فعال بودن عبور مستقیم ترافیک سرورهای ایرانی.
     */
    public boolean isBypassIranTraffic() {
        return isBypassDomesticTraffic();
    }

    /**
     * Adds a custom domain to bypass the proxy/tunnel (direct connection).
     * افزودن دامنه اختصاصی برای اتصال مستقیم و عبور از خارج از تونل.
     *
     * @param domain Domain pattern (e.g. "shaparak.ir", "*.snapp.ir")
     * @return this instance
     */
    public TahrimGozar addBypassDomain(@NonNull String domain) {
        smartRoutingRules.addBypassDomain(domain);
        return this;
    }

    /**
     * Adds a custom domain to always route through the proxy/tunnel.
     * افزودن دامنه اختصاصی جهت الزام به عبور از درون تونل تحریم‌گذر.
     *
     * @param domain Domain pattern (e.g. "openai.com", "*.docker.com")
     * @return this instance
     */
    public TahrimGozar addProxyDomain(@NonNull String domain) {
        smartRoutingRules.addProxyDomain(domain);
        return this;
    }

    /**
     * Gets the active smart routing rules instance.
     * دریافت قوانین جاری تفکیک ترافیک.
     */
    public SmartRoutingRules getSmartRoutingRules() {
        return smartRoutingRules;
    }

    // =========================================================================
    // Concurrent DNS Race / حل همزمان و مسابقه‌ای دی‌ان‌اس
    // =========================================================================

    /**
     * Enables or disables Happy Eyeballs concurrent DNS racing.
     * فعال یا غیرفعال‌سازی الگوریتم مسابقه همزمان سرورهای DNS برای رسیدن به حداقل پینگ (زیر ۳۰ میلی‌ثانیه).
     *
     * @param enable True to enable / فعال یا غیرفعال
     * @return this instance
     */
    public TahrimGozar enableDnsRace(boolean enable) {
        this.dnsRaceEnabled = enable;
        return this;
    }

    /**
     * Checks if DNS racing is enabled.
     * بررسی فعال بودن مسابقه همزمان DNS.
     */
    public boolean isDnsRaceEnabled() {
        return dnsRaceEnabled;
    }

    /**
     * Automatically configures Tahrim Gozar's proxy onto an OkHttpClient.Builder without hard compile-time dependency.
     * اعمال خودکار پروکسی تحریم‌گذر روی هر نمونه از OkHttpClient.Builder بدون وابستگی اجباری کامپایل.
     *
     * @param okHttpClientBuilder OkHttpClient.Builder instance
     * @param <T> Builder generic type
     * @return Configured builder instance
     */
    public <T> T applyTo(T okHttpClientBuilder) {
        if (okHttpClientBuilder == null) return null;
        try {
            int port = (activeSession != null) ? activeSession.getLocalPort() : 0;
            if (port > 0) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", port));
                Method proxyMethod = okHttpClientBuilder.getClass().getMethod("proxy", Proxy.class);
                proxyMethod.invoke(okHttpClientBuilder, proxy);
            }
        } catch (Throwable ignored) {
        }
        return okHttpClientBuilder;
    }

    // =========================================================================
    // Backward Compatible Legacy Proxy Methods / متدهای پروکسی قدیمی
    // =========================================================================

    /**
     * Removes system/WebView proxy configuration.
     * حذف پروکسی سیستمی/وب‌ویو.
     */
    public void removeProxy() {
        stopTunnel();
        currentProxy = null;
        isUseProxy = false;

        Utils.getInstance().clearSystemProxy();
        if (useInWebView) {
            Utils.getInstance().clearWebViewProxy(null);
        }

        if (listener != null) {
            try {
                listener.onProxyRemoved();
            } catch (Exception e) {
                Utils.getInstance().showLog("Listener error: " + e.getMessage(), Log.ERROR);
            }
        }
    }

    /**
     * Sets up default automatic proxy.
     * تنظیم خودکار پروکسی پیش‌فرض.
     */
    public void setupAutomaticProxy(boolean useForWebView) {
        setUseInWebView(useForWebView);
        if (proxyProvider != null && !proxyProvider.getAvailableProxies().isEmpty()) {
            setCustomProxy(proxyProvider.getAvailableProxies().get(0));
        } else {
            setCustomProxy(new ProxyInfo(Constants.PROXY_HOST_1, Constants.PROXY_PORT_1));
        }
    }

    public void setIsDebugMode(boolean isDebugMode) {
        Constants.isDebugMode = isDebugMode;
    }

    public void setCustomProxy(ProxyInfo proxyInfo) {
        if (proxyInfo == null || proxyInfo.getPort() == 0) {
            removeProxy();
            return;
        }

        ProxyInfo oldProxy = currentProxy;

        try {
            isUseProxy = true;
            currentProxy = proxyInfo;

            Utils.getInstance().setSystemProxy(proxyInfo);

            if (useInWebView) {
                Utils.getInstance().setWebViewProxy(proxyInfo, null, null);
            }

            if (listener != null) {
                listener.onProxyChanged(oldProxy, proxyInfo);
            }
        } catch (Exception e) {
            Utils.getInstance().showLog("Failed to set proxy: " + e.toString(), Log.ERROR);
            if (listener != null) {
                listener.onProxyFailed(proxyInfo, e);
            }
        }
    }

    public void setUseInWebView(boolean canUse) {
        useInWebView = canUse;
    }

    public boolean canUseInWebView() {
        return useInWebView;
    }

    public boolean isUseProxy() {
        return isUseProxy;
    }

    @Nullable
    public ProxyInfo getCurrentProxy() {
        return currentProxy;
    }

    public void setProxyProvider(@NonNull ProxyProvider provider) {
        this.proxyProvider = provider;
    }

    @Nullable
    public ProxyProvider getProxyProvider() {
        return proxyProvider;
    }

    public void setListener(@Nullable TahrimGozarListener listener) {
        this.listener = listener;
    }

    @Nullable
    public TahrimGozarListener getListener() {
        return listener;
    }

    // =========================================================================
    // Fluent TunnelBuilder / کلاس فلوئنت TunnelBuilder
    // =========================================================================

    /**
     * Fluent builder for assembling and launching tunnels.
     * کلاس فلوئنت برای پیکربندی و اجرای سریع تونل.
     */
    public static class TunnelBuilder {
        private final TahrimGozar instance;

        public TunnelBuilder(TahrimGozar instance) {
            this.instance = instance;
        }

        /** Sets tunnel configuration from raw string / تنظیم کانفیگ از روی رشته ورودی */
        public TunnelBuilder config(@NonNull String rawConfig) {
            instance.setTunnelConfig(rawConfig);
            return this;
        }

        /** Sets structured TunnelConfig / تنظیم شیء TunnelConfig */
        public TunnelBuilder config(@NonNull TunnelConfig config) {
            instance.setTunnelConfig(config);
            return this;
        }

        /** Sets subscription URL or content / تنظیم لینک یا متن سابسکرپشن */
        public TunnelBuilder subscription(@NonNull String subscriptionUrlOrContent) {
            instance.setSubscription(subscriptionUrlOrContent);
            return this;
        }

        /** Intercepts WebView traffic through this tunnel / هدایت وب‌ویوهای برنامه به تونل */
        public TunnelBuilder enableWebView(boolean enable) {
            instance.setUseInWebView(enable);
            return this;
        }

        /** Auto-selects fastest reachable node / انتخاب خودکار سریع‌ترین کانفیگ بر اساس پینگ */
        public TunnelBuilder autoSelectBestConfig(boolean auto) {
            instance.getSubscriptionManager().setAutoSelectBestConfig(auto);
            return this;
        }

        /** Enables auto failover on ping drops / فعال‌سازی سوییچ خودکار در صورت افت پینگ */
        public TunnelBuilder enableAutoFailover(boolean enable) {
            instance.enableAutoFailover(enable);
            return this;
        }

        /** Sets periodic health check interval / تنظیم بازه زمانی پایش سلامت */
        public TunnelBuilder healthCheckInterval(long interval, TimeUnit unit) {
            instance.setHealthCheckInterval(interval, unit);
            return this;
        }

        /** Sets custom health check URL / تنظیم آدرس دلخواه بررسی سلامت */
        public TunnelBuilder healthCheckUrl(@NonNull String url) {
            instance.setHealthCheckUrl(url);
            return this;
        }

        /** Sets health check & failover listener / تنظیم شنونده رخدادهای سلامت و انتقال */
        public TunnelBuilder healthListener(@Nullable SubscriptionHealthListener listener) {
            instance.setSubscriptionHealthListener(listener);
            return this;
        }

        /** Enables or disables real-time traffic and speed metering / فعال‌سازی ثبت ترافیک و سرعت */
        public TunnelBuilder trafficStats(boolean enable) {
            instance.enableTrafficStats(enable);
            return this;
        }

        /** Enables traffic stats with a listener / فعال‌سازی ثبت ترافیک همراه با شنونده */
        public TunnelBuilder trafficStats(boolean enable, @Nullable TrafficStatsListener listener) {
            instance.enableTrafficStats(enable);
            instance.setTrafficStatsListener(listener);
            return this;
        }

        /** Enables or disables split tunneling / فعال‌سازی تفکیک ترافیک */
        public TunnelBuilder splitTunneling(boolean enable) {
            instance.enableSplitTunneling(enable);
            return this;
        }

        /** Enables or disables domestic country traffic bypass / عبور مستقیم ترافیک دامنه‌های ملی داخلی */
        public TunnelBuilder bypassDomesticTraffic(boolean bypass) {
            instance.setBypassDomesticTraffic(bypass);
            return this;
        }

        /** Adds a domestic top-level domain (TLD) / افزودن پسوند دامنه ملی برای عبور مستقیم */
        public TunnelBuilder addDomesticTld(@NonNull String tld) {
            instance.addDomesticTld(tld);
            return this;
        }

        /** Enables or disables domestic Iranian (.ir) traffic bypass / عبور مستقیم ترافیک سایت‌های ایرانی */
        public TunnelBuilder bypassIranTraffic(boolean bypass) {
            instance.setBypassDomesticTraffic(bypass);
            return this;
        }

        /** Adds a domain to bypass the tunnel / افزودن دامنه برای عبور مستقیم */
        public TunnelBuilder addBypassDomain(@NonNull String domain) {
            instance.addBypassDomain(domain);
            return this;
        }

        /** Adds a domain to force through the tunnel / افزودن دامنه برای عبور اجباری از تونل */
        public TunnelBuilder addProxyDomain(@NonNull String domain) {
            instance.addProxyDomain(domain);
            return this;
        }

        /** Enables or disables Happy Eyeballs concurrent DNS race / فعال‌سازی مسابقه همزمان DNS */
        public TunnelBuilder dnsRace(boolean enable) {
            instance.enableDnsRace(enable);
            return this;
        }

        /** Starts the tunnel asynchronously / اجرای ناهمگام تونل */
        public void start(@Nullable TunnelCallback callback) {
            instance.startTunnel(callback);
        }
    }
}
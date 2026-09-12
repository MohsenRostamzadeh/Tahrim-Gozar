package ir.M_Rostamzadeh.Tahrim_Gozar.tunnel;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.UniversalConfigParser;

/**
 * Manages multi-node subscriptions, latency testing, candidate pooling, and intelligent failovers.
 * مدیریت سابسکرپشن‌های چندکانفیگه، تست پینگ/تاخیر کانفیگ‌ها و انتخاب هوشمند سریع‌ترین کانفیگ
 * همراه با استراتژی اتصال پایدار (Sticky Connection)، استخر نامزدها و سیستم Auto-Failover.
 */
public class SubscriptionManager {

    /** Default target URL for zero-payload connectivity check / آدرس پیش‌فرض پروب سبک بررسی اتصال */
    public static final String DEFAULT_HEALTH_CHECK_URL = "https://www.gstatic.com/generate_204";

    /** Default health check interval in milliseconds (20 seconds) / بازه زمانی پیش‌فرض پایش سلامت (۲۰ ثانیه) */
    public static final long DEFAULT_HEALTH_CHECK_INTERVAL_MS = 20_000L;

    /** Callback for subscription fetching and latency testing / کال‌بک دریافت و ارزیابی سابسکرپشن */
    public interface SubscriptionCallback {
        void onSuccess(List<ConfigWithPing> configs, TunnelConfig selected);
        void onError(Throwable throwable);
    }

    /** Callback for finding healthy alternative node / کال‌بک یافتن کانفیگ جایگزین سالم */
    public interface AlternativeCallback {
        void onAlternativeFound(TunnelConfig newConfig, long pingMs);
        void onNoAlternativeAvailable();
    }

    /**
     * Represents a configuration with its measured round-trip latency.
     * مدل نگهداری کانفیگ به همراه پینگ ثبت‌شده.
     */
    public static class ConfigWithPing {
        private final TunnelConfig config;
        private final long pingMs;

        public ConfigWithPing(TunnelConfig config, long pingMs) {
            this.config = config;
            this.pingMs = pingMs;
        }

        public TunnelConfig getConfig() {
            return config;
        }

        public long getPingMs() {
            return pingMs;
        }

        public boolean isReachable() {
            return pingMs >= 0;
        }

        @Override
        public String toString() {
            return (config != null ? config.getName() : "Unknown") + " (" + (isReachable() ? pingMs + "ms" : "Timeout") + ")";
        }
    }

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final List<TunnelConfig> candidateConfigs = new CopyOnWriteArrayList<>();
    private volatile TunnelConfig activeConfig;

    private boolean autoSelectBestConfig = true;
    private boolean autoFailoverEnabled = true;
    private int pingTimeoutMs = 3000;
    private int preferredManualIndex = 0;
    private long healthCheckIntervalMs = DEFAULT_HEALTH_CHECK_INTERVAL_MS;
    private String healthCheckUrl = DEFAULT_HEALTH_CHECK_URL;

    public SubscriptionManager() {
    }

    /**
     * Enables or disables automatic selection of the lowest-ping configuration.
     * فعال یا غیرفعال‌سازی انتخاب خودکار سریع‌ترین کانفیگ بر اساس کمترین پینگ.
     */
    public SubscriptionManager setAutoSelectBestConfig(boolean autoSelect) {
        this.autoSelectBestConfig = autoSelect;
        return this;
    }

    public boolean isAutoSelectBestConfig() {
        return autoSelectBestConfig;
    }

    /**
     * Enables or disables automatic failover to healthy alternatives on ping drops.
     * فعال یا غیرفعال‌سازی سوییچ خودکار به کانکشن سالم در صورت افت پینگ (Auto-Failover).
     */
    public SubscriptionManager setAutoFailoverEnabled(boolean enabled) {
        this.autoFailoverEnabled = enabled;
        return this;
    }

    public boolean isAutoFailoverEnabled() {
        return autoFailoverEnabled;
    }

    /**
     * Sets ping test timeout in milliseconds (default: 3000ms).
     * تنظیم مهلت زمانی تست پینگ به میلی‌ثانیه (پیش‌فرض ۳۰۰۰ میلی‌ثانیه).
     */
    public SubscriptionManager setPingTimeoutMs(int timeoutMs) {
        this.pingTimeoutMs = timeoutMs;
        return this;
    }

    public int getPingTimeoutMs() {
        return pingTimeoutMs;
    }

    /**
     * Sets periodic health check interval in milliseconds (default: 20000ms).
     * تنظیم بازه زمانی پایش سلامت به میلی‌ثانیه (پیش‌فرض ۲۰ ثانیه = ۲۰۰۰۰ میلی‌ثانیه).
     */
    public SubscriptionManager setHealthCheckIntervalMs(long intervalMs) {
        this.healthCheckIntervalMs = Math.max(3000L, intervalMs);
        return this;
    }

    public long getHealthCheckIntervalMs() {
        return healthCheckIntervalMs;
    }

    /**
     * Sets default or custom target URL for periodic health checking.
     * تنظیم آدرس پیش‌فرض یا دلخواه جهت تست اتصال و سلامت در پایش ۲۰ ثانیه‌ای.
     *
     * @param url Target HTTP/HTTPS URL (e.g. gstatic 204 or user endpoint)
     */
    public SubscriptionManager setHealthCheckUrl(String url) {
        if (url != null && !url.trim().isEmpty()) {
            this.healthCheckUrl = url.trim();
        } else {
            this.healthCheckUrl = DEFAULT_HEALTH_CHECK_URL;
        }
        return this;
    }

    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    /**
     * Sets preferred manual configuration index when auto-select is disabled.
     * تعیین ایندکس کانفیگ انتخابی در صورت غیرفعال بودن انتخاب خودکار.
     */
    public SubscriptionManager setManualConfigIndex(int index) {
        this.preferredManualIndex = index;
        return this;
    }

    /**
     * Gets unmodifiable view of candidate configurations in the pool.
     * دریافت لیست غیرقابل تغییر کاندیداهای موجود در استخر سابسکرپشن.
     */
    public List<TunnelConfig> getCandidateConfigs() {
        return Collections.unmodifiableList(new ArrayList<>(candidateConfigs));
    }

    /**
     * Sets candidate configurations in the pool.
     * تنظیم استخر کانفیگ‌های کاندید سابسکرپشن.
     */
    public void setCandidateConfigs(List<TunnelConfig> configs) {
        this.candidateConfigs.clear();
        if (configs != null) {
            this.candidateConfigs.addAll(configs);
        }
    }

    public TunnelConfig getActiveConfig() {
        return activeConfig;
    }

    public void setActiveConfig(TunnelConfig config) {
        this.activeConfig = config;
    }

    /**
     * Fetches subscription content from URL, parses configs, and tests their pings.
     * دریافت و پردازش محتوای لینک سابسکرپشن و تست پینگ کانفیگ‌ها.
     *
     * @param subscriptionUrl Remote subscription HTTP/HTTPS URL / آدرس اینترنتی سابسکرپشن
     * @param callback Result callback / کال‌بک بازگشت نتایج
     */
    public void fetchAndTest(String subscriptionUrl, SubscriptionCallback callback) {
        executor.execute(() -> {
            try {
                String content = fetchUrl(subscriptionUrl);
                List<TunnelConfig> configs = parseSubscriptionContent(content);

                if (configs.isEmpty()) {
                    postError(callback, new IllegalStateException("No valid configurations found in subscription."));
                    return;
                }

                testConfigsAndSelect(configs, callback);
            } catch (Throwable t) {
                postError(callback, t);
            }
        });
    }

    /**
     * Parses raw local subscription content (plain multiline or base64) and tests pings.
     * پردازش متن یا کانفیگ‌های چندخطی سابسکرپشن محلی.
     */
    public void parseAndTest(String rawContent, SubscriptionCallback callback) {
        executor.execute(() -> {
            try {
                List<TunnelConfig> configs = parseSubscriptionContent(rawContent);
                if (configs.isEmpty()) {
                    postError(callback, new IllegalStateException("No valid configurations found in raw input."));
                    return;
                }
                testConfigsAndSelect(configs, callback);
            } catch (Throwable t) {
                postError(callback, t);
            }
        });
    }

    private void testConfigsAndSelect(List<TunnelConfig> configs, SubscriptionCallback callback) {
        // Save candidate pool / ذخیره استخر کاندیداها
        candidateConfigs.clear();
        candidateConfigs.addAll(configs);

        List<ConfigWithPing> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(configs.size());

        for (TunnelConfig cfg : configs) {
            executor.execute(() -> {
                long ping = testConfigPing(cfg, pingTimeoutMs);
                results.add(new ConfigWithPing(cfg, ping));
                latch.countDown();
            });
        }

        try {
            latch.await(pingTimeoutMs + 1500L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }

        // Sort by ascending latency / مرتب‌سازی بر اساس کمترین پینگ
        Collections.sort(results, (o1, o2) -> {
            if (o1.getPingMs() < 0 && o2.getPingMs() < 0) return 0;
            if (o1.getPingMs() < 0) return 1;
            if (o2.getPingMs() < 0) return -1;
            return Long.compare(o1.getPingMs(), o2.getPingMs());
        });

        TunnelConfig selected;
        if (autoSelectBestConfig) {
            selected = results.get(0).getConfig();
            for (ConfigWithPing r : results) {
                if (r.isReachable()) {
                    selected = r.getConfig();
                    break;
                }
            }
        } else {
            int idx = Math.min(Math.max(0, preferredManualIndex), configs.size() - 1);
            selected = configs.get(idx);
        }

        this.activeConfig = selected;
        final TunnelConfig finalSelected = selected;
        postCallback(() -> {
            if (callback != null) {
                callback.onSuccess(new ArrayList<>(results), finalSelected);
            }
        });
    }

    /**
     * Finds the fastest reachable alternative node from candidate pool with pre-switch ping check.
     * یافتن سریع‌ترین کانفیگ جایگزین زنده از میان استخر نامزدها با انجام تست پینگ پیش از اتصال.
     */
    public void findBestAliveAlternative(TunnelConfig failedConfig, AlternativeCallback callback) {
        executor.execute(() -> {
            List<TunnelConfig> candidates = new ArrayList<>();
            for (TunnelConfig cfg : candidateConfigs) {
                if (failedConfig != null) {
                    if (cfg.equals(failedConfig) ||
                            (cfg.getHost() != null && cfg.getHost().equalsIgnoreCase(failedConfig.getHost()) &&
                                    cfg.getPort() == failedConfig.getPort())) {
                        continue;
                    }
                }
                candidates.add(cfg);
            }

            if (candidates.isEmpty()) {
                postCallback(() -> {
                    if (callback != null) callback.onNoAlternativeAvailable();
                });
                return;
            }

            // Concurrent ping verification on candidates before switching
            // تست پینگ همزمان بر روی تمام کاندیدها قبل از هرگونه سوییچ
            List<ConfigWithPing> results = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(candidates.size());

            for (TunnelConfig candidate : candidates) {
                executor.execute(() -> {
                    long ping = testConfigPing(candidate, pingTimeoutMs);
                    results.add(new ConfigWithPing(candidate, ping));
                    latch.countDown();
                });
            }

            try {
                latch.await(pingTimeoutMs + 1000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            }

            Collections.sort(results, (o1, o2) -> {
                if (o1.getPingMs() < 0 && o2.getPingMs() < 0) return 0;
                if (o1.getPingMs() < 0) return 1;
                if (o2.getPingMs() < 0) return -1;
                return Long.compare(o1.getPingMs(), o2.getPingMs());
            });

            TunnelConfig bestAlive = null;
            long bestPing = -1;
            for (ConfigWithPing r : results) {
                if (r.isReachable()) {
                    bestAlive = r.getConfig();
                    bestPing = r.getPingMs();
                    break;
                }
            }

            final TunnelConfig finalBest = bestAlive;
            final long finalPing = bestPing;

            postCallback(() -> {
                if (callback != null) {
                    if (finalBest != null) {
                        this.activeConfig = finalBest;
                        callback.onAlternativeFound(finalBest, finalPing);
                    } else {
                        callback.onNoAlternativeAvailable();
                    }
                }
            });
        });
    }

    /**
     * Measures TCP or DNS handshake latency for a given configuration.
     * تست پینگ جامع یک کانفیگ (بررسی TCP یا DNS).
     */
    public static long testConfigPing(TunnelConfig config, int timeoutMs) {
        if (config == null) return -1;
        String host = config.getHost();
        int port = config.getPort();

        if (config.isDnsConfig() && !config.getDnsServers().isEmpty()) {
            host = config.getDnsServers().get(0);
            port = 53;
        }

        if (host == null || port <= 0) return -1;
        return testTcpPing(host, port, timeoutMs);
    }

    /**
     * Tests direct TCP connection latency to remote host and port.
     * تست پینگ TCP تا سرور ریموت.
     */
    public static long testTcpPing(String host, int port, int timeoutMs) {
        if (host == null || port <= 0) return -1;
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Probes HTTP connectivity through local in-app proxy port.
     * بررسی سلامت اتصال از طریق درخواست HTTP به URL تست.
     *
     * @param targetUrl Target HTTP URL (default gstatic 204 or custom)
     * @param localProxyPort Local proxy port
     * @param timeoutMs Timeout in milliseconds
     * @return Latency in ms, or -1 if unreachable
     */
    public static long testHttpProbe(String targetUrl, int localProxyPort, int timeoutMs) {
        if (targetUrl == null || targetUrl.trim().isEmpty()) {
            targetUrl = DEFAULT_HEALTH_CHECK_URL;
        }
        long start = System.currentTimeMillis();
        try {
            URL url = new URL(targetUrl);
            HttpURLConnection conn;
            if (localProxyPort > 0) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", localProxyPort));
                conn = (HttpURLConnection) url.openConnection(proxy);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }

            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "TahrimGozar-HealthProbe/1.0");

            int code = conn.getResponseCode();
            if (code >= 200 && code < 400) {
                return System.currentTimeMillis() - start;
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Parses subscription content (detects Base64 blobs or multi-line plain text).
     * پارس رشته سابسکرپشن (با تشخیص Base64 یا متن ساده).
     */
    public static List<TunnelConfig> parseSubscriptionContent(String rawContent) {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String content = rawContent.trim();

        // Check if the content already contains direct plaintext config lines
        boolean looksLikePlainTextConfigs = false;
        String[] preliminaryLines = content.split("[\r\n]+");
        for (String pLine : preliminaryLines) {
            String t = pLine.trim();
            if (t.startsWith("vless://") || t.startsWith("vmess://") || t.startsWith("trojan://")
                    || t.startsWith("ss://") || t.startsWith("ssr://") || t.startsWith("hy2://")
                    || t.startsWith("hysteria2://") || t.startsWith("tuic://") || t.startsWith("wireguard://")
                    || t.startsWith("http://") || t.startsWith("https://") || t.startsWith("[Interface]")
                    || t.startsWith("proxies:") || t.startsWith("{")) {
                looksLikePlainTextConfigs = true;
                break;
            }
        }

        // If not plain text configs, handle single-line or multi-line (MIME wrapped) Base64 blobs
        if (!looksLikePlainTextConfigs) {
            try {
                String cleaned = content.replaceAll("\\s+", "");
                String decoded = UniversalConfigParser.decodeBase64(cleaned);
                if (decoded != null && !decoded.trim().isEmpty()) {
                    content = decoded.trim();
                }
            } catch (Exception ignored) {
            }
        }

        List<TunnelConfig> list = new ArrayList<>();
        String[] lines = content.split("[\r\n]+");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) continue;
            try {
                TunnelConfig config = UniversalConfigParser.parse(trimmed);
                list.add(config);
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    private String fetchUrl(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "v2rayNG/1.8.5 (TahrimGozar)");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    private void postError(SubscriptionCallback callback, Throwable t) {
        postCallback(() -> {
            if (callback != null) {
                callback.onError(t);
            }
        });
    }

    private void postCallback(Runnable r) {
        try {
            if (Looper.getMainLooper() != null) {
                new Handler(Looper.getMainLooper()).post(r);
                return;
            }
        } catch (Throwable ignored) {
        }
        r.run();
    }
}

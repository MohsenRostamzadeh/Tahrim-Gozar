package ir.M_Rostamzadeh.Tahrim_Gozar.tunnel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ir.M_Rostamzadeh.Tahrim_Gozar.TahrimGozar;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;

/**
 * Intelligent health monitor for active in-app tunnels and subscriptions.
 * ناظر هوشمند سلامت اتصال تونل و سابسکرپشن با تایمر دوره‌ای (پیش‌فرض ۲۰ ثانیه).
 * <p>
 * Implements a sticky connection strategy (maintains current active node while healthy)
 * and smart auto-failover with pre-switch ping verification of candidate nodes.
 * Supports connectivity checking via default HTTP 204 endpoint or custom developer URLs.
 * <br>
 * پیاده‌سازی استراتژی اتصال چسبنده (Sticky Connection) و فرآیند انتقال خودکار به کانفیگ سالم (Auto-Failover)
 * همراه با قابلیت بررسی از طریق URL پیش‌فرض (HTTP 204) یا لینک دلخواه تعیین‌شده توسط کاربر.
 * </p>
 */
public class TunnelHealthMonitor {

    private static final String TAG = "TunnelHealthMonitor";

    private final SubscriptionManager subscriptionManager;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> monitorTask;

    private volatile TunnelConfig activeConfig;
    private volatile int localProxyPort = 0;
    private volatile SubscriptionHealthListener listener;

    private long intervalMs = SubscriptionManager.DEFAULT_HEALTH_CHECK_INTERVAL_MS;
    private String healthCheckUrl = SubscriptionManager.DEFAULT_HEALTH_CHECK_URL;
    private int timeoutMs = 4000;

    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private final AtomicBoolean isFailingOver = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private int maxConsecutiveFailures = 3;

    /**
     * Constructs a health monitor associated with a SubscriptionManager.
     * ساخت ناظر سلامت متصل به یک نمونه مدیریت سابسکرپشن.
     *
     * @param subscriptionManager Subscription manager / ارائه‌دهنده سابسکرپشن
     */
    public TunnelHealthMonitor(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = (subscriptionManager != null) ? subscriptionManager : new SubscriptionManager();
        this.intervalMs = this.subscriptionManager.getHealthCheckIntervalMs();
        this.healthCheckUrl = this.subscriptionManager.getHealthCheckUrl();
    }

    /**
     * Sets periodic check interval in milliseconds (default: 20000ms = 20s).
     * تنظیم بازه زمانی بررسی دوره‌ای به میلی‌ثانیه (پیش‌فرض ۲۰ ثانیه = ۲۰۰۰۰ میلی‌ثانیه).
     */
    public TunnelHealthMonitor setIntervalMs(long intervalMs) {
        this.intervalMs = Math.max(3000L, intervalMs);
        return this;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    /**
     * Sets the default or custom target URL for health checking probes.
     * تنظیم آدرس پیش‌فرض یا اختصاصی جهت تست سلامت اتصال.
     *
     * @param url Target URL (e.g. gstatic 204 or developer endpoint) / لینک مورد نظر
     */
    public TunnelHealthMonitor setHealthCheckUrl(String url) {
        if (url != null && !url.trim().isEmpty()) {
            this.healthCheckUrl = url.trim();
        } else {
            this.healthCheckUrl = SubscriptionManager.DEFAULT_HEALTH_CHECK_URL;
        }
        return this;
    }

    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    /**
     * Gets the number of consecutive failed health checks required before triggering auto-failover.
     * دریافت تعداد خطاهای متوالی تست سلامت قبل از انجام سوییچ خودکار.
     */
    public int getMaxConsecutiveFailures() {
        return maxConsecutiveFailures;
    }

    /**
     * Sets the number of consecutive failed health checks required before triggering auto-failover (default: 3).
     * تنظیم تعداد خطاهای متوالی تست سلامت قبل از اقدام به سوییچ خودکار (پیش‌فرض: ۳ بار جهت جلوگیری از سوییچ لحظه‌ای).
     *
     * @param max Max consecutive failures (minimum 1)
     */
    public TunnelHealthMonitor setMaxConsecutiveFailures(int max) {
        this.maxConsecutiveFailures = Math.max(1, max);
        return this;
    }

    /**
     * Sets network timeout for health probes.
     * تنظیم مهلت زمانی پروب‌های سلامت به میلی‌ثانیه.
     */
    public TunnelHealthMonitor setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * Sets listener for health check results and failover lifecycle.
     * تنظیم شنونده وضعیت سلامت و رویدادهای انتقال خودکار.
     */
    public void setListener(SubscriptionHealthListener listener) {
        this.listener = listener;
    }

    /**
     * Returns whether health monitoring is actively running.
     * بررسی فعال بودن پایش سلامت در پس‌زمینه.
     */
    public boolean isMonitoring() {
        return isMonitoring.get();
    }

    /**
     * Gets the currently monitored active config.
     * دریافت کانفیگ فعال فعلی تحت پایش.
     */
    public TunnelConfig getActiveConfig() {
        return activeConfig;
    }

    /**
     * Gets the local proxy port used for in-app HTTP probes.
     * دریافت پورت محلی پروکسی درون‌برنامه‌ای.
     */
    public int getLocalProxyPort() {
        return localProxyPort;
    }

    /**
     * Starts periodic health monitoring for the active configuration.
     * آغاز مانیتورینگ دوره‌ای ۲۰ ثانیه‌ای برای کانفیگ فعال.
     *
     * @param config Connected configuration / کانفیگ متصل‌شده
     * @param localPort Local proxy port (if running) / پورت محلی پروکسی
     * @param healthListener Listener for health and failover / شنونده رویدادها
     */
    public synchronized void startMonitoring(TunnelConfig config, int localPort, SubscriptionHealthListener healthListener) {
        stopMonitoring();

        this.activeConfig = config;
        this.localProxyPort = localPort;
        this.listener = healthListener;
        this.isMonitoring.set(true);
        this.isFailingOver.set(false);

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TahrimGozar-HealthMonitor");
            t.setDaemon(true);
            return t;
        });

        // Schedule periodic check every intervalMs (default 20 seconds)
        // زمان‌بندی بررسی دوره‌ای هر ۲۰ ثانیه یک‌بار
        monitorTask = scheduler.scheduleWithFixedDelay(this::checkHealthTick, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        Log.i(TAG, "Health monitoring started with interval " + intervalMs + "ms on URL: " + healthCheckUrl);
    }

    /**
     * Stops the health monitor and releases background executor resources.
     * توقف فرآیند مانیتورینگ و آزادسازی تایمر پس‌زمینه.
     */
    public synchronized void stopMonitoring() {
        isMonitoring.set(false);
        isFailingOver.set(false);
        consecutiveFailures.set(0);

        if (monitorTask != null) {
            monitorTask.cancel(true);
            monitorTask = null;
        }

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        Log.i(TAG, "Health monitoring stopped.");
    }

    /**
     * Periodic tick executed by background timer (Sticky Connection Strategy).
     * بررسی دوره‌ای پینگ در هر تیک تایمر (طبق استراتژی Sticky Connection).
     */
    public void checkHealthTick() {
        if (!isMonitoring.get()) return;

        final TunnelConfig current = activeConfig;
        if (current == null) return;

        // Verify that the tunnel is still active
        // بررسی فعال بودن تونل
        if (!TahrimGozar.getInstance().isTunnelActive()) {
            return;
        }

        long ping = -1;

        // 1. Probe target URL through local in-app proxy port
        // ۱. بررسی سلامت از طریق پروب HTTP به آدرس پیش‌فرض یا دلخواه از پورت لوکال
        if (localProxyPort > 0 && healthCheckUrl != null && !healthCheckUrl.isEmpty()) {
            ping = SubscriptionManager.testHttpProbe(healthCheckUrl, localProxyPort, timeoutMs);
        }

        // 2. If HTTP probe fails or not applicable, perform direct TCP handshake to server host/port
        // ۲. در صورت عدم پاسخ پروب، تست پینگ مستقیم TCP با هاست و پورت سرور انجام می‌شود
        if (ping < 0) {
            ping = SubscriptionManager.testConfigPing(current, timeoutMs);
        }

        final boolean isAlive = (ping >= 0);
        final long currentPing = ping;

        // Notify listener / اطلاع‌رسانی به شنونده
        if (listener != null) {
            postCallback(() -> {
                if (listener != null) {
                    listener.onHealthCheck(current, isAlive, currentPing);
                }
            });
        }

        if (isAlive) {
            // Sticky strategy: maintain current connection as long as it responds
            // استراتژی چسبنده: حفظ اتصال جاری تا زمانی که پینگ پاسخگو است
            consecutiveFailures.set(0);
            Log.d(TAG, "Connection is healthy (" + currentPing + "ms) - maintaining sticky connection.");
        } else {
            // Connection lost: debounce transient drops before initiating smart failover
            // در صورت افت موقت اتصال، ابتدا خطاها شمارش شده و پس از رسیدن به حد آستانه سوییچ می‌شود
            int failures = consecutiveFailures.incrementAndGet();
            Log.w(TAG, "Active connection ping dropped (failure " + failures + "/" + maxConsecutiveFailures + ").");
            if (failures >= maxConsecutiveFailures) {
                Log.w(TAG, "Max consecutive failures reached. Initiating smart failover...");
                if (subscriptionManager.isAutoFailoverEnabled()) {
                    triggerFailover(current);
                }
            }
        }
    }

    /**
     * Executes auto-failover to the fastest alive candidate after pre-switch ping check.
     * اجرای فرآیند جابجایی هوشمند به بهترین کانفیگ زنده با تست پینگ قبل از سوییچ.
     */
    private void triggerFailover(TunnelConfig failedConfig) {
        if (!isFailingOver.compareAndSet(false, true)) {
            return; // Failover already in progress / فرآیند سوییچ هم‌اکنون در حال اجراست
        }

        if (listener != null) {
            postCallback(() -> {
                if (listener != null) {
                    listener.onFailoverTriggered(failedConfig);
                }
            });
        }

        // Test candidate pings before switching to avoid connecting to another dead server
        // تست پینگ کاندیدها پیش از جابجایی برای جلوگیری از سوییچ به نود مرده
        subscriptionManager.findBestAliveAlternative(failedConfig, new SubscriptionManager.AlternativeCallback() {
            @Override
            public void onAlternativeFound(TunnelConfig newConfig, long pingMs) {
                Log.i(TAG, "Found healthy replacement: " + newConfig.getName() + " with ping " + pingMs + "ms. Switching...");

                // Seamless in-place switch
                // سوییچ یکپارچه به کانفیگ سالم جدید
                TahrimGozar.getInstance().switchActiveTunnel(newConfig, new TunnelCallback() {
                    @Override
                    public void onConnected(TunnelSession session) {
                        activeConfig = newConfig;
                        localProxyPort = session.getLocalPort();
                        isFailingOver.set(false);
                        consecutiveFailures.set(0);

                        Log.i(TAG, "Failover success! Now connected to: " + newConfig.getName());
                        if (listener != null) {
                            postCallback(() -> {
                                if (listener != null) {
                                    listener.onFailoverSuccess(failedConfig, newConfig, pingMs);
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(TAG, "Failover switch failed to connect: " + throwable.getMessage());
                        isFailingOver.set(false);
                        if (listener != null) {
                            postCallback(() -> {
                                if (listener != null) {
                                    listener.onFailoverFailed("Failed to connect to alternative config: " + throwable.getMessage());
                                }
                            });
                        }
                    }

                    @Override
                    public void onDisconnected() {
                        isFailingOver.set(false);
                    }
                });
            }

            @Override
            public void onNoAlternativeAvailable() {
                Log.w(TAG, "No alternative reachable configs found in subscription pool.");
                isFailingOver.set(false);
                if (listener != null) {
                    postCallback(() -> {
                        if (listener != null) {
                            listener.onFailoverFailed("No reachable healthy configs found in subscription pool.");
                        }
                    });
                }
            }
        });
    }

    /**
     * Updates the tracked active session config and port.
     * به‌روزرسانی نشست فعال کانفیگ و پورت تحت پایش.
     */
    public void updateActiveSession(TunnelConfig config, int port) {
        this.activeConfig = config;
        this.localProxyPort = port;
    }

    private static volatile Handler mainHandler;

    private static Handler getMainHandler() {
        if (mainHandler == null) {
            synchronized (TunnelHealthMonitor.class) {
                if (mainHandler == null && Looper.getMainLooper() != null) {
                    mainHandler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return mainHandler;
    }

    private void postCallback(Runnable r) {
        try {
            Handler handler = getMainHandler();
            if (handler != null) {
                handler.post(r);
                return;
            }
        } catch (Throwable ignored) {
        }
        r.run();
    }
}

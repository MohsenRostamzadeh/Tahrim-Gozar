package ir.M_Rostamzadeh.Tahrim_Gozar;

import android.util.Log;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**Monitors proxy health and automatically switches to a working proxy when the current one fails.
 * <p>Usage example:</p>
 * <pre>
 * ProxyHealthChecker checker = new ProxyHealthChecker(30000); // 30 seconds
 * checker.startMonitoring(new ProxyHealthChecker.ProxyHealthListener() {
 *     public void onHealthy(ProxyInfo proxy) { }
 *     public void onUnhealthy(ProxyInfo proxy) { }
 * });
 * // When done:
 * checker.shutdown();
 * </pre>*/
public class ProxyHealthChecker {

    /**Listener for proxy health status changes*/
    public interface ProxyHealthListener {
        /**Called when proxy health check succeeds
         * @param proxy The healthy proxy*/
        void onHealthy(ProxyInfo proxy);

        /**Called when proxy health check fails
         * @param proxy The unhealthy proxy*/
        void onUnhealthy(ProxyInfo proxy);
    }

    private static final long DEFAULT_INTERVAL_MS = 30_000L;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> healthCheckTask;
    private final long intervalMs;
    private ProxyHealthListener listener;
    private volatile boolean isMonitoring = false;

    /**Create health checker with default interval (30 seconds)*/
    public ProxyHealthChecker() {
        this(DEFAULT_INTERVAL_MS);
    }

    /**Create health checker with custom interval
     * @param intervalMs Check interval in milliseconds*/
    public ProxyHealthChecker(long intervalMs) {
        this.intervalMs = intervalMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TahrimGozar-HealthCheck");
            t.setDaemon(true);
            return t;
        });
    }

    /**Start monitoring proxy health
     * @param listener Listener for health status changes*/
    public void startMonitoring(ProxyHealthListener listener) {
        if (isMonitoring) {
            stopMonitoring();
        }
        this.listener = listener;
        this.isMonitoring = true;

        healthCheckTask = scheduler.scheduleWithFixedDelay(() -> {
            try {
                TahrimGozar instance = TahrimGozar.getInstance();
                if (!instance.isUseProxy()) return;

                ProxyInfo currentProxy = instance.getCurrentProxy();
                if (currentProxy == null) return;

                int response = Utils.getInstance().getLinkResponse(null);
                if (response == Constants.RESULT_OKY_CODE) {
                    if (listener != null) listener.onHealthy(currentProxy);
                } else {
                    if (listener != null) listener.onUnhealthy(currentProxy);
                    tryAutoSwitch(currentProxy);
                }
            } catch (Exception e) {
                Utils.getInstance().showLog("Health check error: " + e.getMessage(), Log.ERROR);
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    /**Stop monitoring proxy health*/
    public void stopMonitoring() {
        isMonitoring = false;
        if (healthCheckTask != null) {
            healthCheckTask.cancel(false);
            healthCheckTask = null;
        }
        listener = null;
    }

    /**Check if health monitoring is active
     * @return true if monitoring is active*/
    public boolean isMonitoring() {
        return isMonitoring;
    }

    /**Release resources. Call this when done using the health checker.*/
    public void shutdown() {
        stopMonitoring();
        scheduler.shutdown();
    }

    /**Try to automatically switch to next available proxy
     * @param failedProxy The proxy that failed health check*/
    private void tryAutoSwitch(ProxyInfo failedProxy) {
        TahrimGozar instance = TahrimGozar.getInstance();
        ProxyProvider provider = instance.getProxyProvider();
        if (provider == null) return;

        List<ProxyInfo> proxies = provider.getAvailableProxies();
        for (ProxyInfo proxy : proxies) {
            if (!proxy.equals(failedProxy)) {
                instance.setCustomProxy(proxy);
                Utils.getInstance().showLog("Auto-switched proxy to: " + proxy, Log.INFO);
                return;
            }
        }
        Utils.getInstance().showLog("No alternative proxy available", Log.WARN);
    }
}

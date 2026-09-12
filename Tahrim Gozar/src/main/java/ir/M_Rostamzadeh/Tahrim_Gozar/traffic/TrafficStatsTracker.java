package ir.M_Rostamzadeh.Tahrim_Gozar.traffic;

import android.os.Handler;
import android.os.Looper;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-time traffic tracker and speed calculator for in-app tunnels.
 * پایشگر و سرعت‌سنج ترافیک لحظه‌ای شبکه تحریم‌گذر.
 */
public class TrafficStatsTracker {

    private final AtomicLong totalRxBytes = new AtomicLong(0);
    private final AtomicLong totalTxBytes = new AtomicLong(0);

    private long lastRxSnapshot = 0;
    private long lastTxSnapshot = 0;
    private long lastSpeedRx = 0;
    private long lastSpeedTx = 0;

    private final AtomicBoolean isEnabled = new AtomicBoolean(false);
    private volatile TrafficStatsListener listener;

    private ScheduledExecutorService tickerService;
    private ScheduledFuture<?> tickerTask;
    private static volatile Handler mainHandler;

    private static Handler getMainHandler() {
        if (mainHandler == null) {
            synchronized (TrafficStatsTracker.class) {
                if (mainHandler == null && Looper.getMainLooper() != null) {
                    mainHandler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return mainHandler;
    }

    /**
     * Enables or disables real-time traffic statistics tracking.
     * فعال یا غیرفعال‌سازی محاسبه آمار ترافیک و سرعت لحظه‌ای.
     */
    public synchronized void setEnabled(boolean enabled) {
        this.isEnabled.set(enabled);
        if (enabled) {
            startTicker();
        } else {
            stopTicker();
        }
    }

    public boolean isEnabled() {
        return isEnabled.get();
    }

    /**
     * Sets the listener for real-time speed and bandwidth callbacks.
     * تنظیم شنونده آمار لحظه‌ای ترافیک.
     */
    public void setListener(TrafficStatsListener listener) {
        this.listener = listener;
    }

    /**
     * Records downloaded bytes through the tunnel.
     * ثبت بایت‌های دریافتی از تونل.
     */
    public void recordRx(long bytes) {
        if (isEnabled.get() && bytes > 0) {
            totalRxBytes.addAndGet(bytes);
        }
    }

    /**
     * Records uploaded bytes through the tunnel.
     * ثبت بایت‌های ارسالی به تونل.
     */
    public void recordTx(long bytes) {
        if (isEnabled.get() && bytes > 0) {
            totalTxBytes.addAndGet(bytes);
        }
    }

    public long getTotalRxBytes() {
        return totalRxBytes.get();
    }

    public long getTotalTxBytes() {
        return totalTxBytes.get();
    }

    public long getLastSpeedRx() {
        return lastSpeedRx;
    }

    public long getLastSpeedTx() {
        return lastSpeedTx;
    }

    public long getCurrentRxSpeed() {
        return lastSpeedRx;
    }

    public long getCurrentTxSpeed() {
        return lastSpeedTx;
    }

    /**
     * Resets counters to zero.
     * بازنشانی تمام شمارنده‌های ترافیک.
     */
    public void reset() {
        totalRxBytes.set(0);
        totalTxBytes.set(0);
        lastRxSnapshot = 0;
        lastTxSnapshot = 0;
        lastSpeedRx = 0;
        lastSpeedTx = 0;
    }

    private synchronized void startTicker() {
        if (tickerTask != null && !tickerTask.isCancelled()) {
            return;
        }

        tickerService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TahrimGozar-TrafficTicker");
            t.setDaemon(true);
            return t;
        });

        lastRxSnapshot = totalRxBytes.get();
        lastTxSnapshot = totalTxBytes.get();

        tickerTask = tickerService.scheduleWithFixedDelay(this::tick, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private synchronized void stopTicker() {
        if (tickerTask != null) {
            tickerTask.cancel(true);
            tickerTask = null;
        }
        if (tickerService != null && !tickerService.isShutdown()) {
            tickerService.shutdownNow();
            tickerService = null;
        }
        lastSpeedRx = 0;
        lastSpeedTx = 0;
    }

    private void tick() {
        if (!isEnabled.get()) return;

        long currentRx = totalRxBytes.get();
        long currentTx = totalTxBytes.get();

        long speedRx = Math.max(0, currentRx - lastRxSnapshot);
        long speedTx = Math.max(0, currentTx - lastTxSnapshot);

        lastRxSnapshot = currentRx;
        lastTxSnapshot = currentTx;

        this.lastSpeedRx = speedRx;
        this.lastSpeedTx = speedTx;

        final TrafficStatsListener l = this.listener;
        if (l != null) {
            Handler handler = getMainHandler();
            if (handler != null) {
                handler.post(() -> l.onTrafficStats(speedRx, speedTx, currentRx, currentTx));
            } else {
                l.onTrafficStats(speedRx, speedTx, currentRx, currentTx);
            }
        }
    }

    /**
     * Formats bytes per second into human-readable speed (e.g. "1.2 MB/s", "450 KB/s").
     * قالب‌بندی سرعت به صورت رشته خوانا.
     */
    public static String formatSpeed(long bytesPerSec) {
        if (bytesPerSec <= 0) return "0 B/s";
        if (bytesPerSec < 1024) return bytesPerSec + " B/s";
        if (bytesPerSec < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB/s", bytesPerSec / 1024.0);
        }
        return String.format(Locale.ROOT, "%.2f MB/s", bytesPerSec / (1024.0 * 1024.0));
    }

    /**
     * Formats total bytes into human-readable size (e.g. "15.4 MB", "1.2 GB").
     * قالب‌بندی حجم به صورت رشته خوانا.
     */
    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}

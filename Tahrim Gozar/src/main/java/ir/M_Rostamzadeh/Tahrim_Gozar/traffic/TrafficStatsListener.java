package ir.M_Rostamzadeh.Tahrim_Gozar.traffic;

/**
 * Listener for real-time in-app traffic metrics and network speeds.
 * شنونده آمار ترافیک و سرعت دانلود/آپلود لحظه‌ای در کتابخانه تحریم‌گذر.
 */
public interface TrafficStatsListener {

    /**
     * Called periodically (typically every second) with current network metrics.
     * فراخوانی دوره‌ای (معمولاً هر یک ثانیه) با آمار ترافیک شبکه.
     *
     * @param rxBytesPerSec Download speed in bytes per second / سرعت دانلود لحظه‌ای (بایت بر ثانیه)
     * @param txBytesPerSec Upload speed in bytes per second / سرعت آپلود لحظه‌ای (بایت بر ثانیه)
     * @param totalRxBytes Total bytes downloaded through the tunnel / کل بایت‌های دریافت شده
     * @param totalTxBytes Total bytes uploaded through the tunnel / کل بایت‌های ارسال شده
     */
    void onTrafficStats(long rxBytesPerSec, long txBytesPerSec, long totalRxBytes, long totalTxBytes);
}

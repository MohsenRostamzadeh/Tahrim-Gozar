package ir.M_Rostamzadeh.Tahrim_Gozar.tunnel;

import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;

/**
 * Listener interface for periodic connection health monitoring and smart auto-failover events.
 * اینترفیس شنونده رخدادهای پایش سلامت اتصال و جابجایی خودکار کانفیگ (Auto-Failover)
 * در سابسکرپشن‌های چندکانفیگه و اتصالات تونل درون‌برنامه‌ای.
 */
public interface SubscriptionHealthListener {

    /**
     * Called on each periodic health check tick (default every 20 seconds).
     * فراخوانی در هر بار اجرای تست سلامت دوره‌ای (پیش‌فرض هر ۲۰ ثانیه).
     *
     * @param activeConfig Active configuration being probed / کانفیگ فعال فعلی
     * @param isAlive Whether the connection is responsive / وضعیت در دسترس بودن و سلامت کانکشن
     * @param pingMs Round-trip latency in ms (-1 if unreachable) / تاخیر/پینگ به میلی‌ثانیه
     */
    void onHealthCheck(TunnelConfig activeConfig, boolean isAlive, long pingMs);

    /**
     * Called when active connection drops ping and smart failover process is initiated.
     * فراخوانی در زمان تشخیص قطعی کانفیگ فعال و آغاز فرآیند Failover هوشمند.
     *
     * @param failedConfig Configuration that became unresponsive / کانفیگی که پاسخگو نبوده و دچار افت پینگ شده
     */
    default void onFailoverTriggered(TunnelConfig failedConfig) {
    }

    /**
     * Called when failover successfully connects to a verified healthy alternative candidate.
     * فراخوانی در زمان اتصال موفق به کانفیگ جایگزین پس از تست نامزدها.
     *
     * @param oldConfig Previous disconnected node / کانفیگ قدیمی قطع‌شده
     * @param newConfig Newly connected node with lowest latency / کانفیگ جدید متصل‌شده با بهترین پینگ
     * @param newPingMs Latency of the new node in ms / پینگ ثبت‌شده کانفیگ جدید
     */
    default void onFailoverSuccess(TunnelConfig oldConfig, TunnelConfig newConfig, long newPingMs) {
    }

    /**
     * Called when auto-failover fails to find any reachable alternative node.
     * فراخوانی در صورتی که هیچ‌یک از کانفیگ‌های موجود در لیست سابسکرپشن پاسخگو نباشند.
     *
     * @param reason Failure explanation / دلیل عدم موفقیت
     */
    default void onFailoverFailed(String reason) {
    }
}

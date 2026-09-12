package ir.M_Rostamzadeh.Tahrim_Gozar.tunnel;

/**
 * Interface for receiving in-app tunnel lifecycle and connection status events.
 * اینترفیس دریافت رویدادهای وضعیت اتصال و چرخه حیات تونل درون‌برنامه‌ای.
 */
public interface TunnelCallback {

    /**
     * Called when the in-app tunnel is successfully connected and local proxy/DNS is ready.
     * فراخوانی در زمان اتصال موفق و فعال شدن تونل درون‌برنامه‌ای.
     *
     * @param session Active session containing local port and config / اطلاعات نشست فعال تونل
     */
    void onConnected(TunnelSession session);

    /**
     * Called when an error occurs during connection setup or runtime.
     * فراخوانی در صورت بروز خطا در برقراری یا پایداری تونل.
     *
     * @param throwable Exception that occurred / خطای رخ‌داده
     */
    void onError(Throwable throwable);

    /**
     * Called when the tunnel is stopped or disconnected.
     * فراخوانی در زمان قطع شدن یا متوقف شدن تونل.
     */
    default void onDisconnected() {
    }
}

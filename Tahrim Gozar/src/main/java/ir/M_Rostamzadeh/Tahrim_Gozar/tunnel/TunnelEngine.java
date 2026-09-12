package ir.M_Rostamzadeh.Tahrim_Gozar.tunnel;

import android.content.Context;

import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;

/**
 * Modular interface for in-app tunneling engines in Tahrim Gozar.
 * اینترفیس ماژولار موتورهای تونلینگ درون‌برنامه‌ای تحریم‌گذر.
 */
public interface TunnelEngine {

    /**
     * Starts the tunnel engine with the specified configuration.
     * راه‌اندازی موتور تونل با کانفیگ مشخص.
     *
     * @param context Application context / کانتکست برنامه
     * @param config Parsed tunnel configuration / کانفیگ استخراج‌شده
     * @param callback Status event callback / کال‌بک رویدادهای وضعیت
     * @throws Exception On startup failure / در صورت بروز خطا در راه‌اندازی
     */
    void start(Context context, TunnelConfig config, TunnelCallback callback) throws Exception;

    /**
     * Stops the engine and releases all allocated ports and system resources.
     * متوقف‌سازی کامل موتور و آزادسازی پورت‌ها و ریسورس‌ها.
     */
    void stop();

    /**
     * Checks whether the engine is currently running.
     * بررسی وضعیت فعال بودن موتور.
     */
    boolean isRunning();

    /**
     * Gets the local inbound proxy or DNS port.
     * پورت محلی لوکال که اینباند روی آن فعال است.
     */
    int getLocalPort();

    /**
     * Gets the active configuration being handled by this engine.
     * دریافت کانفیگ فعال فعلی.
     */
    TunnelConfig getCurrentConfig();
}

package ir.M_Rostamzadeh.Tahrim_Gozar.tunnel;

import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;

/**
 * Encapsulates the active state and runtime details of an in-app tunnel session.
 * کپسول اطلاعات وضعیت نشست فعال تونل درون‌برنامه‌ای.
 */
public class TunnelSession {

    private final TunnelConfig config;
    private final int localPort;
    private final long startTime;
    private volatile boolean active;

    public TunnelSession(TunnelConfig config, int localPort) {
        this.config = config;
        this.localPort = localPort;
        this.startTime = System.currentTimeMillis();
        this.active = true;
    }

    /** Gets the active tunnel configuration / دریافت کانفیگ فعال تونل */
    public TunnelConfig getConfig() {
        return config;
    }

    /** Gets the local inbound port / دریافت پورت محلی لوکال */
    public int getLocalPort() {
        return localPort;
    }

    /** Gets the session start timestamp in ms / زمان شروع نشست */
    public long getStartTime() {
        return startTime;
    }

    /** Gets total uptime duration of this session in ms / مدت زمان فعال بودن نشست */
    public long getDurationMs() {
        return System.currentTimeMillis() - startTime;
    }

    /** Checks whether the session is currently active / بررسی فعال بودن نشست */
    public boolean isActive() {
        return active;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "TunnelSession{" +
                "type=" + (config != null ? config.getType() : "UNKNOWN") +
                ", localPort=" + localPort +
                ", duration=" + getDurationMs() + "ms" +
                ", active=" + active +
                '}';
    }
}

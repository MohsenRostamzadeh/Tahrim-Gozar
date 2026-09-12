package ir.M_Rostamzadeh.Tahrim_Gozar.tunnel;

import android.content.Context;
import android.util.Log;

import java.net.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import ir.M_Rostamzadeh.Tahrim_Gozar.TahrimGozar;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.ConfigType;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;
import ir.M_Rostamzadeh.Tahrim_Gozar.dns.LocalDnsForwarder;
import ir.M_Rostamzadeh.Tahrim_Gozar.dns.TahrimDns;

/**
 * Standard, pure-Java engine for DNS presets, DoH, and standard HTTP/SOCKS proxies.
 * موتور استاندارد و سبک پیاده‌سازی شده با جاوا برای هندل کردن کدهای DNS، پروکسی‌های HTTP و SOCKS.
 * <p>
 * Zero NDK dependency, under 100KB, runs natively on all Android versions.
 * بدون نیاز به NDK، حجم فوق‌العاده کم و بدون وابستگی‌های سنگین.
 * </p>
 */
public class StandardProxyEngine implements TunnelEngine {

    private static final String TAG = "StandardProxyEngine";

    private final InAppTrafficRouter router = new InAppTrafficRouter();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private LocalDnsForwarder dnsForwarder;
    private TunnelConfig currentConfig;
    private int localPort = 0;
    private Context context;

    @Override
    public synchronized void start(Context context, TunnelConfig config, TunnelCallback callback) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("TunnelConfig cannot be null");
        }

        this.context = context.getApplicationContext();
        this.currentConfig = config;

        try {
            router.setRoutingRules(TahrimGozar.getInstance().getSmartRoutingRules());

            if (config.isDnsConfig()) {
                // 1. Anti-sanction DNS / DoH scenario
                // ۱. سناریوی کدهای DNS ضدتحریم و DoH
                TahrimDns dns = new TahrimDns(config.getDnsServers(), config.getDohUrl());
                dns.setRaceEnabled(TahrimGozar.getInstance().isDnsRaceEnabled());
                dnsForwarder = new LocalDnsForwarder(dns, TahrimGozar.getInstance().getTrafficStatsTracker());
                localPort = dnsForwarder.start();

                router.applyRouting(this.context,
                        "127.0.0.1",
                        localPort,
                        Proxy.Type.HTTP,
                        null,
                        null,
                        true,
                        true);

                isRunning.set(true);
                Log.i(TAG, "In-app DNS forwarder started on 127.0.0.1:" + localPort + " for " + config.getName());
                if (callback != null) {
                    callback.onConnected(new TunnelSession(config, localPort));
                }
            } else if (config.isStandardProxy()) {
                // 2. Direct HTTP/SOCKS proxy scenario
                // ۲. سناریوی پروکسی‌های مستقیم HTTP و SOCKS5
                Proxy.Type type = (config.getType() == ConfigType.PROXY_SOCKS5 || config.getType() == ConfigType.PROXY_SOCKS4)
                        ? Proxy.Type.SOCKS
                        : Proxy.Type.HTTP;

                localPort = config.getPort();

                router.applyRouting(this.context,
                        config.getHost(),
                        config.getPort(),
                        type,
                        config.getUsername(),
                        config.getPassword(),
                        true,
                        true);

                isRunning.set(true);
                Log.i(TAG, "Standard in-app proxy applied: " + config.getHost() + ":" + config.getPort());
                if (callback != null) {
                    callback.onConnected(new TunnelSession(config, localPort));
                }
            } else {
                throw new UnsupportedOperationException("StandardProxyEngine does not handle " + config.getType());
            }
        } catch (Throwable t) {
            stop();
            if (callback != null) {
                callback.onError(t);
            }
            throw t;
        }
    }

    @Override
    public synchronized void stop() {
        if (!isRunning.get() && dnsForwarder == null && !router.isRouted()) {
            return;
        }

        if (dnsForwarder != null) {
            dnsForwarder.stop();
            dnsForwarder = null;
        }

        if (context != null) {
            router.resetRouting(context);
        }

        isRunning.set(false);
        localPort = 0;
        currentConfig = null;
        Log.i(TAG, "StandardProxyEngine stopped and routing restored.");
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public int getLocalPort() {
        return localPort;
    }

    @Override
    public TunnelConfig getCurrentConfig() {
        return currentConfig;
    }

    public InAppTrafficRouter getRouter() {
        return router;
    }
}

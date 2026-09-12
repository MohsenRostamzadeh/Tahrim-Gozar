package ir.M_Rostamzadeh.Tahrim_Gozar.tunnel;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import ir.M_Rostamzadeh.Tahrim_Gozar.config.ConfigType;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;

/**
 * پل ارتباطی ماژولار بین کتابخانه تحریم‌گذر و هسته‌های V2Ray / Xray / Sing-box
 * دارای تولیدکننده خودکار کانفیگ استاندارد JSON و شناسایی هوشمند هسته‌های معتبر گیت‌هاب (مانند libv2ray.aar)
 */
public class XrayBridgeEngine implements TunnelEngine {

    private static final String TAG = "XrayBridgeEngine";

    /**
     * اینترفیس اختیاری برای اجرای مستقیم هسته بومی توسط برنامه‌نویس
     */
    public interface NativeCoreRunner {
        void startCore(String configJson) throws Exception;
        void stopCore() throws Exception;
    }

    private static NativeCoreRunner customRunner;
    private final InAppTrafficRouter router = new InAppTrafficRouter();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Object activeCoreController;
    private TunnelConfig currentConfig;
    private int localPort = 0;
    private int localPortHttp = 0;
    private Context context;

    public static void setCustomCoreRunner(NativeCoreRunner runner) {
        customRunner = runner;
    }

    @Override
    public synchronized void start(Context context, TunnelConfig config, TunnelCallback callback) throws Exception {
        if (context == null) throw new IllegalArgumentException("Context cannot be null");
        if (config == null) throw new IllegalArgumentException("TunnelConfig cannot be null");

        this.context = context.getApplicationContext();
        this.currentConfig = config;

        try {
            router.setRoutingRules(ir.M_Rostamzadeh.Tahrim_Gozar.TahrimGozar.getInstance().getSmartRoutingRules());

            // ۱. انتخاب پورت‌های آزاد لوکال برای اینباندهای SOCKS و HTTP
            localPort = findFreePort();
            localPortHttp = findFreePort();

            // ۲. تولید فایل کانفیگ استاندارد کلاینت JSON بر اساس پروتکل ورودی
            String configJson = generateClientJson(config, localPort, localPortHttp);
            Log.d(TAG, "Generated Xray/V2Ray client JSON on SOCKS:" + localPort + " and HTTP:" + localPortHttp);

            // ۳. راه‌اندازی هسته بومی در پس‌زمینه
            startNativeCore(configJson);

            // ۴. انتظار برای باز شدن پورت محلی هسته
            waitForPort(localPort, 25);

            // ۵. هدایت ترافیک سراسری برنامه (SOCKS) و وب‌ویوها (HTTP Proxy) به اینباند محلی هسته
            router.applyRouting(this.context,
                    "127.0.0.1",
                    localPort,
                    java.net.Proxy.Type.SOCKS,
                    null,
                    null,
                    true,
                    true,
                    localPortHttp,
                    java.net.Proxy.Type.HTTP);

            isRunning.set(true);
            Log.i(TAG, "V2Ray/Xray in-app tunnel established on 127.0.0.1:" + localPort);

            if (callback != null) {
                callback.onConnected(new TunnelSession(config, localPort));
            }
        } catch (Throwable t) {
            stop();
            if (callback != null) {
                callback.onError(t);
            }
            throw t;
        }
    }

    private void startNativeCore(String configJson) throws Exception {
        // اولویت اول: استفاده از NativeCoreRunner اختصاصی در صورت تنظیم
        if (customRunner != null) {
            customRunner.startCore(configJson);
            return;
        }

        // اولویت دوم: استفاده از هسته رسمی libv2ray.Libv2ray (پروژه AndroidLibXrayLite)
        try {
            Class<?> libClass = Class.forName("libv2ray.Libv2ray");
            Method initMethod = libClass.getMethod("initCoreEnv", String.class, String.class);
            String filesDir = context != null ? context.getFilesDir().getAbsolutePath() : "";
            initMethod.invoke(null, filesDir, "");

            Class<?> handlerClass = Class.forName("libv2ray.CoreCallbackHandler");
            Object callbackProxy = Proxy.newProxyInstance(
                    handlerClass.getClassLoader(),
                    new Class<?>[]{handlerClass},
                    (p, m, args) -> 0L
            );

            Method newController = libClass.getMethod("newCoreController", handlerClass);
            activeCoreController = newController.invoke(null, callbackProxy);

            Method startLoop = activeCoreController.getClass().getMethod("startLoop", String.class, int.class);
            new Thread(() -> {
                try {
                    startLoop.invoke(activeCoreController, configJson, 0);
                } catch (Throwable t) {
                    Log.e(TAG, "Xray startLoop stopped or error: " + t.getMessage());
                }
            }, "XrayCoreThread").start();

            Log.i(TAG, "Successfully started libv2ray CoreController loop");
            return;
        } catch (ClassNotFoundException ignored) {
            Log.d(TAG, "libv2ray.Libv2ray not found, trying other classes...");
        } catch (Throwable t) {
            Log.w(TAG, "Error initializing libv2ray", t);
        }

        // Priority 3: Check other known Xray/V2Ray packages
        // اولویت سوم: بررسی وجود پکیج‌های معروف Xray دیگر
        boolean coreStarted = tryInvokeKnownCore(configJson);
        if (!coreStarted) {
            throw new IllegalStateException(
                    "V2Ray/Xray native core not found on classpath.\n" +
                    "To use VLESS, VMess, or Trojan protocols, please add a native library like 'libv2ray.aar' or 'AndroidLibXrayLite' to your dependencies, " +
                    "or register a custom core runner via XrayBridgeEngine.setCustomCoreRunner(runner)."
            );
        }
    }

    private boolean tryInvokeKnownCore(String configJson) {
        String[] candidateClasses = new String[]{
                "com.github.libxray.LibXray",
                "io.nekohasekai.libbox.BoxService",
                "ir.M_Rostamzadeh.Tahrim_Gozar.xray.NativeCoreService"
        };

        for (String className : candidateClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                for (Method m : clazz.getMethods()) {
                    if (m.getName().toLowerCase(Locale.ROOT).contains("start") &&
                            m.getParameterCount() == 1 &&
                            m.getParameterTypes()[0] == String.class) {
                        m.invoke(null, configJson);
                        Log.i(TAG, "Successfully started native core via " + className + "." + m.getName());
                        return true;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private void waitForPort(int port, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("127.0.0.1", port), 100);
                Log.d(TAG, "Inbound port " + port + " is active and ready.");
                return;
            } catch (Exception e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }
        Log.w(TAG, "Waiting for local port " + port + " completed.");
    }

    @Override
    public synchronized void stop() {
        if (!isRunning.get() && !router.isRouted()) {
            return;
        }

        try {
            if (customRunner != null) {
                customRunner.stopCore();
            } else {
                stopKnownCore();
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error stopping native core", t);
        }

        if (context != null) {
            router.resetRouting(context);
        }

        isRunning.set(false);
        localPort = 0;
        localPortHttp = 0;
        currentConfig = null;
        Log.i(TAG, "XrayBridgeEngine stopped and routing restored.");
    }

    private void stopKnownCore() {
        if (activeCoreController != null) {
            try {
                Method stopLoop = activeCoreController.getClass().getMethod("stopLoop");
                stopLoop.invoke(activeCoreController);
                Log.i(TAG, "Successfully stopped libv2ray CoreController loop");
            } catch (Throwable t) {
                Log.w(TAG, "Error calling stopLoop on CoreController", t);
            }
            activeCoreController = null;
        }

        String[] candidateClasses = new String[]{
                "com.github.libxray.LibXray",
                "io.nekohasekai.libbox.BoxService",
                "ir.M_Rostamzadeh.Tahrim_Gozar.xray.NativeCoreService"
        };

        for (String className : candidateClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                for (Method m : clazz.getMethods()) {
                    if (m.getName().toLowerCase(Locale.ROOT).contains("stop") && m.getParameterCount() == 0) {
                        m.invoke(null);
                        return;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public int getLocalPort() {
        return localPort;
    }

    public int getLocalPortHttp() {
        return localPortHttp;
    }

    @Override
    public TunnelConfig getCurrentConfig() {
        return currentConfig;
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    public static String generateClientJson(TunnelConfig config, int inboundSocksPort) {
        return generateClientJson(config, inboundSocksPort, inboundSocksPort + 1);
    }

    /**
     * ساخت کانفیگ کلاینت استاندارد Xray/V2Ray سازگار با هسته‌های رسمی با هر دو اینباند SOCKS و HTTP
     */
    public static String generateClientJson(TunnelConfig config, int inboundSocksPort, int inboundHttpPort) {
        if (config.getType() == ConfigType.RAW_JSON && config.getRawJson() != null) {
            return processRawJsonConfig(config.getRawJson(), inboundSocksPort, inboundHttpPort);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"log\": { \"loglevel\": \"warning\" },\n");

        // ۱. اینباندهای محلی SOCKS و HTTP
        appendInboundsJson(sb, inboundSocksPort, inboundHttpPort);

        // ۲. تنظیمات DNS داخلی جهت دور زدن مسمومیت دی‌ان‌اس اپراتورها
        sb.append("  \"dns\": {\n");
        sb.append("    \"servers\": [\"1.1.1.1\", \"8.8.8.8\", \"https+local://cloudflare-dns.com/dns-query\"]\n");
        sb.append("  },\n");

        // ۳. آوتباند متناسب با پروتکل
        sb.append("  \"outbounds\": [\n");
        appendOutbound(sb, config);
        sb.append(",\n");
        sb.append("    { \"protocol\": \"freedom\", \"tag\": \"direct\" }\n");
        sb.append("  ]\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * تزریق هوشمند اینباندهای SOCKS و HTTP به فایل JSON خام دریافتی
     */
    public static String processRawJsonConfig(String rawJson, int inboundSocksPort, int inboundHttpPort) {
        String trimmed = rawJson.trim();

        // بررسی اینکه آیا یک آبجکت اوتباند تکی بدون فیلدهای اصلی است
        boolean isSingleOutbound = trimmed.startsWith("{") && (trimmed.contains("\"protocol\"") || trimmed.contains("\"settings\""))
                && !trimmed.contains("\"inbounds\"") && !trimmed.contains("\"outbounds\"");

        if (isSingleOutbound) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"log\": { \"loglevel\": \"warning\" },\n");
            appendInboundsJson(sb, inboundSocksPort, inboundHttpPort);
            sb.append("  \"dns\": {\n");
            sb.append("    \"servers\": [\"1.1.1.1\", \"8.8.8.8\", \"https+local://cloudflare-dns.com/dns-query\"]\n");
            sb.append("  },\n");
            sb.append("  \"outbounds\": [\n");
            sb.append("    ").append(trimmed).append(",\n");
            sb.append("    { \"protocol\": \"freedom\", \"tag\": \"direct\" }\n");
            sb.append("  ]\n");
            sb.append("}\n");
            return sb.toString();
        }

        // اگر JSON فاقد inbounds باشد
        if (!trimmed.contains("\"inbounds\"")) {
            int firstBrace = trimmed.indexOf('{');
            if (firstBrace != -1) {
                StringBuilder sb = new StringBuilder();
                sb.append(trimmed.substring(0, firstBrace + 1)).append("\n");
                appendInboundsJson(sb, inboundSocksPort, inboundHttpPort);
                sb.append(trimmed.substring(firstBrace + 1));
                return sb.toString();
            }
        } else {
            // تزریق اینباندهای لوکال تحریم‌گذر در ابتدای آرایه inbounds
            int inboundsIdx = trimmed.indexOf("\"inbounds\"");
            int openBracket = trimmed.indexOf('[', inboundsIdx);
            if (openBracket != -1) {
                StringBuilder sb = new StringBuilder();
                sb.append(trimmed.substring(0, openBracket + 1)).append("\n");
                sb.append("    {\n");
                sb.append("      \"tag\": \"socks-in-tahrim\",\n");
                sb.append("      \"listen\": \"127.0.0.1\",\n");
                sb.append("      \"port\": ").append(inboundSocksPort).append(",\n");
                sb.append("      \"protocol\": \"socks\",\n");
                sb.append("      \"settings\": { \"auth\": \"noauth\", \"udp\": true }\n");
                sb.append("    },\n");
                sb.append("    {\n");
                sb.append("      \"tag\": \"http-in-tahrim\",\n");
                sb.append("      \"listen\": \"127.0.0.1\",\n");
                sb.append("      \"port\": ").append(inboundHttpPort).append(",\n");
                sb.append("      \"protocol\": \"http\"\n");
                sb.append("    },\n");
                sb.append(trimmed.substring(openBracket + 1));
                return sb.toString();
            }
        }

        return trimmed;
    }

    private static void appendInboundsJson(StringBuilder sb, int socksPort, int httpPort) {
        sb.append("  \"inbounds\": [\n");
        sb.append("    {\n");
        sb.append("      \"tag\": \"socks-in\",\n");
        sb.append("      \"listen\": \"127.0.0.1\",\n");
        sb.append("      \"port\": ").append(socksPort).append(",\n");
        sb.append("      \"protocol\": \"socks\",\n");
        sb.append("      \"settings\": { \"auth\": \"noauth\", \"udp\": true }\n");
        sb.append("    },\n");
        sb.append("    {\n");
        sb.append("      \"tag\": \"http-in\",\n");
        sb.append("      \"listen\": \"127.0.0.1\",\n");
        sb.append("      \"port\": ").append(httpPort).append(",\n");
        sb.append("      \"protocol\": \"http\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
    }

    private static void appendOutbound(StringBuilder sb, TunnelConfig config) {
        sb.append("    {\n");
        sb.append("      \"tag\": \"proxy\",\n");

        ConfigType type = config.getType();
        if (type == ConfigType.V2RAY_VLESS) {
            appendVlessOutbound(sb, config);
        } else if (type == ConfigType.V2RAY_VMESS) {
            appendVmessOutbound(sb, config);
        } else if (type == ConfigType.V2RAY_TROJAN) {
            appendTrojanOutbound(sb, config);
        } else if (type == ConfigType.SHADOWSOCKS || type == ConfigType.SHADOWSOCKSR) {
            appendShadowsocksOutbound(sb, config);
        } else if (type == ConfigType.WIREGUARD) {
            appendWireguardOutbound(sb, config);
        } else {
            sb.append("      \"protocol\": \"freedom\"\n");
        }

        sb.append("    }");
    }

    private static void appendVlessOutbound(StringBuilder sb, TunnelConfig config) {
        sb.append("      \"protocol\": \"vless\",\n");
        sb.append("      \"settings\": {\n");
        sb.append("        \"vnext\": [{\n");
        sb.append("          \"address\": \"").append(config.getHost()).append("\",\n");
        sb.append("          \"port\": ").append(config.getPort()).append(",\n");
        sb.append("          \"users\": [{\n");
        sb.append("            \"id\": \"").append(config.getUuid()).append("\",\n");
        sb.append("            \"encryption\": \"none\"");

        String flow = config.getExtraParams().get("flow");
        if (flow != null && !flow.isEmpty()) {
            sb.append(",\n            \"flow\": \"").append(flow).append("\"");
        }
        sb.append("\n          }]\n");
        sb.append("        }]\n");
        sb.append("      },\n");

        appendStreamSettings(sb, config);
    }

    private static void appendVmessOutbound(StringBuilder sb, TunnelConfig config) {
        sb.append("      \"protocol\": \"vmess\",\n");
        sb.append("      \"settings\": {\n");
        sb.append("        \"vnext\": [{\n");
        sb.append("          \"address\": \"").append(config.getHost()).append("\",\n");
        sb.append("          \"port\": ").append(config.getPort()).append(",\n");

        String cipher = config.getExtraParams().get("scy");
        if (cipher == null || cipher.isEmpty()) cipher = "auto";

        String aid = config.getExtraParams().get("aid");
        int alterId = (aid != null && !aid.isEmpty()) ? (int) Double.parseDouble(aid) : 0;

        sb.append("          \"users\": [{\n");
        sb.append("            \"id\": \"").append(config.getUuid()).append("\",\n");
        sb.append("            \"alterId\": ").append(alterId).append(",\n");
        sb.append("            \"security\": \"").append(cipher).append("\"\n");
        sb.append("          }]\n");
        sb.append("        }]\n");
        sb.append("      },\n");

        appendStreamSettings(sb, config);
    }

    private static void appendTrojanOutbound(StringBuilder sb, TunnelConfig config) {
        sb.append("      \"protocol\": \"trojan\",\n");
        sb.append("      \"settings\": {\n");
        sb.append("        \"servers\": [{\n");
        sb.append("          \"address\": \"").append(config.getHost()).append("\",\n");
        sb.append("          \"port\": ").append(config.getPort()).append(",\n");
        sb.append("          \"password\": \"").append(config.getPassword() != null ? config.getPassword() : "").append("\"\n");
        sb.append("        }]\n");
        sb.append("      },\n");

        appendStreamSettings(sb, config);
    }

    private static void appendShadowsocksOutbound(StringBuilder sb, TunnelConfig config) {
        String method = config.getExtraParams().get("method");
        if (method == null || method.isEmpty()) method = "aes-128-gcm";

        sb.append("      \"protocol\": \"shadowsocks\",\n");
        sb.append("      \"settings\": {\n");
        sb.append("        \"servers\": [{\n");
        sb.append("          \"address\": \"").append(config.getHost()).append("\",\n");
        sb.append("          \"port\": ").append(config.getPort()).append(",\n");
        sb.append("          \"method\": \"").append(method).append("\",\n");
        sb.append("          \"password\": \"").append(config.getPassword() != null ? config.getPassword() : "").append("\"\n");
        sb.append("        }]\n");
        sb.append("      }\n");
    }

    private static void appendWireguardOutbound(StringBuilder sb, TunnelConfig config) {
        sb.append("      \"protocol\": \"wireguard\",\n");
        sb.append("      \"settings\": {\n");
        String secretKey = config.getPassword() != null ? config.getPassword() : config.getExtraParams().get("privateKey");
        if (secretKey == null) secretKey = "";
        sb.append("        \"secretKey\": \"").append(secretKey).append("\",\n");

        String address = config.getExtraParams().get("address");
        if (address == null || address.isEmpty()) address = "10.0.0.2/32";
        sb.append("        \"address\": [\"").append(address).append("\"],\n");

        sb.append("        \"peers\": [{\n");
        sb.append("          \"publicKey\": \"").append(config.getPublicKey() != null ? config.getPublicKey() : "").append("\",\n");
        sb.append("          \"endpoint\": \"").append(config.getHost()).append(":").append(config.getPort()).append("\"\n");
        sb.append("        }]\n");
        sb.append("      }\n");
    }

    private static void appendStreamSettings(StringBuilder sb, TunnelConfig config) {
        sb.append("      \"streamSettings\": {\n");
        String net = config.getNetwork() != null ? config.getNetwork().toLowerCase(Locale.ROOT) : "tcp";
        sb.append("        \"network\": \"").append(net).append("\",\n");

        String sec = config.getSecurity() != null ? config.getSecurity() : "none";
        sb.append("        \"security\": \"").append(sec).append("\"");

        if ("reality".equalsIgnoreCase(sec)) {
            sb.append(",\n        \"realitySettings\": {\n");
            sb.append("          \"serverName\": \"").append(config.getSni() != null ? config.getSni() : config.getHost()).append("\",\n");
            sb.append("          \"fingerprint\": \"").append(config.getFingerprint() != null ? config.getFingerprint() : "chrome").append("\",\n");
            sb.append("          \"publicKey\": \"").append(config.getPublicKey() != null ? config.getPublicKey() : "").append("\",\n");
            sb.append("          \"shortId\": \"").append(config.getShortId() != null ? config.getShortId() : "").append("\"");

            String spx = config.getExtraParams().get("spx");
            if (spx != null && !spx.isEmpty()) {
                sb.append(",\n          \"spiderX\": \"").append(spx).append("\"");
            }
            sb.append("\n        }");
        } else if ("tls".equalsIgnoreCase(sec)) {
            sb.append(",\n        \"tlsSettings\": {\n");
            sb.append("          \"serverName\": \"").append(config.getSni() != null ? config.getSni() : config.getHost()).append("\"");

            String fp = config.getFingerprint();
            if (fp != null && !fp.isEmpty()) {
                sb.append(",\n          \"fingerprint\": \"").append(fp).append("\"");
            }

            String alpn = config.getAlpn();
            if (alpn != null && !alpn.isEmpty()) {
                String[] alpnParts = alpn.split("[,;]");
                sb.append(",\n          \"alpn\": [");
                for (int i = 0; i < alpnParts.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append("\"").append(alpnParts[i].trim()).append("\"");
                }
                sb.append("]");
            }

            String allowInsecure = config.getExtraParams().get("allowInsecure");
            if ("1".equals(allowInsecure) || "true".equalsIgnoreCase(allowInsecure) || "true".equalsIgnoreCase(config.getExtraParams().get("insecure"))) {
                sb.append(",\n          \"allowInsecure\": true");
            }
            sb.append("\n        }");
        }

        // بررسی و اضافه کردن تنظیمات شبکه (xhttp, httpupgrade, ws, grpc, kcp)
        if ("xhttp".equalsIgnoreCase(net) || "splithttp".equalsIgnoreCase(net)) {
            sb.append(",\n        \"xhttpSettings\": {\n");
            String mode = config.getExtraParams().get("mode");
            sb.append("          \"mode\": \"").append(mode != null ? mode : "auto").append("\",\n");
            String path = config.getPath() != null ? config.getPath() : "/";
            sb.append("          \"path\": \"").append(path).append("\"");
            String host = config.getExtraParams().get("host");
            if (host != null && !host.isEmpty()) {
                sb.append(",\n          \"host\": \"").append(host).append("\"");
            }
            String extra = config.getExtraParams().get("extra");
            if (extra != null && !extra.isEmpty()) {
                sb.append(",\n          \"extra\": ").append(extra);
            }
            sb.append("\n        }\n");
        } else if ("httpupgrade".equalsIgnoreCase(net)) {
            sb.append(",\n        \"httpupgradeSettings\": {\n");
            String path = config.getPath() != null ? config.getPath() : "/";
            sb.append("          \"path\": \"").append(path).append("\"");
            String host = config.getExtraParams().get("host");
            if (host != null && !host.isEmpty()) {
                sb.append(",\n          \"host\": \"").append(host).append("\"");
            }
            sb.append("\n        }\n");
        } else if ("ws".equalsIgnoreCase(net)) {
            sb.append(",\n        \"wsSettings\": {\n");
            String path = config.getPath() != null ? config.getPath() : "/";
            sb.append("          \"path\": \"").append(path).append("\"");
            String host = config.getExtraParams().get("host");
            if (host != null && !host.isEmpty()) {
                sb.append(",\n          \"headers\": { \"Host\": \"").append(host).append("\" }");
            }
            sb.append("\n        }\n");
        } else if ("grpc".equalsIgnoreCase(net)) {
            sb.append(",\n        \"grpcSettings\": {\n");
            String serviceName = config.getPath() != null ? config.getPath() : "";
            sb.append("          \"serviceName\": \"").append(serviceName).append("\"\n");
            sb.append("        }\n");
        } else if ("kcp".equalsIgnoreCase(net)) {
            sb.append(",\n        \"kcpSettings\": {\n");
            String headerType = config.getExtraParams().get("headerType");
            if (headerType == null) headerType = "none";
            sb.append("          \"header\": { \"type\": \"").append(headerType).append("\" }\n");
            sb.append("        }\n");
        } else {
            sb.append("\n");
        }

        sb.append("      }\n");
    }
}

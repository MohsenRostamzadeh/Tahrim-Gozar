package ir.M_Rostamzadeh.Tahrim_Gozar.dns;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Directory and specifications of anti-sanction DNS providers and international resolvers.
 * Contains IP addresses and DoH endpoints for Shecan, Electro, Radar, 403, Begzar, Cloudflare, Google.
 * <p>
 * دایرکتوری و مشخصات سرورهای DNS ضدتحریم و دی‌ان‌اس‌های معتبر بین‌المللی
 */
public final class AntiSanctionDns {

    public static final List<String> SHECAN = Collections.unmodifiableList(
            Arrays.asList("178.22.122.100", "185.51.200.2")
    );
    public static final String SHECAN_DOH = "https://free.shecan.ir/dns-query";

    public static final List<String> ELECTRO = Collections.unmodifiableList(
            Arrays.asList("78.157.42.100", "78.157.42.101")
    );
    public static final String ELECTRO_DOH = "https://elc.electrotm.org/dns-query";

    public static final List<String> RADAR = Collections.unmodifiableList(
            Arrays.asList("10.202.10.10", "10.202.10.11")
    );

    public static final List<String> DNS_403 = Collections.unmodifiableList(
            Arrays.asList("10.202.10.202", "10.202.10.102")
    );
    public static final String DNS_403_DOH = "https://dns.403.online/dns-query";

    public static final List<String> BEGZAR = Collections.unmodifiableList(
            Arrays.asList("185.55.226.26", "185.55.225.25")
    );

    public static final List<String> CLOUDFLARE = Collections.unmodifiableList(
            Arrays.asList("1.1.1.1", "1.0.0.1")
    );
    public static final String CLOUDFLARE_DOH = "https://cloudflare-dns.com/dns-query";

    public static final List<String> GOOGLE = Collections.unmodifiableList(
            Arrays.asList("8.8.8.8", "8.8.4.4")
    );
    public static final String GOOGLE_DOH = "https://dns.google/dns-query";

    private static final Map<String, List<String>> REGISTRY = new HashMap<>();
    private static final Map<String, String> DOH_REGISTRY = new HashMap<>();

    static {
        register("shecan", SHECAN, SHECAN_DOH);
        register("electro", ELECTRO, ELECTRO_DOH);
        register("radar", RADAR, null);
        register("403", DNS_403, DNS_403_DOH);
        register("begzar", BEGZAR, null);
        register("cloudflare", CLOUDFLARE, CLOUDFLARE_DOH);
        register("google", GOOGLE, GOOGLE_DOH);
    }

    private AntiSanctionDns() {
        // Utility class
    }

    public static void register(String name, List<String> servers, String dohUrl) {
        String key = name.toLowerCase(Locale.ROOT);
        REGISTRY.put(key, Collections.unmodifiableList(servers));
        if (dohUrl != null) {
            DOH_REGISTRY.put(key, dohUrl);
        }
    }

    public static List<String> getServers(String name) {
        if (name == null) return SHECAN;
        List<String> list = REGISTRY.get(name.toLowerCase(Locale.ROOT));
        return list != null ? list : SHECAN;
    }

    public static String getDoHUrl(String name) {
        if (name == null) return null;
        return DOH_REGISTRY.get(name.toLowerCase(Locale.ROOT));
    }

    public static Map<String, List<String>> getAllPresets() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}

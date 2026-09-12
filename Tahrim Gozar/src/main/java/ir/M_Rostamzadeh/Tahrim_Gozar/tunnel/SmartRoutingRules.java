package ir.M_Rostamzadeh.Tahrim_Gozar.tunnel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Smart routing rules engine for in-app Split Tunneling.
 * Allows domestic Iranian traffic (.ir and local services) to bypass the proxy,
 * while directing blocked/sanctioned domains through the in-app tunnel.
 * <p>
 * موتور قوانین مسیریابی هوشمند برای تفکیک ترافیک داخلی و خارجی (Split Tunneling).
 * امکان عبور مستقیم سایت‌های ایرانی و محلی را فراهم می‌آورد در حالی که دامنه‌های خارجی از تونل عبور می‌کنند.
 */
public class SmartRoutingRules {

    private final AtomicBoolean isEnabled = new AtomicBoolean(false);
    private final AtomicBoolean bypassDomesticTraffic = new AtomicBoolean(true);

    private final Set<String> domesticTlds = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> bypassDomains = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> proxyDomains = Collections.synchronizedSet(new HashSet<>());

    public SmartRoutingRules() {
        // Default domestic TLD is .ir, can be customized or replaced for other countries
        domesticTlds.add(".ir");
    }

    /**
     * Enables or disables split tunneling rules.
     * فعال یا غیرفعال‌سازی تفکیک ترافیک.
     */
    public SmartRoutingRules setEnabled(boolean enabled) {
        this.isEnabled.set(enabled);
        return this;
    }

    public boolean isEnabled() {
        return isEnabled.get();
    }

    /**
     * Enables or disables bypassing all domestic domains for the configured country TLDs (default: true).
     * فعال یا غیرفعال‌سازی عبور مستقیم دامنه‌های ملی/داخلی کشورها.
     */
    public SmartRoutingRules setBypassDomesticTraffic(boolean bypass) {
        this.bypassDomesticTraffic.set(bypass);
        return this;
    }

    public boolean isBypassDomesticTraffic() {
        return bypassDomesticTraffic.get();
    }

    /**
     * Backward-compatible alias for {@link #setBypassDomesticTraffic(boolean)}.
     */
    public SmartRoutingRules setBypassIranTraffic(boolean bypass) {
        return setBypassDomesticTraffic(bypass);
    }

    /**
     * Backward-compatible alias for {@link #isBypassDomesticTraffic()}.
     */
    public boolean isBypassIranTraffic() {
        return isBypassDomesticTraffic();
    }

    /**
     * Adds a domestic top-level domain (TLD) for automatic bypass (e.g. ".ir", ".ru", ".cn", ".tr").
     * افزودن پسوند دامنه ملی برای عبور مستقیم خودکار ترافیک داخلی.
     *
     * @param tld Domain extension (with or without leading dot, e.g. "ir" or ".ir")
     */
    public SmartRoutingRules addDomesticTld(String tld) {
        if (tld != null && !tld.trim().isEmpty()) {
            String clean = tld.trim().toLowerCase(Locale.ROOT);
            if (!clean.startsWith(".")) clean = "." + clean;
            domesticTlds.add(clean);
        }
        return this;
    }

    /**
     * Removes a domestic TLD from the bypass list.
     * حذف پسوند دامنه ملی.
     */
    public SmartRoutingRules removeDomesticTld(String tld) {
        if (tld != null) {
            String clean = tld.trim().toLowerCase(Locale.ROOT);
            if (!clean.startsWith(".")) clean = "." + clean;
            domesticTlds.remove(clean);
        }
        return this;
    }

    /**
     * Sets the active domestic TLDs, replacing current ones.
     * جایگزینی کامل لیست پسوندهای ملی داخلی.
     */
    public SmartRoutingRules setDomesticTlds(java.util.Collection<String> tlds) {
        domesticTlds.clear();
        if (tlds != null) {
            for (String tld : tlds) {
                addDomesticTld(tld);
            }
        }
        return this;
    }

    /**
     * Gets unmodifiable view of configured domestic TLDs.
     * دریافت لیست پسوندهای دامنه ملی فعال.
     */
    public Set<String> getDomesticTlds() {
        return Collections.unmodifiableSet(new HashSet<>(domesticTlds));
    }

    /**
     * Adds a domain or wildcard pattern to bypass the tunnel (direct connection).
     * افزودن دامنه یا الگوی Wildcard برای عبور مستقیم بدون پروکسی.
     *
     * @param domain Domain name or pattern (e.g. "*.bank.ir", "api.internal.com")
     */
    public SmartRoutingRules addBypassDomain(String domain) {
        if (domain != null && !domain.trim().isEmpty()) {
            bypassDomains.add(domain.trim().toLowerCase(Locale.ROOT));
        }
        return this;
    }

    /**
     * Removes a domain from the bypass list.
     * حذف دامنه از لیست عبور مستقیم.
     */
    public SmartRoutingRules removeBypassDomain(String domain) {
        if (domain != null) {
            bypassDomains.remove(domain.trim().toLowerCase(Locale.ROOT));
        }
        return this;
    }

    /**
     * Adds a domain or wildcard pattern that MUST go through the tunnel.
     * افزودن دامنه جهت اجبار به عبور از تونل تحریم‌گذر.
     */
    public SmartRoutingRules addProxyDomain(String domain) {
        if (domain != null && !domain.trim().isEmpty()) {
            proxyDomains.add(domain.trim().toLowerCase(Locale.ROOT));
        }
        return this;
    }

    /**
     * Clears all custom bypass and proxy domains.
     * پاکسازی تمام دامنه‌های سفارشی.
     */
    public SmartRoutingRules clearRules() {
        bypassDomains.clear();
        proxyDomains.clear();
        return this;
    }

    public Set<String> getBypassDomains() {
        return Collections.unmodifiableSet(new HashSet<>(bypassDomains));
    }

    public Set<String> getProxyDomains() {
        return Collections.unmodifiableSet(new HashSet<>(proxyDomains));
    }

    /**
     * Determines whether traffic to the target host should bypass the in-app tunnel.
     * بررسی اینکه آیا ترافیک مقصد باید مستقیماً (بدون پروکسی) عبور کند یا خیر.
     *
     * @param host Target hostname or IP / نام هاست یا IP مقصد
     * @return True if connection should be DIRECT, false if it should go through tunnel
     */
    public boolean shouldBypass(String host) {
        if (host == null || host.trim().isEmpty()) {
            return false;
        }

        String h = host.trim().toLowerCase(Locale.ROOT);

        // Always bypass localhost and private RFC-1918 subnets
        if (isLocalOrPrivate(h)) {
            return true;
        }

        if (!isEnabled.get()) {
            return false;
        }

        // 1. If explicitly in proxyDomains, do NOT bypass
        for (String rule : proxyDomains) {
            if (matchesDomainRule(h, rule)) {
                return false;
            }
        }

        // 2. If explicitly in bypassDomains, DO bypass
        for (String rule : bypassDomains) {
            if (matchesDomainRule(h, rule)) {
                return true;
            }
        }

        // 3. If bypassDomesticTraffic is true and host matches any configured domestic country TLD, DO bypass
        if (bypassDomesticTraffic.get()) {
            for (String tld : domesticTlds) {
                if (h.endsWith(tld) || h.contains(tld + ":") || h.contains(tld + "/")) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isLocalOrPrivate(String host) {
        if (host.equalsIgnoreCase("localhost") || host.startsWith("127.") || host.equals("::1")) {
            return true;
        }
        if (host.startsWith("10.") || host.startsWith("192.168.")) {
            return true;
        }
        if (host.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) {
            return true;
        }
        return false;
    }

    private static boolean matchesDomainRule(String host, String rule) {
        if (rule.equals(host)) {
            return true;
        }
        if (rule.startsWith("*.")) {
            String suffix = rule.substring(2);
            return host.endsWith(suffix) || host.equals(suffix);
        }
        if (rule.startsWith(".")) {
            return host.endsWith(rule) || host.equals(rule.substring(1));
        }
        return false;
    }
}

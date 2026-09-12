package ir.M_Rostamzadeh.Tahrim_Gozar.config;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ir.M_Rostamzadeh.Tahrim_Gozar.dns.AntiSanctionDns;

/**
 * Universal Intelligent Configuration Parser.
 * Supports DNS presets, custom DNS, DoH/DoT, HTTP/SOCKS proxies, V2Ray/Xray protocols
 * (VLESS, VMess, Trojan, Shadowsocks, ShadowsocksR, Hysteria2, TUIC, WireGuard), Clash YAML, and Raw JSON.
 * <p>
 * پارسر جامع و هوشمند انواع کانفیگ‌های شبکه و تونلینگ ضدتحریم
 */
public class UniversalConfigParser {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
    );

    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^\\[?([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}\\]?$"
    );

    /**
     * Parses and converts any raw configuration string into a structured {@link TunnelConfig} instance.
     * <p>
     * تحلیل و تبدیل هر نوع رشته کانفیگ به شیء ساختاریافته {@link TunnelConfig}
     *
     * @param input Raw config string (DNS preset, vless, vmess, trojan, socks5, http, json, etc.)
     * @return Structured {@link TunnelConfig} instance
     * @throws IllegalArgumentException If input is null, empty, or format is unsupported
     */
    public static TunnelConfig parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Config input cannot be empty.");
        }

        String raw = input.trim().replace("\\&", "&");

        // 0. Handle multi-line configs (Clash YAML, WireGuard Conf, raw JSON, multi-config pastes)
        // ۰. بررسی ورودی‌های چندخطی (مانند کانفیگ‌های متنی، سابسکرپشن‌ها، WireGuard Conf، Clash YAML)
        if (raw.contains("\n") || raw.contains("\r")) {
            if (isDnsIpList(raw)) {
                return parseDnsIpList(raw);
            }
            if ((raw.startsWith("{") && raw.endsWith("}")) || (raw.startsWith("[") && raw.endsWith("]"))) {
                try {
                    return parseRawJson(raw);
                } catch (Exception ignored) {
                }
            }
            if (raw.contains("[Interface]") && raw.contains("[Peer]")) {
                return parseWireguardConf(raw);
            }
            if (raw.contains("proxies:") || (raw.contains("- name:") && raw.contains("type:"))) {
                return parseClashYaml(raw);
            }
            // Extract the first valid config if multiple lines are pasted
            // اگر کاربر چندین کانفیگ را همزمان پیست کرده باشد، اولین کانفیگ معتبر را استخراج می‌کنیم
            String[] lines = raw.split("[\r\n]+");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) continue;
                try {
                    return parse(trimmed);
                } catch (Exception ignored) {
                }
            }
        }

        // 1. Direct DNS IP list (IPv4 / IPv6 addresses or bracketed IPv6)
        // ۱. بررسی IP مستقیم DNS (شامل IPv4 و IPv6 کروشه‌دار یا ساده)
        if (isDnsIpList(raw)) {
            return parseDnsIpList(raw);
        }

        // 2. Anti-sanction DNS preset names (shecan, electro, 403, radar, begzar, etc.)
        // ۲. بررسی پری‌ست‌های DNS ضدتحریم
        String lower = raw.toLowerCase(Locale.ROOT);
        if (isDnsPreset(lower)) {
            return parseDnsPreset(lower, raw);
        }

        // 3. Raw JSON object or array
        // ۳. بررسی JSON خام (شیء یا آرایه)
        if ((raw.startsWith("{") && raw.endsWith("}")) || (raw.startsWith("[") && raw.endsWith("]"))) {
            try {
                return parseRawJson(raw);
            } catch (Exception ignored) {
            }
        }

        // 4. URL scheme matching for various protocols
        // ۴. بررسی URLها و پروتکل‌های مختلف
        if (lower.startsWith("vless://")) {
            return parseVless(raw);
        } else if (lower.startsWith("vmess://")) {
            return parseVmess(raw);
        } else if (lower.startsWith("trojan://")) {
            return parseTrojan(raw);
        } else if (lower.startsWith("ss://")) {
            return parseShadowsocks(raw);
        } else if (lower.startsWith("ssr://")) {
            return parseSsr(raw);
        } else if (lower.startsWith("hysteria2://") || lower.startsWith("hy2://")) {
            return parseHysteria2(raw);
        } else if (lower.startsWith("tuic://")) {
            return parseTuic(raw);
        } else if (lower.startsWith("wireguard://")) {
            return parseWireguard(raw);
        } else if (lower.startsWith("socks5://") || lower.startsWith("socks://")) {
            return parseStandardProxy(ConfigType.PROXY_SOCKS5, raw);
        } else if (lower.startsWith("socks4://")) {
            return parseStandardProxy(ConfigType.PROXY_SOCKS4, raw);
        } else if (lower.startsWith("https://") && (lower.contains("/dns-query") || lower.contains("dns"))) {
            return parseDoH(raw);
        } else if (lower.startsWith("doh://")) {
            return parseDoH(raw.replace("doh://", "https://"));
        } else if (lower.startsWith("tls://") || lower.startsWith("dot://")) {
            return parseDoT(raw);
        } else if (lower.startsWith("dns://")) {
            return parseDnsUri(raw);
        } else if (lower.startsWith("http://") || lower.startsWith("https://")) {
            // Check if subscription or HTTP proxy
            // ممکن است پروکسی HTTP باشد یا لینک سابسکرپشن
            if (isSubscriptionUrl(raw)) {
                return new TunnelConfig.Builder(ConfigType.SUBSCRIPTION)
                        .rawConfig(raw)
                        .name("Subscription")
                        .build();
            }
            ConfigType type = lower.startsWith("https://") ? ConfigType.PROXY_HTTPS : ConfigType.PROXY_HTTP;
            return parseStandardProxy(type, raw);
        }

        // 5. Host:port format without scheme (IPv4, hostname, or bracketed IPv6)
        // ۵. در صورتی که ورودی به صورت host:port ساده باشد (بدون پیشوند)
        if (raw.matches("^[a-zA-Z0-9.-]+:\\d+$")) {
            String[] parts = raw.split(":");
            return new TunnelConfig.Builder(ConfigType.PROXY_HTTP)
                    .rawConfig(raw)
                    .host(parts[0])
                    .port(Integer.parseInt(parts[1]))
                    .build();
        } else if (raw.matches("^\\[[0-9a-fA-F:]+\\]:\\d+$")) {
            int closingBracket = raw.indexOf("]:");
            String host = raw.substring(1, closingBracket);
            int port = Integer.parseInt(raw.substring(closingBracket + 2));
            return new TunnelConfig.Builder(ConfigType.PROXY_HTTP)
                    .rawConfig(raw)
                    .host(host)
                    .port(port)
                    .build();
        }

        // 6. Base64 decoded config check
        // ۶. اگر رشته طولانی فاقد پروتکل صریح است، بررسی می‌کنیم که آیا یک سابسکرپشن یا کانفیگ Base64 است یا خیر
        if (!raw.contains("://") && !raw.contains(" ") && raw.length() > 16) {
            try {
                String decoded = decodeBase64(raw);
                if (decoded != null && !decoded.trim().isEmpty() && !decoded.equals(raw)) {
                    if (decoded.contains("://") || decoded.contains("[Interface]") || decoded.contains("proxies:")
                            || decoded.contains("{") || decoded.contains("\n") || isDnsPreset(decoded.toLowerCase(Locale.ROOT))) {
                        return parse(decoded);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        throw new IllegalArgumentException("Unsupported or unknown configuration format: " + raw);
    }

    private static boolean isDnsPreset(String lower) {
        return lower.equals("shecan") || lower.equals("شکن") ||
                lower.equals("electro") || lower.equals("الکترو") ||
                lower.equals("radar") || lower.equals("رادار") ||
                lower.equals("403") || lower.equals("۴۰۳") ||
                lower.equals("begzar") || lower.equals("بگذر") ||
                lower.equals("cloudflare") || lower.equals("google");
    }

    private static TunnelConfig parseDnsPreset(String key, String raw) {
        String normalizedKey = key;
        if (key.equals("شکن")) normalizedKey = "shecan";
        else if (key.equals("الکترو")) normalizedKey = "electro";
        else if (key.equals("رادار")) normalizedKey = "radar";
        else if (key.equals("۴۰۳")) normalizedKey = "403";
        else if (key.equals("بگذر")) normalizedKey = "begzar";

        List<String> servers = AntiSanctionDns.getServers(normalizedKey);
        String doh = AntiSanctionDns.getDoHUrl(normalizedKey);

        return new TunnelConfig.Builder(ConfigType.DNS_PRESET)
                .rawConfig(raw)
                .name(normalizedKey.toUpperCase(Locale.ROOT))
                .dnsServers(servers)
                .dohUrl(doh)
                .build();
    }

    private static boolean isIpAddress(String str) {
        if (str == null || str.isEmpty()) return false;
        String trimmed = str.trim();
        if (IPV4_PATTERN.matcher(trimmed).matches()) return true;
        String cleaned = (trimmed.startsWith("[") && trimmed.endsWith("]"))
                ? trimmed.substring(1, trimmed.length() - 1) : trimmed;
        return IPV6_PATTERN.matcher(cleaned).matches();
    }

    private static boolean isDnsIpList(String raw) {
        String[] parts = raw.split("[,;\\s]+");
        if (parts.length == 0) return false;
        for (String p : parts) {
            String trimmed = p.trim();
            if (trimmed.isEmpty()) continue;
            if (!isIpAddress(trimmed)) {
                return false;
            }
        }
        return true;
    }

    private static TunnelConfig parseDnsIpList(String raw) {
        String[] parts = raw.split("[,;\\s]+");
        List<String> servers = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                String clean = (trimmed.startsWith("[") && trimmed.endsWith("]"))
                        ? trimmed.substring(1, trimmed.length() - 1) : trimmed;
                servers.add(clean);
            }
        }
        return new TunnelConfig.Builder(ConfigType.DNS_CUSTOM)
                .rawConfig(raw)
                .name("Custom DNS")
                .dnsServers(servers)
                .build();
    }

    private static TunnelConfig parseDnsUri(String raw) {
        // dns://178.22.122.100:53 or dns://[2001:4860:4860::8888]:53
        String host = raw.substring(6);
        int port = 53;
        if (host.startsWith("[") && host.contains("]:")) {
            int closingBracket = host.indexOf("]:");
            String portStr = host.substring(closingBracket + 2);
            host = host.substring(1, closingBracket);
            port = Integer.parseInt(portStr);
        } else if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        } else if (host.contains(":") && host.indexOf(':') == host.lastIndexOf(':')) {
            String[] parts = host.split(":");
            host = parts[0];
            port = Integer.parseInt(parts[1]);
        }
        return new TunnelConfig.Builder(ConfigType.DNS_CUSTOM)
                .rawConfig(raw)
                .name("DNS URI")
                .host(host)
                .port(port)
                .dnsServers(Collections.singletonList(host))
                .build();
    }

    private static TunnelConfig parseDoH(String raw) {
        return new TunnelConfig.Builder(ConfigType.DNS_OVER_HTTPS)
                .rawConfig(raw)
                .name("DoH Server")
                .dohUrl(raw)
                .build();
    }

    private static TunnelConfig parseDoT(String raw) {
        String clean = raw.replaceAll("^(tls|dot)://", "");
        String host = clean;
        int port = 853;
        if (clean.contains(":")) {
            String[] parts = clean.split(":");
            host = parts[0];
            port = Integer.parseInt(parts[1]);
        }
        return new TunnelConfig.Builder(ConfigType.DNS_OVER_TLS)
                .rawConfig(raw)
                .name("DoT Server")
                .host(host)
                .port(port)
                .build();
    }

    /**
     * Parses standard HTTP, HTTPS, SOCKS4, and SOCKS5 proxy URLs.
     * <p>
     * پارس پروتکل‌های استاندارد پروکسی HTTP، HTTPS، SOCKS4 و SOCKS5
     */
    private static TunnelConfig parseStandardProxy(ConfigType type, String raw) {
        try {
            URI uri = new URI(raw);
            String host = uri.getHost();
            int port = uri.getPort();
            if (port == -1) {
                port = (type == ConfigType.PROXY_HTTPS) ? 443 :
                        (type == ConfigType.PROXY_SOCKS5 || type == ConfigType.PROXY_SOCKS4) ? 1080 : 80;
            }

            String username = null;
            String password = null;
            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isEmpty()) {
                if (userInfo.contains(":")) {
                    String[] creds = userInfo.split(":", 2);
                    username = urlDecode(creds[0]);
                    password = urlDecode(creds[1]);
                } else {
                    username = urlDecode(userInfo);
                }
            }

            return new TunnelConfig.Builder(type)
                    .rawConfig(raw)
                    .host(host)
                    .port(port)
                    .username(username)
                    .password(password)
                    .name(type.name())
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing standard proxy configuration: " + raw, e);
        }
    }

    /**
     * Parses VLESS protocol URI format (e.g. vless://uuid@host:port?query#name).
     * Supports Reality, TLS, WebSocket, gRPC, and custom SNI.
     * <p>
     * پارس پروتکل VLESS با قابلیت استخراج پارامترهای Reality، TLS، WebSocket، gRPC و غیره.
     */
    private static TunnelConfig parseVless(String raw) {
        try {
            int schemeEnd = raw.indexOf("://");
            String remainder = raw.substring(schemeEnd + 3);

            String remark = "";
            int hashIndex = remainder.indexOf('#');
            if (hashIndex != -1) {
                remark = urlDecode(remainder.substring(hashIndex + 1));
                remainder = remainder.substring(0, hashIndex);
            }

            Map<String, String> queryParams = new HashMap<>();
            int questionIndex = remainder.indexOf('?');
            if (questionIndex != -1) {
                String query = remainder.substring(questionIndex + 1);
                remainder = remainder.substring(0, questionIndex);
                queryParams = parseQuery(query);
            }

            int atIndex = remainder.indexOf('@');
            if (atIndex == -1) {
                throw new IllegalArgumentException("VLESS config requires a valid UUID: " + raw);
            }
            String uuid = remainder.substring(0, atIndex);
            String hostPort = remainder.substring(atIndex + 1);

            int colonIndex = hostPort.lastIndexOf(':');
            if (colonIndex == -1) {
                throw new IllegalArgumentException("VLESS config requires a valid port: " + raw);
            }
            String host = hostPort.substring(0, colonIndex);
            int port = Integer.parseInt(hostPort.substring(colonIndex + 1));

            return new TunnelConfig.Builder(ConfigType.V2RAY_VLESS)
                    .rawConfig(raw)
                    .name(remark.isEmpty() ? "VLESS Server" : remark)
                    .uuid(uuid)
                    .host(host)
                    .port(port)
                    .security(queryParams.get("security"))
                    .sni(queryParams.containsKey("sni") ? queryParams.get("sni") : queryParams.get("host"))
                    .network(queryParams.containsKey("type") ? queryParams.get("type") : "tcp")
                    .path(queryParams.get("path"))
                    .publicKey(queryParams.get("pbk"))
                    .shortId(queryParams.get("sid"))
                    .fingerprint(queryParams.get("fp"))
                    .alpn(queryParams.get("alpn"))
                    .extraParams(queryParams)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing VLESS configuration: " + raw, e);
        }
    }

    /**
     * Parses VMess protocol configuration (both base64 encoded JSON and direct URI schemes).
     * <p>
     * پارس پروتکل VMess (فرمت base64 استاندارد حاوی JSON یا لینک مستقیم URI)
     */
    private static TunnelConfig parseVmess(String raw) {
        try {
            String remainder = raw.substring(8).trim();
            if (remainder.contains("@") && remainder.contains(":")) {
                return parseVmessUri(raw, remainder);
            }

            String json = decodeBase64(remainder);
            Map<String, String> map = parseSimpleJson(json);

            String host = map.get("add");
            String portStr = map.get("port");
            int port = (portStr != null && !portStr.isEmpty()) ? (int) Double.parseDouble(portStr) : 443;
            String uuid = map.get("id");
            String remark = map.get("ps");
            String security = map.get("tls");
            String network = map.get("net");
            String path = map.get("path");
            String sni = map.get("sni");
            if (sni == null || sni.isEmpty()) {
                sni = map.get("host");
            }

            return new TunnelConfig.Builder(ConfigType.V2RAY_VMESS)
                    .rawConfig(raw)
                    .name(remark != null && !remark.isEmpty() ? remark : "VMess Server")
                    .host(host)
                    .port(port)
                    .uuid(uuid)
                    .security(security)
                    .network(network != null ? network : "tcp")
                    .path(path)
                    .sni(sni)
                    .rawJson(json)
                    .extraParams(map)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing VMess configuration: " + raw, e);
        }
    }

    /**
     * Parses direct VMess URI scheme (e.g. vmess://uuid@host:port?query#name).
     * <p>
     * پارس لینک مستقیم پروتکل VMess
     */
    private static TunnelConfig parseVmessUri(String raw, String remainder) {
        String remark = "";
        int hash = remainder.indexOf('#');
        if (hash != -1) {
            remark = urlDecode(remainder.substring(hash + 1));
            remainder = remainder.substring(0, hash);
        }

        Map<String, String> queryParams = new HashMap<>();
        int q = remainder.indexOf('?');
        if (q != -1) {
            queryParams = parseQuery(remainder.substring(q + 1));
            remainder = remainder.substring(0, q);
        }

        int at = remainder.indexOf('@');
        String uuid = remainder.substring(0, at);
        String hostPort = remainder.substring(at + 1);
        int colon = hostPort.lastIndexOf(':');
        String host = hostPort.substring(0, colon);
        int port = Integer.parseInt(hostPort.substring(colon + 1));

        return new TunnelConfig.Builder(ConfigType.V2RAY_VMESS)
                .rawConfig(raw)
                .name(remark.isEmpty() ? "VMess Server" : remark)
                .uuid(uuid)
                .host(host)
                .port(port)
                .security(queryParams.get("security"))
                .sni(queryParams.containsKey("sni") ? queryParams.get("sni") : queryParams.get("host"))
                .network(queryParams.containsKey("type") ? queryParams.get("type") : "tcp")
                .path(queryParams.get("path"))
                .extraParams(queryParams)
                .build();
    }

    /**
     * Parses Trojan protocol URI format (e.g. trojan://password@host:port?query#name).
     * Supports TLS, Reality, WebSocket, gRPC, and custom SNI.
     * <p>
     * پارس پروتکل Trojan با پشتیبانی از Reality و TLS
     */
    private static TunnelConfig parseTrojan(String raw) {
        try {
            String remainder = raw.substring(9);
            String remark = "";
            int hashIndex = remainder.indexOf('#');
            if (hashIndex != -1) {
                remark = urlDecode(remainder.substring(hashIndex + 1));
                remainder = remainder.substring(0, hashIndex);
            }

            Map<String, String> queryParams = new HashMap<>();
            int questionIndex = remainder.indexOf('?');
            if (questionIndex != -1) {
                queryParams = parseQuery(remainder.substring(questionIndex + 1));
                remainder = remainder.substring(0, questionIndex);
            }

            int atIndex = remainder.indexOf('@');
            if (atIndex == -1) {
                throw new IllegalArgumentException("Trojan config requires a password: " + raw);
            }
            String password = urlDecode(remainder.substring(0, atIndex));
            String hostPort = remainder.substring(atIndex + 1);

            int colonIndex = hostPort.lastIndexOf(':');
            String host = hostPort.substring(0, colonIndex);
            int port = Integer.parseInt(hostPort.substring(colonIndex + 1));

            String security = queryParams.containsKey("security") ? queryParams.get("security") : "tls";
            String sni = queryParams.containsKey("sni") ? queryParams.get("sni") : (queryParams.containsKey("host") ? queryParams.get("host") : host);

            return new TunnelConfig.Builder(ConfigType.V2RAY_TROJAN)
                    .rawConfig(raw)
                    .name(remark.isEmpty() ? "Trojan Server" : remark)
                    .password(password)
                    .host(host)
                    .port(port)
                    .security(security)
                    .sni(sni)
                    .network(queryParams.containsKey("type") ? queryParams.get("type") : "tcp")
                    .path(queryParams.get("path"))
                    .publicKey(queryParams.get("pbk"))
                    .shortId(queryParams.get("sid"))
                    .fingerprint(queryParams.get("fp"))
                    .alpn(queryParams.get("alpn"))
                    .extraParams(queryParams)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing Trojan configuration: " + raw, e);
        }
    }

    /**
     * Parses Shadowsocks URI formats (SIP002 and legacy base64 encoded).
     * <p>
     * پارس پروتکل Shadowsocks (فرمت‌های استاندارد SIP002 و base64 کلاسیک)
     */
    private static TunnelConfig parseShadowsocks(String raw) {
        try {
            String remainder = raw.substring(5);
            String remark = "";
            int hashIndex = remainder.indexOf('#');
            if (hashIndex != -1) {
                remark = urlDecode(remainder.substring(hashIndex + 1));
                remainder = remainder.substring(0, hashIndex);
            }

            String method = null;
            String password = null;
            String host = null;
            int port = 8388;
            Map<String, String> queryParams = new HashMap<>();

            int atIndex = remainder.indexOf('@');
            if (atIndex != -1) {
                // SIP002: ss://base64(method:password)@host:port/?plugin=...
                String userinfoB64 = remainder.substring(0, atIndex);
                String userinfo = decodeBase64(userinfoB64);
                if (userinfo.contains(":")) {
                    String[] creds = userinfo.split(":", 2);
                    method = creds[0];
                    password = creds[1];
                }
                String hostPort = remainder.substring(atIndex + 1);
                int qIdx = hostPort.indexOf('?');
                if (qIdx != -1) {
                    queryParams = parseQuery(hostPort.substring(qIdx + 1));
                    hostPort = hostPort.substring(0, qIdx);
                }
                if (hostPort.endsWith("/")) {
                    hostPort = hostPort.substring(0, hostPort.length() - 1);
                }
                int colonIndex = hostPort.lastIndexOf(':');
                host = hostPort.substring(0, colonIndex);
                port = Integer.parseInt(hostPort.substring(colonIndex + 1));
            } else {
                // Legacy: ss://base64(method:password@host:port)
                String decoded = decodeBase64(remainder);
                int at = decoded.indexOf('@');
                if (at != -1) {
                    String userinfo = decoded.substring(0, at);
                    if (userinfo.contains(":")) {
                        String[] creds = userinfo.split(":", 2);
                        method = creds[0];
                        password = creds[1];
                    }
                    String hostPort = decoded.substring(at + 1);
                    int colon = hostPort.lastIndexOf(':');
                    host = hostPort.substring(0, colon);
                    port = Integer.parseInt(hostPort.substring(colon + 1));
                }
            }

            Map<String, String> extra = new HashMap<>(queryParams);
            if (method != null) extra.put("method", method);

            return new TunnelConfig.Builder(ConfigType.SHADOWSOCKS)
                    .rawConfig(raw)
                    .name(remark.isEmpty() ? "Shadowsocks" : remark)
                    .host(host)
                    .port(port)
                    .password(password)
                    .extraParams(extra)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing Shadowsocks configuration: " + raw, e);
        }
    }

    /**
     * Parses ShadowsocksR (SSR) URI format (ssr://base64(...)).
     * <p>
     * پارس پروتکل ShadowsocksR (SSR)
     */
    private static TunnelConfig parseSsr(String raw) {
        try {
            String b64 = raw.substring(6).trim();
            String decoded = decodeBase64(b64);

            String mainPart = decoded;
            String queryPart = "";
            int qIdx = decoded.indexOf("/?");
            if (qIdx != -1) {
                mainPart = decoded.substring(0, qIdx);
                queryPart = decoded.substring(qIdx + 2);
            } else {
                int qIdx2 = decoded.indexOf('?');
                if (qIdx2 != -1) {
                    mainPart = decoded.substring(0, qIdx2);
                    queryPart = decoded.substring(qIdx2 + 1);
                }
            }

            String[] parts = mainPart.split(":");
            if (parts.length < 6) {
                throw new IllegalArgumentException("Invalid SSR configuration format: " + decoded);
            }

            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            String protocol = parts[2];
            String method = parts[3];
            String obfs = parts[4];
            String password = decodeBase64(parts[5]);

            Map<String, String> queryParams = parseQuery(queryPart);
            String remarks = queryParams.get("remarks");
            String name = "SSR Server";
            if (remarks != null && !remarks.isEmpty()) {
                try {
                    name = decodeBase64(remarks);
                } catch (Exception ignored) {
                    name = remarks;
                }
            }

            Map<String, String> extra = new HashMap<>(queryParams);
            extra.put("ssr_protocol", protocol);
            extra.put("ssr_obfs", obfs);
            extra.put("method", method);

            return new TunnelConfig.Builder(ConfigType.SHADOWSOCKSR)
                    .rawConfig(raw)
                    .name(name)
                    .host(host)
                    .port(port)
                    .password(password)
                    .extraParams(extra)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing ShadowsocksR configuration: " + raw, e);
        }
    }

    /**
     * Parses Hysteria2 (hy2) protocol URI format based on UDP/QUIC.
     * <p>
     * پارس پروتکل Hysteria2 مبتنی بر UDP/QUIC
     */
    private static TunnelConfig parseHysteria2(String raw) {
        try {
            int schemeEnd = raw.indexOf("://");
            String remainder = raw.substring(schemeEnd + 3);

            String remark = "";
            int hashIndex = remainder.indexOf('#');
            if (hashIndex != -1) {
                remark = urlDecode(remainder.substring(hashIndex + 1));
                remainder = remainder.substring(0, hashIndex);
            }

            Map<String, String> queryParams = new HashMap<>();
            int questionIndex = remainder.indexOf('?');
            if (questionIndex != -1) {
                queryParams = parseQuery(remainder.substring(questionIndex + 1));
                remainder = remainder.substring(0, questionIndex);
            }

            String auth = "";
            int atIndex = remainder.indexOf('@');
            if (atIndex != -1) {
                auth = urlDecode(remainder.substring(0, atIndex));
                remainder = remainder.substring(atIndex + 1);
            }

            int colonIndex = remainder.lastIndexOf(':');
            String host = remainder.substring(0, colonIndex);
            int port = Integer.parseInt(remainder.substring(colonIndex + 1));

            return new TunnelConfig.Builder(ConfigType.HYSTERIA2)
                    .rawConfig(raw)
                    .name(remark.isEmpty() ? "Hysteria2" : remark)
                    .password(auth)
                    .host(host)
                    .port(port)
                    .sni(queryParams.containsKey("sni") ? queryParams.get("sni") : host)
                    .extraParams(queryParams)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing Hysteria2 configuration: " + raw, e);
        }
    }

    /**
     * Parses TUIC protocol URI format.
     * <p>
     * پارس پروتکل TUIC
     */
    private static TunnelConfig parseTuic(String raw) {
        try {
            String remainder = raw.substring(7);
            String remark = "";
            int hash = remainder.indexOf('#');
            if (hash != -1) {
                remark = urlDecode(remainder.substring(hash + 1));
                remainder = remainder.substring(0, hash);
            }

            Map<String, String> queryParams = new HashMap<>();
            int q = remainder.indexOf('?');
            if (q != -1) {
                queryParams = parseQuery(remainder.substring(q + 1));
                remainder = remainder.substring(0, q);
            }

            int at = remainder.indexOf('@');
            String uuid = "";
            String pass = "";
            if (at != -1) {
                String auth = remainder.substring(0, at);
                remainder = remainder.substring(at + 1);
                if (auth.contains(":")) {
                    String[] parts = auth.split(":", 2);
                    uuid = parts[0];
                    pass = parts[1];
                } else {
                    uuid = auth;
                }
            }

            int colon = remainder.lastIndexOf(':');
            String host = remainder.substring(0, colon);
            int port = Integer.parseInt(remainder.substring(colon + 1));

            return new TunnelConfig.Builder(ConfigType.TUIC)
                    .rawConfig(raw)
                    .name(remark.isEmpty() ? "TUIC" : remark)
                    .uuid(uuid)
                    .password(pass)
                    .host(host)
                    .port(port)
                    .sni(queryParams.containsKey("sni") ? queryParams.get("sni") : host)
                    .extraParams(queryParams)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing TUIC configuration: " + raw, e);
        }
    }

    /**
     * Parses WireGuard URI format.
     * <p>
     * پارس پروتکل WireGuard
     */
    private static TunnelConfig parseWireguard(String raw) {
        try {
            String remainder = raw.substring(12);
            String remark = "";
            int hash = remainder.indexOf('#');
            if (hash != -1) {
                remark = urlDecode(remainder.substring(hash + 1));
                remainder = remainder.substring(0, hash);
            }

            Map<String, String> queryParams = new HashMap<>();
            int q = remainder.indexOf('?');
            if (q != -1) {
                queryParams = parseQuery(remainder.substring(q + 1));
                remainder = remainder.substring(0, q);
            }

            int colon = remainder.lastIndexOf(':');
            String host = remainder.substring(0, colon);
            int port = Integer.parseInt(remainder.substring(colon + 1));

            return new TunnelConfig.Builder(ConfigType.WIREGUARD)
                    .rawConfig(raw)
                    .name(remark.isEmpty() ? "WireGuard" : remark)
                    .host(host)
                    .port(port)
                    .publicKey(queryParams.get("publickey"))
                    .extraParams(queryParams)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing WireGuard configuration: " + raw, e);
        }
    }

    /**
     * Parses native WireGuard configuration file (.conf format with [Interface] and [Peer]).
     * <p>
     * پارس فایل ساختاریافته کانفیگ WireGuard
     */
    private static TunnelConfig parseWireguardConf(String conf) {
        try {
            String privateKey = "";
            String address = "";
            String publicKey = "";
            String endpoint = "";
            String allowedIps = "0.0.0.0/0, ::/0";

            String[] lines = conf.split("[\r\n]+");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;
                int eq = trimmed.indexOf('=');
                if (eq != -1) {
                    String key = trimmed.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                    String val = trimmed.substring(eq + 1).trim();
                    if (key.equals("privatekey")) privateKey = val;
                    else if (key.equals("address")) address = val;
                    else if (key.equals("publickey")) publicKey = val;
                    else if (key.equals("endpoint")) endpoint = val;
                    else if (key.equals("allowedips")) allowedIps = val;
                }
            }

            String host = "";
            int port = 51820;
            if (endpoint.contains(":")) {
                int c = endpoint.lastIndexOf(':');
                host = endpoint.substring(0, c);
                try {
                    port = Integer.parseInt(endpoint.substring(c + 1));
                } catch (Exception ignored) {
                }
            } else if (!endpoint.isEmpty()) {
                host = endpoint;
            }

            Map<String, String> extra = new HashMap<>();
            extra.put("privateKey", privateKey);
            extra.put("address", address);
            extra.put("allowedIPs", allowedIps);

            return new TunnelConfig.Builder(ConfigType.WIREGUARD)
                    .rawConfig(conf)
                    .name("WireGuard Conf")
                    .host(host)
                    .port(port)
                    .password(privateKey)
                    .publicKey(publicKey)
                    .extraParams(extra)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing WireGuard conf file: " + conf, e);
        }
    }

    /**
     * Parses single proxy YAML definition from Clash configuration files.
     * <p>
     * پارس کانفیگ پراکسی از ساختار YAML کلاش
     */
    private static TunnelConfig parseClashYaml(String yaml) {
        try {
            String name = "Clash Proxy";
            String type = "vmess";
            String server = "";
            int port = 443;
            String uuid = "";
            String cipher = "auto";
            String network = "tcp";
            String path = "/";
            String host = "";
            String sni = "";
            boolean tls = false;

            String[] lines = yaml.split("[\r\n]+");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("- name:") || trimmed.startsWith("name:")) {
                    name = extractYamlValue(trimmed);
                } else if (trimmed.startsWith("type:")) {
                    type = extractYamlValue(trimmed).toLowerCase(Locale.ROOT);
                } else if (trimmed.startsWith("server:")) {
                    server = extractYamlValue(trimmed);
                } else if (trimmed.startsWith("port:")) {
                    try {
                        port = Integer.parseInt(extractYamlValue(trimmed));
                    } catch (Exception ignored) {
                    }
                } else if (trimmed.startsWith("uuid:") || trimmed.startsWith("password:")) {
                    uuid = extractYamlValue(trimmed);
                } else if (trimmed.startsWith("cipher:")) {
                    cipher = extractYamlValue(trimmed);
                } else if (trimmed.startsWith("network:")) {
                    network = extractYamlValue(trimmed);
                } else if (trimmed.startsWith("path:")) {
                    path = extractYamlValue(trimmed);
                } else if (trimmed.startsWith("Host:") || trimmed.startsWith("host:")) {
                    host = extractYamlValue(trimmed);
                } else if (trimmed.startsWith("servername:") || trimmed.startsWith("sni:")) {
                    sni = extractYamlValue(trimmed);
                } else if (trimmed.startsWith("tls:")) {
                    tls = "true".equalsIgnoreCase(extractYamlValue(trimmed));
                }
            }

            ConfigType configType = ConfigType.V2RAY_VMESS;
            if ("vless".equals(type)) configType = ConfigType.V2RAY_VLESS;
            else if ("trojan".equals(type)) configType = ConfigType.V2RAY_TROJAN;
            else if ("ss".equals(type) || "shadowsocks".equals(type)) configType = ConfigType.SHADOWSOCKS;

            Map<String, String> extra = new HashMap<>();
            extra.put("scy", cipher);
            extra.put("method", cipher);
            if (!host.isEmpty()) extra.put("host", host);

            return new TunnelConfig.Builder(configType)
                    .rawConfig(yaml)
                    .name(name)
                    .host(server)
                    .port(port)
                    .uuid(uuid)
                    .password(uuid)
                    .network(network)
                    .path(path)
                    .sni(sni.isEmpty() ? (host.isEmpty() ? server : host) : sni)
                    .security(tls ? "tls" : "none")
                    .extraParams(extra)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing Clash YAML configuration: " + yaml, e);
        }
    }

    private static String extractYamlValue(String line) {
        int idx = line.indexOf(':');
        if (idx == -1) return "";
        String val = line.substring(idx + 1).trim();
        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
            val = val.substring(1, val.length() - 1);
        }
        return val.trim();
    }

    /**
     * Parses raw JSON outbound configurations (Xray, Sing-box, etc.).
     * <p>
     * پارس کانفیگ خام ساختاریافته JSON (مانند کانفیگ‌های کامل Xray یا Sing-box)
     */
    private static TunnelConfig parseRawJson(String json) {
        String name = "Raw JSON Config";
        String host = null;
        int port = 0;
        ConfigType type = ConfigType.RAW_JSON;

        Map<String, String> simpleMap = parseSimpleJson(json);

        // 1. Single node JSON (Sing-box, Clash, or Xray Outbound): {"type": "...", "server": "..."}
        // ۱. قالب تک نود JSON (Sing-box یا Clash یا Xray Outbound): {"type": "...", "server": "..."}
        if (simpleMap.containsKey("server") && (simpleMap.containsKey("type") || simpleMap.containsKey("port") || simpleMap.containsKey("server_port") || simpleMap.containsKey("uuid"))) {
            String sType = simpleMap.getOrDefault("type", "vmess").toLowerCase(Locale.ROOT);
            host = simpleMap.get("server");

            if (simpleMap.containsKey("server_port")) {
                try {
                    port = Integer.parseInt(simpleMap.get("server_port"));
                } catch (Exception ignored) {
                }
            } else if (simpleMap.containsKey("port")) {
                try {
                    port = Integer.parseInt(simpleMap.get("port"));
                } catch (Exception ignored) {
                }
            } else {
                port = 443;
            }

            if (simpleMap.containsKey("name")) {
                name = simpleMap.get("name");
            } else if (simpleMap.containsKey("tag")) {
                name = simpleMap.get("tag");
            } else {
                name = sType.toUpperCase(Locale.ROOT) + " Proxy";
            }

            if ("vless".equals(sType)) type = ConfigType.V2RAY_VLESS;
            else if ("vmess".equals(sType)) type = ConfigType.V2RAY_VMESS;
            else if ("trojan".equals(sType)) type = ConfigType.V2RAY_TROJAN;
            else if ("ss".equals(sType) || "shadowsocks".equals(sType)) type = ConfigType.SHADOWSOCKS;
            else if ("ssr".equals(sType)) type = ConfigType.SHADOWSOCKSR;
            else if ("hysteria2".equals(sType) || "hy2".equals(sType)) type = ConfigType.HYSTERIA2;
            else if ("tuic".equals(sType)) type = ConfigType.TUIC;
            else if ("wireguard".equals(sType)) type = ConfigType.WIREGUARD;

            return new TunnelConfig.Builder(type)
                    .rawConfig(json)
                    .rawJson(json)
                    .name(name)
                    .host(host)
                    .port(port)
                    .uuid(simpleMap.get("uuid"))
                    .password(simpleMap.get("password"))
                    .build();
        }

        // 2. Full Xray config or single outbound: extract address and port via Regex
        // ۲. کانفیگ کامل Xray یا اوتباند تکی Xray: استخراج address و port با Regex
        Matcher addrMatcher = Pattern.compile("\"address\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (addrMatcher.find()) {
            host = addrMatcher.group(1);
        }
        Matcher portMatcher = Pattern.compile("\"port\"\\s*:\\s*(\\d+)").matcher(json);
        if (portMatcher.find()) {
            try {
                port = Integer.parseInt(portMatcher.group(1));
            } catch (Exception ignored) {
            }
        }
        Matcher protoMatcher = Pattern.compile("\"protocol\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (protoMatcher.find()) {
            name = protoMatcher.group(1).toUpperCase(Locale.ROOT) + " JSON";
        }

        return new TunnelConfig.Builder(ConfigType.RAW_JSON)
                .rawConfig(json)
                .rawJson(json)
                .name(name)
                .host(host)
                .port(port)
                .build();
    }

    private static boolean isSubscriptionUrl(String url) {
        String l = url.toLowerCase(Locale.ROOT);
        return l.contains("/sub") || l.contains("subscription") || l.contains("token=")
                || l.contains("api/v1/client/subscribe");
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = urlDecode(pair.substring(0, idx));
                String value = urlDecode(pair.substring(idx + 1));
                map.put(key, value);
            } else if (!pair.isEmpty()) {
                map.put(urlDecode(pair), "");
            }
        }
        return map;
    }

    private static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    /**
     * Lightweight Base64 decoder compatible with standard and URL-safe formats across all Android/JVM versions.
     * <p>
     * دیکودر سبک Base64 سازگار با فرمت‌های استاندارد و URL-safe در تمام نسخه‌های اندروید و جاوا
     */
    public static String decodeBase64(String input) {
        String clean = input.replaceAll("\\s+", "");
        byte[] bytes = base64DecodeToBytes(clean);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] base64DecodeToBytes(String s) {
        // Handle URL-safe and standard base64
        String base64 = s.replace('-', '+').replace('_', '/');
        int pad = 4 - (base64.length() % 4);
        if (pad > 0 && pad < 4) {
            StringBuilder sb = new StringBuilder(base64);
            for (int i = 0; i < pad; i++) sb.append('=');
            base64 = sb.toString();
        }

        int len = base64.length();
        int padding = 0;
        if (len > 0 && base64.charAt(len - 1) == '=') padding++;
        if (len > 1 && base64.charAt(len - 2) == '=') padding++;

        int outLen = (len * 3) / 4 - padding;
        byte[] out = new byte[outLen];

        int[] table = getBase64Table();
        int bIdx = 0;
        for (int i = 0; i < len; i += 4) {
            int b0 = table[base64.charAt(i)];
            int b1 = table[base64.charAt(i + 1)];
            int b2 = (i + 2 < len && base64.charAt(i + 2) != '=') ? table[base64.charAt(i + 2)] : 0;
            int b3 = (i + 3 < len && base64.charAt(i + 3) != '=') ? table[base64.charAt(i + 3)] : 0;

            int triple = (b0 << 18) | (b1 << 12) | (b2 << 6) | b3;

            if (bIdx < outLen) out[bIdx++] = (byte) ((triple >> 16) & 0xFF);
            if (bIdx < outLen) out[bIdx++] = (byte) ((triple >> 8) & 0xFF);
            if (bIdx < outLen) out[bIdx++] = (byte) (triple & 0xFF);
        }
        return out;
    }

    private static int[] getBase64Table() {
        int[] table = new int[128];
        Arrays.fill(table, -1);
        for (int i = 0; i < 26; i++) table['A' + i] = i;
        for (int i = 0; i < 26; i++) table['a' + i] = 26 + i;
        for (int i = 0; i < 10; i++) table['0' + i] = 52 + i;
        table['+'] = 62;
        table['/'] = 63;
        return table;
    }

    /**
     * Lightweight zero-dependency JSON parser for flat key-value pairs.
     * <p>
     * پارسر JSON سبک بدون وابستگی خارجی برای کانفیگ‌های ساده VMess و Sing-box
     */
    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"([^\"]*)\"|([0-9.]+)|true|false)");
        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1);
            String val = matcher.group(3);
            if (val == null) {
                val = matcher.group(2);
            }
            map.put(key, val);
        }
        return map;
    }
}

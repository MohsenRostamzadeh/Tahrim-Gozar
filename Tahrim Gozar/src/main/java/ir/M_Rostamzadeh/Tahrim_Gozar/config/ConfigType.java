package ir.M_Rostamzadeh.Tahrim_Gozar.config;

/**
 * Supported configuration and tunnel types in Tahrim-Gozar.
 * <p>
 * انواع کانفیگ‌ها و پروتکل‌های قابل پشتیبانی در تحریم‌گذر
 */
public enum ConfigType {
    /**
     * Anti-sanction DNS presets (Shecan, Electro, 403, Radar, Begzar, Cloudflare, Google).
     * <p>
     * دی‌ان‌اس‌های ضدتحریم آماده مانند شکن، الکترو، ۴۰۳، رادار، بگذر، کلادفلر، گوگل
     */
    DNS_PRESET,

    /**
     * Custom DNS IP addresses (e.g. "178.22.122.100, 185.51.200.2" or "dns://1.1.1.1").
     * <p>
     * آی‌پی‌های سفارشی دی‌ان‌اس (مانند "178.22.122.100, 185.51.200.2" یا "dns://1.1.1.1")
     */
    DNS_CUSTOM,

    /**
     * DNS over HTTPS protocol (e.g. "https://cloudflare-dns.com/dns-query" or "doh://...").
     * <p>
     * پروتکل DNS over HTTPS (مانند "https://cloudflare-dns.com/dns-query" یا "doh://...")
     */
    DNS_OVER_HTTPS,

    /**
     * DNS over TLS protocol (e.g. "tls://1.1.1.1:853" or "dot://...").
     * <p>
     * پروتکل DNS over TLS (مانند "tls://1.1.1.1:853")
     */
    DNS_OVER_TLS,

    /**
     * Standard HTTP proxy.
     * <p>
     * پروکسی استاندارد HTTP
     */
    PROXY_HTTP,

    /**
     * Standard HTTPS proxy.
     * <p>
     * پروکسی استاندارد HTTPS
     */
    PROXY_HTTPS,

    /**
     * Standard SOCKS4 proxy.
     * <p>
     * پروکسی استاندارد SOCKS4
     */
    PROXY_SOCKS4,

    /**
     * Standard SOCKS5 proxy with optional authentication.
     * <p>
     * پروکسی استاندارد SOCKS5 با یا بدون احراز هویت
     */
    PROXY_SOCKS5,

    /**
     * V2Ray VMess protocol.
     * <p>
     * پروتکل V2Ray VMess
     */
    V2RAY_VMESS,

    /**
     * V2Ray/Xray VLESS protocol (supports Reality, TLS, WebSocket, gRPC).
     * <p>
     * پروتکل V2Ray/Xray VLESS (شامل Reality, TLS, WebSocket, gRPC)
     */
    V2RAY_VLESS,

    /**
     * Trojan protocol.
     * <p>
     * پروتکل Trojan
     */
    V2RAY_TROJAN,

    /**
     * Shadowsocks protocol (SIP002 and legacy).
     * <p>
     * پروتکل Shadowsocks
     */
    SHADOWSOCKS,

    /**
     * ShadowsocksR (SSR) protocol.
     * <p>
     * پروتکل ShadowsocksR (SSR)
     */
    SHADOWSOCKSR,

    /**
     * Hysteria2 protocol based on UDP/QUIC.
     * <p>
     * پروتکل Hysteria2 مبتنی بر QUIC
     */
    HYSTERIA2,

    /**
     * TUIC protocol.
     * <p>
     * پروتکل TUIC
     */
    TUIC,

    /**
     * WireGuard protocol / Cloudflare Warp.
     * <p>
     * پروتکل WireGuard / Warp
     */
    WIREGUARD,

    /**
     * Raw structured JSON configuration (Xray core, Sing-box).
     * <p>
     * کانفیگ خام ساختاریافته JSON (مانند Xray / Sing-box JSON)
     */
    RAW_JSON,

    /**
     * Subscription URL containing multiple configuration entries.
     * <p>
     * لینک سابسکرپشن (حاوی چندین کانفیگ)
     */
    SUBSCRIPTION
}

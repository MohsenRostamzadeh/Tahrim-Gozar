# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-09-12

### Added
- **In-App / Per-App Tunneling Engine**: Complete process-level tunneling without `VpnService`, affecting only the host app and never interfering with banking apps, food delivery, or other device apps.
- **Universal Configuration Parser (`UniversalConfigParser`)**: Automatically parses DNS presets, direct IPs, DoH, DoT, HTTP, HTTPS, SOCKS4, SOCKS5, VLESS (Reality/TLS), VMess, Trojan, Shadowsocks, Hysteria2, TUIC, WireGuard, and raw JSON configurations.
- **Anti-Sanction DNS Engine (`TahrimDns`, `AntiSanctionDns`)**: Built-in support for Shecan, Electro, 403.online, Radar Game, Begzar, Cloudflare, and Google DNS, with high-performance UDP query, DoH, in-memory caching, and failover.
- **Local DNS Forwarder (`LocalDnsForwarder`)**: Ultra-lightweight loopback HTTP forwarder that enables Android WebView anti-sanction bypassing using pure DNS without requiring an external proxy.
- **In-App Traffic Router (`InAppTrafficRouter`)**: Coordinates JVM `ProxySelector`, `System.setProperty`, `ProxyController`, and `Authenticator` for transparent app-wide routing.
- **Xray / V2Ray Bridge (`XrayBridgeEngine`)**: Pluggable bridge with automated client JSON generation and dynamic native core detection (compatible with `AndroidLibXrayLite` and custom runners).
- **Multi-Config Subscription Manager (`SubscriptionManager`)**: Downloads subscription URLs, parses multi-line or Base64 configs, executes concurrent TCP ping tests, and selects the lowest-latency config with developer toggle.
- **Fluent Builder API**: `TahrimGozar.with(context).config("shecan").enableWebView(true).start(callback)`.
- **OkHttp Integration**: `TahrimGozar.applyTo(okHttpClientBuilder)` and `TahrimGozar.getDns()`.
- **New Unit Tests**: 35+ new tests covering `UniversalConfigParser`, `AntiSanctionDns`, `TunnelConfig`, `XrayBridgeEngine`, and `SubscriptionManager`.
- **ProxyType support**: HTTP, HTTPS, SOCKS4, and SOCKS5 proxy types via `ProxyInfo.ProxyType` enum.
- **Authenticated proxies**: `ProxyInfo` now supports `username` and `password` for proxy authentication.
- **ProxyProvider interface**: Dynamic proxy list support — implement `ProxyProvider` to supply custom proxy lists (e.g., from a remote API).
- **DefaultProxyProvider**: Built-in implementation with add/remove/clear proxy management.
- **TahrimGozarListener**: Callback interface for proxy events (`onProxyConnected`, `onProxyFailed`, `onProxyChanged`, `onProxyRemoved`).
- **ProxyHealthChecker**: Automatic proxy health monitoring with configurable intervals and auto-failover.
- **getCurrentProxy()**: Method to get the currently active proxy.
- **init() with ProxyProvider**: New `init()` overloads accepting custom `ProxyProvider`.
- **Unit tests**: Comprehensive tests for `ProxyInfo`, `DefaultProxyProvider`, `Constants`, and `TahrimGozar`.
- **CI/CD**: GitHub Actions workflow for automated build, test, and lint.
- **CHANGELOG.md**: This file.

### Changed
- **ProxyController**: Replaced fragile reflection-based proxy setting with official `androidx.webkit.ProxyController` API.
- **Thread Safety**: All singletons (`TahrimGozar`, `Utils`, `GozarProxySetter`) now use double-checked locking.
- **Memory Leak Fix**: `TahrimGozar.init()` now always stores `context.getApplicationContext()`.
- **Error Handling**: Improved error handling with listener callbacks instead of silent log-only errors.
- **getLinkResponse()**: Fixed unsafe instance-variable-based retry with parameter-based retry and added connection timeouts.
- **HTTP URL handling**: Fixed `ClassCastException` when connecting to HTTP (non-HTTPS) URLs.
- **`PROXY_NUMBER`**: Fixed from 2 to 1 to match the actual number of defined proxies.
- **`NO_PROXY_NUMBER`**: Fixed from 2 to 0 to correctly represent "no proxy".
- **GozarProxySetter**: Uses `ProxyProvider` instead of hardcoded proxy constants; uses explicit `Looper.getMainLooper()` for Handler.

### Fixed
- **`postUrl` bug**: `GozarWebView.postUrl()` was silently discarding `postData` by calling `loadUrl()` instead of `postUrl()`.

### Removed
- **`easyproxy` dependency**: Removed stale third-party library (`com.github.vijayrawatsan:easyproxy:1.0`).
- **Reflection-based proxy methods**: Removed `setProxyICS()`, `setProxyJB()`, and reflection code in `setProxyKKPlus()` (dead code since `minSdk=23`).
- **`getFieldValueSafely()`**: Removed — no longer needed without reflection.
- **`getConsValue()`**: Removed — replaced by `ProxyProvider`.

## [1.1] - Initial Release

### Added
- Basic proxy setup for Android WebView and HTTP connections.
- Support for API level 14+.
- Default anti-sanctions proxy.
- `GozarWebView` and `GozarClient` for easy WebView proxy integration.

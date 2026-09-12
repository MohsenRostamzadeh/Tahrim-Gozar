# Tahrim-Gozar (تحریم‌گذر)

[![JitPack](https://jitpack.io/v/MohsenRostamzadeh/Tahrim-Gozar.svg)](https://jitpack.io/#MohsenRostamzadeh/Tahrim-Gozar)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Java-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=23)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-blue.svg)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> [!NOTE]
> **Language / زبان:**
> 🇬🇧 **English Documentation** | [🇮🇷 راهنمای فارسی (Persian)](README_FA.md)

A lightweight, enterprise-grade Android & Java library for overcoming software sanctions, geographic restrictions, and censorship using **In-App (Per-App) Tunneling** — completely eliminating the need for system `VpnService` permissions, avoiding banking app disruptions, and keeping the rest of the device untouched.

---

## 🌟 Why In-App Tunneling?

Traditional whole-device VPN applications capture all device traffic via Android's `VpnService`. While useful for general browsing, this causes severe issues inside Android apps:
- ❌ **Disrupts Banking & Local Services:** Banking apps, ride-hailing (Snapp/Tap30), domestic streaming, and government portals immediately block users or fail when an active device VPN is detected.
- ❌ **Annoying OS Dialogs:** Demands runtime system permissions and displays intrusive OS warning dialogs on connection.
- ❌ **Single-VPN Limitation:** Android allows only **one** active VPN service at a time. If your app launches a device VPN, it terminates the user's personal VPN.

### The Tahrim-Gozar Solution:
- ✅ **100% In-App Routing:** Tunnels **only your application's network traffic** (WebViews, OkHttp, Retrofit, Volley, and raw Java sockets).
- ✅ **Zero System Permissions:** Operates purely through local loopback forwarders and application-level proxies — requiring **zero OS VPN permissions** or warning dialogs.
- ✅ **Concurrent & Non-Intrusive:** Coexists seamlessly with any personal VPN the user might already be running.
- ✅ **Banking-Friendly:** The user's other apps and banking services connect through their normal local connection without interference.

---

## 📱 Interactive Showcase App: تحریم‌گذر (نسخه نمایشی)

The repository includes a complete, modern showcase demo application in the `:app` module demonstrating **all features 0 to 100** — featuring live speed gauges, real-time traffic statistics, DNS racing benchmarks, dynamic domestic Split Tunneling, and an in-app WebView browser. You can install the pre-compiled APK from:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🚀 Universal Configuration Support

Tahrim-Gozar includes an intelligent **Universal Config Parser** that automatically detects and parses any configuration string:

| Protocol / Type | Supported Formats | Engine |
| :--- | :--- | :--- |
| **Anti-Sanction DNS Presets** | `"shecan"`, `"electro"`, `"403"`, `"radar"`, `"begzar"`, `"cloudflare"`, `"google"` | Pure Java (`TahrimDns` + `LocalDnsForwarder`) |
| **Direct DNS IPs & URIs** | `"178.22.122.100, 185.51.200.2"`, `"dns://1.1.1.1:53"` | Pure Java UDP Resolver |
| **Encrypted DNS (DoH / DoT)** | `"https://cloudflare-dns.com/dns-query"`, `"tls://1.1.1.1:853"`, `"doh://..."` | Pure Java DoH / TLS Engine |
| **Standard Proxies** | `http://`, `https://`, `socks4://`, `socks5://` (with auth) | Pure Java Proxy Router |
| **V2Ray / Xray Protocols** | `vless://` (Reality, TLS, WS, gRPC), `vmess://` (Base64 JSON & URI), `trojan://` | `XrayBridgeEngine` |
| **Modern QUIC Protocols** | `hysteria2://`, `hy2://`, `tuic://`, `wireguard://` | `XrayBridgeEngine` |
| **Multi-Node Configs** | Clash YAML proxies, Sing-box / Xray Outbound JSON, Base64 bundles | Universal Parser |
| **Remote Subscriptions** | Standard subscription URLs (`https://.../sub...`) | `SubscriptionManager` with Auto-Ping |

---

## ⚡ Advanced Roadmap Capabilities

### 1. Real-Time Traffic & Speed Metering
- Lock-free atomic tracking (`AtomicLong`) for Tx and Rx bytes.
- Autonomous 1-second ticker calculating live download and upload speeds (`KB/s` and `MB/s`).
- Main-thread-safe listener callbacks (`TrafficStatsListener`).

### 2. Split Tunneling & Configurable Domestic TLDs
- **Global Compatibility:** Fully customizable country TLDs (e.g. `.ir`, `.ru`, `.cn`, `.tr`) with `.ir` enabled by default.
- Automatically routes domestic traffic directly without proxying to conserve bandwidth and prevent IP blocks on banking portals.
- Add custom inclusion (`addProxyDomain`) and exclusion (`addBypassDomain`) rules.

### 3. Happy Eyeballs Concurrent DNS Race Multi-Resolver
- Resolves hostnames concurrently across all configured DNS servers.
- First valid response under 30ms wins (`queryUdpRace`), drastically slashing page load TTFB.

### 4. Drop-in Status Dialog (`TahrimGozarUI`)
- Pure Java Android dialog requiring **zero XML layouts**: `TahrimGozarUI.showStatusDialog(activity);`
- Displays animated status, active node, ping, speeds, total data transferred, and a reconnect button.

### 5. Sticky Connections & 20s Periodic Health Monitor
- Automatically picks the lowest-latency node on subscription load.
- Probes connectivity every 20 seconds using a lightweight HTTP 204 request.
- Triggers failover only after 3 consecutive failures, verifying candidate health before switching.

---

## 📦 Installation via JitPack

### Step 1: Add JitPack repository
In your root `settings.gradle` or `build.gradle`:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
    }
}
```

### Step 2: Add Tahrim-Gozar dependency
In your app's `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.MohsenRostamzadeh:Tahrim-Gozar:2.0'
    
    // Optional: for advanced WebView proxy control
    implementation 'androidx.webkit:webkit:1.13.0'
}
```

---

## 💻 Quickstart & Examples

### 1. Fluent Setup with `TunnelBuilder`

```java
TahrimGozar.with(context)
    .config("shecan")                  // Preset, VLESS URI, or subscription URL
    .enableWebView(true)               // Route app WebViews
    .splitTunneling(true)              // Enable Split Tunneling
    .bypassDomesticTraffic(true)       // Bypass national TLDs (default: .ir)
    .addDomesticTld(".ir")             // Add any country TLD
    .addBypassDomain("*.snapp.ir")     // Custom direct bypass domain
    .dnsRace(true)                    // Sub-30ms Happy Eyeballs DNS
    .trafficStats(true, (rxSpeed, txSpeed, totalRx, totalTx) -> {
        Log.d("Speed", TrafficStatsTracker.formatSpeed(rxSpeed));
    })
    .start(new TunnelCallback() {
        @Override
        public void onConnected(TunnelSession session) {
            TahrimGozarUI.showStatusDialog(MainActivity.this);
        }

        @Override
        public void onError(Throwable error) {
            Log.e("TahrimGozar", "Connection failed", error);
        }

        @Override
        public void onDisconnected() {}
    });
```

### 2. OkHttp & Retrofit Integration

```java
OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

// Automatically configures proxy and TahrimDns onto OkHttp
TahrimGozar.getInstance().applyTo(clientBuilder);

OkHttpClient client = clientBuilder.build();
```

### 3. Dedicated Anti-Sanction DNS Resolver

```java
TahrimDns dns = TahrimDns.shecan();
dns.setRaceEnabled(true);

List<InetAddress> addresses = dns.lookup("developer.android.com");
```

### 4. Stop Tunnel & Cleanup

```java
TahrimGozar.getInstance().stopTunnel();
```

---

## ⚙️ Fluent Builder API Reference (`TunnelBuilder`)

| Method | Description |
| :--- | :--- |
| `with(Context context)` | Initializes builder with Application Context |
| `config(String rawConfig)` | Sets DNS preset, proxy URI, V2Ray link, JSON, or subscription URL |
| `enableWebView(boolean enable)` | Directs in-app WebView traffic through the tunnel |
| `splitTunneling(boolean enable)` | Enables or disables split tunneling rules |
| `bypassDomesticTraffic(boolean bypass)` | Direct connection for configured national TLDs (default: `.ir`) |
| `addDomesticTld(String tld)` | Adds a country TLD for direct connection (`.ir`, `.ru`, `.cn`, `.tr`) |
| `addBypassDomain(String domain)` | Adds specific domain to bypass proxy |
| `addProxyDomain(String domain)` | Forces specific domain through the tunnel |
| `dnsRace(boolean enable)` | Enables concurrent Happy Eyeballs sub-30ms DNS racing |
| `trafficStats(boolean enable, TrafficStatsListener l)` | Enables real-time speed calculation and bandwidth tracker |
| `autoSelectBestConfig(boolean auto)` | Pings subscription nodes concurrently and connects to fastest |
| `enableAutoFailover(boolean enable)` | Enables smart auto-failover on connection drops |
| `healthCheckInterval(long interval, TimeUnit unit)` | Sets periodic health check interval (default: 20 seconds) |
| `start(TunnelCallback callback)` | Asynchronously initializes and launches the tunnel |
| `stopTunnel()` | Stops active tunnel, terminates monitors, and restores networking |

---

## 🛡️ ProGuard / R8 Rules

If using `minifyEnabled true`, add the following line to your `proguard-rules.pro`:

```proguard
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.** { *; }
```

---

## 📄 License

This library is licensed under the **Apache-2.0 License**. See [LICENSE](LICENSE) for details.

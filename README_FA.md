# تحریم‌گذر (Tahrim-Gozar)

[![JitPack](https://jitpack.io/v/MohsenRostamzadeh/Tahrim-Gozar.svg)](https://jitpack.io/#MohsenRostamzadeh/Tahrim-Gozar)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Java-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=23)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-blue.svg)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> [!NOTE]
> **Language / زبان:**
> 🇮🇷 **راهنمای فارسی** | [🇬🇧 English Documentation](README.md)

کتابخانه جامع، سبک و قدرتمند اندروید و جاوا برای عبور از تحریم‌های نرم‌افزاری و فیلترینگ اینترنت به صورت **تونلینگ درون‌برنامه‌ای (In-App / Per-App Tunneling)** — بدون نیاز به مجوز `VpnService`، بدون قطع شدن اپلیکیشن‌های بانکی و تاکسی اینترنتی، و بدون دستکاری ترافیک سایر بخش‌های دستگاه کاربر.

---

## 🌟 چرا تونلینگ درون‌برنامه‌ای (In-App Tunneling)؟

در فیلترشکن‌ها و روش‌های متداول که از `VpnService` سیستم‌عامل اندروید استفاده می‌کنند:
- ❌ **اختلال در برنامه‌های بانکی و داخلی:** ترافیک کل گوشی تغییر می‌کند؛ برنامه‌های بانکی، همراه‌بانک‌ها، اسنپ، تپسی، دیجی‌کالا و وب‌سایت‌های دولتی بلافاصله آی‌پی را مسدود کرده یا از کار می‌افتند.
- ❌ **دیالوگ هشدار سیستمی:** اندروید به کاربر پیام هشدار امنیتی نمایش می‌دهد و نیاز به تایید دستی دارد.
- ❌ **محدودیت تک‌اتصاله (Single VPN):** در اندروید فقط یک سرویس VPN می‌تواند همزمان فعال باشد. اگر برنامه شما وی‌پی‌ان دستگاه را فعال کند، فیلترشکن شخصی خود کاربر قطع می‌شود.

### مزایای راهکار تحریم‌گذر:
- ✅ **۱۰۰٪ ترافیک درون‌برنامه‌ای:** فقط و فقط ترافیک شبکه همین اپلیکیشن (مانند WebViews، کلاینت‌های OkHttp، کتابخانه Retrofit، Volley و سوکت‌های خام جاوا) هدایت و تونل می‌شود.
- ✅ **بدون نیاز به هیچ مجوزی (Zero Permissions):** با استفاده از فورواردر لوکال لوپ‌بک و پروکسی درون‌برنامه‌ای، هیچ مجوز سیستمی یا دیالوگ هشداری از کاربر درخواست نمی‌شود.
- ✅ **همزیستی بدون تداخل با سایر برنامه‌ها:** در حالی که کاربر فیلترشکن شخصی خود را روشن دارد، تحریم‌گذر بدون کوچک‌ترین تداخلی به صورت موازی کار می‌کند.
- ✅ **سازگار کامل با بانک‌ها:** سایر برنامه‌های کاربر و خدمات بانکی با اینترنت عادی و بدون قطعی به کار خود ادامه می‌دهند.

---

## 📱 اپلیکیشن ویترین و دمو: تحریم‌گذر (نسخه نمایشی)

یک اپلیکیشن کامل و مدرن به عنوان ویترین درون سورس پروژه در ماژول `:app` قرار دارد که ۰ تا ۱۰۰ تمامی قابلیت‌های این کتابخانه را به صورت زنده، همراه با سرعت‌سنج گرافیکی، بنچمارک مقایسه‌ای DNS، تفکیک ترافیک و مرورگر درون‌برنامه‌ای نمایش می‌دهد. می‌توانید فایل APK کامپایل‌شده آن را از مسیر زیر دریافت و روی هر دستگاه اندرویدی نصب نمایید:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🚀 پشتیبانی جامع از انواع کانفیگ‌ها (Universal Config Parser)

کافی است هر رشته یا لینک کانفیگی را مستقیماً به تحریم‌گذر بدهید؛ سیستم هوشمند به صورت خودکار پروتکل آن را شناسایی و پارس می‌کند:

| نوع پروتکل / کانفیگ | فرمت‌های ورودی پشتیبانی‌شده | موتور پردازشگر |
| :--- | :--- | :--- |
| **پری‌ست‌های DNS ضدتحریم** | `"shecan"` (شکن)، `"electro"` (الکترو)، `"403"`، `"radar"` (رادار بازی)، `"begzar"` (بگذر)، `"cloudflare"`، `"google"` | جاوا خالص (`TahrimDns` + `LocalDnsForwarder`) |
| **آی‌پی‌های مستقیم و URI دی‌ان‌اس** | `"178.22.122.100, 185.51.200.2"`, `"dns://1.1.1.1:53"` | رزولور UDP جاوا خالص |
| **دی‌ان‌اس رمزنگاری‌شده (DoH / DoT)** | `"https://cloudflare-dns.com/dns-query"`, `"tls://1.1.1.1:853"`, `"doh://..."` | موتور DoH و TLS جاوا خالص |
| **پروکسی‌های استاندارد** | `http://`, `https://`, `socks4://`, `socks5://` (با یا بدون احراز هویت) | روتر پروکسی استاندارد جاوا |
| **پروتکل‌های V2Ray و Xray** | `vless://` (شامل Reality, TLS, WS, gRPC), `vmess://` (فرمت‌های Base64 JSON و لینک مستقیم), `trojan://` | `XrayBridgeEngine` |
| **پروتکل‌های مدرن مبتنی بر UDP/QUIC** | `hysteria2://`, `hy2://`, `tuic://`, `wireguard://` | `XrayBridgeEngine` |
| **کانفیگ‌های چندگانه و فایل‌ها** | پروکسی‌های Clash YAML، کانفیگ‌های خام JSON سینگ‌باکس و Xray، کانفیگ WireGuard Conf | پارسر یونیورسال تحریم‌گذر |
| **سابسکرپشن‌های اینترنتی** | لینک‌های سابسکرپشن متداول (`https://.../sub...`) با محتوای Base64 یا متنی | ماژول `SubscriptionManager` همراه با تست پینگ خودکار |

---

## ⚡ امکانات پیشرفته نسخه جدید

### ۱. پایش زنده ترافیک و سرعت لحظه‌ای (Traffic & Speed Metering)
- ثبت بایت‌های ارسالی و دریافتی با شمارنده‌های اتمیک بدون قفل (`AtomicLong`).
- تایمر پس‌زمینه ۱ ثانیه‌ای برای محاسبه زنده سرعت دانلود/آپلود (`KB/s` و `MB/s`).
- ارسال خودکار کال‌بک‌ها به ترد اصلی UI بدون ایجاد فریز در برنامه.

### ۲. تفکیک هوشمند ترافیک بین‌المللی (Split Tunneling & Domestic TLDs)
- **پشتیبانی بین‌المللی:** عدم محدودیت به یک کشور خاص؛ پشتیبانی از هر پسوند دامنه‌ای (`.ir`, `.ru`, `.cn`, `.tr` و...) با مقدار پیش‌فرض `.ir`.
- عبور مستقیم سایت‌های داخلی بدون پروکسی، جهت جلوگیری از مسدود شدن IP در درگاه‌های بانکی و سامانه‌های ملی.
- امکان افزودن دامنه‌های دلخواه جهت عبور مستقیم (`addBypassDomain`) یا اجبار به عبور از تونل (`addProxyDomain`).

### ۳. استعلام همزمان و مسابقه‌ای دی‌ان‌اس (Happy Eyeballs DNS Racing)
- ارسال همزمان بسته‌های DNS به چندین سرور ضدتحریم و انتخاب نخستین پاسخ معتبر زیر ۳۰ میلی‌ثانیه.
- کاهش چشمگیر زمان تاخیر اولیه باز شدن صفحات وب به زیر ۳۰ms.

### ۴. دیالوگ گرافیکی آماده وضعیت اتصال (`TahrimGozarUI`)
- پیاده‌سازی ۱۰۰٪ جاوا با یک خط کد: `TahrimGozarUI.showStatusDialog(activity);`
- نمایش وضعیت زنده، نام نود، پینگ لحظه‌ای، سرعت دانلود/آپلود، حجم مصرفی و دکمه اتصال مجدد.

### ۵. پایش سلامت ۲۰ ثانیه‌ای و پایداری (Sticky Auto-Failover)
- اتصال پایدار به سریع‌ترین سرور بر اساس کمترین پینگ اولیه.
- پایش دوره‌ای هر ۲۰ ثانیه با درخواست فوق‌سبک به `generate_204`.
- سوییچ هوشمند تنها پس از ۳ خطای متوالی و تایید سلامت سرور جایگزین قبل از سوییچ.

---

## 📦 نحوه افزودن به پروژه (Installation)

### گام اول: افزودن مخزن JitPack
در فایل `settings.gradle` یا `build.gradle` ریشه پروژه:

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

### گام دوم: افزودن وابستگی کتابخانه
در فایل `build.gradle` ماژول اپلیکیشن:

```groovy
dependencies {
    implementation 'com.github.MohsenRostamzadeh:Tahrim-Gozar:2.0'
    
    // اختیاری: در صورت تمایل به کنترل پیشرفته پراکسی وب‌ویو
    implementation 'androidx.webkit:webkit:1.13.0'
}
```

---

## 💻 نمونه‌کدهای کاربردی (Code Examples)

### ۱. راه‌اندازی سریع با سازنده فلوئنت (`TunnelBuilder`)

```java
TahrimGozar.with(context)
    .config("shecan")                 // یا هر لینک vless:// یا سابسکرپشن
    .enableWebView(true)              // هدایت وب‌ویوهای برنامه به تونل
    .splitTunneling(true)             // فعال‌سازی تفکیک ترافیک
    .bypassDomesticTraffic(true)      // عبور مستقیم دامنه‌های ملی (پیش‌فرض: .ir)
    .addDomesticTld(".ir")            // افزودن پسوند کشور دلخواه
    .addBypassDomain("*.snapp.ir")    // عبور مستقیم اسنپ
    .dnsRace(true)                   // فعال‌سازی مسابقه همزمان DNS زیر ۳۰ms
    .trafficStats(true, (rxSpeed, txSpeed, totalRx, totalTx) -> {
        // دریافت آمار زنده سرعت و مصرف
        Log.d("Speed", TrafficStatsTracker.formatSpeed(rxSpeed));
    })
    .start(new TunnelCallback() {
        @Override
        public void onConnected(TunnelSession session) {
            // نمایش دیالوگ وضعیت آماده
            TahrimGozarUI.showStatusDialog(MainActivity.this);
        }

        @Override
        public void onError(Throwable error) {
            Log.e("TahrimGozar", "خطا: " + error.getMessage());
        }

        @Override
        public void onDisconnected() {}
    });
```

### ۲. اتصال کلاینت‌های شبکه (OkHttp و Retrofit) بدون وب‌ویو

```java
OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

// اعمال خودکار پراکسی تحریم‌گذر و رزولور TahrimDns روی کلاینت شما
TahrimGozar.getInstance().applyTo(clientBuilder);

OkHttpClient client = clientBuilder.build();
```

### ۳. استفاده از رزولور اختصاصی DNS

```java
TahrimDns dns = TahrimDns.shecan();
dns.setRaceEnabled(true); // فعال‌سازی مسابقه همزمان سرورها

List<InetAddress> ips = dns.lookup("developer.android.com");
```

### ۴. قطع کامل تونل و بازگردانی تنظیمات

```java
TahrimGozar.getInstance().stopTunnel();
```

---

## ⚙️ راهنمای متدهای سازنده (`TunnelBuilder`)

| متد | توضیح |
| :--- | :--- |
| `with(Context context)` | مقداردهی اولیه بیلدر با کانتکست اپلیکیشن |
| `config(String rawConfig)` | ارسال نام پری‌ست، لینک پروکسی، کانفیگ V2Ray، فایل JSON یا لینک سابسکرپشن |
| `enableWebView(boolean enable)` | فعال‌سازی هدایت خودکار تمام ترافیک WebView با ProxyController |
| `splitTunneling(boolean enable)` | فعال‌سازی تفکیک هوشمند ترافیک داخلی و خارجی |
| `bypassDomesticTraffic(boolean bypass)` | عبور مستقیم خودکار دامنه‌های پسوند ملی (پیش‌فرض: `.ir`) |
| `addDomesticTld(String tld)` | افزودن پسوند دامنه ملی برای هر کشور دلخواه (`.ir`, `.ru`, `.cn`, `.tr`) |
| `addBypassDomain(String domain)` | افزودن دامنه اختصاصی برای عبور مستقیم بدون پروکسی |
| `addProxyDomain(String domain)` | اجبار دامنه خاص به عبور از تونل تحریم‌گذر |
| `dnsRace(boolean enable)` | فعال‌سازی الگوریتم مسابقه همزمان سرورهای DNS جهت حداقل‌سازی تاخیر |
| `trafficStats(boolean enable, TrafficStatsListener l)` | فعال‌سازی سیستم پایش زنده مصرف و سرعت دانلود/آپلود |
| `autoSelectBestConfig(boolean auto)` | پینگ همزمان نودهای سابسکرپشن و اتصال به سریع‌ترین نود |
| `enableAutoFailover(boolean enable)` | فعال‌سازی سوئیچ خودکار هوشمند به نود سالم بعدی در صورت افت اتصال |
| `healthCheckInterval(long interval, TimeUnit unit)` | تنظیم بازه زمانی مانیتور سلامت دوره‌ای (پیش‌فرض: ۲۰ ثانیه) |
| `start(TunnelCallback callback)` | شروع غیرهمگام (Async) پارس کانفیگ، رزولوشن و برقراری ارتباط تونل |
| `stopTunnel()` | قطع تونل، توقف مانیتور سلامت، بستن پورت‌های لوکال و بازگردانی تنظیمات شبکه |

---

## 🛡️ قوانین ProGuard / R8

در صورتی که `minifyEnabled true` را در پروژه فعال کرده‌اید، دستورات زیر را در فایل `proguard-rules.pro` قرار دهید:

```proguard
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.** { *; }
```

---

## 📄 لایسنس (License)

این کتابخانه تحت مجوز **Apache-2.0** منتشر شده است. برای اطلاعات بیشتر فایل [LICENSE](LICENSE) را مشاهده فرمایید.

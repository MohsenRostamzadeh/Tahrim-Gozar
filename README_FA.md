# تحریم گذر
کتابخانه ساده برای استفاده آسان پروکسی در اندروید و غلبه بر تحریم های نرم افزاری و اینترنتی

# امکانات
- تنظیم پروکسی منفرد برای بازدیدهای وب.
- پشتیبانی از سطح 14+ API
- امکان استفاده از پروکسی پیش فرض ضد تحریم برای آدرس های تحریم شده.
- امکان انتخاب حالت پروکسی (وب ویو ، پس زمینه ، کل برنامه).
- راه اندازی پروکسی منفرد برای همه آدرس ها.
- راه اندازی پروکسی منفرد برای همه آدرس های اینترنتی به جز برخی از قوانین محرومیت.
- راه اندازی پروکسی منفرد برای همه آدرس ها به استثنای برخی از قوانین محرومیت و برخی از قوانین پروکسی جداگانه برای برخی از الگوهای URL.
- آگنیستیک کتابخانه. می توانید از هر کتابخانه ای که می خواهید Volley ، OkHttp ، Ion یا هر کتابخانه http دیگری استفاده کنید.

# کتابخانه راه اندازی
مرحله 1: مخزن JitPack را به پرونده gradle build خود اضافه کنید
```gradle
repositories {
    maven {
        url "https://jitpack.io"
    }
}
مرحله 2: افزودن وابستگی
```gradle
dependencies {
  
}

```

# ProGuard rules
اگر از پروژه محافظ حرفه ای خود در پروژه استفاده می کنید ، قوانین محافظ حرفه ای را اضافه کنید:
```pro guard

-keep class ir.M_Rostamzadeh.Tahrim_Gozar.TahrimGozar
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.GozarWebView
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.GozarClient
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.Utils
-keep class com.vijayrawatsan.easyproxy.ProxyInfo

#for webView
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}

```
# مثال استفاده
مرحله 1: ابتدا باید کتابخانه را با ** Application context ** راه اندازی کنید.
```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TahrimGozar tahrimGozar=TahrimGozar.getInstance();
        //Init Tahrim Gozar
        //you must pass application context
        tahrimGozar.init(getApplicationContext());
```

مرحله 2: پروکسی راه اندازی

- پراکسی پیش فرض: اگر می خواهید از پروکسی پیش فرض استفاده کنید ، کد زیر را اضافه کنید:
```java
// If you want use in web view , pass true ,otherwise pass false
tahrimGozar.setupAutomaticProxy(true);
```

- پروکسی سفارشی: اگر می خواهید از پروکسی سفارشی استفاده کنید ، کد زیر را اضافه کنید:
```java
TahrimGozar.getInstance().setCustomProxy(new ProxyInfo(PROXY_HOST,PROXY_PORT));
```

- استفاده در webView: <br/>
XML عنصر:
```xml
<ir.M_Rostamzadeh.Tahrim_Gozar.GozarWebView
            android:id="@+id/webView"
            android:layout_gravity="center"
            android:layout_width="match_parent"
            android:layout_height="match_parent"/>
```
کد جاوا:
```java
 GozarWebView gozarWebView =findViewById(R.id.webView);
 //If you want to override all webView urls and get over Internet sanctions completely , pass true , other wise pass false
 //It is better to pass true in web applications
 GozarClient gozarClient=new GozarClient(false);
 gozarWebView.setWebViewClient(gozarClient);
```

# مشارکت

کمک های مالی شما می تواند امیدوار کننده باشد. <br/>
خوشحال میشوم اگر از توسعه این کتابخانه پشتیبانی کنید. <br/>
https://www.payping.ir/@rostamzadeh
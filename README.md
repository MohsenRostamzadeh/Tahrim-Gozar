# Tahrim-Gozar
Simple library for easy use proxy in android and Overcoming software sanctions

# Features
- Setup single proxy for web views.
- Support api level 14+
- Ability to use default anti-sanctions proxy for sanctioned urls.
- Ability to select proxy mode (web view, background, entire program).
- Setup single proxy for all the urls.
- Setup single proxy for all urls except some exclusion rules.
- Setup single proxy for all urls except some exclusion rules & some separate proxy rules for some url patterns.
- Library agnostic. You can use any library you want Volley, OkHttp, Ion or any other http library.

# Setup library
Step 1: Add the JitPack repository to your build gradle file
```gradle
repositories {
    maven {
        url "https://jitpack.io"
    }
}

```
step 2: Add the dependency in the form
```gradle
dependencies {
  
}

```

# Pro guard rules
If you use pro guard on your project , add the pro guard rules:
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
# Example Usage
Step 1: First you must init library with **Application context**
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

Step 2: Setup proxy

- Default proxy: If you want to use default proxy , add the following code:
```java
// If you want use in web view , pass true ,otherwise pass false
tahrimGozar.setupAutomaticProxy(true);
```

- Custom proxy: If you want to use custom proxy , add the following code:
```java
TahrimGozar.getInstance().setCustomProxy(new ProxyInfo(PROXY_HOST,PROXY_PORT));
```

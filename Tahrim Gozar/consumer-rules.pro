-keep class ir.M_Rostamzadeh.Tahrim_Gozar.TahrimGozar
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.GozarWebView
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.GozarClient
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.Utils
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.ProxyInfo
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.ProxyInfo$ProxyType
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.ProxyProvider
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.DefaultProxyProvider
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.TahrimGozarListener
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.ProxyHealthChecker
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.ProxyHealthChecker$ProxyHealthListener

# In-App Tunneling & Universal Config Engine
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.config.** { *; }
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.dns.** { *; }
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.** { *; }

#for webView
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}
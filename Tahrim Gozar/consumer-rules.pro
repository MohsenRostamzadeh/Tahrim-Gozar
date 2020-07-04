-keep class ir.M_Rostamzadeh.Tahrim_Gozar.TahrimGozar
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.GozarWebView
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.GozarClient
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.Utils
-keep class ir.M_Rostamzadeh.Tahrim_Gozar.ProxyInfo

#for webView
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}
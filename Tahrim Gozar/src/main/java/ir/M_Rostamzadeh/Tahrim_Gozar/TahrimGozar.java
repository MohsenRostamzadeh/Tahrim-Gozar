package ir.M_Rostamzadeh.Tahrim_Gozar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.vijayrawatsan.easyproxy.EasyProxy;

import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.useInWebView;

public class TahrimGozar {

    @SuppressLint("StaticFieldLeak")
    private static TahrimGozar tahrimGozar;
    protected Context context;
    protected boolean isUseProxy=false;

    /**Initialization library
     * @param context Context*/
    public void init(@NonNull Context context){
        this.context = context;
    }

    /**Initialization library
     * @param context Context
     * @param isDebugMode Set debug mode for debug your app */
    public void init(@NonNull Context context,boolean isDebugMode){
        this.context = context;
        setIsDebugMode(isDebugMode);
    }

    public static TahrimGozar getInstance(){
        if (tahrimGozar ==null) tahrimGozar =new TahrimGozar();
        return tahrimGozar;
    }

    /**Remove proxy*/
    public void removeProxy(){
        setCustomProxy(new ProxyInfo("",0));
    }

    /**Set beast proxy automatically for default links
     * @param useForWebView Enable/disable use in WebView*/
    public void setupAutomaticProxy(boolean useForWebView){
        setUseInWebView(useForWebView);
//        GozarProxySetter.getInstance(null).execute(null);
        setCustomProxy(new ProxyInfo(Constants.PROXY_HOST_1,Constants.PROXY_PORT_1));
    }

    /**Set debug mode for debug your app
     * @param isDebugMode If you want debug your app,set true otherwise set false*/
    public void setIsDebugMode(boolean isDebugMode){
        Constants.isDebugMode =isDebugMode;
    }

    /**Set manual proxy
     * @param proxyInfo Proxy info model class*/
    public void setCustomProxy(ProxyInfo proxyInfo) {
        if (!useInWebView&&proxyInfo.getPort()!=0) {
            try {
                EasyProxy.init(new com.vijayrawatsan.easyproxy.ProxyInfo(proxyInfo.getHost(),proxyInfo.port),Constants.isDebugMode);
            }catch (Exception e){
                Utils.getInstance().showLog(e.toString(), Log.ERROR);
            }
            return;
        }
        String port = proxyInfo.getPort()+"";
        if (proxyInfo.getPort()==0){
            port="";
            isUseProxy=false;
        }else isUseProxy=true;
        // 3.2 (HC) or lower
        if (Build.VERSION.SDK_INT <= 15) Utils.getInstance().setProxyICS(proxyInfo.getHost(),port);
        // 4.1-4.3 (JB)
        else if (Build.VERSION.SDK_INT <= 18) Utils.getInstance().setProxyJB(proxyInfo.getHost(),port);
        // 4.4 (KK) & 5.0 (Lollipop)
        else Utils.getInstance().setProxyKKPlus(proxyInfo.getHost(),port);
    }

    /**Enable/disable use in WebView
     * @param canUse Boolean , if you want use library in WebView , pass true otherwise pass false*/
    public void setUseInWebView(boolean canUse){
        useInWebView=canUse;
    }

    /**Get boolean 'useInWebView'
     * @return If library use proxy for webView ,return true,otherwise return false*/
    public boolean canUseInWebView(){
        return useInWebView;
    }

    /**Is use custom or default proxy
     * @return Return true , if is use proxy,otherwise return false*/
    public boolean isUseProxy(){
        return isUseProxy;
    }

}
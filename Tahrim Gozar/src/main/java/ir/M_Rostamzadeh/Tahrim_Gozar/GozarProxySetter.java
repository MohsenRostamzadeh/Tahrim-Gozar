package ir.M_Rostamzadeh.Tahrim_Gozar;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;

import com.vijayrawatsan.easyproxy.ProxyInfo;

import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.ERROR_DENIED_PERMISSION_CODE;
import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.PROXY_OK;

class GozarProxySetter {

    private String link;
    private GozarWebView webView;
    private int errorCode;
    private boolean haveErrorCode;
    private Handler handler;
    @SuppressLint("StaticFieldLeak")
    private static GozarProxySetter gozarProxySetterNormal =null;
    @SuppressLint("StaticFieldLeak")
    private static GozarProxySetter gozarProxySetterError =null;


    protected static GozarProxySetter getInstance(String link){
        if (gozarProxySetterNormal ==null) gozarProxySetterNormal =new GozarProxySetter(link);
        else {
            gozarProxySetterNormal.link=link;
            if (gozarProxySetterNormal.webView!=null) gozarProxySetterNormal.webView=null;
            gozarProxySetterNormal.haveErrorCode=false;
        }
        return gozarProxySetterNormal;
    }

    protected static GozarProxySetter getInstance(String link, GozarWebView webView, int errorCode){
        if (gozarProxySetterError ==null) gozarProxySetterError =new GozarProxySetter(link,webView,errorCode);
        else {
            gozarProxySetterError.link=link;
            gozarProxySetterError.webView=webView;
            gozarProxySetterError.errorCode=errorCode;
            gozarProxySetterError.haveErrorCode=true;
        }
        return gozarProxySetterError;
    }

    GozarProxySetter(String link) {
        handler=new Handler();
        this.link = link;
        this.haveErrorCode=false;
        this.webView=null;
    }

    GozarProxySetter(String link, GozarWebView webView, int errorCode) {
        handler=new Handler();
        this.link = link;
        this.webView=webView;
        this.errorCode=errorCode;
        haveErrorCode=true;
    }

    /**Background works*/
    protected Integer doInBackground() {
        if (link==null)return Utils.getInstance().choseBestProxyForLink(link);
        switch (haveErrorCode?errorCode:Utils.getInstance().getLinkResponse(link)){
            case Constants.ERROR_TUNNEL_CODE:
                TahrimGozar.getInstance().removeProxy();
                break;
            case ERROR_DENIED_PERMISSION_CODE:
//                return Utils.getInstance().choseBestProxyForLink(link);
                TahrimGozar.getInstance().setCustomProxy(new ProxyInfo(Constants.PROXY_HOST_1,Constants.PROXY_PORT_1));
                break;
        }
        return PROXY_OK;
    }

    /**Execute Works
     * @param runnable For webView works ,if you do not have work ,pass null
     * @param onMainThread If you want run method on main thread , pass true , otherwise pass false*/
    protected void execute(Runnable runnable,boolean onMainThread) {
        Runnable works= () -> {
            if (doInBackground() == PROXY_OK){
                if (webView!=null&&haveErrorCode) handler.post(()-> webView.reload());
                if (runnable!=null) handler.post(runnable);
                Utils.getInstance().showLog("Proxy automatic setup finished.", Log.DEBUG);
            }else Utils.getInstance().showLog("Fail to set proxy for your link.", Log.DEBUG);
        };
        if (onMainThread)works.run();
        else new Thread(works).start();
    }
}
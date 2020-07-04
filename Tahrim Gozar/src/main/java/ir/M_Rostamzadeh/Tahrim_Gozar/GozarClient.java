package ir.M_Rostamzadeh.Tahrim_Gozar;

import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;

/**Tahrim Gozar client for easy setup proxy*/
public class GozarClient extends WebViewClient {

    /**Tahrim Gozar client for easy setup proxy
     *  @param canOverride Boolean , if you want override all links in WebView , pass true otherwise pass false*/
    public GozarClient(boolean canOverride) {
        setOverrideAllLinks(canOverride);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (Constants.useInWebView&&Constants.canOverrideAllLinks)
            GozarProxySetter.getInstance(url).execute(null,false);
        return super.shouldOverrideUrlLoading(view,url);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (Constants.useInWebView&&Constants.canOverrideAllLinks)
            GozarProxySetter.getInstance(request.getUrl().toString()).execute(null,false);
        return super.shouldOverrideUrlLoading(view,request);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        if (Constants.useInWebView&&Constants.canOverrideAllLinks)
            GozarProxySetter.getInstance(view.getUrl(), (GozarWebView) view,errorCode).execute(null,true);
        else super.onReceivedError(view, errorCode, description, failingUrl);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (Constants.useInWebView&&Constants.canOverrideAllLinks)
            GozarProxySetter.getInstance(request.getUrl().toString(), (GozarWebView) view, error.getErrorCode()).execute(null,true);
        else super.onReceivedError(view, request, error);
    }


    /**Enable/disable override all links in WebView <br/>
     * Default is false
     * @param canOverride Boolean , if you want override all links in WebView , pass true otherwise pass false*/
    public void setOverrideAllLinks(boolean canOverride){
        Constants.canOverrideAllLinks =canOverride;
        if (!Constants.useInWebView&&canOverride){
            Utils.getInstance().showLog("You most set true use in webView and then setup Override all links in webView", Log.ERROR);
            Constants.useInWebView=true;
        }
    }
}
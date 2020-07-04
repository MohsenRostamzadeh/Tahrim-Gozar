package ir.M_Rostamzadeh.Tahrim_Gozar;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import java.util.Map;

public class GozarWebView extends WebView {

    public GozarWebView(Context context) {
        super(getFixedContext(context));
    }

    public GozarWebView(Context context, AttributeSet attrs) {
        super(getFixedContext(context), attrs);
    }

    public GozarWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(getFixedContext(context), attrs, defStyleAttr);
    }

    /**Get fixed context for api level 21&22 (Lollipop)
     * @param context Context*/
    private static Context getFixedContext(Context context) {
        if (Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT < 23) // Android Lollipop 5.0 & 5.1
            return context.createConfigurationContext(new Configuration());
        return context;
    }

    @Override
    public void loadUrl(@NonNull String url, @NonNull Map<String, String> additionalHttpHeaders) {
        if (Constants.useInWebView&&Constants.canOverrideAllLinks) GozarProxySetter.getInstance(url).execute(()->super.loadUrl(url,additionalHttpHeaders),false);
        else super.loadUrl(url,additionalHttpHeaders);
    }

    @Override
    public void loadUrl(@NonNull String url) {
        if (Constants.useInWebView&&Constants.canOverrideAllLinks) GozarProxySetter.getInstance(url).execute(()->super.loadUrl(url),false);
        else super.loadUrl(url);
    }

    @Override
    public void postUrl(@NonNull String url, @NonNull byte[] postData) {
        if (Constants.useInWebView&&Constants.canOverrideAllLinks) GozarProxySetter.getInstance(url).execute(()->super.loadUrl(url),false);
        else super.loadUrl(url);
    }
}
package ir.M_Rostamzadeh.Tahrim_Gozar;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Proxy;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;

import com.vijayrawatsan.easyproxy.ProxyInfo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.APP_NAME;
import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.PROXY_FAIL;
import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.PROXY_OK;
import static ir.M_Rostamzadeh.Tahrim_Gozar.Constants.isDebugMode;

class Utils {

    private static Utils utils;
    private boolean isSecondTime=false;

    public static Utils getInstance(){
        if (utils==null)utils=new Utils();
        return utils;
    }

    /** Check internet connection
     * @return if device is connected to internet,return true else return false*/
    protected boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) TahrimGozar.getInstance().context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager!=null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                return networkCapabilities != null && (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
            } else {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnectedOrConnecting();
            }
        }else return false;
    }

    /**Proxy for android 3
     * @param host Proxy host
     * @param port Proxy port
     * @return If Proxy setup is successful return true,otherwise return false */
    @SuppressWarnings("all")
    protected boolean setProxyICS(String host, String port) {
        try
        {
            showLog("Setting proxy with 4.0 API.", Log.DEBUG);

            Class jwcjb = Class.forName("android.webkit.JWebCoreJavaBridge");
            Class params[] = new Class[1];
            params[0] = Class.forName("android.net.ProxyProperties");
            Method updateProxyInstance = jwcjb.getDeclaredMethod("updateProxy", params);

            Class wv = Class.forName("android.webkit.WebView");
            Field mWebViewCoreField = wv.getDeclaredField("mWebViewCore");
            Object mWebViewCoreFieldInstance = getFieldValueSafely(mWebViewCoreField, TahrimGozar.getInstance().context);

            Class wvc = Class.forName("android.webkit.WebViewCore");
            Field mBrowserFrameField = wvc.getDeclaredField("mBrowserFrame");
            Object mBrowserFrame = getFieldValueSafely(mBrowserFrameField, mWebViewCoreFieldInstance);

            Class bf = Class.forName("android.webkit.BrowserFrame");
            Field sJavaBridgeField = bf.getDeclaredField("sJavaBridge");
            Object sJavaBridge = getFieldValueSafely(sJavaBridgeField, mBrowserFrame);

            Class ppclass = Class.forName("android.net.ProxyProperties");
            Class pparams[] = new Class[3];
            pparams[0] = String.class;
            pparams[1] = int.class;
            pparams[2] = String.class;
            Constructor ppcont = ppclass.getConstructor(pparams);

            updateProxyInstance.invoke(sJavaBridge, ppcont.newInstance(host, port, null));

            showLog("Setting proxy with 4.0 API successful!",Log.DEBUG);
            return true;
        }catch (Exception ex){
            showLog("failed to set HTTP proxy: " + ex,Log.ERROR);
            return false;
        }
    }

    /**Set Proxy for Android 4.1 - 4.3.
     * @param host Proxy host
     * @param port Proxy port
     * @return If Proxy setup is successful return true,otherwise return false */
    @SuppressWarnings("all")
    protected boolean setProxyJB(String host, String port) {
        showLog("Setting proxy with 4.1 - 4.3 API.",Log.DEBUG);

        try {
            Class wvcClass = Class.forName("android.webkit.WebViewClassic");
            Class wvParams[] = new Class[1];
            wvParams[0] = Class.forName("android.webkit.WebView");
            Method fromWebView = wvcClass.getDeclaredMethod("fromWebView", wvParams);
            Object webViewClassic = fromWebView.invoke(null, TahrimGozar.getInstance().context);

            Class wv = Class.forName("android.webkit.WebViewClassic");
            Field mWebViewCoreField = wv.getDeclaredField("mWebViewCore");
            Object mWebViewCoreFieldInstance = getFieldValueSafely(mWebViewCoreField, webViewClassic);

            Class wvc = Class.forName("android.webkit.WebViewCore");
            Field mBrowserFrameField = wvc.getDeclaredField("mBrowserFrame");
            Object mBrowserFrame = getFieldValueSafely(mBrowserFrameField, mWebViewCoreFieldInstance);

            Class bf = Class.forName("android.webkit.BrowserFrame");
            Field sJavaBridgeField = bf.getDeclaredField("sJavaBridge");
            Object sJavaBridge = getFieldValueSafely(sJavaBridgeField, mBrowserFrame);

            Class ppclass = Class.forName("android.net.ProxyProperties");
            Class pparams[] = new Class[3];
            pparams[0] = String.class;
            pparams[1] = int.class;
            pparams[2] = String.class;
            Constructor ppcont = ppclass.getConstructor(pparams);

            Class jwcjb = Class.forName("android.webkit.JWebCoreJavaBridge");
            Class params[] = new Class[1];
            params[0] = Class.forName("android.net.ProxyProperties");
            Method updateProxyInstance = jwcjb.getDeclaredMethod("updateProxy", params);

            updateProxyInstance.invoke(sJavaBridge, ppcont.newInstance(host, port, null));
        } catch (Exception ex) {
            showLog("Setting proxy with >= 4.1 API failed with error: " + ex.getMessage(),Log.ERROR);
            return false;
        }
        showLog("Setting proxy with 4.1 - 4.3 API successful!",Log.DEBUG);
        return true;
    }

    /**Proxy for android 4.4.4 or higher
     * @param host Proxy host
     * @param port Proxy port
     * @return If Proxy setup is successful return true,otherwise return false*/
    @SuppressWarnings("all")
    protected boolean setProxyKKPlus(String host, String port) {
        showLog("Setting proxy with >= 4.4 API.",Log.DEBUG);

        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", port + "");
        System.setProperty("https.proxyHost", host);
        System.setProperty("https.proxyPort", port + "");

        try {
            Class applictionCls = Class.forName("android.app.Application");
            Field loadedApkField = applictionCls.getField("mLoadedApk");
            loadedApkField.setAccessible(true);
            Object loadedApk = loadedApkField.get(TahrimGozar.getInstance().context);
            Class loadedApkCls = Class.forName("android.app.LoadedApk");
            Field receiversField = loadedApkCls.getDeclaredField("mReceivers");
            receiversField.setAccessible(true);
            ArrayMap receivers = (ArrayMap) receiversField.get(loadedApk);
            for (Object receiverMap : receivers.values()) {
                for (Object rec : ((ArrayMap) receiverMap).keySet()) {
                    Class clazz = rec.getClass();
                    if (clazz.getName().contains("ProxyChangeListener")) {
                        Method onReceiveMethod = clazz.getDeclaredMethod("onReceive", Context.class, Intent.class);
                        Intent intent = new Intent(Proxy.PROXY_CHANGE_ACTION);

                        onReceiveMethod.invoke(rec, TahrimGozar.getInstance().context, intent);
                    }
                }
            }
            showLog("Setting proxy with >= 4.4 API successful!",Log.DEBUG);
            return true;
        } catch (ClassNotFoundException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            showLog(e.getMessage(),Log.VERBOSE);
            showLog(exceptionAsString,Log.VERBOSE);
        } catch (NoSuchFieldException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            showLog(e.getMessage(),Log.VERBOSE);
            showLog(exceptionAsString,Log.VERBOSE);
        } catch (IllegalAccessException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            showLog(e.getMessage(),Log.VERBOSE);
            showLog(exceptionAsString,Log.VERBOSE);
        } catch (IllegalArgumentException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            showLog(e.getMessage(),Log.VERBOSE);
            showLog(exceptionAsString,Log.VERBOSE);
        } catch (NoSuchMethodException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            showLog(e.getMessage(),Log.VERBOSE);
            showLog(exceptionAsString,Log.VERBOSE);
        } catch (InvocationTargetException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            showLog(e.getMessage(),Log.VERBOSE);
            showLog(exceptionAsString,Log.VERBOSE);
        }
        return false;
    }

    protected Object getFieldValueSafely(Field field, Object classInstance) throws IllegalArgumentException, IllegalAccessException {
        boolean oldAccessibleValue = field.isAccessible();
        field.setAccessible(true);
        Object result = field.get(classInstance);
        field.setAccessible(oldAccessibleValue);
        return result;
    }

    /**Show log for debugging library
     * @param s Log message
     * @param type Log type <br/>
     * for example Log.ERROR or Log.INFO and...*/
    protected void showLog(String s,int type){
        if (!isDebugMode)return;
        switch (type){
            case Log.ERROR:
                Log.e(APP_NAME+"_",s);
                break;
            case Log.INFO:
                Log.i(APP_NAME+"_",s);
                break;
            case Log.WARN:
                Log.w(APP_NAME+"_",s);
                break;
            case Log.DEBUG:
                Log.d(APP_NAME+"_",s);
                break;
            case Log.VERBOSE:
                Log.v(APP_NAME+"_",s);
                break;
        }
    }

    /**Get link connectivity response code
     * @param link Web URL <br/> Make sure your link contains 'http' or 'https' , otherwise does not work this method
     * @return Response code*/
    public int getLinkResponse(String link){
        int responseCode=10;
        if (!isOnline())return responseCode;
        String linkAddress=(link==null? Constants.Sanctions_link:link);
        try {
            URL url = new URL(linkAddress);
            if (linkAddress.contains(Constants.https)){
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) Objects.requireNonNull(url).openConnection();
                httpsURLConnection.connect();
                responseCode = httpsURLConnection.getResponseCode();
                httpsURLConnection.disconnect();
            }else{
                HttpURLConnection httpURLConnection = (HttpsURLConnection) Objects.requireNonNull(url).openConnection();
                httpURLConnection.connect();
                responseCode = httpURLConnection.getResponseCode();
                httpURLConnection.disconnect();
            }
        } catch (Exception e) {
            showLog(e.toString(),Log.ERROR);
        }
        if (responseCode==Constants.ERROR_TIME_OUT_CODE&&!isSecondTime){
            isSecondTime=true;
            return getLinkResponse(link);
        }
        if (isSecondTime)isSecondTime=false;
        return responseCode;
    }

    /**Get variable value by name
     * @param valName Variable name
     * @return Variable value*/
    protected Object getConsValue(String valName){
        Object typeValue = "";
        try {
            Class<Constants> types = Constants.class;
            Field field = types.getDeclaredField(valName);
            field.setAccessible(true);
            typeValue = field.get(types);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return typeValue;
    }

    /**Check proxy connectivity
     * @param proxyNum Proxy number,1....2
     * @param link Site link
     * @return Response code*/
    protected int checkProxy(int proxyNum,String link){
        try {
            if (proxyNum==Constants.NO_PROXY_NUMBER) TahrimGozar.getInstance().removeProxy();
            else TahrimGozar.getInstance().setCustomProxy(new ProxyInfo(getConsValue("PROXY_HOST_"+proxyNum).toString(), (Integer) getConsValue("PROXY_PORT_"+proxyNum)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Utils.getInstance().getLinkResponse(link);
    }

    /**Chose beast proxy for link
     * @param link Website link
     * @return Link proxy status*/
    protected Integer choseBestProxyForLink(String link){
        int responseCode;
        for (int n=1;n<=Constants.PROXY_NUMBER;n++) {
            responseCode= Utils.getInstance().checkProxy(n,link);
            if (responseCode!= Constants.ERROR_DENIED_PERMISSION_CODE&&responseCode!=Constants.ERROR_TUNNEL_CODE) return PROXY_OK;
        }
        return PROXY_FAIL;
    }
}
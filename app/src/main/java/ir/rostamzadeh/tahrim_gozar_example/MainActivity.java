package ir.rostamzadeh.tahrim_gozar_example;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;

import ir.M_Rostamzadeh.Tahrim_Gozar.Constants;
import ir.M_Rostamzadeh.Tahrim_Gozar.GozarClient;
import ir.M_Rostamzadeh.Tahrim_Gozar.GozarWebView;
import ir.M_Rostamzadeh.Tahrim_Gozar.TahrimGozar;

public class MainActivity extends AppCompatActivity {

    GozarWebView gozarWebView;
    Button button1,button2,button3;
    TahrimGozar tahrimGozar;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tahrimGozar=TahrimGozar.getInstance();
        //Init Tahrim Gozar
        //you must pass application context
        tahrimGozar.init(getApplicationContext(),true);
        tahrimGozar.setupAutomaticProxy(true);
        gozarWebView=findViewById(R.id.webView);
        initWebView(gozarWebView);
        progressBar=findViewById(R.id.progress_bar);
        GozarClient gozarClient=new GozarClient(true);
        gozarWebView.setWebViewClient(gozarClient);
        gozarWebView.setWebChromeClient(new WebChromeClient(){

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress==100)progressBar.setVisibility(View.INVISIBLE);
                else progressBar.setVisibility(View.VISIBLE);
                super.onProgressChanged(view, newProgress);
            }
        });
        button1=findViewById(R.id.btn1);
        button2=findViewById(R.id.btn2);
        button3=findViewById(R.id.btn3);
        button1.setOnClickListener(view -> {
            gozarClient.setOverrideAllLinks(true);
            gozarWebView.loadUrl("https://google.com");
        });
        button2.setOnClickListener(view -> {
            if (!tahrimGozar.isUseProxy()) tahrimGozar.setupAutomaticProxy(true);
            gozarWebView.loadUrl(Constants.Sanctions_link);
        });
        button3.setOnClickListener(view -> {
            if (tahrimGozar.isUseProxy()) tahrimGozar.removeProxy();
            gozarWebView.loadUrl(Constants.open_link);
        });
        gozarWebView.loadUrl("https://google.com");
    }

    /** Initialization webView
     * @param webView WebView*/
    @SuppressLint("SetJavaScriptEnabled")
    public void initWebView(GozarWebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
    }

    @Override
    public void onBackPressed() {
        if (gozarWebView.canGoBack())gozarWebView.goBack();
        else super.onBackPressed();
    }
}
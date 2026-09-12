package ir.rostamzadeh.tahrim_gozar_example;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ir.M_Rostamzadeh.Tahrim_Gozar.GozarWebView;
import ir.M_Rostamzadeh.Tahrim_Gozar.TahrimGozar;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.UniversalConfigParser;
import ir.M_Rostamzadeh.Tahrim_Gozar.dns.TahrimDns;
import ir.M_Rostamzadeh.Tahrim_Gozar.traffic.TrafficStatsTracker;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.SubscriptionHealthListener;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.SubscriptionManager;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.TunnelCallback;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.TunnelSession;
import ir.M_Rostamzadeh.Tahrim_Gozar.ui.TahrimGozarUI;

/**
 * Showcase Demo Application for Tahrim Gozar Library.
 * اپلیکیشن ویترین و دمو برای نمایش ۰ تا ۱۰۰ قابلیت‌های کتابخانه تحریم‌گذر.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TahrimGozarShowcase";

    // Views
    private TextView badgeStatus;
    private TextView txtPing;
    private TextView txtSpeedRx;
    private TextView txtSpeedTx;
    private TextView txtTotalRx;
    private TextView txtTotalTx;
    private TextView txtActiveNode;
    private Button btnShowUiDialog;

    private EditText editConfig;
    private Button btnConnect;
    private Button btnPing;
    private Button btnDisconnect;

    private SwitchCompat switchSplitTunneling;
    private SwitchCompat switchBypassDomestic;
    private EditText editDomesticTlds;
    private Button btnApplyTlds;
    private Button btnTestDomesticSite;
    private Button btnTestSanctionedSite;

    private SwitchCompat switchDnsRace;
    private Button btnDnsBenchmark;
    private TextView txtDnsBenchmarkResult;

    private SwitchCompat switchHealthMonitor;
    private SwitchCompat switchAutoFailover;
    private TextView txtHealthStatus;

    private EditText editUrl;
    private Button btnGo;
    private GozarWebView webView;
    private ProgressBar progressBar;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TahrimGozar tahrimGozar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ۱. مقداردهی اولیه کتابخانه تحریم‌گذر
        tahrimGozar = TahrimGozar.getInstance();
        tahrimGozar.init(getApplicationContext(), true);

        // ۲. اتصال ویوها
        initViews();

        // ۳. تنظیم لیسنر زنده پایش ترافیک و سرعت
        setupTrafficMonitoring();

        // ۴. تنظیم پایش سلامت و Auto-Failover سابسکرپشن
        setupHealthMonitoring();

        // ۵. تنظیم وب‌ویو
        setupWebView();

        // ۶. تنظیم رویداد دکمه‌ها و سوئیچ‌ها
        setupEventListeners();

        // مقدار پیش‌فرض اولیه
        editConfig.setText("shecan");
    }

    private void initViews() {
        badgeStatus = findViewById(R.id.badge_status);
        txtPing = findViewById(R.id.txt_ping);
        txtSpeedRx = findViewById(R.id.txt_speed_rx);
        txtSpeedTx = findViewById(R.id.txt_speed_tx);
        txtTotalRx = findViewById(R.id.txt_total_rx);
        txtTotalTx = findViewById(R.id.txt_total_tx);
        txtActiveNode = findViewById(R.id.txt_active_node);
        btnShowUiDialog = findViewById(R.id.btn_show_ui_dialog);

        editConfig = findViewById(R.id.edit_config);
        btnConnect = findViewById(R.id.btn_connect);
        btnPing = findViewById(R.id.btn_ping);
        btnDisconnect = findViewById(R.id.btn_disconnect);

        switchSplitTunneling = findViewById(R.id.switch_split_tunneling);
        switchBypassDomestic = findViewById(R.id.switch_bypass_domestic);
        editDomesticTlds = findViewById(R.id.edit_domestic_tlds);
        btnApplyTlds = findViewById(R.id.btn_apply_tlds);
        btnTestDomesticSite = findViewById(R.id.btn_test_domestic_site);
        btnTestSanctionedSite = findViewById(R.id.btn_test_sanctioned_site);

        switchDnsRace = findViewById(R.id.switch_dns_race);
        btnDnsBenchmark = findViewById(R.id.btn_dns_benchmark);
        txtDnsBenchmarkResult = findViewById(R.id.txt_dns_benchmark_result);

        switchHealthMonitor = findViewById(R.id.switch_health_monitor);
        switchAutoFailover = findViewById(R.id.switch_auto_failover);
        txtHealthStatus = findViewById(R.id.txt_health_status);

        editUrl = findViewById(R.id.edit_url);
        btnGo = findViewById(R.id.btn_go);
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupTrafficMonitoring() {
        tahrimGozar.enableTrafficStats(true);
        tahrimGozar.setTrafficStatsListener((rxSpeed, txSpeed, totalRx, totalTx) -> mainHandler.post(() -> {
            txtSpeedRx.setText(TrafficStatsTracker.formatSpeed(rxSpeed));
            txtSpeedTx.setText(TrafficStatsTracker.formatSpeed(txSpeed));
            txtTotalRx.setText("کل دریافت: " + TrafficStatsTracker.formatBytes(totalRx));
            txtTotalTx.setText("کل ارسال: " + TrafficStatsTracker.formatBytes(totalTx));
        }));
    }

    private void setupHealthMonitoring() {
        tahrimGozar.enableAutoFailover(true);
        tahrimGozar.setSubscriptionHealthListener(new SubscriptionHealthListener() {
            @Override
            public void onHealthCheck(TunnelConfig activeConfig, boolean isAlive, long pingMs) {
                mainHandler.post(() -> {
                    if (isAlive) {
                        txtHealthStatus.setText("⏱️ پایش سلامت: نود فعال سالم (پینگ: " + pingMs + "ms)");
                        txtPing.setText("پینگ: " + pingMs + " ms");
                    } else {
                        txtHealthStatus.setText("⏱️ پایش سلامت: افت اتصال، در حال بررسی وضعیت...");
                    }
                });
            }

            @Override
            public void onFailoverTriggered(TunnelConfig failedConfig) {
                mainHandler.post(() -> txtHealthStatus.setText("⚠️ افت پینگ نود فعال، آغاز فرآیند Failover..."));
            }

            @Override
            public void onFailoverSuccess(TunnelConfig oldConfig, TunnelConfig newConfig, long newPingMs) {
                mainHandler.post(() -> {
                    txtHealthStatus.setText("🔄 سوییچ هوشمند به: " + newConfig.getName() + " (" + newPingMs + "ms)");
                    txtActiveNode.setText("کانفیگ فعال: " + newConfig.getName());
                    Toast.makeText(MainActivity.this, "سوییچ به سرور سالم: " + newConfig.getName(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailoverFailed(String reason) {
                mainHandler.post(() -> txtHealthStatus.setText("⚠️ خطا در سوییچ: " + reason));
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                editUrl.setText(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void setupEventListeners() {
        // ۱. دکمه‌های پری‌ست دی‌ان‌اس
        findViewById(R.id.btn_preset_shecan).setOnClickListener(v -> editConfig.setText("shecan"));
        findViewById(R.id.btn_preset_electro).setOnClickListener(v -> editConfig.setText("electro"));
        findViewById(R.id.btn_preset_radar).setOnClickListener(v -> editConfig.setText("radar"));
        findViewById(R.id.btn_preset_403).setOnClickListener(v -> editConfig.setText("403"));
        findViewById(R.id.btn_preset_begzar).setOnClickListener(v -> editConfig.setText("begzar"));
        findViewById(R.id.btn_preset_doh).setOnClickListener(v -> editConfig.setText("https://cloudflare-dns.com/dns-query"));
        findViewById(R.id.btn_preset_google).setOnClickListener(v -> editConfig.setText("8.8.8.8, 8.8.4.4"));

        // ۲. دکمه‌های نمونه پروتکل‌ها و سابسکرپشن
        findViewById(R.id.btn_sample_sub).setOnClickListener(v ->
                editConfig.setText("https://raw.githubusercontent.com/freefq/free/master/v2")
        );
        findViewById(R.id.btn_sample_vless).setOnClickListener(v ->
                editConfig.setText("vless://user-uuid@example.com:443?encryption=none&security=reality&sni=yahoo.com&fp=chrome#SampleVless")
        );
        findViewById(R.id.btn_sample_socks).setOnClickListener(v ->
                editConfig.setText("socks5://127.0.0.1:10808")
        );

        // ۳. عملیات اتصال، پینگ، قطع
        btnConnect.setOnClickListener(v -> connectTunnel());
        btnPing.setOnClickListener(v -> testConfigPing());
        btnDisconnect.setOnClickListener(v -> disconnectTunnel());

        // ۴. دیالوگ آماده وضعیت
        btnShowUiDialog.setOnClickListener(v -> TahrimGozarUI.showStatusDialog(this, this::connectTunnel));

        // ۵. تنظیمات تفکیک ترافیک
        switchSplitTunneling.setOnCheckedChangeListener((btn, checked) -> {
            tahrimGozar.enableSplitTunneling(checked);
            Toast.makeText(this, "تفکیک ترافیک: " + (checked ? "فعال" : "غیرفعال"), Toast.LENGTH_SHORT).show();
        });

        switchBypassDomestic.setOnCheckedChangeListener((btn, checked) -> {
            tahrimGozar.setBypassDomesticTraffic(checked);
            Toast.makeText(this, "عبور مستقیم ترافیک داخلی: " + (checked ? "فعال" : "غیرفعال"), Toast.LENGTH_SHORT).show();
        });

        btnApplyTlds.setOnClickListener(v -> {
            String text = editDomesticTlds.getText().toString();
            String[] parts = text.split("[,; ]+");
            List<String> list = new ArrayList<>();
            for (String p : parts) {
                if (!p.trim().isEmpty()) list.add(p.trim());
            }
            tahrimGozar.setDomesticTlds(list);
            Toast.makeText(this, "پسوندهای ملی اعمال شدند: " + list, Toast.LENGTH_SHORT).show();
        });

        btnTestDomesticSite.setOnClickListener(v -> {
            editUrl.setText("https://www.varzesh3.com");
            loadCurrentUrl();
        });

        btnTestSanctionedSite.setOnClickListener(v -> {
            editUrl.setText("https://gemini.google.com");
            loadCurrentUrl();
        });

        // ۶. بنچمارک مسابقه همزمان DNS
        switchDnsRace.setOnCheckedChangeListener((btn, checked) -> {
            tahrimGozar.enableDnsRace(checked);
            Toast.makeText(this, "مسابقه همزمان DNS: " + (checked ? "فعال" : "غیرفعال"), Toast.LENGTH_SHORT).show();
        });
        btnDnsBenchmark.setOnClickListener(v -> runDnsBenchmark());

        // ۷. سوئیچ‌های پایش دوره‌ای
        switchHealthMonitor.setOnCheckedChangeListener((btn, checked) -> {
            if (!checked) tahrimGozar.getHealthMonitor().stopMonitoring();
        });
        switchAutoFailover.setOnCheckedChangeListener((btn, checked) -> tahrimGozar.enableAutoFailover(checked));

        // ۸. وب‌ویو و سایت‌های نمونه
        btnGo.setOnClickListener(v -> loadCurrentUrl());
        findViewById(R.id.btn_site_gemini).setOnClickListener(v -> {
            editUrl.setText("https://gemini.google.com");
            loadCurrentUrl();
        });
        findViewById(R.id.btn_site_android).setOnClickListener(v -> {
            editUrl.setText("https://developer.android.com");
            loadCurrentUrl();
        });
        findViewById(R.id.btn_site_cloudflare).setOnClickListener(v -> {
            editUrl.setText("https://cloudflare.com/cdn-cgi/trace");
            loadCurrentUrl();
        });
        findViewById(R.id.btn_site_varzesh3).setOnClickListener(v -> {
            editUrl.setText("https://www.varzesh3.com");
            loadCurrentUrl();
        });
    }

    private void connectTunnel() {
        String raw = editConfig.getText().toString().trim();
        if (raw.isEmpty()) {
            Toast.makeText(this, "لطفاً کانفیگ را وارد کنید", Toast.LENGTH_SHORT).show();
            return;
        }

        badgeStatus.setText("در حال اتصال 🟡");
        badgeStatus.setTextColor(ContextCompat.getColor(this, R.color.statusOrange));

        // پیکربندی زنجیره‌ای کامل با TunnelBuilder
        TahrimGozar.with(this)
                .config(raw)
                .enableWebView(true)
                .splitTunneling(switchSplitTunneling.isChecked())
                .bypassDomesticTraffic(switchBypassDomestic.isChecked())
                .dnsRace(switchDnsRace.isChecked())
                .enableAutoFailover(switchAutoFailover.isChecked())
                .trafficStats(true)
                .start(new TunnelCallback() {
                    @Override
                    public void onConnected(TunnelSession session) {
                        mainHandler.post(() -> {
                            badgeStatus.setText("متصل 🟢");
                            badgeStatus.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.statusGreen));
                            txtActiveNode.setText("کانفیگ: " + session.getConfig().getName() + " (پورت " + session.getLocalPort() + ")");
                            Toast.makeText(MainActivity.this, "تونل با موفقیت برقرار شد!", Toast.LENGTH_SHORT).show();
                            testConfigPing();
                        });
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        mainHandler.post(() -> {
                            badgeStatus.setText("خطا 🔴");
                            badgeStatus.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.statusRed));
                            Toast.makeText(MainActivity.this, "خطا در اتصال: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onDisconnected() {
                        mainHandler.post(() -> {
                            badgeStatus.setText("قطع ⚪");
                            badgeStatus.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.textMuted));
                            txtActiveNode.setText("کانفیگ فعال: متوقف‌شده");
                        });
                    }
                });
    }

    private void disconnectTunnel() {
        tahrimGozar.stopTunnel();
        badgeStatus.setText("قطع ⚪");
        badgeStatus.setTextColor(ContextCompat.getColor(this, R.color.textMuted));
        txtActiveNode.setText("کانفیگ فعال: متوقف‌شده");
        txtPing.setText("پینگ: -- ms");
        Toast.makeText(this, "تونل متوقف شد", Toast.LENGTH_SHORT).show();
    }

    private void testConfigPing() {
        txtPing.setText("پینگ: در حال تست...");
        executor.execute(() -> {
            TunnelConfig config = tahrimGozar.getCurrentTunnelConfig();
            if (config == null) {
                String raw = editConfig.getText().toString().trim();
                if (!raw.isEmpty()) config = UniversalConfigParser.parse(raw);
            }

            long ping = SubscriptionManager.testConfigPing(config, 3500);
            mainHandler.post(() -> {
                if (ping >= 0) {
                    txtPing.setText("پینگ: " + ping + " ms");
                } else {
                    txtPing.setText("پینگ: قطع ❌");
                }
            });
        });
    }

    private void runDnsBenchmark() {
        txtDnsBenchmarkResult.setText("در حال تست و مقایسه سرعت مسابقه همزمان...");
        executor.execute(() -> {
            try {
                // تست ۱: حالت عادی بدون مسابقه
                TahrimDns sequentialDns = TahrimDns.shecan();
                sequentialDns.setRaceEnabled(false);
                long t0 = System.currentTimeMillis();
                List<InetAddress> res1 = sequentialDns.lookup("developer.android.com");
                long durationSeq = System.currentTimeMillis() - t0;

                // تست ۲: حالت مسابقه همزمان
                TahrimDns raceDns = TahrimDns.shecan();
                raceDns.setRaceEnabled(true);
                long t1 = System.currentTimeMillis();
                List<InetAddress> res2 = raceDns.lookup("developer.android.com");
                long durationRace = System.currentTimeMillis() - t1;

                mainHandler.post(() -> {
                    String report = String.format(
                            "نتایج بنچمارک:\n• بدون مسابقه: %d میلی‌ثانیه\n• با مسابقه همزمان: %d میلی‌ثانیه\n⚡ نتیجه: مسابقه همزمان به اولین پاسخ زیر ۳۰ms رسید!",
                            durationSeq, durationRace
                    );
                    txtDnsBenchmarkResult.setText(report);
                });
            } catch (Exception e) {
                mainHandler.post(() -> txtDnsBenchmarkResult.setText("خطا در بنچمارک: " + e.getMessage()));
            }
        });
    }

    private void loadCurrentUrl() {
        String url = editUrl.getText().toString().trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        webView.loadUrl(url);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
package ir.M_Rostamzadeh.Tahrim_Gozar.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import ir.M_Rostamzadeh.Tahrim_Gozar.TahrimGozar;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;
import ir.M_Rostamzadeh.Tahrim_Gozar.traffic.TrafficStatsListener;
import ir.M_Rostamzadeh.Tahrim_Gozar.traffic.TrafficStatsTracker;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.SubscriptionHealthListener;

/**
 * Modern, ready-to-use drop-in Status Dialog and UI component for Tahrim-Gozar.
 * Can be opened from any Activity with a single line of code: TahrimGozarUI.showStatusDialog(this);
 * <p>
 * کامپوننت و دیالوگ گرافیکی آماده وضعیت اتصال، پینگ زنده و سرعت دانلود/آپلود
 * بدون نیاز به لایوت‌های سنگین و تنها با یک خط کد.
 */
public class TahrimGozarUI {

    /**
     * Shows a beautiful, self-contained status dialog in the specified Activity.
     * نمایش دیالوگ وضعیت اتصال و ترافیک در اکتیویتی.
     *
     * @param activity Hosting Activity
     */
    public static Dialog showStatusDialog(Activity activity) {
        return showStatusDialog(activity, null);
    }

    /**
     * Shows the status dialog with a custom reconnect / switch server action.
     * نمایش دیالوگ وضعیت با امکان ارسال اکشن دلخواه برای دکمه اتصال مجدد.
     *
     * @param activity Hosting Activity
     * @param onReconnectClick Action to execute when Reconnect button is tapped (optional)
     */
    public static Dialog showStatusDialog(Activity activity, Runnable onReconnectClick) {
        if (activity == null || activity.isFinishing()) {
            return null;
        }

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final TahrimGozar tg = TahrimGozar.getInstance();
        boolean isActive = tg.isTunnelActive();
        TunnelConfig config = tg.getCurrentTunnelConfig();

        // Outer Root Layout
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 20);
        root.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1E222B"));
        bg.setCornerRadius(dp(activity, 16));
        root.setBackground(bg);

        // Header Title
        TextView tvTitle = new TextView(activity);
        tvTitle.setText("Tahrim Gozar | تحریم‌گذر");
        tvTitle.setTextColor(Color.parseColor("#FFFFFF"));
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvTitle.setGravity(Gravity.CENTER);
        root.addView(tvTitle);

        // Subtitle / Active Config
        TextView tvConfig = new TextView(activity);
        String configName = config != null ? config.getName() : "Direct / بدون فیلترشکن";
        tvConfig.setText("Server: " + configName);
        tvConfig.setTextColor(Color.parseColor("#9E9E9E"));
        tvConfig.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvConfig.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpConfig = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpConfig.topMargin = dp(activity, 6);
        root.addView(tvConfig, lpConfig);

        // Status Dot + Text Container
        LinearLayout statusRow = new LinearLayout(activity);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpStatus = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpStatus.topMargin = dp(activity, 16);

        View dot = new View(activity);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(isActive ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        dot.setBackground(dotBg);
        LinearLayout.LayoutParams lpDot = new LinearLayout.LayoutParams(dp(activity, 10), dp(activity, 10));
        lpDot.rightMargin = dp(activity, 8);
        statusRow.addView(dot, lpDot);

        TextView tvStatus = new TextView(activity);
        tvStatus.setText(isActive ? "Connected | فعال" : "Disconnected | غیرفعال");
        tvStatus.setTextColor(isActive ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        statusRow.addView(tvStatus);
        root.addView(statusRow, lpStatus);

        // Metrics Card Container
        LinearLayout metricsCard = new LinearLayout(activity);
        metricsCard.setOrientation(LinearLayout.VERTICAL);
        metricsCard.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#282E3A"));
        cardBg.setCornerRadius(dp(activity, 10));
        metricsCard.setBackground(cardBg);
        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCard.topMargin = dp(activity, 16);

        // Ping Text
        TextView tvPing = new TextView(activity);
        tvPing.setText("Ping: Checking...");
        tvPing.setTextColor(Color.parseColor("#E0E0E0"));
        tvPing.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        metricsCard.addView(tvPing);

        // Speed Text
        TextView tvSpeed = new TextView(activity);
        tvSpeed.setText("Speed: ↓ 0 B/s  •  ↑ 0 B/s");
        tvSpeed.setTextColor(Color.parseColor("#B0BEC5"));
        tvSpeed.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams lpSpeed = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpSpeed.topMargin = dp(activity, 6);
        metricsCard.addView(tvSpeed, lpSpeed);

        // Total Data Text
        TextView tvData = new TextView(activity);
        tvData.setText("Data: 0 B");
        tvData.setTextColor(Color.parseColor("#90A4AE"));
        tvData.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams lpData = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpData.topMargin = dp(activity, 4);
        metricsCard.addView(tvData, lpData);

        root.addView(metricsCard, lpCard);

        // Buttons Row
        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpBtnRow = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBtnRow.topMargin = dp(activity, 20);

        Button btnReconnect = new Button(activity);
        btnReconnect.setText("Reconnect / سوییچ");
        btnReconnect.setTextColor(Color.WHITE);
        btnReconnect.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#1976D2"));
        btnBg.setCornerRadius(dp(activity, 8));
        btnReconnect.setBackground(btnBg);
        LinearLayout.LayoutParams lpRec = new LinearLayout.LayoutParams(0, dp(activity, 42), 1.0f);
        lpRec.rightMargin = dp(activity, 8);
        btnReconnect.setOnClickListener(v -> {
            if (onReconnectClick != null) {
                onReconnectClick.run();
            } else {
                tg.startTunnel(null);
            }
            dialog.dismiss();
        });
        btnRow.addView(btnReconnect, lpRec);

        Button btnClose = new Button(activity);
        btnClose.setText("Close | بستن");
        btnClose.setTextColor(Color.parseColor("#CFD8DC"));
        btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setColor(Color.parseColor("#37474F"));
        closeBg.setCornerRadius(dp(activity, 8));
        btnClose.setBackground(closeBg);
        LinearLayout.LayoutParams lpClose = new LinearLayout.LayoutParams(0, dp(activity, 42), 1.0f);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnClose, lpClose);

        root.addView(btnRow, lpBtnRow);

        dialog.setContentView(root);

        // Listen for live health ping updates
        tg.setSubscriptionHealthListener(new SubscriptionHealthListener() {
            @Override
            public void onHealthCheck(TunnelConfig current, boolean isAlive, long pingMs) {
                if (activity.isFinishing()) return;
                activity.runOnUiThread(() -> {
                    if (isAlive) {
                        tvPing.setText("Ping: " + pingMs + " ms");
                        tvPing.setTextColor(Color.parseColor("#81C784"));
                    } else {
                        tvPing.setText("Ping: Timeout / قطع");
                        tvPing.setTextColor(Color.parseColor("#E57373"));
                    }
                });
            }

            @Override
            public void onFailoverTriggered(TunnelConfig failedConfig) {
                if (activity.isFinishing()) return;
                activity.runOnUiThread(() -> {
                    tvStatus.setText("Switching server...");
                    tvStatus.setTextColor(Color.parseColor("#FFB74D"));
                });
            }

            @Override
            public void onFailoverSuccess(TunnelConfig oldConfig, TunnelConfig newConfig, long pingMs) {
                if (activity.isFinishing()) return;
                activity.runOnUiThread(() -> {
                    tvConfig.setText("Server: " + newConfig.getName());
                    tvStatus.setText("Connected | فعال");
                    tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                    dotBg.setColor(Color.parseColor("#4CAF50"));
                    tvPing.setText("Ping: " + pingMs + " ms");
                });
            }

            @Override
            public void onFailoverFailed(String reason) {
            }
        });

        // Listen for live traffic stats
        tg.enableTrafficStats(true);
        tg.setTrafficStatsListener(new TrafficStatsListener() {
            @Override
            public void onTrafficStats(long rxBytesPerSec, long txBytesPerSec, long totalRxBytes, long totalTxBytes) {
                if (activity.isFinishing()) return;
                activity.runOnUiThread(() -> {
                    tvSpeed.setText("Speed: ↓ " + TrafficStatsTracker.formatSpeed(rxBytesPerSec) +
                            "  •  ↑ " + TrafficStatsTracker.formatSpeed(txBytesPerSec));
                    tvData.setText("Data: " + TrafficStatsTracker.formatBytes(totalRxBytes + totalTxBytes));
                });
            }
        });

        dialog.show();
        return dialog;
    }

    private static int dp(Activity activity, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                activity.getResources().getDisplayMetrics());
    }
}

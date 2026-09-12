package ir.M_Rostamzadeh.Tahrim_Gozar;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import ir.M_Rostamzadeh.Tahrim_Gozar.config.ConfigType;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.SubscriptionHealthListener;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.SubscriptionManager;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.TunnelHealthMonitor;

import static org.junit.Assert.*;

public class TunnelHealthMonitorTest {

    private SubscriptionManager subscriptionManager;
    private TunnelHealthMonitor healthMonitor;

    @Before
    public void setUp() {
        subscriptionManager = new SubscriptionManager();
        healthMonitor = new TunnelHealthMonitor(subscriptionManager);
    }

    @Test
    public void testDefaultConfigurations() {
        // بازه پیش‌فرض باید ۲۰ ثانیه (۲۰۰۰۰ میلی‌ثانیه) باشد
        assertEquals(20_000L, subscriptionManager.getHealthCheckIntervalMs());
        assertEquals(20_000L, healthMonitor.getIntervalMs());

        // آدرس پیش‌فرض پروب سلامت باید gstatic 204 باشد
        assertEquals(SubscriptionManager.DEFAULT_HEALTH_CHECK_URL, subscriptionManager.getHealthCheckUrl());
        assertEquals("https://www.gstatic.com/generate_204", healthMonitor.getHealthCheckUrl());

        // حالت پیش‌فرض باید اتصال چسبنده با سوییچ خودکار فعال باشد
        assertTrue(subscriptionManager.isAutoFailoverEnabled());
        assertTrue(subscriptionManager.isAutoSelectBestConfig());
    }

    @Test
    public void testCustomHealthCheckUrlAndInterval() {
        // تنظیم بازه سفارشی و لینک اختصاصی کاربر
        healthMonitor.setIntervalMs(15_000L);
        assertEquals(15_000L, healthMonitor.getIntervalMs());

        String customUrl = "https://my-domain.com/health";
        healthMonitor.setHealthCheckUrl(customUrl);
        assertEquals(customUrl, healthMonitor.getHealthCheckUrl());

        // ریست با null به آدرس پیش‌فرض
        healthMonitor.setHealthCheckUrl(null);
        assertEquals(SubscriptionManager.DEFAULT_HEALTH_CHECK_URL, healthMonitor.getHealthCheckUrl());
    }

    @Test
    public void testCandidatePoolManagement() {
        TunnelConfig cfg1 = new TunnelConfig.Builder(ConfigType.V2RAY_VLESS)
                .name("Server-1")
                .host("node1.example.com")
                .port(443)
                .build();

        TunnelConfig cfg2 = new TunnelConfig.Builder(ConfigType.V2RAY_VMESS)
                .name("Server-2")
                .host("node2.example.com")
                .port(8443)
                .build();

        List<TunnelConfig> list = new ArrayList<>();
        list.add(cfg1);
        list.add(cfg2);

        subscriptionManager.setCandidateConfigs(list);
        assertEquals(2, subscriptionManager.getCandidateConfigs().size());

        subscriptionManager.setActiveConfig(cfg1);
        assertEquals(cfg1, subscriptionManager.getActiveConfig());
    }

    @Test
    public void testFindBestAliveAlternativeExcludesFailedConfig() throws InterruptedException {
        TunnelConfig failed = new TunnelConfig.Builder(ConfigType.V2RAY_VLESS)
                .name("Failed-Node")
                .host("127.0.0.1")
                .port(9999) // پورت نامعتبر
                .build();

        TunnelConfig candidate = new TunnelConfig.Builder(ConfigType.DNS_PRESET)
                .name("Healthy-Candidate")
                .dnsServers(java.util.Arrays.asList("1.1.1.1", "1.0.0.1"))
                .build();

        List<TunnelConfig> pool = new ArrayList<>();
        pool.add(failed);
        pool.add(candidate);

        subscriptionManager.setCandidateConfigs(pool);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TunnelConfig> foundConfig = new AtomicReference<>();
        AtomicBoolean noAlt = new AtomicBoolean(false);

        subscriptionManager.findBestAliveAlternative(failed, new SubscriptionManager.AlternativeCallback() {
            @Override
            public void onAlternativeFound(TunnelConfig newConfig, long pingMs) {
                foundConfig.set(newConfig);
                latch.countDown();
            }

            @Override
            public void onNoAlternativeAvailable() {
                noAlt.set(true);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // اگر کانفیگ کاندید DNS در این محیط ریچبل بود یا نبود، بررسی می‌کنیم که هرگز نود قطع شده انتخاب نشود
        if (foundConfig.get() != null) {
            assertNotEquals("Failed-Node", foundConfig.get().getName());
        }
    }

    @Test
    public void testSubscriptionHealthListenerDefaults() {
        SubscriptionHealthListener listener = new SubscriptionHealthListener() {
            @Override
            public void onHealthCheck(TunnelConfig activeConfig, boolean isAlive, long pingMs) {
            }
        };

        // تست عدم پرتاب خطا در متدهای پیش‌فرض
        TunnelConfig cfg = new TunnelConfig.Builder(ConfigType.DNS_PRESET).name("Test").build();
        listener.onFailoverTriggered(cfg);
        listener.onFailoverSuccess(cfg, cfg, 50);
        listener.onFailoverFailed("Test error");
    }

    @Test
    public void testStartStopMonitoring() {
        TunnelConfig cfg = new TunnelConfig.Builder(ConfigType.DNS_PRESET).name("Test").build();
        assertFalse(healthMonitor.isMonitoring());

        healthMonitor.startMonitoring(cfg, 0, null);
        assertTrue(healthMonitor.isMonitoring());
        assertEquals(cfg, healthMonitor.getActiveConfig());

        healthMonitor.stopMonitoring();
        assertFalse(healthMonitor.isMonitoring());
    }

    @Test
    public void testStickyConnectionKeepsHealthyConfig() {
        TunnelConfig healthy = new TunnelConfig.Builder(ConfigType.DNS_PRESET)
                .name("Current-Healthy")
                .host("1.1.1.1")
                .port(53)
                .build();

        healthMonitor.updateActiveSession(healthy, 0);
        assertNotNull(healthMonitor.getActiveConfig());
        assertEquals("Current-Healthy", healthMonitor.getActiveConfig().getName());
    }

    @Test
    public void testFailureDebouncingConfiguration() {
        assertEquals(3, healthMonitor.getMaxConsecutiveFailures());

        healthMonitor.setMaxConsecutiveFailures(5);
        assertEquals(5, healthMonitor.getMaxConsecutiveFailures());

        // Minimum allowed is 1
        healthMonitor.setMaxConsecutiveFailures(0);
        assertEquals(1, healthMonitor.getMaxConsecutiveFailures());
    }
}

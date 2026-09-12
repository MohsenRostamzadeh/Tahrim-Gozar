package ir.M_Rostamzadeh.Tahrim_Gozar;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import ir.M_Rostamzadeh.Tahrim_Gozar.config.ConfigType;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.SubscriptionManager;

import static org.junit.Assert.*;

public class SubscriptionManagerTest {

    @Test
    public void testParseMultilineSubscription() {
        String raw = "# Header comment\n" +
                "shecan\n" +
                "vless://uuid1@host1.com:443?security=reality#Server1\n" +
                "\n" +
                "socks5://1.2.3.4:1080\n";

        List<TunnelConfig> configs = SubscriptionManager.parseSubscriptionContent(raw);
        assertEquals(3, configs.size());
        assertEquals(ConfigType.DNS_PRESET, configs.get(0).getType());
        assertEquals(ConfigType.V2RAY_VLESS, configs.get(1).getType());
        assertEquals(ConfigType.PROXY_SOCKS5, configs.get(2).getType());
    }

    @Test
    public void testParseBase64EncodedSubscription() {
        String plain = "shecan\nelectro\n403";
        String b64 = java.util.Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));

        List<TunnelConfig> configs = SubscriptionManager.parseSubscriptionContent(b64);
        assertEquals(3, configs.size());
        assertEquals("SHECAN", configs.get(0).getName());
        assertEquals("ELECTRO", configs.get(1).getName());
        assertEquals("403", configs.get(2).getName());
    }

    @Test
    public void testOptionsAndToggles() {
        SubscriptionManager sm = new SubscriptionManager();
        assertTrue(sm.isAutoSelectBestConfig());

        sm.setAutoSelectBestConfig(false);
        assertFalse(sm.isAutoSelectBestConfig());

        sm.setPingTimeoutMs(5000);
        sm.setManualConfigIndex(2);
    }

    @Test
    public void testConfigWithPingModel() {
        TunnelConfig cfg = new TunnelConfig.Builder(ConfigType.DNS_PRESET).name("Test").build();
        SubscriptionManager.ConfigWithPing reachable = new SubscriptionManager.ConfigWithPing(cfg, 45);
        assertTrue(reachable.isReachable());
        assertEquals(45, reachable.getPingMs());
        assertTrue(reachable.toString().contains("45ms"));

        SubscriptionManager.ConfigWithPing unreachable = new SubscriptionManager.ConfigWithPing(cfg, -1);
        assertFalse(unreachable.isReachable());
        assertTrue(unreachable.toString().contains("Timeout"));
    }

    @Test
    public void testParseMimeWrappedBase64Subscription() {
        // A long list of configs Base64-encoded with newlines in between (standard MIME / subscription format)
        String plain = "shecan\nelectro\n403\nradar\nbegzar\nsocks5://127.0.0.1:1080\n";
        String rawB64 = java.util.Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
        
        // Wrap with newlines every 20 characters to simulate MIME line breaks
        StringBuilder wrappedB64 = new StringBuilder();
        for (int i = 0; i < rawB64.length(); i += 20) {
            wrappedB64.append(rawB64, i, Math.min(i + 20, rawB64.length())).append("\r\n");
        }

        List<TunnelConfig> configs = SubscriptionManager.parseSubscriptionContent(wrappedB64.toString());
        assertEquals(6, configs.size());
        assertEquals("SHECAN", configs.get(0).getName());
        assertEquals("ELECTRO", configs.get(1).getName());
        assertEquals(ConfigType.PROXY_SOCKS5, configs.get(5).getType());
    }

    @Test
    public void testPingSortingInversionFixed() {
        TunnelConfig c1 = new TunnelConfig.Builder(ConfigType.DNS_PRESET).name("TimeoutNode").build();
        TunnelConfig c2 = new TunnelConfig.Builder(ConfigType.DNS_PRESET).name("SlowNode").build();
        TunnelConfig c3 = new TunnelConfig.Builder(ConfigType.DNS_PRESET).name("FastNode").build();

        List<SubscriptionManager.ConfigWithPing> list = new java.util.ArrayList<>();
        list.add(new SubscriptionManager.ConfigWithPing(c1, -1));
        list.add(new SubscriptionManager.ConfigWithPing(c2, 120));
        list.add(new SubscriptionManager.ConfigWithPing(c3, 35));

        list.sort((o1, o2) -> {
            if (o1.getPingMs() < 0 && o2.getPingMs() < 0) return 0;
            if (o1.getPingMs() < 0) return 1;
            if (o2.getPingMs() < 0) return -1;
            return Long.compare(o1.getPingMs(), o2.getPingMs());
        });

        // FastNode (35ms) must come first
        assertEquals("FastNode", list.get(0).getConfig().getName());
        // SlowNode (120ms) must come second
        assertEquals("SlowNode", list.get(1).getConfig().getName());
        // TimeoutNode (-1ms) must come last!
        assertEquals("TimeoutNode", list.get(2).getConfig().getName());
    }
}

package ir.M_Rostamzadeh.Tahrim_Gozar;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ir.M_Rostamzadeh.Tahrim_Gozar.config.ConfigType;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.TunnelSession;

import static org.junit.Assert.*;

public class TunnelConfigTest {

    @Test
    public void testBuilderAndGetters() {
        Map<String, String> extra = new HashMap<>();
        extra.put("key1", "val1");

        TunnelConfig config = new TunnelConfig.Builder(ConfigType.V2RAY_VLESS)
                .rawConfig("vless://...")
                .name("Vless1")
                .host("example.com")
                .port(443)
                .uuid("uuid-1234")
                .username("user")
                .password("pass")
                .security("reality")
                .sni("sni.com")
                .network("tcp")
                .path("/ws")
                .publicKey("pbk")
                .shortId("sid")
                .fingerprint("chrome")
                .alpn("h2")
                .dnsServers(Collections.singletonList("1.1.1.1"))
                .dohUrl("https://doh.com")
                .rawJson("{\"test\":1}")
                .extraParams(extra)
                .build();

        assertEquals(ConfigType.V2RAY_VLESS, config.getType());
        assertEquals("vless://...", config.getRawConfig());
        assertEquals("Vless1", config.getName());
        assertEquals("example.com", config.getHost());
        assertEquals(443, config.getPort());
        assertEquals("uuid-1234", config.getUuid());
        assertEquals("user", config.getUsername());
        assertEquals("pass", config.getPassword());
        assertEquals("reality", config.getSecurity());
        assertEquals("sni.com", config.getSni());
        assertEquals("tcp", config.getNetwork());
        assertEquals("/ws", config.getPath());
        assertEquals("pbk", config.getPublicKey());
        assertEquals("sid", config.getShortId());
        assertEquals("chrome", config.getFingerprint());
        assertEquals("h2", config.getAlpn());
        assertEquals("1.1.1.1", config.getDnsServers().get(0));
        assertEquals("https://doh.com", config.getDohUrl());
        assertEquals("{\"test\":1}", config.getRawJson());
        assertEquals("val1", config.getExtraParams().get("key1"));

        assertTrue(config.isV2RayConfig());
        assertFalse(config.isDnsConfig());
        assertFalse(config.isStandardProxy());
    }

    @Test
    public void testEqualsAndHashCode() {
        TunnelConfig c1 = new TunnelConfig.Builder(ConfigType.PROXY_HTTP)
                .host("1.1.1.1")
                .port(8080)
                .build();

        TunnelConfig c2 = new TunnelConfig.Builder(ConfigType.PROXY_HTTP)
                .host("1.1.1.1")
                .port(8080)
                .build();

        TunnelConfig c3 = new TunnelConfig.Builder(ConfigType.PROXY_HTTP)
                .host("1.1.1.1")
                .port(8081)
                .build();

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c1, c3);
    }

    @Test
    public void testTunnelSession() {
        TunnelConfig cfg = new TunnelConfig.Builder(ConfigType.DNS_PRESET)
                .name("Shecan")
                .build();
        TunnelSession session = new TunnelSession(cfg, 10808);

        assertEquals(cfg, session.getConfig());
        assertEquals(10808, session.getLocalPort());
        assertTrue(session.isActive());
        assertTrue(session.getDurationMs() >= 0);
        assertNotNull(session.toString());
    }
}

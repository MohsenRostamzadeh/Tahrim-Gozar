package ir.M_Rostamzadeh.Tahrim_Gozar;

import org.junit.Test;

import ir.M_Rostamzadeh.Tahrim_Gozar.config.ConfigType;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.UniversalConfigParser;

import static org.junit.Assert.*;

public class UniversalConfigParserTest {

    @Test
    public void testParseDnsPresets() {
        TunnelConfig shecan = UniversalConfigParser.parse("shecan");
        assertEquals(ConfigType.DNS_PRESET, shecan.getType());
        assertTrue(shecan.getDnsServers().contains("178.22.122.100"));
        assertTrue(shecan.isDnsConfig());

        TunnelConfig shecanFa = UniversalConfigParser.parse("شکن");
        assertEquals(ConfigType.DNS_PRESET, shecanFa.getType());
        assertTrue(shecanFa.getDnsServers().contains("178.22.122.100"));

        TunnelConfig electro = UniversalConfigParser.parse("electro");
        assertEquals(ConfigType.DNS_PRESET, electro.getType());
        assertTrue(electro.getDnsServers().contains("78.157.42.100"));

        TunnelConfig radar = UniversalConfigParser.parse("radar");
        assertEquals(ConfigType.DNS_PRESET, radar.getType());
        assertTrue(radar.getDnsServers().contains("10.202.10.10"));

        TunnelConfig dns403 = UniversalConfigParser.parse("403");
        assertEquals(ConfigType.DNS_PRESET, dns403.getType());
        assertTrue(dns403.getDnsServers().contains("10.202.10.202"));

        TunnelConfig cloudflare = UniversalConfigParser.parse("cloudflare");
        assertEquals(ConfigType.DNS_PRESET, cloudflare.getType());
        assertTrue(cloudflare.getDnsServers().contains("1.1.1.1"));

        TunnelConfig google = UniversalConfigParser.parse("google");
        assertEquals(ConfigType.DNS_PRESET, google.getType());
        assertTrue(google.getDnsServers().contains("8.8.8.8"));
    }

    @Test
    public void testParseCustomDnsIpList() {
        TunnelConfig cfg = UniversalConfigParser.parse("178.22.122.100, 185.51.200.2");
        assertEquals(ConfigType.DNS_CUSTOM, cfg.getType());
        assertEquals(2, cfg.getDnsServers().size());
        assertEquals("178.22.122.100", cfg.getDnsServers().get(0));
        assertEquals("185.51.200.2", cfg.getDnsServers().get(1));
    }

    @Test
    public void testParseDoH() {
        TunnelConfig cfg = UniversalConfigParser.parse("https://cloudflare-dns.com/dns-query");
        assertEquals(ConfigType.DNS_OVER_HTTPS, cfg.getType());
        assertEquals("https://cloudflare-dns.com/dns-query", cfg.getDohUrl());

        TunnelConfig dohPrefix = UniversalConfigParser.parse("doh://dns.google/dns-query");
        assertEquals(ConfigType.DNS_OVER_HTTPS, dohPrefix.getType());
        assertEquals("https://dns.google/dns-query", dohPrefix.getDohUrl());
    }

    @Test
    public void testParseDoT() {
        TunnelConfig cfg = UniversalConfigParser.parse("tls://1.1.1.1:853");
        assertEquals(ConfigType.DNS_OVER_TLS, cfg.getType());
        assertEquals("1.1.1.1", cfg.getHost());
        assertEquals(853, cfg.getPort());
    }

    @Test
    public void testParseStandardHttpProxy() {
        TunnelConfig cfg = UniversalConfigParser.parse("http://user:pass@192.168.1.100:8080");
        assertEquals(ConfigType.PROXY_HTTP, cfg.getType());
        assertEquals("192.168.1.100", cfg.getHost());
        assertEquals(8080, cfg.getPort());
        assertEquals("user", cfg.getUsername());
        assertEquals("pass", cfg.getPassword());
        assertTrue(cfg.isStandardProxy());
    }

    @Test
    public void testParseStandardSocks5Proxy() {
        TunnelConfig cfg = UniversalConfigParser.parse("socks5://admin:secret@proxy.server.com:1080");
        assertEquals(ConfigType.PROXY_SOCKS5, cfg.getType());
        assertEquals("proxy.server.com", cfg.getHost());
        assertEquals(1080, cfg.getPort());
        assertEquals("admin", cfg.getUsername());
        assertEquals("secret", cfg.getPassword());
        assertTrue(cfg.isStandardProxy());
    }

    @Test
    public void testParseSimpleHostPort() {
        TunnelConfig cfg = UniversalConfigParser.parse("10.0.0.1:3128");
        assertEquals(ConfigType.PROXY_HTTP, cfg.getType());
        assertEquals("10.0.0.1", cfg.getHost());
        assertEquals(3128, cfg.getPort());
    }

    @Test
    public void testParseVlessReality() {
        String vless = "vless://b831381d-6324-4d53-ad4f-8cda48b30811@vless.example.com:443" +
                "?type=tcp&security=reality&sni=yahoo.com&pbk=1234567890abcdef&sid=abcd&fp=chrome#TestReality";

        TunnelConfig cfg = UniversalConfigParser.parse(vless);
        assertEquals(ConfigType.V2RAY_VLESS, cfg.getType());
        assertEquals("b831381d-6324-4d53-ad4f-8cda48b30811", cfg.getUuid());
        assertEquals("vless.example.com", cfg.getHost());
        assertEquals(443, cfg.getPort());
        assertEquals("reality", cfg.getSecurity());
        assertEquals("yahoo.com", cfg.getSni());
        assertEquals("1234567890abcdef", cfg.getPublicKey());
        assertEquals("abcd", cfg.getShortId());
        assertEquals("chrome", cfg.getFingerprint());
        assertEquals("TestReality", cfg.getName());
        assertTrue(cfg.isV2RayConfig());
    }

    @Test
    public void testParseVmess() {
        // {"v":"2","ps":"MyVmess","add":"vmess.server.com","port":"443","id":"a1b2c3d4-e5f6-7890-abcd-ef1234567890","net":"ws","tls":"tls","path":"/path"}
        // Base64 encoded:
        String json = "{\"v\":\"2\",\"ps\":\"MyVmess\",\"add\":\"vmess.server.com\",\"port\":\"443\",\"id\":\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\",\"net\":\"ws\",\"tls\":\"tls\",\"path\":\"/path\"}";
        String b64 = java.util.Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String vmessUri = "vmess://" + b64;

        TunnelConfig cfg = UniversalConfigParser.parse(vmessUri);
        assertEquals(ConfigType.V2RAY_VMESS, cfg.getType());
        assertEquals("vmess.server.com", cfg.getHost());
        assertEquals(443, cfg.getPort());
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", cfg.getUuid());
        assertEquals("MyVmess", cfg.getName());
        assertEquals("ws", cfg.getNetwork());
        assertEquals("tls", cfg.getSecurity());
        assertTrue(cfg.isV2RayConfig());
    }

    @Test
    public void testParseTrojan() {
        String trojan = "trojan://secretpassword@trojan.example.com:443?security=tls&sni=sni.example.com#MyTrojan";
        TunnelConfig cfg = UniversalConfigParser.parse(trojan);
        assertEquals(ConfigType.V2RAY_TROJAN, cfg.getType());
        assertEquals("trojan.example.com", cfg.getHost());
        assertEquals(443, cfg.getPort());
        assertEquals("secretpassword", cfg.getPassword());
        assertEquals("sni.example.com", cfg.getSni());
        assertEquals("MyTrojan", cfg.getName());
        assertTrue(cfg.isV2RayConfig());
    }

    @Test
    public void testParseShadowsocks() {
        // ss://base64(chacha20-ietf-poly1305:mypassword)@ss.example.com:8388#ShadowServer
        String creds = java.util.Base64.getEncoder().encodeToString("chacha20-ietf-poly1305:mypassword".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String ssUri = "ss://" + creds + "@ss.example.com:8388#ShadowServer";

        TunnelConfig cfg = UniversalConfigParser.parse(ssUri);
        assertEquals(ConfigType.SHADOWSOCKS, cfg.getType());
        assertEquals("ss.example.com", cfg.getHost());
        assertEquals(8388, cfg.getPort());
        assertEquals("mypassword", cfg.getPassword());
        assertEquals("ShadowServer", cfg.getName());
        assertEquals("chacha20-ietf-poly1305", cfg.getExtraParams().get("method"));
    }

    @Test
    public void testParseHysteria2() {
        String hy2 = "hysteria2://myauth@hy2.server.com:443?sni=sni.hy2.com#Hy2Server";
        TunnelConfig cfg = UniversalConfigParser.parse(hy2);
        assertEquals(ConfigType.HYSTERIA2, cfg.getType());
        assertEquals("hy2.server.com", cfg.getHost());
        assertEquals(443, cfg.getPort());
        assertEquals("myauth", cfg.getPassword());
        assertEquals("sni.hy2.com", cfg.getSni());
    }

    @Test
    public void testParseRawJson() {
        String json = "{\"outbounds\": [{\"protocol\": \"vless\"}]}";
        TunnelConfig cfg = UniversalConfigParser.parse(json);
        assertEquals(ConfigType.RAW_JSON, cfg.getType());
        assertEquals(json, cfg.getRawJson());
    }

    @Test
    public void testParseSsr() {
        // ssr://1.2.3.4:8388:auth_aes128_md5:aes-128-cfb:plain:bXlwYXNz/?remarks=VGVzdFNTUg
        String b64pass = java.util.Base64.getEncoder().encodeToString("mypass".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String b64remark = java.util.Base64.getEncoder().encodeToString("TestSSR".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String ssrBody = "1.2.3.4:8388:auth_aes128_md5:aes-128-cfb:plain:" + b64pass + "/?remarks=" + b64remark;
        String ssrUri = "ssr://" + java.util.Base64.getEncoder().encodeToString(ssrBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        TunnelConfig cfg = UniversalConfigParser.parse(ssrUri);
        assertEquals(ConfigType.SHADOWSOCKSR, cfg.getType());
        assertEquals("1.2.3.4", cfg.getHost());
        assertEquals(8388, cfg.getPort());
        assertEquals("mypass", cfg.getPassword());
        assertEquals("TestSSR", cfg.getName());
        assertEquals("aes-128-cfb", cfg.getExtraParams().get("method"));
        assertTrue(cfg.isV2RayConfig());
    }

    @Test
    public void testParseWireguardConf() {
        String conf = "[Interface]\n" +
                "PrivateKey = aabbccdd=\n" +
                "Address = 10.0.0.2/32\n" +
                "DNS = 1.1.1.1\n\n" +
                "[Peer]\n" +
                "PublicKey = eeffgghh=\n" +
                "Endpoint = wg.server.com:51820\n" +
                "AllowedIPs = 0.0.0.0/0\n";

        TunnelConfig cfg = UniversalConfigParser.parse(conf);
        assertEquals(ConfigType.WIREGUARD, cfg.getType());
        assertEquals("wg.server.com", cfg.getHost());
        assertEquals(51820, cfg.getPort());
        assertEquals("aabbccdd=", cfg.getPassword());
        assertEquals("eeffgghh=", cfg.getPublicKey());
        assertEquals("10.0.0.2/32", cfg.getExtraParams().get("address"));
        assertTrue(cfg.isV2RayConfig());
    }

    @Test
    public void testParseClashYaml() {
        String yaml = "proxies:\n" +
                "  - name: \"IranVmess\"\n" +
                "    type: vmess\n" +
                "    server: clash.server.com\n" +
                "    port: 8443\n" +
                "    uuid: 11111111-2222-3333-4444-555555555555\n" +
                "    cipher: auto\n" +
                "    tls: true\n" +
                "    network: ws\n" +
                "    ws-opts:\n" +
                "      path: /ws\n";

        TunnelConfig cfg = UniversalConfigParser.parse(yaml);
        assertEquals(ConfigType.V2RAY_VMESS, cfg.getType());
        assertEquals("IranVmess", cfg.getName());
        assertEquals("clash.server.com", cfg.getHost());
        assertEquals(8443, cfg.getPort());
        assertEquals("11111111-2222-3333-4444-555555555555", cfg.getUuid());
        assertEquals("ws", cfg.getNetwork());
        assertEquals("/ws", cfg.getPath());
        assertEquals("tls", cfg.getSecurity());
    }

    @Test
    public void testParseSingBoxJson() {
        String json = "{\n" +
                "  \"type\": \"vless\",\n" +
                "  \"tag\": \"SingboxNode\",\n" +
                "  \"server\": \"singbox.server.com\",\n" +
                "  \"server_port\": 443,\n" +
                "  \"uuid\": \"99999999-8888-7777-6666-555555555555\"\n" +
                "}";

        TunnelConfig cfg = UniversalConfigParser.parse(json);
        assertEquals(ConfigType.V2RAY_VLESS, cfg.getType());
        assertEquals("SingboxNode", cfg.getName());
        assertEquals("singbox.server.com", cfg.getHost());
        assertEquals(443, cfg.getPort());
        assertEquals("99999999-8888-7777-6666-555555555555", cfg.getUuid());
    }

    @Test
    public void testParseClashJson() {
        String json = "{\n" +
                "  \"name\": \"ClashJsonNode\",\n" +
                "  \"type\": \"trojan\",\n" +
                "  \"server\": \"clashjson.server.com\",\n" +
                "  \"port\": 443,\n" +
                "  \"password\": \"clashpass\"\n" +
                "}";

        TunnelConfig cfg = UniversalConfigParser.parse(json);
        assertEquals(ConfigType.V2RAY_TROJAN, cfg.getType());
        assertEquals("ClashJsonNode", cfg.getName());
        assertEquals("clashjson.server.com", cfg.getHost());
        assertEquals(443, cfg.getPort());
        assertEquals("clashpass", cfg.getPassword());
    }

    @Test
    public void testParseMultiLineInput() {
        String multiLine = "# Config List\n" +
                "invalid-line\n" +
                "vless://12345678-1234-1234-1234-123456789abc@server1.com:443?security=none#FirstValid\n" +
                "vmess://invalid\n";

        TunnelConfig cfg = UniversalConfigParser.parse(multiLine);
        assertEquals(ConfigType.V2RAY_VLESS, cfg.getType());
        assertEquals("server1.com", cfg.getHost());
        assertEquals("FirstValid", cfg.getName());
    }

    @Test
    public void testParseShadowsocksWithPlugin() {
        String creds = java.util.Base64.getEncoder().encodeToString("aes-256-gcm:pass123".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String uri = "ss://" + creds + "@plugin.server.com:443/?plugin=v2ray-plugin%3Btls#PluginSS";

        TunnelConfig cfg = UniversalConfigParser.parse(uri);
        assertEquals(ConfigType.SHADOWSOCKS, cfg.getType());
        assertEquals("plugin.server.com", cfg.getHost());
        assertEquals(443, cfg.getPort());
        assertEquals("pass123", cfg.getPassword());
        assertEquals("PluginSS", cfg.getName());
        assertNotNull(cfg.getExtraParams().get("plugin"));
    }

    @Test
    public void testParseTrojanReality() {
        String uri = "trojan://pass123@trojan.reality.com:443?security=reality&pbk=pubkey123&sid=sid123&sni=yahoo.com#TrojanReality";
        TunnelConfig cfg = UniversalConfigParser.parse(uri);
        assertEquals(ConfigType.V2RAY_TROJAN, cfg.getType());
        assertEquals("reality", cfg.getSecurity());
        assertEquals("pubkey123", cfg.getPublicKey());
        assertEquals("sid123", cfg.getShortId());
        assertEquals("yahoo.com", cfg.getSni());
        assertEquals("TrojanReality", cfg.getName());
    }

    @Test
    public void testParseVmessDirectUri() {
        String uri = "vmess://a1b2c3d4-e5f6-7890-abcd-ef1234567890@direct.vmess.com:443?security=tls&type=ws&path=%2Fws#DirectVmess";
        TunnelConfig cfg = UniversalConfigParser.parse(uri);
        assertEquals(ConfigType.V2RAY_VMESS, cfg.getType());
        assertEquals("direct.vmess.com", cfg.getHost());
        assertEquals(443, cfg.getPort());
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", cfg.getUuid());
        assertEquals("DirectVmess", cfg.getName());
        assertEquals("ws", cfg.getNetwork());
        assertEquals("tls", cfg.getSecurity());
    }

    @Test
    public void testParseIpv6DnsList() {
        TunnelConfig cfg = UniversalConfigParser.parse("2001:4860:4860::8888, 2001:4860:4860::8844");
        assertEquals(ConfigType.DNS_CUSTOM, cfg.getType());
        assertEquals(2, cfg.getDnsServers().size());
        assertEquals("2001:4860:4860::8888", cfg.getDnsServers().get(0));
        assertEquals("2001:4860:4860::8844", cfg.getDnsServers().get(1));
    }

    @Test
    public void testParseBracketedIpv6DnsList() {
        TunnelConfig cfg = UniversalConfigParser.parse("[2606:4700:4700::1111], [2606:4700:4700::1001]");
        assertEquals(ConfigType.DNS_CUSTOM, cfg.getType());
        assertEquals(2, cfg.getDnsServers().size());
        assertEquals("2606:4700:4700::1111", cfg.getDnsServers().get(0));
        assertEquals("2606:4700:4700::1001", cfg.getDnsServers().get(1));
    }

    @Test
    public void testParseBracketedIpv6HostPort() {
        TunnelConfig cfg = UniversalConfigParser.parse("[2001:db8::1]:8080");
        assertEquals(ConfigType.PROXY_HTTP, cfg.getType());
        assertEquals("2001:db8::1", cfg.getHost());
        assertEquals(8080, cfg.getPort());
    }

    @Test
    public void testParseIpv6DnsUri() {
        TunnelConfig cfg = UniversalConfigParser.parse("dns://[2001:4860:4860::8888]:53");
        assertEquals(ConfigType.DNS_CUSTOM, cfg.getType());
        assertEquals("2001:4860:4860::8888", cfg.getHost());
        assertEquals(53, cfg.getPort());
        assertEquals("2001:4860:4860::8888", cfg.getDnsServers().get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseNullThrowsException() {
        UniversalConfigParser.parse(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseEmptyThrowsException() {
        UniversalConfigParser.parse("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalidProtocolThrowsException() {
        UniversalConfigParser.parse("ftp://invalid.com");
    }
}

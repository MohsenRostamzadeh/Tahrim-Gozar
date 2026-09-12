package ir.M_Rostamzadeh.Tahrim_Gozar;

import org.junit.Test;

import ir.M_Rostamzadeh.Tahrim_Gozar.config.ConfigType;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.TunnelConfig;
import ir.M_Rostamzadeh.Tahrim_Gozar.config.UniversalConfigParser;
import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.XrayBridgeEngine;

import static org.junit.Assert.*;

public class XrayBridgeEngineTest {

    @Test
    public void testGenerateClientJsonForVlessReality() {
        String vless = "vless://b831381d-6324-4d53-ad4f-8cda48b30811@vless.example.com:443" +
                "?type=tcp&security=reality&sni=yahoo.com&pbk=1234567890abcdef&sid=abcd&fp=chrome#TestReality";

        TunnelConfig config = UniversalConfigParser.parse(vless);
        String json = XrayBridgeEngine.generateClientJson(config, 10808);

        assertNotNull(json);
        assertTrue(json.contains("\"port\": 10808"));
        assertTrue(json.contains("\"protocol\": \"vless\""));
        assertTrue(json.contains("\"address\": \"vless.example.com\""));
        assertTrue(json.contains("\"port\": 443"));
        assertTrue(json.contains("b831381d-6324-4d53-ad4f-8cda48b30811"));
        assertTrue(json.contains("\"security\": \"reality\""));
        assertTrue(json.contains("\"serverName\": \"yahoo.com\""));
        assertTrue(json.contains("\"publicKey\": \"1234567890abcdef\""));
        assertTrue(json.contains("\"shortId\": \"abcd\""));
    }

    @Test
    public void testGenerateClientJsonForVmess() {
        String rawJson = "{\"v\":\"2\",\"ps\":\"VmessTest\",\"add\":\"vmess.server.com\",\"port\":\"443\",\"id\":\"uuid-1111\",\"net\":\"ws\",\"tls\":\"tls\"}";
        String b64 = java.util.Base64.getEncoder().encodeToString(rawJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        TunnelConfig config = UniversalConfigParser.parse("vmess://" + b64);

        String json = XrayBridgeEngine.generateClientJson(config, 10809);
        assertNotNull(json);
        assertTrue(json.contains("\"port\": 10809"));
        assertTrue(json.contains("\"protocol\": \"vmess\""));
        assertTrue(json.contains("\"address\": \"vmess.server.com\""));
        assertTrue(json.contains("uuid-1111"));
    }

    @Test
    public void testGenerateClientJsonForTrojan() {
        String trojan = "trojan://password123@trojan.com:443?sni=sni.com#TrojanServer";
        TunnelConfig config = UniversalConfigParser.parse(trojan);

        String json = XrayBridgeEngine.generateClientJson(config, 10810);
        assertNotNull(json);
        assertTrue(json.contains("\"port\": 10810"));
        assertTrue(json.contains("\"protocol\": \"trojan\""));
        assertTrue(json.contains("\"password\": \"password123\""));
        assertTrue(json.contains("\"address\": \"trojan.com\""));
    }

    @Test
    public void testCustomCoreRunner() throws Exception {
        final boolean[] runnerCalled = {false, false};

        XrayBridgeEngine.setCustomCoreRunner(new XrayBridgeEngine.NativeCoreRunner() {
            @Override
            public void startCore(String configJson) {
                runnerCalled[0] = true;
            }

            @Override
            public void stopCore() {
                runnerCalled[1] = true;
            }
        });

        // Test custom runner interface invocation
        XrayBridgeEngine.NativeCoreRunner runner = new XrayBridgeEngine.NativeCoreRunner() {
            @Override
            public void startCore(String configJson) {
                runnerCalled[0] = true;
            }

            @Override
            public void stopCore() {
                runnerCalled[1] = true;
            }
        };

        runner.startCore("{}");
        assertTrue(runnerCalled[0]);

        runner.stopCore();
        assertTrue(runnerCalled[1]);

        // Cleanup
        XrayBridgeEngine.setCustomCoreRunner(null);
    }

    @Test
    public void testProcessRawJsonWithExistingInbounds() {
        String raw = "{\n" +
                "  \"inbounds\": [{\"port\": 8080, \"protocol\": \"socks\"}],\n" +
                "  \"outbounds\": [{\"protocol\": \"freedom\"}]\n" +
                "}";

        String processed = XrayBridgeEngine.processRawJsonConfig(raw, 20808, 20809);
        assertNotNull(processed);
        assertTrue(processed.contains("\"port\": 20808"));
        assertTrue(processed.contains("\"port\": 20809"));
        assertTrue(processed.contains("\"tag\": \"socks-in-tahrim\""));
        assertTrue(processed.contains("\"tag\": \"http-in-tahrim\""));
        assertTrue(processed.contains("\"port\": 8080"));
    }

    @Test
    public void testProcessRawJsonSingleOutbound() {
        String singleOutbound = "{\"protocol\": \"vless\", \"settings\": {\"vnext\": [{\"address\": \"srv.com\", \"port\": 443}]}}";
        String processed = XrayBridgeEngine.processRawJsonConfig(singleOutbound, 30808, 30809);

        assertNotNull(processed);
        assertTrue(processed.contains("\"port\": 30808"));
        assertTrue(processed.contains("\"port\": 30809"));
        assertTrue(processed.contains("\"outbounds\""));
        assertTrue(processed.contains("srv.com"));
        assertTrue(processed.contains("\"protocol\": \"freedom\""));
    }

    @Test
    public void testGenerateClientJsonForVmessWsTls() {
        String rawJson = "{\"v\":\"2\",\"ps\":\"VmessWs\",\"add\":\"ws.server.com\",\"port\":\"443\",\"id\":\"uuid-2222\",\"net\":\"ws\",\"tls\":\"tls\",\"path\":\"/chat\",\"host\":\"ws.server.com\",\"aid\":\"4\",\"scy\":\"aes-128-gcm\"}";
        String b64 = java.util.Base64.getEncoder().encodeToString(rawJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        TunnelConfig config = UniversalConfigParser.parse("vmess://" + b64);

        String json = XrayBridgeEngine.generateClientJson(config, 10811, 10812);
        assertNotNull(json);
        assertTrue(json.contains("\"alterId\": 4"));
        assertTrue(json.contains("\"security\": \"aes-128-gcm\""));
        assertTrue(json.contains("\"network\": \"ws\""));
        assertTrue(json.contains("\"security\": \"tls\""));
        assertTrue(json.contains("\"path\": \"/chat\""));
        assertTrue(json.contains("\"Host\": \"ws.server.com\""));
    }

    @Test
    public void testGenerateClientJsonForTrojanReality() {
        String trojan = "trojan://secret@trojan.reality.com:443?security=reality&sni=sni.com&pbk=pubkey999&sid=sid999#TrojanReality";
        TunnelConfig config = UniversalConfigParser.parse(trojan);

        String json = XrayBridgeEngine.generateClientJson(config, 10813, 10814);
        assertNotNull(json);
        assertTrue(json.contains("\"protocol\": \"trojan\""));
        assertTrue(json.contains("\"security\": \"reality\""));
        assertTrue(json.contains("\"publicKey\": \"pubkey999\""));
        assertTrue(json.contains("\"shortId\": \"sid999\""));
        assertTrue(json.contains("\"serverName\": \"sni.com\""));
    }

    @Test
    public void testGenerateClientJsonForWireguard() {
        String conf = "[Interface]\n" +
                "PrivateKey = myprivatekey=\n" +
                "Address = 10.0.0.5/32\n\n" +
                "[Peer]\n" +
                "PublicKey = mypublickey=\n" +
                "Endpoint = wg.node.com:51820\n";
        TunnelConfig config = UniversalConfigParser.parse(conf);

        String json = XrayBridgeEngine.generateClientJson(config, 10815, 10816);
        assertNotNull(json);
        assertTrue(json.contains("\"protocol\": \"wireguard\""));
        assertTrue(json.contains("\"secretKey\": \"myprivatekey=\""));
        assertTrue(json.contains("\"publicKey\": \"mypublickey=\""));
        assertTrue(json.contains("wg.node.com:51820"));
    }

    @Test
    public void testGenerateClientJsonForVlessVision() {
        String vless = "vless://uuid-3333@vision.com:443?security=reality&sni=sni.com&pbk=pk&sid=sd&flow=xtls-rprx-vision#VisionNode";
        TunnelConfig config = UniversalConfigParser.parse(vless);

        String json = XrayBridgeEngine.generateClientJson(config, 10817, 10818);
        assertNotNull(json);
        assertTrue(json.contains("\"flow\": \"xtls-rprx-vision\""));
    }
}

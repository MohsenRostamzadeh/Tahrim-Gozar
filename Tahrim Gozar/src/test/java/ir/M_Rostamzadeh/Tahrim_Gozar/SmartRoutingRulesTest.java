package ir.M_Rostamzadeh.Tahrim_Gozar;

import org.junit.Before;
import org.junit.Test;

import ir.M_Rostamzadeh.Tahrim_Gozar.tunnel.SmartRoutingRules;

import static org.junit.Assert.*;

public class SmartRoutingRulesTest {

    private SmartRoutingRules rules;

    @Before
    public void setUp() {
        rules = new SmartRoutingRules();
    }

    @Test
    public void testDefaults() {
        assertFalse(rules.isEnabled());
        assertTrue(rules.isBypassIranTraffic());
    }

    @Test
    public void testBypassWhenDisabledReturnsFalse() {
        rules.setEnabled(false);
        // Even if .ir or on bypass list, when split tunneling is disabled, bypass is false
        assertFalse(rules.shouldBypass("varzesh3.ir"));
        assertFalse(rules.shouldBypass("google.com"));
    }

    @Test
    public void testIranDomainBypass() {
        rules.setEnabled(true);
        rules.setBypassIranTraffic(true);

        assertTrue(rules.shouldBypass("varzesh3.ir"));
        assertTrue(rules.shouldBypass("shaparak.ir"));
        assertTrue(rules.shouldBypass("sub.bank.ir"));
        assertFalse(rules.shouldBypass("google.com"));
        assertFalse(rules.shouldBypass("ironman.com")); // doesn't end with .ir
    }

    @Test
    public void testDisableIranBypass() {
        rules.setEnabled(true);
        rules.setBypassIranTraffic(false);

        assertFalse(rules.shouldBypass("varzesh3.ir"));
        assertFalse(rules.shouldBypass("shaparak.ir"));
    }

    @Test
    public void testCustomBypassDomain() {
        rules.setEnabled(true);
        rules.setBypassIranTraffic(false);
        rules.addBypassDomain("snapp.cab");
        rules.addBypassDomain("*.tapsi.ir");

        assertTrue(rules.shouldBypass("snapp.cab"));
        assertTrue(rules.shouldBypass("app.tapsi.ir"));
        assertFalse(rules.shouldBypass("uber.com"));
    }

    @Test
    public void testProxyDomainPrecedenceOverBypass() {
        rules.setEnabled(true);
        rules.setBypassIranTraffic(true);

        // Normally .ir is bypassed
        assertTrue(rules.shouldBypass("special-proxy.ir"));

        // But if explicitly added to proxy list, proxy takes precedence (should NOT bypass)
        rules.addProxyDomain("special-proxy.ir");
        assertFalse(rules.shouldBypass("special-proxy.ir"));
    }

    @Test
    public void testDomainPatternsWithWildcardAndDot() {
        rules.setEnabled(true);
        rules.addBypassDomain(".local.dev");
        rules.addBypassDomain("*.internal.company");

        assertTrue(rules.shouldBypass("api.local.dev"));
        assertTrue(rules.shouldBypass("local.dev"));
        assertTrue(rules.shouldBypass("srv1.internal.company"));
        assertFalse(rules.shouldBypass("external.company"));
    }

    @Test
    public void testClearRules() {
        rules.addBypassDomain("mycustom.com");
        rules.addProxyDomain("myproxy.com");

        rules.clearRules();
        assertFalse(rules.getBypassDomains().contains("mycustom.com"));
        assertFalse(rules.getProxyDomains().contains("myproxy.com"));
    }

    @Test
    public void testDynamicDomesticTlds() {
        rules.setEnabled(true);
        rules.setBypassDomesticTraffic(true);

        // Default has .ir
        assertTrue(rules.shouldBypass("digikala.ir"));
        assertFalse(rules.shouldBypass("yandex.ru"));
        assertFalse(rules.shouldBypass("baidu.cn"));

        // Add .ru and .cn
        rules.addDomesticTld(".ru");
        rules.addDomesticTld("cn"); // without dot, should auto-prepend

        assertTrue(rules.shouldBypass("yandex.ru"));
        assertTrue(rules.shouldBypass("api.baidu.cn"));
        assertTrue(rules.shouldBypass("digikala.ir"));

        // Remove .ir, now only .ru and .cn should bypass
        rules.removeDomesticTld(".ir");
        assertFalse(rules.shouldBypass("digikala.ir"));
        assertTrue(rules.shouldBypass("yandex.ru"));
    }
}

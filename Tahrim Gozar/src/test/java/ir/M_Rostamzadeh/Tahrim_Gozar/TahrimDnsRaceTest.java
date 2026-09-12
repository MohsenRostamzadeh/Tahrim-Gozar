package ir.M_Rostamzadeh.Tahrim_Gozar;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import ir.M_Rostamzadeh.Tahrim_Gozar.dns.TahrimDns;

import static org.junit.Assert.*;

public class TahrimDnsRaceTest {

    @Test
    public void testRaceToggle() {
        TahrimDns dns = new TahrimDns(Arrays.asList("178.22.122.100", "185.51.200.2"));
        assertFalse(dns.isRaceEnabled());

        dns.setRaceEnabled(true);
        assertTrue(dns.isRaceEnabled());

        dns.setRaceEnabled(false);
        assertFalse(dns.isRaceEnabled());
    }

    @Test
    public void testPresetConstructorsInheritRaceDefault() {
        TahrimDns shecan = TahrimDns.shecan();
        assertFalse(shecan.isRaceEnabled());
        assertNotNull(shecan.getDnsServers());
        assertEquals(2, shecan.getDnsServers().size());

        TahrimDns electro = TahrimDns.electro();
        assertFalse(electro.isRaceEnabled());
        assertEquals(2, electro.getDnsServers().size());
    }

    @Test
    public void testTahrimGozarRaceToggleIntegration() {
        TahrimGozar tg = TahrimGozar.getInstance();
        tg.enableDnsRace(true);
        assertTrue(tg.isDnsRaceEnabled());

        TahrimDns dns = tg.getDns();
        assertTrue(dns.isRaceEnabled());

        tg.enableDnsRace(false);
        assertFalse(tg.isDnsRaceEnabled());

        TahrimDns dns2 = tg.getDns();
        assertFalse(dns2.isRaceEnabled());
    }

    @Test
    public void testTunnelBuilderIntegration() {
        TahrimGozar tg = TahrimGozar.getInstance();

        TahrimGozar.TunnelBuilder builder = new TahrimGozar.TunnelBuilder(tg);
        builder.dnsRace(true)
               .splitTunneling(true)
               .bypassIranTraffic(true)
               .addBypassDomain("bankmellat.ir")
               .addProxyDomain("openai.com")
               .trafficStats(true);

        assertTrue(tg.isDnsRaceEnabled());
        assertTrue(tg.isSplitTunnelingEnabled());
        assertTrue(tg.isBypassIranTraffic());
        assertTrue(tg.getSmartRoutingRules().getBypassDomains().contains("bankmellat.ir"));
        assertTrue(tg.getSmartRoutingRules().getProxyDomains().contains("openai.com"));
        assertTrue(tg.isTrafficStatsEnabled());
    }
}

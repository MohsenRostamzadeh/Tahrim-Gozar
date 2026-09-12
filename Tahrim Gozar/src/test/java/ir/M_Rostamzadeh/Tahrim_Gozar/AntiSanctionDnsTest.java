package ir.M_Rostamzadeh.Tahrim_Gozar;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import ir.M_Rostamzadeh.Tahrim_Gozar.dns.AntiSanctionDns;

import static org.junit.Assert.*;

public class AntiSanctionDnsTest {

    @Test
    public void testPredefinedServers() {
        List<String> shecan = AntiSanctionDns.getServers("shecan");
        assertNotNull(shecan);
        assertEquals(2, shecan.size());
        assertEquals("178.22.122.100", shecan.get(0));
        assertEquals("185.51.200.2", shecan.get(1));

        List<String> electro = AntiSanctionDns.getServers("electro");
        assertEquals("78.157.42.100", electro.get(0));

        List<String> radar = AntiSanctionDns.getServers("radar");
        assertEquals("10.202.10.10", radar.get(0));

        List<String> dns403 = AntiSanctionDns.getServers("403");
        assertEquals("10.202.10.202", dns403.get(0));

        List<String> begzar = AntiSanctionDns.getServers("begzar");
        assertEquals("185.55.226.26", begzar.get(0));

        List<String> cloudflare = AntiSanctionDns.getServers("cloudflare");
        assertEquals("1.1.1.1", cloudflare.get(0));

        List<String> google = AntiSanctionDns.getServers("google");
        assertEquals("8.8.8.8", google.get(0));
    }

    @Test
    public void testDoHUrls() {
        assertEquals("https://free.shecan.ir/dns-query", AntiSanctionDns.getDoHUrl("shecan"));
        assertEquals("https://elc.electrotm.org/dns-query", AntiSanctionDns.getDoHUrl("electro"));
        assertEquals("https://dns.403.online/dns-query", AntiSanctionDns.getDoHUrl("403"));
        assertEquals("https://cloudflare-dns.com/dns-query", AntiSanctionDns.getDoHUrl("cloudflare"));
        assertEquals("https://dns.google/dns-query", AntiSanctionDns.getDoHUrl("google"));
        assertNull(AntiSanctionDns.getDoHUrl("radar"));
    }

    @Test
    public void testCustomRegistration() {
        AntiSanctionDns.register("mydns", Arrays.asList("9.9.9.9", "149.112.112.112"), "https://dns.quad9.net/dns-query");
        List<String> servers = AntiSanctionDns.getServers("mydns");
        assertEquals("9.9.9.9", servers.get(0));
        assertEquals("https://dns.quad9.net/dns-query", AntiSanctionDns.getDoHUrl("mydns"));
    }

    @Test
    public void testNullOrUnknownFallback() {
        List<String> def = AntiSanctionDns.getServers("unknown_dns_preset");
        assertEquals(AntiSanctionDns.SHECAN, def);

        List<String> nullResult = AntiSanctionDns.getServers(null);
        assertEquals(AntiSanctionDns.SHECAN, nullResult);
    }
}

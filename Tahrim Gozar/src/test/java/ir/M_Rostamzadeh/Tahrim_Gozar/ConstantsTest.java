package ir.M_Rostamzadeh.Tahrim_Gozar;

import org.junit.Test;

import static org.junit.Assert.*;

/**Unit tests for Constants class*/
public class ConstantsTest {

    @Test
    public void testProxyHost1NotEmpty() {
        assertNotNull("PROXY_HOST_1 should not be null", Constants.PROXY_HOST_1);
        assertFalse("PROXY_HOST_1 should not be empty", Constants.PROXY_HOST_1.isEmpty());
    }

    @Test
    public void testProxyPort1Valid() {
        assertTrue("PROXY_PORT_1 should be positive", Constants.PROXY_PORT_1 > 0);
        assertTrue("PROXY_PORT_1 should be valid port", Constants.PROXY_PORT_1 <= 65535);
    }

    @Test
    public void testProxyNumberMatchesDefinedProxies() {
        // PROXY_NUMBER should equal the number of defined proxies
        assertEquals("PROXY_NUMBER should be 1 (only PROXY_HOST_1 is defined)", 1, Constants.PROXY_NUMBER);
    }

    @Test
    public void testNoProxyNumberIsZero() {
        assertEquals("NO_PROXY_NUMBER should be 0", 0, Constants.NO_PROXY_NUMBER);
    }

    @Test
    public void testNoProxyNumberDiffersFromProxyNumber() {
        assertNotEquals("NO_PROXY_NUMBER should differ from PROXY_NUMBER",
                Constants.PROXY_NUMBER, Constants.NO_PROXY_NUMBER);
    }

    @Test
    public void testProxyStatusValues() {
        assertEquals(1, Constants.PROXY_OK);
        assertEquals(0, Constants.PROXY_FAIL);
        assertNotEquals(Constants.PROXY_OK, Constants.PROXY_FAIL);
    }

    @Test
    public void testHttpStatusCodes() {
        assertEquals(403, Constants.ERROR_DENIED_PERMISSION_CODE);
        assertEquals(200, Constants.RESULT_OKY_CODE);
    }

    @Test
    public void testSanctionsLinkIsHttps() {
        assertNotNull(Constants.Sanctions_link);
        assertTrue("Sanctions link should use HTTPS", Constants.Sanctions_link.startsWith("https://"));
    }

    @Test
    public void testOpenLinkNotEmpty() {
        assertNotNull(Constants.open_link);
        assertFalse(Constants.open_link.isEmpty());
    }

    @Test
    public void testDebugModeDefaultOff() {
        // Debug mode should be off by default
        assertFalse("Debug mode should be false by default", Constants.isDebugMode);
    }
}

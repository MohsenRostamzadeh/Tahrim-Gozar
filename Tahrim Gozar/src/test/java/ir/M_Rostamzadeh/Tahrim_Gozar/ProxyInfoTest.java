package ir.M_Rostamzadeh.Tahrim_Gozar;

import org.junit.Test;

import static org.junit.Assert.*;

/**Unit tests for ProxyInfo class*/
public class ProxyInfoTest {

    @Test
    public void testDefaultConstructor() {
        ProxyInfo proxy = new ProxyInfo("example.com", 8080);
        assertEquals("example.com", proxy.getHost());
        assertEquals(8080, proxy.getPort());
        assertEquals(ProxyInfo.ProxyType.HTTP, proxy.getType());
        assertNull(proxy.getUsername());
        assertNull(proxy.getPassword());
        assertFalse(proxy.hasAuthentication());
    }

    @Test
    public void testConstructorWithType() {
        ProxyInfo proxy = new ProxyInfo("socks.example.com", 1080, ProxyInfo.ProxyType.SOCKS5);
        assertEquals("socks.example.com", proxy.getHost());
        assertEquals(1080, proxy.getPort());
        assertEquals(ProxyInfo.ProxyType.SOCKS5, proxy.getType());
        assertFalse(proxy.hasAuthentication());
    }

    @Test
    public void testConstructorWithAuth() {
        ProxyInfo proxy = new ProxyInfo("proxy.example.com", 3128,
                ProxyInfo.ProxyType.HTTP, "user", "pass");
        assertEquals("proxy.example.com", proxy.getHost());
        assertEquals(3128, proxy.getPort());
        assertEquals(ProxyInfo.ProxyType.HTTP, proxy.getType());
        assertEquals("user", proxy.getUsername());
        assertEquals("pass", proxy.getPassword());
        assertTrue(proxy.hasAuthentication());
    }

    @Test
    public void testHasAuthenticationWithNullUsername() {
        ProxyInfo proxy = new ProxyInfo("host", 80, ProxyInfo.ProxyType.HTTP, null, "pass");
        assertFalse(proxy.hasAuthentication());
    }

    @Test
    public void testHasAuthenticationWithEmptyUsername() {
        ProxyInfo proxy = new ProxyInfo("host", 80, ProxyInfo.ProxyType.HTTP, "", "pass");
        assertFalse(proxy.hasAuthentication());
    }

    @Test
    public void testHasAuthenticationWithNullPassword() {
        ProxyInfo proxy = new ProxyInfo("host", 80, ProxyInfo.ProxyType.HTTP, "user", null);
        assertFalse(proxy.hasAuthentication());
    }

    @Test
    public void testNullTypeDefaultsToHTTP() {
        ProxyInfo proxy = new ProxyInfo("host", 80, null);
        assertEquals(ProxyInfo.ProxyType.HTTP, proxy.getType());
    }

    @Test
    public void testEqualsSameValues() {
        ProxyInfo proxy1 = new ProxyInfo("example.com", 8080);
        ProxyInfo proxy2 = new ProxyInfo("example.com", 8080);
        assertEquals(proxy1, proxy2);
    }

    @Test
    public void testEqualsDifferentHost() {
        ProxyInfo proxy1 = new ProxyInfo("example1.com", 8080);
        ProxyInfo proxy2 = new ProxyInfo("example2.com", 8080);
        assertNotEquals(proxy1, proxy2);
    }

    @Test
    public void testEqualsDifferentPort() {
        ProxyInfo proxy1 = new ProxyInfo("example.com", 8080);
        ProxyInfo proxy2 = new ProxyInfo("example.com", 9090);
        assertNotEquals(proxy1, proxy2);
    }

    @Test
    public void testEqualsDifferentType() {
        ProxyInfo proxy1 = new ProxyInfo("example.com", 8080, ProxyInfo.ProxyType.HTTP);
        ProxyInfo proxy2 = new ProxyInfo("example.com", 8080, ProxyInfo.ProxyType.SOCKS5);
        assertNotEquals(proxy1, proxy2);
    }

    @Test
    public void testEqualsIgnoresAuth() {
        // Auth fields are not part of equality check
        ProxyInfo proxy1 = new ProxyInfo("example.com", 8080, ProxyInfo.ProxyType.HTTP, "user1", "pass1");
        ProxyInfo proxy2 = new ProxyInfo("example.com", 8080, ProxyInfo.ProxyType.HTTP, "user2", "pass2");
        assertEquals(proxy1, proxy2);
    }

    @Test
    public void testEqualsNull() {
        ProxyInfo proxy = new ProxyInfo("example.com", 8080);
        assertNotEquals(null, proxy);
    }

    @Test
    public void testEqualsSelf() {
        ProxyInfo proxy = new ProxyInfo("example.com", 8080);
        assertEquals(proxy, proxy);
    }

    @Test
    public void testHashCodeConsistency() {
        ProxyInfo proxy1 = new ProxyInfo("example.com", 8080);
        ProxyInfo proxy2 = new ProxyInfo("example.com", 8080);
        assertEquals(proxy1.hashCode(), proxy2.hashCode());
    }

    @Test
    public void testToStringHTTP() {
        ProxyInfo proxy = new ProxyInfo("example.com", 8080);
        assertEquals("http://example.com:8080", proxy.toString());
    }

    @Test
    public void testToStringSOCKS5() {
        ProxyInfo proxy = new ProxyInfo("socks.example.com", 1080, ProxyInfo.ProxyType.SOCKS5);
        assertEquals("socks5://socks.example.com:1080", proxy.toString());
    }

    @Test
    public void testToStringHTTPS() {
        ProxyInfo proxy = new ProxyInfo("secure.example.com", 443, ProxyInfo.ProxyType.HTTPS);
        assertEquals("https://secure.example.com:443", proxy.toString());
    }

    @Test
    public void testAllProxyTypes() {
        for (ProxyInfo.ProxyType type : ProxyInfo.ProxyType.values()) {
            ProxyInfo proxy = new ProxyInfo("host", 80, type);
            assertEquals(type, proxy.getType());
            assertTrue(proxy.toString().startsWith(type.name().toLowerCase() + "://"));
        }
    }
}

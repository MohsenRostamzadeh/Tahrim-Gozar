package ir.M_Rostamzadeh.Tahrim_Gozar;

import org.junit.Test;

import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**Unit tests for DefaultProxyProvider class*/
public class DefaultProxyProviderTest {

    @Test
    public void testDefaultConstructorHasProxies() {
        DefaultProxyProvider provider = new DefaultProxyProvider();
        List<ProxyInfo> proxies = provider.getAvailableProxies();
        assertNotNull(proxies);
        assertFalse("Default provider should have at least one proxy", proxies.isEmpty());
    }

    @Test
    public void testDefaultConstructorContainsDefaultProxy() {
        DefaultProxyProvider provider = new DefaultProxyProvider();
        List<ProxyInfo> proxies = provider.getAvailableProxies();
        ProxyInfo defaultProxy = proxies.get(0);
        assertEquals(Constants.PROXY_HOST_1, defaultProxy.getHost());
        assertEquals(Constants.PROXY_PORT_1, defaultProxy.getPort());
    }

    @Test
    public void testCustomConstructor() {
        List<ProxyInfo> customProxies = new ArrayList<>();
        customProxies.add(new ProxyInfo("custom1.com", 1111));
        customProxies.add(new ProxyInfo("custom2.com", 2222));

        DefaultProxyProvider provider = new DefaultProxyProvider(customProxies);
        List<ProxyInfo> proxies = provider.getAvailableProxies();
        assertEquals(2, proxies.size());
        assertEquals("custom1.com", proxies.get(0).getHost());
        assertEquals("custom2.com", proxies.get(1).getHost());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetAvailableProxiesReturnsUnmodifiableList() {
        DefaultProxyProvider provider = new DefaultProxyProvider();
        List<ProxyInfo> proxies = provider.getAvailableProxies();
        proxies.add(new ProxyInfo("hack.com", 9999)); // Should throw
    }

    @Test
    public void testAddProxy() {
        DefaultProxyProvider provider = new DefaultProxyProvider();
        int initialSize = provider.getAvailableProxies().size();

        ProxyInfo newProxy = new ProxyInfo("new.proxy.com", 3333);
        provider.addProxy(newProxy);

        assertEquals(initialSize + 1, provider.getAvailableProxies().size());
    }

    @Test
    public void testAddDuplicateProxy() {
        DefaultProxyProvider provider = new DefaultProxyProvider();
        ProxyInfo proxy = new ProxyInfo("dup.proxy.com", 4444);
        provider.addProxy(proxy);
        int sizeAfterFirst = provider.getAvailableProxies().size();

        provider.addProxy(proxy); // Duplicate
        assertEquals("Duplicate proxy should not be added", sizeAfterFirst, provider.getAvailableProxies().size());
    }

    @Test
    public void testAddNullProxy() {
        DefaultProxyProvider provider = new DefaultProxyProvider();
        int initialSize = provider.getAvailableProxies().size();
        provider.addProxy(null);
        assertEquals("Null proxy should not be added", initialSize, provider.getAvailableProxies().size());
    }

    @Test
    public void testRemoveProxy() {
        DefaultProxyProvider provider = new DefaultProxyProvider();
        ProxyInfo proxy = new ProxyInfo("removable.com", 5555);
        provider.addProxy(proxy);
        int sizeAfterAdd = provider.getAvailableProxies().size();

        boolean removed = provider.removeProxy(proxy);
        assertTrue(removed);
        assertEquals(sizeAfterAdd - 1, provider.getAvailableProxies().size());
    }

    @Test
    public void testRemoveNonExistentProxy() {
        DefaultProxyProvider provider = new DefaultProxyProvider();
        ProxyInfo proxy = new ProxyInfo("nonexistent.com", 6666);
        boolean removed = provider.removeProxy(proxy);
        assertFalse(removed);
    }

    @Test
    public void testClearProxies() {
        DefaultProxyProvider provider = new DefaultProxyProvider();
        assertFalse(provider.getAvailableProxies().isEmpty());

        provider.clearProxies();
        assertTrue("All proxies should be cleared", provider.getAvailableProxies().isEmpty());
    }

    @Test
    public void testImplementsProxyProvider() {
        DefaultProxyProvider provider = new DefaultProxyProvider();
        assertTrue("Should implement ProxyProvider", provider instanceof ProxyProvider);
    }
}

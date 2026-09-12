package ir.M_Rostamzadeh.Tahrim_Gozar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**Unit tests for TahrimGozar singleton and proxy state management.
 * Note: Tests that require Android Context (init, setupAutomaticProxy) are excluded
 * as they need instrumented tests.*/
public class TahrimGozarTest {

    @Before
    public void setUp() throws Exception {
        // Reset singleton before each test
        resetSingleton();
    }

    @After
    public void tearDown() throws Exception {
        resetSingleton();
        // Reset Constants state
        Constants.isDebugMode = false;
        Constants.useInWebView = true;
    }

    /**Reset the TahrimGozar singleton using reflection*/
    private void resetSingleton() throws Exception {
        Field field = TahrimGozar.class.getDeclaredField("tahrimGozar");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    public void testSingletonNotNull() {
        TahrimGozar instance = TahrimGozar.getInstance();
        assertNotNull("getInstance should never return null", instance);
    }

    @Test
    public void testSingletonReturnsSameInstance() {
        TahrimGozar instance1 = TahrimGozar.getInstance();
        TahrimGozar instance2 = TahrimGozar.getInstance();
        assertSame("getInstance should return same instance", instance1, instance2);
    }

    @Test
    public void testSingletonThreadSafety() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicReference<TahrimGozar>[] instances = new AtomicReference[threadCount];

        for (int i = 0; i < threadCount; i++) {
            instances[i] = new AtomicReference<>();
            final int index = i;
            new Thread(() -> {
                instances[index].set(TahrimGozar.getInstance());
                latch.countDown();
            }).start();
        }

        latch.await();

        // All threads should get the same instance
        TahrimGozar firstInstance = instances[0].get();
        for (int i = 1; i < threadCount; i++) {
            assertSame("Thread " + i + " got different instance", firstInstance, instances[i].get());
        }
    }

    @Test
    public void testIsUseProxyDefaultFalse() {
        assertFalse("isUseProxy should be false by default", TahrimGozar.getInstance().isUseProxy());
    }

    @Test
    public void testSetDebugMode() {
        TahrimGozar.getInstance().setIsDebugMode(true);
        assertTrue(Constants.isDebugMode);

        TahrimGozar.getInstance().setIsDebugMode(false);
        assertFalse(Constants.isDebugMode);
    }

    @Test
    public void testSetUseInWebView() {
        TahrimGozar.getInstance().setUseInWebView(false);
        assertFalse(TahrimGozar.getInstance().canUseInWebView());

        TahrimGozar.getInstance().setUseInWebView(true);
        assertTrue(TahrimGozar.getInstance().canUseInWebView());
    }

    @Test
    public void testGetCurrentProxyDefaultNull() {
        assertNull("Current proxy should be null by default",
                TahrimGozar.getInstance().getCurrentProxy());
    }

    @Test
    public void testSetProxyProvider() {
        ProxyProvider provider = new DefaultProxyProvider();
        TahrimGozar.getInstance().setProxyProvider(provider);
        assertSame(provider, TahrimGozar.getInstance().getProxyProvider());
    }

    @Test
    public void testSetListener() {
        TahrimGozarListener listener = new TahrimGozarListener() {
            @Override public void onProxyConnected(ProxyInfo proxyInfo) {}
            @Override public void onProxyFailed(ProxyInfo proxyInfo, Exception error) {}
            @Override public void onProxyChanged(ProxyInfo oldProxy, ProxyInfo newProxy) {}
            @Override public void onProxyRemoved() {}
        };

        TahrimGozar.getInstance().setListener(listener);
        assertSame(listener, TahrimGozar.getInstance().getListener());
    }

    @Test
    public void testSetNullListener() {
        TahrimGozar.getInstance().setListener(null);
        assertNull(TahrimGozar.getInstance().getListener());
    }

    @Test
    public void testGetProxyProviderDefaultNull() {
        // Before init, provider should be null
        assertNull(TahrimGozar.getInstance().getProxyProvider());
    }
}

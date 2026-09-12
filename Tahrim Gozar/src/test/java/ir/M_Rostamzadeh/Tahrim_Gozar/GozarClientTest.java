package ir.M_Rostamzadeh.Tahrim_Gozar;

import android.webkit.WebView;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for GozarClient and GozarProxySetter.
 * Ensures no ClassCastException occurs when standard WebView is used.
 */
public class GozarClientTest {

    @Test
    public void testGozarClientCreation() {
        GozarClient clientTrue = new GozarClient(true);
        assertNotNull(clientTrue);
        assertTrue(Constants.canOverrideAllLinks);
        assertTrue(Constants.useInWebView);

        GozarClient clientFalse = new GozarClient(false);
        assertNotNull(clientFalse);
        assertFalse(Constants.canOverrideAllLinks);
    }

    @Test
    public void testGozarProxySetterInstanceCreation() {
        GozarProxySetter setter1 = GozarProxySetter.getInstance("https://example.com");
        assertNotNull(setter1);

        // Verify that passing null or WebView does not throw ClassCastException
        GozarProxySetter setter2 = GozarProxySetter.getInstance("https://example.com", (WebView) null, 403);
        assertNotNull(setter2);
    }

    @Test
    public void testGozarProxySetterBackwardCompatibleOverload() {
        // GozarWebView overload
        GozarProxySetter setter = GozarProxySetter.getInstance("https://example.com", (GozarWebView) null, 403);
        assertNotNull(setter);
    }
}

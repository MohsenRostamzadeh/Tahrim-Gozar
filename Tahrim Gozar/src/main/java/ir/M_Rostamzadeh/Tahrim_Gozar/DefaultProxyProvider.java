package ir.M_Rostamzadeh.Tahrim_Gozar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**Default proxy provider with built-in anti-sanctions proxies.
 * Uses the hardcoded proxy list from Constants as a starting point.
 * Implement {@link ProxyProvider} to supply your own proxy list.*/
public class DefaultProxyProvider implements ProxyProvider {

    private final List<ProxyInfo> proxies;

    /**Create default proxy provider with built-in proxy list*/
    public DefaultProxyProvider() {
        proxies = new ArrayList<>();
        proxies.add(new ProxyInfo(Constants.PROXY_HOST_1, Constants.PROXY_PORT_1));
    }

    /**Create default proxy provider with custom proxy list
     * @param customProxies List of proxy servers to use*/
    public DefaultProxyProvider(List<ProxyInfo> customProxies) {
        proxies = new ArrayList<>(customProxies);
    }

    @Override
    public List<ProxyInfo> getAvailableProxies() {
        return Collections.unmodifiableList(proxies);
    }

    /**Add a proxy to the list
     * @param proxyInfo Proxy to add*/
    public void addProxy(ProxyInfo proxyInfo) {
        if (proxyInfo != null && !proxies.contains(proxyInfo)) {
            proxies.add(proxyInfo);
        }
    }

    /**Remove a proxy from the list
     * @param proxyInfo Proxy to remove
     * @return true if the proxy was removed*/
    public boolean removeProxy(ProxyInfo proxyInfo) {
        return proxies.remove(proxyInfo);
    }

    /**Clear all proxies from the list*/
    public void clearProxies() {
        proxies.clear();
    }
}

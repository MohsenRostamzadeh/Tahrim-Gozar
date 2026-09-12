package ir.M_Rostamzadeh.Tahrim_Gozar;

/**Listener interface for proxy status events.
 * Implement this interface to receive notifications about proxy changes.*/
public interface TahrimGozarListener {

    /**Called when proxy is successfully connected
     * @param proxyInfo The connected proxy information*/
    void onProxyConnected(ProxyInfo proxyInfo);

    /**Called when proxy connection fails
     * @param proxyInfo The proxy that failed
     * @param error The exception that occurred*/
    void onProxyFailed(ProxyInfo proxyInfo, Exception error);

    /**Called when active proxy changes
     * @param oldProxy The previous proxy (null if none)
     * @param newProxy The new proxy*/
    void onProxyChanged(ProxyInfo oldProxy, ProxyInfo newProxy);

    /**Called when proxy is removed*/
    void onProxyRemoved();
}

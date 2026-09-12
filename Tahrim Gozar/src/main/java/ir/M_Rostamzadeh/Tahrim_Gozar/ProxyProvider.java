package ir.M_Rostamzadeh.Tahrim_Gozar;

import java.util.List;

/**Interface for providing proxy server information.
 * Implement this interface to supply custom proxy lists,
 * for example from a remote API or local configuration.*/
public interface ProxyProvider {

    /**Get list of available proxy servers
     * @return List of available ProxyInfo objects*/
    List<ProxyInfo> getAvailableProxies();
}

package ir.M_Rostamzadeh.Tahrim_Gozar;

import java.util.Objects;

/**Model class representing proxy server information.
 * Supports HTTP, HTTPS, SOCKS4, and SOCKS5 proxy types
 * with optional authentication.*/
public class ProxyInfo {

    /**Proxy type enumeration*/
    public enum ProxyType {
        HTTP, HTTPS, SOCKS4, SOCKS5
    }

    int port;
    String host;
    ProxyType type;
    String username;
    String password;

    /**Create proxy info with HTTP type (backward compatible)
     * @param host Proxy host
     * @param port Proxy port*/
    public ProxyInfo(String host, int port) {
        this(host, port, ProxyType.HTTP);
    }

    /**Create proxy info with specified type
     * @param host Proxy host
     * @param port Proxy port
     * @param type Proxy type (HTTP, HTTPS, SOCKS4, SOCKS5)*/
    public ProxyInfo(String host, int port, ProxyType type) {
        this(host, port, type, null, null);
    }

    /**Create proxy info with authentication
     * @param host Proxy host
     * @param port Proxy port
     * @param type Proxy type (HTTP, HTTPS, SOCKS4, SOCKS5)
     * @param username Proxy username for authentication
     * @param password Proxy password for authentication*/
    public ProxyInfo(String host, int port, ProxyType type, String username, String password) {
        this.host = host;
        this.port = port;
        this.type = type != null ? type : ProxyType.HTTP;
        this.username = username;
        this.password = password;
    }

    /**Get proxy port
     * @return Proxy port number*/
    public int getPort() {
        return port;
    }

    /**Get proxy host
     * @return Proxy host address*/
    public String getHost() {
        return host;
    }

    /**Get proxy type
     * @return Proxy type (HTTP, HTTPS, SOCKS4, SOCKS5)*/
    public ProxyType getType() {
        return type;
    }

    /**Get proxy username
     * @return Proxy username, or null if no authentication*/
    public String getUsername() {
        return username;
    }

    /**Get proxy password
     * @return Proxy password, or null if no authentication*/
    public String getPassword() {
        return password;
    }

    /**Check if this proxy has authentication credentials
     * @return true if both username and password are set*/
    public boolean hasAuthentication() {
        return username != null && !username.isEmpty()
                && password != null && !password.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProxyInfo proxyInfo = (ProxyInfo) o;
        return port == proxyInfo.port
                && Objects.equals(host, proxyInfo.host)
                && type == proxyInfo.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, type);
    }

    /**Returns proxy as URI string (e.g. "http://host:port")
     * @return Proxy URI string*/
    @Override
    public String toString() {
        return type.name().toLowerCase() + "://" + host + ":" + port;
    }
}
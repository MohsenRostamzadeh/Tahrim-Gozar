package ir.M_Rostamzadeh.Tahrim_Gozar;

public class ProxyInfo {

    int port;
    String host;

    public ProxyInfo(String host,int port) {
        this.port = port;
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

}
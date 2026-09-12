package ir.M_Rostamzadeh.Tahrim_Gozar.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Structured data model representing an in-app tunnel or DNS configuration.
 * Encapsulates all protocol-specific parameters for proxies, V2Ray/Xray, and DNS servers.
 * <p>
 * مدل ساختاریافته مشخصات یک کانفیگ تونل یا دی‌ان‌اس ضدتحریم
 */
public class TunnelConfig {

    private final ConfigType type;
    private final String rawConfig;
    private final String name;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String uuid;
    private final String security;
    private final String sni;
    private final String network;
    private final String path;
    private final String publicKey;
    private final String shortId;
    private final String fingerprint;
    private final String alpn;
    private final List<String> dnsServers;
    private final String dohUrl;
    private final String rawJson;
    private final Map<String, String> extraParams;

    private TunnelConfig(Builder builder) {
        this.type = builder.type;
        this.rawConfig = builder.rawConfig;
        this.name = builder.name;
        this.host = builder.host;
        this.port = builder.port;
        this.username = builder.username;
        this.password = builder.password;
        this.uuid = builder.uuid;
        this.security = builder.security;
        this.sni = builder.sni;
        this.network = builder.network;
        this.path = builder.path;
        this.publicKey = builder.publicKey;
        this.shortId = builder.shortId;
        this.fingerprint = builder.fingerprint;
        this.alpn = builder.alpn;
        this.dnsServers = builder.dnsServers != null
                ? Collections.unmodifiableList(new ArrayList<>(builder.dnsServers))
                : Collections.emptyList();
        this.dohUrl = builder.dohUrl;
        this.rawJson = builder.rawJson;
        this.extraParams = builder.extraParams != null
                ? Collections.unmodifiableMap(new HashMap<>(builder.extraParams))
                : Collections.emptyMap();
    }

    public ConfigType getType() {
        return type;
    }

    public String getRawConfig() {
        return rawConfig;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getUuid() {
        return uuid;
    }

    public String getSecurity() {
        return security;
    }

    public String getSni() {
        return sni;
    }

    public String getNetwork() {
        return network;
    }

    public String getPath() {
        return path;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getShortId() {
        return shortId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getAlpn() {
        return alpn;
    }

    public List<String> getDnsServers() {
        return dnsServers;
    }

    public String getDohUrl() {
        return dohUrl;
    }

    public String getRawJson() {
        return rawJson;
    }

    public Map<String, String> getExtraParams() {
        return extraParams;
    }

    public boolean isDnsConfig() {
        return type == ConfigType.DNS_PRESET || type == ConfigType.DNS_CUSTOM
                || type == ConfigType.DNS_OVER_HTTPS || type == ConfigType.DNS_OVER_TLS;
    }

    public boolean isV2RayConfig() {
        return type == ConfigType.V2RAY_VLESS || type == ConfigType.V2RAY_VMESS
                || type == ConfigType.V2RAY_TROJAN || type == ConfigType.SHADOWSOCKS
                || type == ConfigType.SHADOWSOCKSR
                || type == ConfigType.HYSTERIA2 || type == ConfigType.TUIC
                || type == ConfigType.WIREGUARD || type == ConfigType.RAW_JSON;
    }

    public boolean isStandardProxy() {
        return type == ConfigType.PROXY_HTTP || type == ConfigType.PROXY_HTTPS
                || type == ConfigType.PROXY_SOCKS4 || type == ConfigType.PROXY_SOCKS5;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TunnelConfig that = (TunnelConfig) o;
        return port == that.port &&
                type == that.type &&
                Objects.equals(host, that.host) &&
                Objects.equals(uuid, that.uuid) &&
                Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, host, port, uuid, password);
    }

    @Override
    public String toString() {
        return "TunnelConfig{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }

    public static class Builder {
        private ConfigType type;
        private String rawConfig;
        private String name;
        private String host;
        private int port;
        private String username;
        private String password;
        private String uuid;
        private String security;
        private String sni;
        private String network;
        private String path;
        private String publicKey;
        private String shortId;
        private String fingerprint;
        private String alpn;
        private List<String> dnsServers;
        private String dohUrl;
        private String rawJson;
        private Map<String, String> extraParams;

        public Builder(ConfigType type) {
            this.type = type;
        }

        public Builder rawConfig(String rawConfig) {
            this.rawConfig = rawConfig;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder security(String security) {
            this.security = security;
            return this;
        }

        public Builder sni(String sni) {
            this.sni = sni;
            return this;
        }

        public Builder network(String network) {
            this.network = network;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder publicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public Builder shortId(String shortId) {
            this.shortId = shortId;
            return this;
        }

        public Builder fingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
            return this;
        }

        public Builder alpn(String alpn) {
            this.alpn = alpn;
            return this;
        }

        public Builder dnsServers(List<String> dnsServers) {
            this.dnsServers = dnsServers;
            return this;
        }

        public Builder dohUrl(String dohUrl) {
            this.dohUrl = dohUrl;
            return this;
        }

        public Builder rawJson(String rawJson) {
            this.rawJson = rawJson;
            return this;
        }

        public Builder extraParams(Map<String, String> extraParams) {
            this.extraParams = extraParams;
            return this;
        }

        public TunnelConfig build() {
            return new TunnelConfig(this);
        }
    }
}

package ir.M_Rostamzadeh.Tahrim_Gozar.dns;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dedicated DNS Resolver for anti-sanction routing supporting UDP DNS and DoH (DNS-over-HTTPS).
 * Features in-memory caching, Concurrent Happy Eyeballs DNS race, and seamless OkHttp integration.
 * <p>
 * رزولور اختصاصی DNS تحریم‌گذر با پشتیبانی از پروتکل‌های UDP DNS و DoH (DNS-over-HTTPS)
 * با قابلیت کش کردن درون‌حافظه‌ای، استعلام همزمان و مسابقه‌ای (Happy Eyeballs) و سازگاری کامل با OkHttp
 */
public class TahrimDns {

    private static final int DNS_PORT = 53;
    private static final int TIMEOUT_MS = 2500;
    private static final long CACHE_TTL_MS = 60 * 1000; // 1-minute default cache / ۱ دقیقه کش پیش‌فرض

    private final List<String> dnsServers;
    private final String dohUrl;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final AtomicBoolean isRaceEnabled = new AtomicBoolean(false);

    public TahrimDns(List<String> dnsServers) {
        this(dnsServers, null);
    }

    public TahrimDns(List<String> dnsServers, String dohUrl) {
        this.dnsServers = dnsServers != null ? new ArrayList<>(dnsServers) : new ArrayList<>(AntiSanctionDns.SHECAN);
        this.dohUrl = dohUrl;
    }

    /**
     * Enables or disables concurrent Happy Eyeballs DNS racing across servers.
     * فعال یا غیرفعال‌سازی استعلام همزمان مسابقه‌ای دی‌ان‌اس جهت حداقل‌سازی پینگ.
     */
    public TahrimDns setRaceEnabled(boolean enabled) {
        this.isRaceEnabled.set(enabled);
        return this;
    }

    public boolean isRaceEnabled() {
        return isRaceEnabled.get();
    }

    public static TahrimDns shecan() {
        return new TahrimDns(AntiSanctionDns.SHECAN, AntiSanctionDns.SHECAN_DOH);
    }

    public static TahrimDns electro() {
        return new TahrimDns(AntiSanctionDns.ELECTRO, AntiSanctionDns.ELECTRO_DOH);
    }

    public static TahrimDns radar() {
        return new TahrimDns(AntiSanctionDns.RADAR, null);
    }

    public static TahrimDns dns403() {
        return new TahrimDns(AntiSanctionDns.DNS_403, AntiSanctionDns.DNS_403_DOH);
    }

    /**
     * Resolves a hostname to a list of IP addresses.
     * <p>
     * رزولو کردن آدرس نام دامنه به لیست IPها
     *
     * @param hostname Hostname to resolve (e.g. api.github.com)
     * @return List of resolved IP addresses
     * @throws UnknownHostException If resolution fails
     */
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        if (hostname == null || hostname.trim().isEmpty()) {
            throw new UnknownHostException("Hostname cannot be null or empty");
        }

        // اگر خود ورودی یک IP است مستقیم بازگردانده شود
        if (isIpAddress(hostname)) {
            return Collections.singletonList(InetAddress.getByName(hostname));
        }

        // بررسی کش
        CacheEntry entry = cache.get(hostname);
        if (entry != null && !entry.isExpired()) {
            return entry.addresses;
        }

        List<InetAddress> result = null;

        // ۱. تلاش با DoH در صورت وجود
        if (dohUrl != null && !dohUrl.isEmpty()) {
            try {
                result = queryDoH(hostname, dohUrl);
            } catch (Exception ignored) {
                // اگر DoH به هر دلیلی خطا داد، به UDP فالبک می‌کنیم
            }
        }

        // ۲. تلاش با سرورهای UDP دی‌ان‌اس ضدتحریم (مسابقه‌ای یا ترتیبی)
        if (result == null || result.isEmpty()) {
            if (isRaceEnabled.get() && dnsServers.size() > 1) {
                result = queryUdpRace(hostname, dnsServers);
            }
            if (result == null || result.isEmpty()) {
                for (String server : dnsServers) {
                    try {
                        result = queryUdp(hostname, server);
                        if (result != null && !result.isEmpty()) {
                            break;
                        }
                    } catch (Exception ignored) {
                        // ادامه به سرور بعدی
                    }
                }
            }
        }

        // ۳. فالبک به DNS سیستم عامل دستگاه در صورت قطع بودن سرورهای اختصاصی
        if (result == null || result.isEmpty()) {
            result = Arrays.asList(InetAddress.getAllByName(hostname));
        }

        if (result.isEmpty()) {
            throw new UnknownHostException("Unable to resolve host: " + hostname);
        }

        cache.put(hostname, new CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS));
        return result;
    }

    /**
     * استعلام مستقیم رکورد A از سرور دی‌ان‌اس با پکت استاندارد RFC 1035 از طریق UDP
     */
    public List<InetAddress> queryUdp(String hostname, String serverIp) throws Exception {
        byte[] queryPacket = buildDnsQueryPacket(hostname);
        InetAddress serverAddress = InetAddress.getByName(serverIp);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            DatagramPacket sendPacket = new DatagramPacket(queryPacket, queryPacket.length, serverAddress, DNS_PORT);
            socket.send(sendPacket);

            byte[] buffer = new byte[512];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(receivePacket);

            return parseDnsResponse(buffer, receivePacket.getLength(), hostname);
        }
    }

    /**
     * Happy Eyeballs DNS race: Queries multiple DNS servers concurrently and returns the fastest response.
     * استعلام همزمان و مسابقه‌ای (Happy Eyeballs) از چند سرور دی‌ان‌اس و بازگرداندن سریع‌ترین پاسخ معتبر.
     */
    public List<InetAddress> queryUdpRace(String hostname, List<String> servers) {
        if (servers == null || servers.isEmpty()) return null;
        if (servers.size() == 1) {
            try {
                return queryUdp(hostname, servers.get(0));
            } catch (Exception e) {
                return null;
            }
        }

        final int count = servers.size();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<List<InetAddress>> winner = new AtomicReference<>(null);

        ExecutorService racePool = Executors.newFixedThreadPool(count, r -> {
            Thread t = new Thread(r, "TahrimGozar-DnsRace");
            t.setDaemon(true);
            return t;
        });

        for (String s : servers) {
            final String server = s;
            racePool.execute(() -> {
                try {
                    List<InetAddress> res = queryUdp(hostname, server);
                    if (res != null && !res.isEmpty()) {
                        if (winner.compareAndSet(null, res)) {
                            latch.countDown();
                        }
                    }
                } catch (Exception ignored) {
                }
            });
        }

        try {
            latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        } finally {
            racePool.shutdownNow();
        }

        return winner.get();
    }

    /**
     * استعلام DoH (DNS-over-HTTPS) با فرمت پیام استاندارد DNS wireformat
     */
    private List<InetAddress> queryDoH(String hostname, String dohEndpoint) throws Exception {
        byte[] queryPacket = buildDnsQueryPacket(hostname);
        URL url = new URL(dohEndpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/dns-message");
        conn.setRequestProperty("Accept", "application/dns-message");
        conn.setDoOutput(true);

        try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
            out.write(queryPacket);
            out.flush();
        }

        if (conn.getResponseCode() == 200) {
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
                byte[] temp = new byte[512];
                int read;
                while ((read = in.read(temp)) != -1) {
                    bout.write(temp, 0, read);
                }
                byte[] responseBytes = bout.toByteArray();
                return parseDnsResponse(responseBytes, responseBytes.length, hostname);
            }
        }
        return null;
    }

    private byte[] buildDnsQueryPacket(String hostname) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Header
        dos.writeShort(random.nextInt(0xFFFF)); // Transaction ID
        dos.writeShort(0x0100);                 // Flags: Standard query, recursion desired
        dos.writeShort(1);                      // Questions: 1
        dos.writeShort(0);                      // Answer RRs: 0
        dos.writeShort(0);                      // Authority RRs: 0
        dos.writeShort(0);                      // Additional RRs: 0

        // Question: Name labels
        String[] parts = hostname.split("\\.");
        for (String part : parts) {
            byte[] bytes = part.getBytes(StandardCharsets.US_ASCII);
            dos.writeByte(bytes.length);
            dos.write(bytes);
        }
        dos.writeByte(0); // Root label null terminator

        dos.writeShort(1); // Type: A (IPv4)
        dos.writeShort(1); // Class: IN (Internet)

        dos.flush();
        return baos.toByteArray();
    }

    private List<InetAddress> parseDnsResponse(byte[] data, int length, String originalHost) throws Exception {
        if (length < 12) return Collections.emptyList();

        DataInputStream dis = new DataInputStream(new java.io.ByteArrayInputStream(data, 0, length));
        dis.readShort(); // ID
        int flags = dis.readUnsignedShort();
        int rcode = flags & 0x000F;
        if (rcode != 0) {
            return Collections.emptyList();
        }

        int qdcount = dis.readUnsignedShort();
        int ancount = dis.readUnsignedShort();
        dis.readUnsignedShort(); // nscount
        dis.readUnsignedShort(); // arcount

        // رد شدن از بخش سوالات
        for (int i = 0; i < qdcount; i++) {
            skipDomainName(dis);
            dis.readShort(); // qtype
            dis.readShort(); // qclass
        }

        List<InetAddress> addresses = new ArrayList<>();

        // خواندن پاسخ‌ها
        for (int i = 0; i < ancount; i++) {
            skipDomainName(dis);
            int type = dis.readUnsignedShort();
            dis.readUnsignedShort(); // class
            dis.readInt();          // ttl
            int rdlength = dis.readUnsignedShort();

            if (type == 1 && rdlength == 4) {
                // Type 1 = A record (IPv4)
                byte[] ip = new byte[4];
                dis.readFully(ip);
                addresses.add(InetAddress.getByAddress(originalHost, ip));
            } else if (type == 28 && rdlength == 16) {
                // Type 28 = AAAA record (IPv6)
                byte[] ip = new byte[16];
                dis.readFully(ip);
                addresses.add(InetAddress.getByAddress(originalHost, ip));
            } else {
                dis.skipBytes(rdlength);
            }
        }

        return addresses;
    }

    private void skipDomainName(DataInputStream dis) throws Exception {
        while (true) {
            int len = dis.readUnsignedByte();
            if (len == 0) {
                break;
            } else if ((len & 0xC0) == 0xC0) {
                // Pointer in message compression
                dis.readUnsignedByte();
                break;
            } else {
                dis.skipBytes(len);
            }
        }
    }

    private boolean isIpAddress(String host) {
        if (host == null || host.isEmpty()) return false;
        if (host.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) return true;
        String cleaned = (host.startsWith("[") && host.endsWith("]")) ? host.substring(1, host.length() - 1) : host;
        return cleaned.contains(":") && cleaned.split(":").length >= 3;
    }

    public List<String> getDnsServers() {
        return Collections.unmodifiableList(dnsServers);
    }

    public String getDohUrl() {
        return dohUrl;
    }

    public void clearCache() {
        cache.clear();
    }

    private static class CacheEntry {
        final List<InetAddress> addresses;
        final long expireTime;

        CacheEntry(List<InetAddress> addresses, long expireTime) {
            this.addresses = addresses;
            this.expireTime = expireTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}

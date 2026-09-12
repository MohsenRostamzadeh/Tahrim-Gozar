package ir.M_Rostamzadeh.Tahrim_Gozar.dns;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import ir.M_Rostamzadeh.Tahrim_Gozar.traffic.TrafficStatsTracker;

/**
 * Ultra-lightweight local forwarder and proxy to route WebView and socket traffic through anti-sanction DNS
 * (such as Shecan/Electro) without requiring external proxy servers or full-device VPN.
 * <p>
 * فورواردر و پروکسی لوکال فوق‌سبک جهت هدایت ترافیک وب‌ویو و سوکت‌ها از طریق دی‌ان‌اس ضدتحریم (مانند شکن/الکترو)
 * بدون نیاز به سرور پروکسی خارجی یا VPN کل دستگاه
 */
public class LocalDnsForwarder {

    private final TahrimDns tahrimDns;
    private final TrafficStatsTracker trafficTracker;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private int boundPort = 0;

    public LocalDnsForwarder(TahrimDns tahrimDns) {
        this(tahrimDns, null);
    }

    public LocalDnsForwarder(TahrimDns tahrimDns, TrafficStatsTracker trafficTracker) {
        this.tahrimDns = tahrimDns;
        this.trafficTracker = trafficTracker;
    }

    /**
     * Starts the local forwarding server on an available random ephemeral port.
     * راه‌اندازی سرور فوروارد محلی روی یک پورت رندوم آزاد.
     */
    public synchronized int start() throws IOException {
        if (isRunning.get()) {
            return boundPort;
        }

        serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        boundPort = serverSocket.getLocalPort();
        isRunning.set(true);

        threadPool = Executors.newCachedThreadPool();
        threadPool.execute(this::listenLoop);

        return boundPort;
    }

    /**
     * Stops the local forwarding server and terminates all active tunnels.
     * توقف سرور فوروارد محلی و بستن تمام تونل‌های فعال.
     */
    public synchronized void stop() {
        if (!isRunning.get()) return;
        isRunning.set(false);

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }

        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdownNow();
        }
        boundPort = 0;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public int getPort() {
        return boundPort;
    }

    public int getBoundPort() {
        return boundPort;
    }

    private void listenLoop() {
        while (isRunning.get() && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                threadPool.execute(() -> handleClient(client));
            } catch (IOException e) {
                if (!isRunning.get()) {
                    break;
                }
            }
        }
    }

    private void handleClient(Socket client) {
        try (Socket c = client) {
            c.setSoTimeout(30000);
            InputStream clientIn = c.getInputStream();
            OutputStream clientOut = c.getOutputStream();

            String initialLine = readAsciiLine(clientIn);
            if (initialLine == null || initialLine.trim().isEmpty()) {
                return;
            }

            String[] parts = initialLine.split(" ");
            if (parts.length < 2) return;

            String method = parts[0];
            String destination = parts[1];

            if ("CONNECT".equalsIgnoreCase(method)) {
                // HTTPS Tunneling: CONNECT host:port HTTP/1.1
                handleConnect(clientIn, clientOut, destination);
            } else {
                // Direct HTTP Request
                handleHttp(clientIn, clientOut, method, destination);
            }
        } catch (Exception ignored) {
            // بستن اتصال
        }
    }

    private void handleConnect(InputStream clientIn, OutputStream clientOut, String destination) throws Exception {
        // خواندن باقی هدرها تا رسیدن به خط خالی مستقیماً بدون بافر کردن بایت‌های بعدی TLS
        String line;
        while ((line = readAsciiLine(clientIn)) != null && !line.isEmpty()) {
            // Skip headers
        }

        String host = destination;
        int port = 443;
        if (destination.contains(":")) {
            String[] hp = destination.split(":");
            host = hp[0];
            port = Integer.parseInt(hp[1]);
        }

        // رزولو با دی‌ان‌اس ضدتحریم تحریم‌گذر
        List<InetAddress> resolved = tahrimDns.lookup(host);
        InetAddress targetIp = resolved.get(0);

        Socket targetSocket = new Socket(targetIp, port);
        targetSocket.setSoTimeout(30000);

        // ارسال پاسخ تایید به وب‌ویو یا کلاینت
        clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
        clientOut.flush();

        // پایپ دوطرفه داده‌ها: بایت اول TLS دست نخورده باقی مانده است
        pipeSockets(clientIn, clientOut, targetSocket);
    }

    private void handleHttp(InputStream clientIn, OutputStream clientOut, String method, String destination) throws Exception {
        // برای درخواست‌های معمولی HTTP، آدرس هاست از URI یا هدر Host استخراج می‌شود
        String host = "";
        int port = 80;

        if (destination.startsWith("http://")) {
            String withoutProto = destination.substring(7);
            int slashIdx = withoutProto.indexOf('/');
            String hostPort = (slashIdx != -1) ? withoutProto.substring(0, slashIdx) : withoutProto;
            if (hostPort.contains(":")) {
                String[] hp = hostPort.split(":");
                host = hp[0];
                port = Integer.parseInt(hp[1]);
            } else {
                host = hostPort;
            }
        }

        List<InetAddress> resolved = tahrimDns.lookup(host);
        InetAddress targetIp = resolved.get(0);

        Socket targetSocket = new Socket(targetIp, port);
        targetSocket.setSoTimeout(30000);

        // ارسال خط اول و هدرها به سرور مقصد
        OutputStream targetOut = targetSocket.getOutputStream();
        targetOut.write((method + " " + destination + " HTTP/1.1\r\n").getBytes(StandardCharsets.US_ASCII));
        String line;
        while ((line = readAsciiLine(clientIn)) != null && !line.isEmpty()) {
            targetOut.write((line + "\r\n").getBytes(StandardCharsets.US_ASCII));
        }
        targetOut.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        targetOut.flush();

        pipeSockets(clientIn, clientOut, targetSocket);
    }

    private void pipeSockets(InputStream clientIn, OutputStream clientOut, Socket target) {
        try (Socket targetSocket = target) {
            InputStream targetIn = targetSocket.getInputStream();
            OutputStream targetOut = targetSocket.getOutputStream();

            Thread t1 = new Thread(() -> {
                forwardStream(clientIn, targetOut, false);
                try {
                    targetSocket.shutdownOutput();
                } catch (Exception ignored) {
                }
            });
            Thread t2 = new Thread(() -> {
                forwardStream(targetIn, clientOut, true);
                try {
                    targetSocket.close();
                } catch (Exception ignored) {
                }
            });

            t1.start();
            t2.start();

            t1.join();
            t2.join();
        } catch (Exception ignored) {
        }
    }

    private void forwardStream(InputStream in, OutputStream out, boolean isDownload) {
        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                out.flush();
                if (trafficTracker != null) {
                    if (isDownload) {
                        trafficTracker.recordRx(read);
                    } else {
                        trafficTracker.recordTx(read);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Reads a line of ASCII characters directly from the InputStream without buffering ahead.
     * Prevents buffering any TLS payload bytes (e.g. ClientHello) that follow HTTP CONNECT headers.
     * 
     * خواندن یک خط اسکی مستقیماً از استریم بدون بافر کردن بایت‌های بعدی؛
     * مانع از بلعیده شدن بایت‌های دست‌تکانی TLS (مثل ClientHello) بعد از هدرهای CONNECT می‌شود.
     */
    private static String readAsciiLine(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                baos.write(b);
            }
        }
        if (b == -1 && baos.size() == 0) {
            return null;
        }
        return baos.toString("US-ASCII");
    }
}

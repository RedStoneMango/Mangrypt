package io.github.redstonemango.mangrypt.back;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Because {@link javafx.scene.media.MediaPlayer} and {@link javafx.scene.media.Media} require a URL, we have to stream
 * the byte[] using a secure local server to avoid creating unprotected, sensitive temp-files on the file system for
 * playback.
 */
public class SecureInMemoryMediaServer {

    private final byte[] mediaBytes;
    private final String mimeType;
    private final int requestedPort;
    private final String path;

    private int actualPort;
    private HttpServer server;
    private String accessToken;

    public SecureInMemoryMediaServer(byte[] mediaBytes, String mimeType, int port, String path) {
        this.mediaBytes = mediaBytes;
        this.mimeType = mimeType;
        this.requestedPort = port;
        this.path = path.startsWith("/") ? path : "/" + path;
        this.accessToken = generateSecureToken();
    }

    public void start() throws IOException {
        InetSocketAddress socketAddress = getLocalSocketAddress(requestedPort);
        server = HttpServer.create(socketAddress, 0);
        actualPort = server.getAddress().getPort();

        server.createContext(path, exchange -> {
            URI requestURI = exchange.getRequestURI();
            String query = requestURI.getRawQuery();
            String token = null;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] keyValue = param.split("=", 2);
                    if (keyValue.length == 2 && keyValue[0].equals("token")) {
                        token = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                        break;
                    }
                }
            }

            if (token == null || !token.equals(accessToken)) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            String rangeHeader = exchange.getRequestHeaders().getFirst("Range");
            boolean isPartial = false;

            int totalLength = mediaBytes.length;
            int start = 0;
            int end = totalLength - 1;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                isPartial = true;
                String[] parts = rangeHeader.substring("bytes=".length()).split("-");
                try {
                    start = Integer.parseInt(parts[0]);
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        end = Integer.parseInt(parts[1]);
                    }
                    end = Math.min(end, totalLength - 1);
                    if (start > end || start >= totalLength) {
                        exchange.sendResponseHeaders(416, -1); // Range Not Satisfiable
                        return;
                    }
                } catch (NumberFormatException e) {
                    exchange.sendResponseHeaders(400, -1); // Bad Request
                    return;
                }
            }

            int contentLength = end - start + 1;

            exchange.getResponseHeaders().add("Content-Type", mimeType);
            exchange.getResponseHeaders().add("Accept-Ranges", "bytes");
            exchange.getResponseHeaders().add("Content-Disposition", "inline; filename=\"media.mp3\"");

            // HEAD method (respond with headers only)
            if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                if (isPartial) {
                    exchange.getResponseHeaders().add("Content-Range",
                            String.format("bytes %d-%d/%d", start, end, totalLength));
                    exchange.getResponseHeaders().add("Content-Length", String.valueOf(contentLength));
                    exchange.sendResponseHeaders(206, -1);
                } else {
                    exchange.getResponseHeaders().add("Content-Length", String.valueOf(totalLength));
                    exchange.sendResponseHeaders(200, -1);
                }
                return;
            }

            // GET method (respond with body)
            if (isPartial) {
                exchange.getResponseHeaders().add("Content-Range",
                        String.format("bytes %d-%d/%d", start, end, totalLength));
                exchange.getResponseHeaders().add("Content-Length", String.valueOf(contentLength));
                exchange.sendResponseHeaders(206, contentLength);
            } else {
                exchange.getResponseHeaders().add("Content-Length", String.valueOf(totalLength));
                exchange.sendResponseHeaders(200, totalLength);
            }

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(mediaBytes, start, contentLength);
            }
        });

        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public String getStreamUrl() {
        return "http://127.0.0.1:" + actualPort + path;
    }

    public String getTokenizedUrl() {
        return getStreamUrl() + "?token=" + accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public int getActualPort() {
        return actualPort;
    }

    private InetSocketAddress getLocalSocketAddress(int port) throws IOException {
        if (port != 0) {
            return new InetSocketAddress("127.0.0.1", port);
        } else {
            try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) {
                int freePort = socket.getLocalPort();
                return new InetSocketAddress("127.0.0.1", freePort);
            }
        }
    }

    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
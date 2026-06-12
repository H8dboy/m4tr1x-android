package com.m4tr1x.node.tor;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client HTTP/1.0 minimale sopra il tunnel SOCKS5 di Tor.
 * HTTP/1.0 + Connection: close = niente chunked encoding da decodificare;
 * il body è lo stream del socket fino a EOF (perfetto per streaming media).
 */
public final class TorHttp {

    public static final class Response {
        public int status;
        public String reason = "OK";
        public final Map<String, String> headers = new LinkedHashMap<>();
        public InputStream body;
    }

    private TorHttp() {}

    public static Response request(String method, String urlStr, Map<String, String> reqHeaders,
                                   byte[] body, int timeoutMs) throws IOException {
        URI u = URI.create(urlStr);
        if (!"http".equalsIgnoreCase(u.getScheme())) throw new IOException("Solo http:// via Tor (gli .onion sono già cifrati end-to-end)");
        String host = u.getHost();
        int port = u.getPort() > 0 ? u.getPort() : 80;
        String path = (u.getRawPath() == null || u.getRawPath().isEmpty()) ? "/" : u.getRawPath();
        if (u.getRawQuery() != null) path += "?" + u.getRawQuery();

        Socket s = Socks5.connect(host, port, timeoutMs);
        try {
            OutputStream out = s.getOutputStream();
            StringBuilder req = new StringBuilder();
            req.append(method).append(' ').append(path).append(" HTTP/1.0\r\n");
            req.append("Host: ").append(host).append("\r\n");
            boolean hasContentLength = false;
            if (reqHeaders != null) {
                for (Map.Entry<String, String> e : reqHeaders.entrySet()) {
                    String k = e.getKey();
                    if (k == null || e.getValue() == null) continue;
                    if (k.equalsIgnoreCase("host") || k.equalsIgnoreCase("connection")
                            || k.equalsIgnoreCase("accept-encoding")) continue;
                    if (k.equalsIgnoreCase("content-length")) hasContentLength = true;
                    req.append(k).append(": ").append(e.getValue()).append("\r\n");
                }
            }
            if (body != null && !hasContentLength) req.append("Content-Length: ").append(body.length).append("\r\n");
            req.append("Connection: close\r\n\r\n");
            out.write(req.toString().getBytes(StandardCharsets.ISO_8859_1));
            if (body != null) out.write(body);
            out.flush();

            BufferedInputStream in = new BufferedInputStream(s.getInputStream(), 8192);
            Response r = new Response();
            String statusLine = readLine(in);
            if (statusLine == null || !statusLine.startsWith("HTTP/")) throw new IOException("Risposta HTTP non valida");
            String[] parts = statusLine.split(" ", 3);
            r.status = Integer.parseInt(parts[1]);
            if (parts.length > 2 && !parts[2].trim().isEmpty()) r.reason = parts[2].trim();

            String line;
            while ((line = readLine(in)) != null && !line.isEmpty()) {
                int i = line.indexOf(':');
                if (i > 0) r.headers.put(line.substring(0, i).trim().toLowerCase(), line.substring(i + 1).trim());
            }
            r.body = in;   // chiudere il body chiude il socket
            return r;
        } catch (IOException e) {
            try { s.close(); } catch (IOException ignored) {}
            throw e;
        }
    }

    /** Legge tutto il body come stringa UTF-8, con tetto massimo. */
    public static String readBody(Response r, int maxBytes) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = r.body.read(chunk)) != -1) {
            buf.write(chunk, 0, n);
            if (buf.size() > maxBytes) throw new IOException("Risposta troppo grande (> " + maxBytes + " byte)");
        }
        r.body.close();
        return buf.toString("UTF-8");
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') b.write(c);
        }
        if (c == -1 && b.size() == 0) return null;
        return b.toString("ISO-8859-1");
    }
}

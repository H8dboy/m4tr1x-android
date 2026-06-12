package com.m4tr1x.node.tor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * SOCKS5 CONNECT minimale verso il SOCKS di Tor. L'hostname viene passato
 * al proxy (ATYP=domain) così la risoluzione .onion avviene dentro Tor —
 * il DNS non lascia mai il tunnel.
 */
public final class Socks5 {

    private Socks5() {}

    public static Socket connect(String host, int port, int timeoutMs) throws IOException {
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress("127.0.0.1", TorManager.SOCKS_PORT), timeoutMs);
            s.setSoTimeout(timeoutMs);
            OutputStream out = s.getOutputStream();
            InputStream in = s.getInputStream();

            // greeting: version 5, 1 metodo, no-auth
            out.write(new byte[]{5, 1, 0});
            out.flush();
            byte[] greet = readN(in, 2);
            if (greet[0] != 5 || greet[1] != 0) throw new IOException("SOCKS5 auth rifiutata");

            // CONNECT con hostname
            byte[] hostB = host.getBytes(StandardCharsets.US_ASCII);
            ByteArrayOutputStream req = new ByteArrayOutputStream();
            req.write(new byte[]{5, 1, 0, 3, (byte) hostB.length}, 0, 5);
            req.write(hostB, 0, hostB.length);
            req.write((port >> 8) & 0xff);
            req.write(port & 0xff);
            out.write(req.toByteArray());
            out.flush();

            byte[] rep = readN(in, 4);
            if (rep[1] != 0) throw new IOException("SOCKS5 connect fallita: codice " + rep[1]);

            // scarta BND.ADDR + BND.PORT
            int atyp = rep[3];
            if (atyp == 1) readN(in, 4 + 2);
            else if (atyp == 3) readN(in, (readN(in, 1)[0] & 0xff) + 2);
            else if (atyp == 4) readN(in, 16 + 2);
            else throw new IOException("SOCKS5 ATYP sconosciuto: " + atyp);

            return s;
        } catch (IOException e) {
            try { s.close(); } catch (IOException ignored) {}
            throw e;
        }
    }

    private static byte[] readN(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(buf, off, n - off);
            if (r < 0) throw new IOException("SOCKS5 stream chiuso");
            off += r;
        }
        return buf;
    }
}

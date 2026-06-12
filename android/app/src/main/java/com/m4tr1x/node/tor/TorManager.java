package com.m4tr1x.node.tor;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Avvia e controlla il demone Tor embedded (binario libtor.so della
 * Guardian Project, eseguito da nativeLibraryDir). Client-only, SOCKS5
 * su 127.0.0.1:39050 — porta diversa da Orbot (9050) per non collidere.
 *
 * Il processo è figlio dell'app: muore con lei. Niente foreground service,
 * niente batteria consumata in background.
 */
public final class TorManager {

    public static final int SOCKS_PORT = 39050;

    private static Process proc;
    private static volatile boolean running = false;
    private static volatile int bootstrap = 0;
    private static volatile String lastLog = "";

    private TorManager() {}

    public static synchronized void start(Context ctx) throws IOException {
        if (running) return;

        File bin = new File(ctx.getApplicationInfo().nativeLibraryDir, "libtor.so");
        if (!bin.exists()) throw new IOException("Binario tor non trovato: " + bin);

        File dataDir = new File(ctx.getFilesDir(), "tor-data");
        if (!dataDir.exists() && !dataDir.mkdirs()) throw new IOException("Impossibile creare " + dataDir);

        File torrc = new File(ctx.getFilesDir(), "torrc");
        try (FileWriter w = new FileWriter(torrc)) {
            w.write("SOCKSPort 127.0.0.1:" + SOCKS_PORT + "\n"
                  + "DataDirectory " + dataDir.getAbsolutePath() + "\n"
                  + "ClientOnly 1\n"
                  + "AvoidDiskWrites 1\n");
        }

        ProcessBuilder pb = new ProcessBuilder(bin.getAbsolutePath(), "-f", torrc.getAbsolutePath());
        pb.environment().put("HOME", ctx.getFilesDir().getAbsolutePath());
        pb.redirectErrorStream(true);
        proc = pb.start();
        running = true;
        bootstrap = 0;

        Thread reader = new Thread(() -> {
            Pattern p = Pattern.compile("Bootstrapped (\\d+)");
            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    lastLog = line;
                    Matcher m = p.matcher(line);
                    if (m.find()) bootstrap = Integer.parseInt(m.group(1));
                }
            } catch (IOException ignored) {
            }
            running = false;
            bootstrap = 0;
        }, "tor-log-reader");
        reader.setDaemon(true);
        reader.start();
    }

    public static synchronized void stop() {
        if (proc != null) {
            proc.destroy();
            proc = null;
        }
        running = false;
        bootstrap = 0;
    }

    public static boolean isRunning() { return running; }

    public static boolean isReady() { return running && bootstrap >= 100; }

    public static int bootstrapProgress() { return bootstrap; }

    public static String lastLogLine() { return lastLog; }
}

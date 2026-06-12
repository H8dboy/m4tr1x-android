package com.m4tr1x.node;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.m4tr1x.node.tor.TorHttp;
import com.m4tr1x.node.tor.TorManager;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Ponte JS ↔ Tor embedded.
 *
 *   TorBridge.start()    → avvia il demone (bootstrap asincrono)
 *   TorBridge.status()   → { running, ready, bootstrap, socksPort }
 *   TorBridge.stop()
 *   TorBridge.request()  → richiesta HTTP via Tor con body (per i POST:
 *                          il WebView non passa il body delle fetch
 *                          all'intercettatore nativo)
 */
@CapacitorPlugin(name = "TorBridge")
public class TorBridgePlugin extends Plugin {

    private static final int MAX_RESPONSE = 10 * 1024 * 1024;

    @PluginMethod
    public void start(PluginCall call) {
        try {
            TorManager.start(getContext());
            call.resolve(statusObj());
        } catch (Exception e) {
            call.reject("Avvio Tor fallito: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        TorManager.stop();
        call.resolve();
    }

    @PluginMethod
    public void status(PluginCall call) {
        call.resolve(statusObj());
    }

    @PluginMethod
    public void request(PluginCall call) {
        final String url = call.getString("url");
        final String method = call.getString("method", "GET");
        final String body = call.getString("body");
        final String contentType = call.getString("contentType", "application/json");
        if (url == null) { call.reject("url mancante"); return; }
        if (!TorManager.isReady()) { call.reject("Tor non pronto"); return; }

        new Thread(() -> {
            try {
                Map<String, String> headers = new HashMap<>();
                if (body != null) headers.put("Content-Type", contentType);
                TorHttp.Response r = TorHttp.request(method, url,
                        headers, body == null ? null : body.getBytes(StandardCharsets.UTF_8), 60000);
                String text = TorHttp.readBody(r, MAX_RESPONSE);
                JSObject ret = new JSObject();
                ret.put("status", r.status);
                ret.put("body", text);
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("Richiesta Tor fallita: " + e.getMessage());
            }
        }, "tor-request").start();
    }

    /**
     * Upload multipart/form-data via Tor. Il WebView non passa i body al
     * nativo, quindi il file arriva in base64 dal JS e il multipart viene
     * assemblato qui.
     *
     * Parametri: url, fileField, fileName, mimeType, dataB64, fields {k:v}
     */
    @PluginMethod
    public void upload(PluginCall call) {
        final String url = call.getString("url");
        final String fileField = call.getString("fileField", "file");
        final String fileName = call.getString("fileName", "upload.bin");
        final String mimeType = call.getString("mimeType", "application/octet-stream");
        final String dataB64 = call.getString("dataB64");
        final JSObject fields = call.getObject("fields", new JSObject());
        if (url == null || dataB64 == null) { call.reject("url o dataB64 mancante"); return; }
        if (!TorManager.isReady()) { call.reject("Tor non pronto"); return; }

        new Thread(() -> {
            try {
                byte[] fileBytes = android.util.Base64.decode(dataB64, android.util.Base64.DEFAULT);
                String boundary = "----M4TR1X" + Long.toHexString(System.currentTimeMillis());

                java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream();
                java.util.Iterator<String> keys = fields.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    String v = fields.getString(k);
                    if (v == null) continue;
                    body.write(("--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; name=\"" + k + "\"\r\n\r\n"
                            + v + "\r\n").getBytes(StandardCharsets.UTF_8));
                }
                body.write(("--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"" + fileField
                        + "\"; filename=\"" + fileName.replace("\"", "") + "\"\r\n"
                        + "Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                body.write(fileBytes);
                body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "multipart/form-data; boundary=" + boundary);
                TorHttp.Response r = TorHttp.request("POST", url, headers, body.toByteArray(), 180000);
                String text = TorHttp.readBody(r, MAX_RESPONSE);
                JSObject ret = new JSObject();
                ret.put("status", r.status);
                ret.put("body", text);
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("Upload Tor fallito: " + e.getMessage());
            }
        }, "tor-upload").start();
    }

    private JSObject statusObj() {
        JSObject o = new JSObject();
        o.put("running", TorManager.isRunning());
        o.put("ready", TorManager.isReady());
        o.put("bootstrap", TorManager.bootstrapProgress());
        o.put("socksPort", TorManager.SOCKS_PORT);
        o.put("lastLog", TorManager.lastLogLine());
        return o;
    }
}

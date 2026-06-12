package com.m4tr1x.node;

import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeWebViewClient;
import com.m4tr1x.node.tor.TorHttp;
import com.m4tr1x.node.tor.TorManager;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Intercetta TUTTE le richieste GET del WebView verso host .onion e le
 * instrada nel tunnel SOCKS5 di Tor: API, immagini, video HLS, stories —
 * tutto passa per Tor in modo trasparente, senza toccare il codice web.
 *
 * I POST non possono essere intercettati (il WebView non espone il body):
 * il layer JS li manda via TorBridge.request().
 */
public class OnionWebViewClient extends BridgeWebViewClient {

    public OnionWebViewClient(Bridge bridge) {
        super(bridge);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        Uri url = request.getUrl();
        String host = url.getHost();
        boolean isOnion = host != null && host.endsWith(".onion");

        if (isOnion && "GET".equalsIgnoreCase(request.getMethod())) {
            if (!TorManager.isReady()) return torError(503, "Tor non pronto — bootstrap in corso");
            try {
                TorHttp.Response r = TorHttp.request("GET", url.toString(), request.getRequestHeaders(), null, 60000);

                String contentType = r.headers.get("content-type");
                if (contentType == null) contentType = "application/octet-stream";
                String mime = contentType.split(";")[0].trim();
                String charset = null;
                int ci = contentType.toLowerCase().indexOf("charset=");
                if (ci >= 0) charset = contentType.substring(ci + 8).trim();

                Map<String, String> respHeaders = new HashMap<>(r.headers);
                // La risposta la sintetizziamo noi: il CORS lo garantiamo noi
                respHeaders.put("Access-Control-Allow-Origin", "*");

                int status = Math.max(r.status, 100);
                String reason = (r.reason == null || r.reason.trim().isEmpty()) ? "OK" : r.reason.trim();

                WebResourceResponse resp = new WebResourceResponse(mime, charset, r.body);
                resp.setStatusCodeAndReasonPhrase(status, reason);
                resp.setResponseHeaders(respHeaders);
                return resp;
            } catch (Exception e) {
                return torError(502, "Errore Tor: " + e.getMessage());
            }
        }

        return super.shouldInterceptRequest(view, request);
    }

    private WebResourceResponse torError(int status, String message) {
        WebResourceResponse resp = new WebResourceResponse(
                "text/plain", "utf-8",
                new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)));
        resp.setStatusCodeAndReasonPhrase(status, "Tor");
        resp.setResponseHeaders(Collections.singletonMap("Access-Control-Allow-Origin", "*"));
        return resp;
    }
}

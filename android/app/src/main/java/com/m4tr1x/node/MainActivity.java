package com.m4tr1x.node;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;
import com.m4tr1x.node.tor.TorManager;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(TorBridgePlugin.class);
        super.onCreate(savedInstanceState);
        // Instrada le richieste .onion del WebView dentro Tor
        this.bridge.getWebView().setWebViewClient(new OnionWebViewClient(this.bridge));
    }

    @Override
    public void onDestroy() {
        TorManager.stop();
        super.onDestroy();
    }
}

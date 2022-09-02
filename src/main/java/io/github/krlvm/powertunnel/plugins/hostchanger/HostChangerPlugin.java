/*
 * This file is part of HostChanger.
 *
 * HostChanger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HostChanger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HostChanger.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.krlvm.powertunnel.plugins.hostchanger;

import io.github.krlvm.powertunnel.sdk.configuration.Configuration;
import io.github.krlvm.powertunnel.sdk.http.ProxyRequest;
import io.github.krlvm.powertunnel.sdk.plugin.PowerTunnelPlugin;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyAdapter;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyListener;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyServer;
import io.github.krlvm.powertunnel.sdk.types.FullAddress;
import io.github.krlvm.powertunnel.sdk.utiities.TextReader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class HostChangerPlugin extends PowerTunnelPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostChangerPlugin.class);

    @Override
    public void onProxyInitialization(@NotNull ProxyServer proxy) {
        final Configuration config = readConfiguration();

        final String fakeHost = config.get("fake_host", "example.com");
        final boolean fakeHttp = config.getBoolean("fake_http", false);
        final boolean fakeHttps = config.getBoolean("fake_https", true);

        if (fakeHttps) {
            proxy.setMITMEnabled(true);
        }

        LOGGER.info("Fake hostname: '{}' [HTTP={}, HTTPS={}]", fakeHost, fakeHttp, fakeHttps);

        registerProxyListener(new ProxyAdapter() {
            @Override
            public void onProxyToServerRequest(@NotNull ProxyRequest request) {
                if (!fakeHttp) return;
                if (request.isEncrypted() || request.address() == null || request.address().getPort() != 80) return;
                if (request.headers().contains("Host")) return;
                request.headers().set("Host", fakeHost);
            }

            @Override
            public Boolean isMITMAllowed(@NotNull FullAddress address) {
                return fakeHttps ? true : null;
            }

            @Override
            public Object onGetSNI(@NotNull String hostname) {
                return fakeHost;
            }
        }, ProxyListener.PRIORITY_HIGH + 1);
    }
}

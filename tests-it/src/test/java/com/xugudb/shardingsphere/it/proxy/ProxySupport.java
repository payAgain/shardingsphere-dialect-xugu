package com.xugudb.shardingsphere.it.proxy;

import com.xugudb.shardingsphere.it.baseline.BaselineSupport;
import org.apache.shardingsphere.proxy.backend.config.ProxyConfigurationLoader;
import org.apache.shardingsphere.proxy.backend.config.YamlProxyConfiguration;
import org.apache.shardingsphere.proxy.frontend.ShardingSphereProxy;
import org.apache.shardingsphere.proxy.initializer.BootstrapInitializer;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Embedded ShardingSphere-Proxy bootstrap for IT (SS 5.5.3 APIs).
 *
 * <p>Starts Proxy in-process via {@link BootstrapInitializer} + {@link ShardingSphereProxy}
 * so MySQL JDBC clients can exercise MySQL wire → XuGu {@code compatiblemode=NONE} storage.</p>
 */
public final class ProxySupport {

    public static final String LOGIC_DB = "logic_db";

    public static final String PROXY_USER = "sharding";

    public static final String PROXY_PASSWORD = "sharding";

    public static final String PHYSICAL_TABLE = "T_ORDER";

    private static final Object LOCK = new Object();

    private static ShardingSphereProxy proxy;

    private static Path confDir;

    private ProxySupport() {
    }

    /**
     * Prepare XuGu databases / physical table, render Proxy conf, start embedded Proxy.
     *
     * @return bound listen port on 127.0.0.1
     */
    public static int startForShardingCrud(final Properties props) throws Exception {
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);
        Path dir = Files.createTempDirectory("ss-proxy-it-");
        confDir = dir;
        writeRenderedConf(dir, props);
        return startEmbedded(dir, findFreePort());
    }

    public static String mysqlJdbcUrl(final int port) {
        return "jdbc:mysql://127.0.0.1:" + port + "/" + LOGIC_DB
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf-8&serverTimezone=UTC";
    }

    public static Connection openMySQLClient(final int port) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(mysqlJdbcUrl(port), PROXY_USER, PROXY_PASSWORD);
    }

    public static void stopQuietly() {
        synchronized (LOCK) {
            if (proxy != null) {
                try {
                    proxy.close();
                } catch (Exception ignored) {
                    // best-effort
                }
                proxy = null;
            }
            if (confDir != null) {
                try {
                    Files.walk(confDir)
                            .sorted((a, b) -> b.compareTo(a))
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                    // best-effort
                                }
                            });
                } catch (IOException ignored) {
                    // best-effort
                }
                confDir = null;
            }
        }
    }

    private static int startEmbedded(final Path confDirectory, final int port) throws Exception {
        synchronized (LOCK) {
            Assumptions.assumeTrue(proxy == null, "embedded Proxy already running");
            String confPath = confDirectory.toAbsolutePath().toString().replace('\\', '/');
            if (!confPath.endsWith("/")) {
                confPath = confPath + "/";
            }
            YamlProxyConfiguration yamlConfig = ProxyConfigurationLoader.load(confPath);
            new BootstrapInitializer().init(yamlConfig, port);
            final ShardingSphereProxy instance = new ShardingSphereProxy();
            final CountDownLatch started = new CountDownLatch(1);
            final AtomicReference<Throwable> bootError = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                try {
                    started.countDown();
                    instance.start(port, Collections.singletonList("127.0.0.1"));
                } catch (Throwable ex) {
                    bootError.set(ex);
                    started.countDown();
                }
            }, "ss-proxy-it");
            thread.setDaemon(true);
            thread.start();
            Assumptions.assumeTrue(started.await(60, TimeUnit.SECONDS), "Proxy starter thread did not begin");
            if (bootError.get() != null) {
                throw new IllegalStateException("embedded Proxy failed to start", bootError.get());
            }
            waitUntilListening(port, 60_000L, bootError);
            if (bootError.get() != null) {
                throw new IllegalStateException("embedded Proxy failed while binding port", bootError.get());
            }
            proxy = instance;
            return port;
        }
    }

    private static void writeRenderedConf(final Path dir, final Properties props) throws Exception {
        byte[] server = BaselineSupport.loadYaml("proxy/server.yaml", props);
        byte[] sharding = BaselineSupport.loadYaml("proxy/config-sharding.yaml", props);
        Files.write(dir.resolve("server.yaml"), server);
        Files.write(dir.resolve("config-sharding.yaml"), sharding);
        // touch a marker so evidence can show conf path
        try (OutputStream out = Files.newOutputStream(dir.resolve(".proxy-it-ready"))) {
            out.write(("port-pending\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void ensurePhysicalTables(final Properties props) throws Exception {
        for (String urlKey : new String[]{"jdbc.url.ds0", "jdbc.url.ds1"}) {
            BaselineSupport.dropTableQuietly(props, urlKey, PHYSICAL_TABLE);
            BaselineSupport.executeOn(props, urlKey,
                    "CREATE TABLE " + PHYSICAL_TABLE
                            + " (ORDER_ID INT PRIMARY KEY, USER_ID INT NOT NULL, STATUS VARCHAR(32))");
        }
    }

    static void cleanupPhysicalTables(final Properties props) {
        BaselineSupport.dropTableQuietly(props, "jdbc.url.ds0", PHYSICAL_TABLE);
        BaselineSupport.dropTableQuietly(props, "jdbc.url.ds1", PHYSICAL_TABLE);
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("127.0.0.1", 0));
            return socket.getLocalPort();
        }
    }

    private static void waitUntilListening(final int port, final long timeoutMs,
                                           final AtomicReference<Throwable> bootError) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (bootError.get() != null) {
                return;
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
                return;
            } catch (IOException ignored) {
                Thread.sleep(200L);
            }
        }
        throw new IllegalStateException("timed out waiting for Proxy listen on 127.0.0.1:" + port);
    }

}

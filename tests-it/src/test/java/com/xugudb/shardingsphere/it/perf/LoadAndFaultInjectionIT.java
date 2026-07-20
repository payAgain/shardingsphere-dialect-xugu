package com.xugudb.shardingsphere.it.perf;

import com.xugudb.shardingsphere.it.baseline.BaselineSupport;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G-004 P1-2 — CRUD + pagination load smoke and client-side fault injection
 * (pool exhaustion / mid-flight connection close) against lab XuGu.
 *
 * <p>Not a formal capacity benchmark; numbers are short-run smoke observations.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoadAndFaultInjectionIT {

    private static final String TABLE = "BASELINE_TX_ORDER";

    private static final int LOAD_THREADS = 24;

    private static final int OPS_PER_THREAD = 20;

    private static final int POOL_EXHAUST_THREADS = 32;

    @Test
    @Order(1)
    void crudPaginationLoadSmoke() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-sharding-db.yaml", props);
        LatencyBucket latency = new LatencyBucket();
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger err = new AtomicInteger();
        AtomicReference<String> firstError = new AtomicReference<>();

        try {
            seedRows(dataSource, 1, 40);

            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(LOAD_THREADS);
            long wallStart = System.nanoTime();
            for (int t = 0; t < LOAD_THREADS; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        start.await(60, TimeUnit.SECONDS);
                        for (int i = 0; i < OPS_PER_THREAD; i++) {
                            long opStart = System.nanoTime();
                            try {
                                runCrudPaginationCycle(dataSource, threadId, i);
                                ok.incrementAndGet();
                                latency.record(System.nanoTime() - opStart);
                            } catch (Exception ex) {
                                err.incrementAndGet();
                                firstError.compareAndSet(null, ex.getClass().getSimpleName() + ": " + ex.getMessage());
                            }
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        err.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                }, "p12-load-" + t).start();
            }
            start.countDown();
            assertTrue(done.await(180, TimeUnit.SECONDS), "load smoke timed out");
            long wallMs = (System.nanoTime() - wallStart) / 1_000_000L;
            int totalOps = LOAD_THREADS * OPS_PER_THREAD;
            double opsPerSec = wallMs <= 0 ? 0 : (totalOps * 1000.0) / wallMs;

            System.out.printf(
                    "[P1-2 load] threads=%d ops=%d ok=%d err=%d elapsedMs=%d opsPerSec=%.1f "
                            + "latAvgMs=%.1f latMaxMs=%.1f latMinMs=%.1f firstError=%s%n",
                    LOAD_THREADS, totalOps, ok.get(), err.get(), wallMs, opsPerSec,
                    latency.avgMs(), latency.maxMs(), latency.minMs(),
                    firstError.get() == null ? "none" : firstError.get());

            assertTrue(ok.get() > totalOps * 0.9, "load smoke ok rate too low: ok=" + ok.get() + " total=" + totalOps);
            assertTrue(err.get() < totalOps * 0.1, "load smoke error rate too high: err=" + err.get());
        } finally {
            cleanup(props);
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    @Test
    @Order(2)
    void poolExhaustionUnderLoad() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);

        // Raw Hikari (same pool class as baseline YAML) — SS logical connections did not pin
        // underlying slots reliably enough to force timeouts in a short smoke.
        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName(props.getProperty("jdbc.driver"));
        cfg.setJdbcUrl(props.getProperty("jdbc.url.ds0"));
        cfg.setUsername(props.getProperty("jdbc.user"));
        cfg.setPassword(props.getProperty("jdbc.password"));
        cfg.setMaximumPoolSize(2);
        cfg.setConnectionTimeout(800);
        cfg.setPoolName("p12-tiny");
        HikariDataSource pool = new HikariDataSource(cfg);

        AtomicInteger acquired = new AtomicInteger();
        AtomicInteger timeoutOrFail = new AtomicInteger();
        AtomicReference<String> sampleFail = new AtomicReference<>();
        List<Connection> held = Collections.synchronizedList(new ArrayList<>());

        try {
            // Occupy both pool slots.
            held.add(pool.getConnection());
            held.add(pool.getConnection());

            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(POOL_EXHAUST_THREADS);
            long wallStart = System.nanoTime();
            for (int t = 0; t < POOL_EXHAUST_THREADS; t++) {
                new Thread(() -> {
                    try {
                        start.await(30, TimeUnit.SECONDS);
                        try (Connection conn = pool.getConnection()) {
                            // Acquisition alone is the fault signal under a fully held tiny pool.
                            acquired.incrementAndGet();
                        } catch (SQLException ex) {
                            timeoutOrFail.incrementAndGet();
                            sampleFail.compareAndSet(null,
                                    ex.getClass().getSimpleName() + ": " + ex.getMessage());
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        timeoutOrFail.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                }, "p12-pool-" + t).start();
            }
            start.countDown();
            assertTrue(done.await(60, TimeUnit.SECONDS), "pool exhaustion run timed out");
            long wallMs = (System.nanoTime() - wallStart) / 1_000_000L;

            System.out.printf(
                    "[P1-2 pool-exhaust] threads=%d maxPool=2 held=%d acquired=%d timeoutOrFail=%d "
                            + "elapsedMs=%d sampleFail=%s%n",
                    POOL_EXHAUST_THREADS, held.size(), acquired.get(), timeoutOrFail.get(), wallMs,
                    sampleFail.get() == null ? "none" : sampleFail.get());

            assertTrue(timeoutOrFail.get() > 0,
                    "pool exhaustion not observed (no timeouts/failures). held=" + held.size()
                            + " acquired=" + acquired.get() + " sample=" + sampleFail.get());
            assertTrue(acquired.get() == 0,
                    "expected zero successful acquires while pool fully held; acquired=" + acquired.get());
        } finally {
            for (Connection c : held) {
                try {
                    c.close();
                } catch (SQLException ignored) {
                    // best-effort
                }
            }
            pool.close();
            cleanup(props);
        }
    }

    @Test
    @Order(3)
    void connectionKillMidFlight() throws Exception {
        Properties props = BaselineSupport.loadProps();
        BaselineSupport.assumeReachable(props);
        BaselineSupport.ensureDatabases(props, BaselineSupport.DB0, BaselineSupport.DB1);
        ensurePhysicalTables(props);

        DataSource dataSource = BaselineSupport.createDataSource("baseline/baseline-sharding-db.yaml", props);
        AtomicReference<String> workerOutcome = new AtomicReference<>("NOT_STARTED");
        AtomicReference<String> killOutcome = new AtomicReference<>("NOT_STARTED");
        AtomicInteger postKillOk = new AtomicInteger();
        AtomicInteger postKillErr = new AtomicInteger();

        try {
            seedRows(dataSource, 200, 30);

            CountDownLatch inFlight = new CountDownLatch(1);
            CountDownLatch mayFinish = new CountDownLatch(1);
            AtomicReference<Connection> live = new AtomicReference<>();
            AtomicReference<Statement> liveStmt = new AtomicReference<>();

            Thread worker = new Thread(() -> {
                try {
                    Connection conn = dataSource.getConnection();
                    live.set(conn);
                    Statement st = conn.createStatement();
                    liveStmt.set(st);
                    // Signal that we hold an open Statement; killer closes Connection mid-flight.
                    inFlight.countDown();
                    if (!mayFinish.await(15, TimeUnit.SECONDS)) {
                        workerOutcome.set("KILL_WAIT_TIMEOUT");
                        return;
                    }
                    int rows = 0;
                    try (ResultSet rs = st.executeQuery(
                            "SELECT id FROM baseline_tx_order ORDER BY id LIMIT 5")) {
                        while (rs.next()) {
                            rows++;
                        }
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO baseline_tx_order (id, user_id, status) VALUES (?, ?, ?)")) {
                        ps.setInt(1, 99901);
                        ps.setInt(2, 1);
                        ps.setString(3, "KILL");
                        ps.executeUpdate();
                    }
                    workerOutcome.set("SURVIVED_AFTER_KILL rows=" + rows);
                } catch (Exception ex) {
                    workerOutcome.set("FAILED_AFTER_KILL " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                } finally {
                    Statement st = liveStmt.get();
                    if (st != null) {
                        try {
                            st.close();
                        } catch (SQLException ignored) {
                            // best-effort
                        }
                    }
                    Connection c = live.get();
                    if (c != null) {
                        try {
                            c.close();
                        } catch (SQLException ignored) {
                            // already closed / kill path
                        }
                    }
                }
            }, "p12-kill-worker");
            worker.start();

            assertTrue(inFlight.await(30, TimeUnit.SECONDS), "worker did not reach in-flight state");
            Connection toKill = live.get();
            try {
                // Prefer JDBC abort when available; fall back to close.
                try {
                    toKill.abort(Runnable::run);
                    killOutcome.set("ABORT_OK");
                } catch (AbstractMethodError | UnsupportedOperationException | SQLException abortEx) {
                    toKill.close();
                    killOutcome.set("CLOSED_OK afterAbortFail=" + abortEx.getClass().getSimpleName());
                }
            } catch (SQLException ex) {
                killOutcome.set("CLOSE_THREW " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
            mayFinish.countDown();
            worker.join(60_000L);
            assertTrue(!worker.isAlive(), "kill worker still alive");

            // Recovery smoke: fresh connections should still work after client-side kill.
            for (int i = 0; i < 8; i++) {
                try (Connection conn = dataSource.getConnection();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT id FROM baseline_tx_order ORDER BY id LIMIT 3")) {
                    while (rs.next()) {
                        // drain
                    }
                    postKillOk.incrementAndGet();
                } catch (SQLException ex) {
                    postKillErr.incrementAndGet();
                }
            }

            System.out.printf(
                    "[P1-2 conn-kill] killOutcome=%s workerOutcome=%s postKillOk=%d postKillErr=%d%n",
                    killOutcome.get(), workerOutcome.get(), postKillOk.get(), postKillErr.get());

            assertTrue(killOutcome.get().startsWith("ABORT") || killOutcome.get().startsWith("CLOSED")
                            || killOutcome.get().startsWith("CLOSE_THREW"),
                    "kill did not run: " + killOutcome.get());
            assertTrue(workerOutcome.get().startsWith("FAILED_AFTER_KILL")
                            || workerOutcome.get().startsWith("SURVIVED_AFTER_KILL"),
                    "unexpected worker outcome: " + workerOutcome.get());
            assertTrue(postKillOk.get() >= 6, "pool/datasource did not recover after kill: ok="
                    + postKillOk.get() + " err=" + postKillErr.get());
        } finally {
            cleanup(props);
            BaselineSupport.closeQuietly(dataSource);
        }
    }

    private static void runCrudPaginationCycle(final DataSource dataSource, final int threadId, final int op)
            throws SQLException {
        int id = 10_000 + threadId * 1000 + op;
        int userId = (threadId + op) % 2 == 0 ? 2 : 3; // route across shards
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO baseline_tx_order (id, user_id, status) VALUES (?, ?, ?)")) {
                insert.setInt(1, id);
                insert.setInt(2, userId);
                insert.setString(3, "L" + threadId + "-" + op);
                insert.executeUpdate();
            }
            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE baseline_tx_order SET status = ? WHERE id = ?")) {
                update.setString(1, "U" + threadId + "-" + op);
                update.setInt(2, id);
                update.executeUpdate();
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT id, status FROM baseline_tx_order ORDER BY id LIMIT 5")) {
                while (rs.next()) {
                    // drain page
                }
            }
            try (PreparedStatement delete = conn.prepareStatement(
                    "DELETE FROM baseline_tx_order WHERE id = ?")) {
                delete.setInt(1, id);
                delete.executeUpdate();
            }
        }
    }

    private static void seedRows(final DataSource dataSource, final int startId, final int count) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement insert = conn.prepareStatement(
                     "INSERT INTO baseline_tx_order (id, user_id, status) VALUES (?, ?, ?)")) {
            for (int i = 0; i < count; i++) {
                int id = startId + i;
                insert.setInt(1, id);
                insert.setInt(2, id);
                insert.setString(3, "SEED" + id);
                insert.executeUpdate();
            }
        }
    }

    private static void ensurePhysicalTables(final Properties props) throws Exception {
        for (String urlKey : new String[]{"jdbc.url.ds0", "jdbc.url.ds1"}) {
            BaselineSupport.dropTableQuietly(props, urlKey, TABLE);
            BaselineSupport.executeOn(props, urlKey,
                    "CREATE TABLE " + TABLE + " (ID INT PRIMARY KEY, USER_ID INT NOT NULL, STATUS VARCHAR(32))");
        }
    }

    private static void cleanup(final Properties props) {
        for (String urlKey : new String[]{"jdbc.url.ds0", "jdbc.url.ds1"}) {
            BaselineSupport.dropTableQuietly(props, urlKey, TABLE);
        }
    }

    private static final class LatencyBucket {
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong sumNanos = new AtomicLong();
        private final AtomicLong maxNanos = new AtomicLong(0);
        private final AtomicLong minNanos = new AtomicLong(Long.MAX_VALUE);

        void record(final long nanos) {
            count.incrementAndGet();
            sumNanos.addAndGet(nanos);
            maxNanos.accumulateAndGet(nanos, Math::max);
            minNanos.accumulateAndGet(nanos, Math::min);
        }

        double avgMs() {
            long c = count.get();
            if (c == 0) {
                return 0;
            }
            return (sumNanos.get() / (double) c) / 1_000_000.0;
        }

        double maxMs() {
            long v = maxNanos.get();
            return v <= 0 ? 0 : v / 1_000_000.0;
        }

        double minMs() {
            long v = minNanos.get();
            return v == Long.MAX_VALUE ? 0 : v / 1_000_000.0;
        }
    }
}

package com.tetv.verticles;

import com.tetv.util.VertxTest;
import io.vertx.core.DeploymentOptions;
import org.junit.*;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("WeakerAccess")
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DefaultModeTest {
    private final static int CHECK_TIME = 5;
    private final static int PONG_WAIT = 50;
    private final static int SEND_TIME = 28; // Should be less than PONG_TIME
    private final static Named<DeploymentOptions> STANDARD = new Named<>("standard", new DeploymentOptions());
    private final static Named<DeploymentOptions> WORKER = new Named<>("worker", new DeploymentOptions().setWorker(true));
    private final static Named<DeploymentOptions> WORKER_MT = new Named<>("workerMT", new DeploymentOptions().setWorker(true).setMultiThreaded(true));
    private final static Named<Boolean> LOCAL = new Named<>("local", false);
    private final static Named<Boolean> CLUSTER = new Named<>("cluster", true);
    private final static Named<Boolean> IGNORED = new Named<>("ignored", true);
    private final static Named<Boolean> ORDERED = new Named<>("ordered", true);
    private final static Named<Boolean> NOT_ORDERED = new Named<>("notOrdered", false);
    private final static Map<Integer, String[]> rangeMap = new HashMap<>();
    private VertxTest pingVertx;
    private VertxTest pongVertx;
    private Pong pong;

    static {
        // Setup range balls map to speed up the tests
        List<String> list = new ArrayList<>();
        for(int index=1; index<=100; index++) {
            list.add(String.format("ball%03d", index));
            rangeMap.put(index, list.toArray(new String[list.size()]));
        }
    }

    private static class Named<T> {
        private final String name;
        private final T value;
        public Named(String name, T value) { this.name = name; this.value = value; }
        public T get() { return value; }
        @Override public String toString() { return name; }
    }

    @Rule public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);

    @Parameters(name = "{index}: {0}/{1}/{2}/{3}/{4}/{5}/{6}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { // PP=Parallel-Processing; wexec=WORKAROUND; jexec=WORKAROUND (alternative)
                { LOCAL,   STANDARD,  "default", IGNORED,      1,  2,  3 }, // 00
                { LOCAL,   STANDARD,  "block",   ORDERED,      1,  2,  3 }, // 01
                { LOCAL,   STANDARD,  "block",   NOT_ORDERED, 20, 39, 40 }, // 02  PP - BUG: Doesn't send instantaneously (times >= 40)
                { LOCAL,   STANDARD,  "wexec",   ORDERED,      1,  2,  3 }, // 03
                { LOCAL,   STANDARD,  "wexec",   NOT_ORDERED, 20, 40, 60 }, // 04  PP
                { LOCAL,   STANDARD,  "jexec",   ORDERED,      1,  2,  3 }, // 05
                { LOCAL,   STANDARD,  "jexec",   NOT_ORDERED, 20, 40, 60 }, // 06  PP
                { LOCAL,   WORKER,    "default", IGNORED,      1,  2,  3 }, // 07
                { LOCAL,   WORKER,    "block",   ORDERED,      1,  2,  3 }, // 08
                { LOCAL,   WORKER,    "block",   NOT_ORDERED, 20, 39, 40 }, // 09  PP - BUG: Doesn't send instantaneously (times >= 40)
                { LOCAL,   WORKER,    "wexec",   ORDERED,      1,  2,  3 }, // 10
                { LOCAL,   WORKER,    "wexec",   NOT_ORDERED, 20, 40, 60 }, // 11  PP
                { LOCAL,   WORKER,    "jexec",   ORDERED,      1,  2,  3 }, // 12
                { LOCAL,   WORKER,    "jexec",   NOT_ORDERED, 20, 40, 60 }, // 13  PP
                { LOCAL,   WORKER_MT, "default", IGNORED,     20, 39, 40 }, // 14  PP - BUG: Doesn't send instantaneously (times >= 40)
                { LOCAL,   WORKER_MT, "block",   ORDERED,      1,  2,  3 }, // 15
                { LOCAL,   WORKER_MT, "block",   NOT_ORDERED, 20, 39, 40 }, // 16  PP - BUG: Doesn't send instantaneously (times >= 40)
                { LOCAL,   WORKER_MT, "wexec",   ORDERED,      1,  2,  3 }, // 17
                { LOCAL,   WORKER_MT, "wexec",   NOT_ORDERED, 20, 40, 60 }, // 18  PP
                { LOCAL,   WORKER_MT, "jexec",   ORDERED,      1,  2,  3 }, // 19
                { LOCAL,   WORKER_MT, "jexec",   NOT_ORDERED, 20, 40, 60 }, // 20  PP

                { CLUSTER, STANDARD,  "default", IGNORED,      1,  2,  3 }, // 21     - BUG: Doesn't send instantaneously (times >= 2)
                { CLUSTER, STANDARD,  "block",   ORDERED,      1,  2,  3 }, // 22
                { CLUSTER, STANDARD,  "block",   NOT_ORDERED, 20, 40, 60 }, // 23  PP
                { CLUSTER, STANDARD,  "wexec",   ORDERED,      1,  2,  3 }, // 24
                { CLUSTER, STANDARD,  "wexec",   NOT_ORDERED, 20, 40, 60 }, // 25  PP
                { CLUSTER, STANDARD,  "jexec",   ORDERED,      1,  2,  3 }, // 26
                { CLUSTER, STANDARD,  "jexec",   NOT_ORDERED, 20, 40, 60 }, // 27  PP
                { CLUSTER, WORKER,    "default", IGNORED,      1,  2,  3 }, // 28     - BUG: Doesn't send instantaneously (times >= 2)
                { CLUSTER, WORKER,    "block",   ORDERED,      1,  2,  3 }, // 29     - BUG: Doesn't send instantaneously (times >= 2)
                { CLUSTER, WORKER,    "block",   NOT_ORDERED, 20, 39, 40 }, // 30  PP - BUG: Doesn't send instantaneously (times >= 40)
                { CLUSTER, WORKER,    "wexec",   ORDERED,      1,  2,  3 }, // 31
                { CLUSTER, WORKER,    "wexec",   NOT_ORDERED, 20, 39, 40 }, // 32  PP
                { CLUSTER, WORKER,    "jexec",   ORDERED,      1,  2,  3 }, // 33
                { CLUSTER, WORKER,    "jexec",   NOT_ORDERED, 20, 39, 40 }, // 34  PP
                { CLUSTER, WORKER_MT, "default", IGNORED,     20, 39, 40 }, // 35  PP - BUG: Doesn't send instantaneously (times >= 40)
                { CLUSTER, WORKER_MT, "block",   ORDERED,      1,  2,  3 }, // 36     - BUG: Doesn't send instantaneously (times >= 2)
                { CLUSTER, WORKER_MT, "block",   NOT_ORDERED, 20, 39, 40 }, // 37  PP - BUG: Doesn't send instantaneously (times >= 40)
                { CLUSTER, WORKER_MT, "wexec",   ORDERED,      1,  2,  3 }, // 38
                { CLUSTER, WORKER_MT, "wexec",   NOT_ORDERED, 20, 40, 60 }, // 39  PP
                { CLUSTER, WORKER_MT, "jexec",   ORDERED,      1,  2,  3 }, // 40
                { CLUSTER, WORKER_MT, "jexec",   NOT_ORDERED, 20, 40, 60 }, // 41  PP
        });
    }

    @Parameter(0) public Named<Boolean> clustered;
    @Parameter(1) public Named<DeploymentOptions> options;
    @Parameter(2) public String mode;
    @Parameter(3) public Named<Boolean> ordered;
    @Parameter(4) public int times1st;
    @Parameter(5) public int times2nd;
    @Parameter(6) public int times3rd;

    @Before
    public void setUp() throws Exception {
        // Prepare vertx instances
        if(clustered == CLUSTER) {
            pingVertx = VertxTest.createCluster();
            pongVertx = VertxTest.createCluster();
        } else {
            pingVertx = pongVertx = VertxTest.createLocal();
        }

        // Deploy pong
        pong = new Pong(PONG_WAIT, mode, ordered.get());
        pongVertx.deploy(pong, options.get());

        // Disable Ping/Pong logging: Logging operation consume time
        Logger log = LogManager.getLogManager().getLogger("");
        for (Handler handler : log.getHandlers()) {
            handler.setLevel(Level.OFF);
        }
    }

    @After
    public void tearDown() throws Exception {
        pingVertx.close(); // BUG: throws IlegalStateException: Result is already complete (since vertx 3.3.3)
        pongVertx.close(); // BUG: throws IlegalStateException: Result is already complete (since vertx 3.3.3)
    }

    @Test
    public void send_1st_message() throws Exception {
        sendMessages(times1st);
    }

    @Test
    public void send_2nd_messages() throws Exception {
        sendMessages(times2nd);
    }

    @Test
    public void send_3rd_messages() throws Exception {
        sendMessages(times3rd);
    }

    private void sendMessages(int size) throws Exception {
        if(size == 0) {
            return;
        }

        System.out.println(String.format("# Testing: [%s/%s/%s/%s]: ping> Send %s ball%s", clustered, options, mode, ordered, size, (size != 1 ? "s" : "")));
        Ping ping = new Ping(size);
        pingVertx.deploy(ping, WORKER.get());

        if(ordered == NOT_ORDERED || (options == WORKER_MT && ordered == IGNORED)) {
            if(options == WORKER_MT) {
                // Parallel-Processing: Order not guaranteed
                for(int index = 20; index < size; index += 20) {
                    assertBeforeTime(getMaxPingReceivedTime(index)).that(ping.receivedBalls()).hasSize(index);
                }
                assertBeforeTime(getMaxPingReceivedTime(size)).that(ping.receivedBalls()).containsOnly(rangeMap.get(size));
            } else {
                // Parallel-Processing: Order guaranteed
                for(int index = 20; index < size; index += 20) {
                    assertBeforeTime(getMaxPingReceivedTime(index)).that(ping.receivedBalls()).containsOnly(rangeMap.get(index));
                }
                assertBeforeTime(getMaxPingReceivedTime(size)).that(ping.receivedBalls()).containsOnly(rangeMap.get(size));
            }
        } else if(options == WORKER_MT) {
            // Sequential-Processing: Order not guaranteed
            for(int index=1; index<size; index++) {
                assertBeforeTime(getMaxPingReceivedTime(index)).that(ping.receivedBalls()).hasSize(index);
            }
            assertBeforeTime(getMaxPingReceivedTime(size)).that(ping.receivedBalls()).containsOnly(rangeMap.get(size));
        } else {
            // Sequential-Processing: Order guaranteed
            for(int index=1; index<=size; index++) {
                assertBeforeTime(getMaxPingReceivedTime(index)).that(ping.receivedBalls()).containsExactly(rangeMap.get(index));
            }
        }
    }

    private long getMaxPingReceivedTime(int size) throws Exception {
        // Ensure x balls were processed by pong
        while (pong.numProcessedBalls() < size) {
            Thread.sleep(CHECK_TIME);
        }
        return pong.getFinishedProcessTime(size) + SEND_TIME;
    }

    // Helper methods

    private AssertInTime assertBeforeTime(long millis) throws Exception {
        return actual -> new AssertInTimeThat() {
            @Override
            public void containsOnly(String... expected) throws Exception {
                while (System.currentTimeMillis() < millis) {
                    if (actual.size() >= expected.length) break;
                    Thread.sleep(CHECK_TIME);
                }
                assertThat(actual).hasSize(expected.length);
                assertThat(actual).containsOnly(expected);
            }

            @Override
            public void containsExactly(String... expected) throws Exception {
                while (System.currentTimeMillis() < millis) {
                    if (actual.size() >= expected.length) break;
                    Thread.sleep(CHECK_TIME);
                }
                assertThat(actual).containsExactly(expected);
            }

            @Override
            public void hasSize(int expected) throws Exception {
                while (System.currentTimeMillis() < millis) {
                    if (actual.size() >= expected) break;
                    Thread.sleep(CHECK_TIME);
                }
                assertThat(actual).hasSize(expected);
            }
        };
    }

    // Interfaces to improve readability of tests

    private interface AssertInTime {
        AssertInTimeThat that(Collection<String> actual) throws Exception;
    }

    private interface AssertInTimeThat {
        void containsOnly(String... expected) throws Exception;
        void containsExactly(String... expected) throws Exception;
        void hasSize(int expected) throws Exception;
    }
}
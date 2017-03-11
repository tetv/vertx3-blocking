package com.tetv.verticles;

import com.tetv.util.Ball;
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
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("WeakerAccess")
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DefaultModeTest {
    private final static int CHECK_TIME =  5;
    private final static int PONG_WAIT  = 50;     // For better results raise it to 500 or 1000
    private final static int MAX_DELAY_TIME = 28; // For better results raise it to  50 or  100 (Should be much less than PONG_WAIT)
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

    @Parameters(name = "{index}: {0}: {1}/{2}/{3}/{4}/{5}/{6}/{7}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { // PP=Parallel-Processing; wexec=WORKAROUND; jexec=WORKAROUND (alternative)
                { "1x1", LOCAL,   STANDARD,  "default", IGNORED,      1,  2,  3 }, // 00
                { "2x1", LOCAL,   STANDARD,  "block",   ORDERED,      1,  2,  3 }, // 01
                { "3x1", LOCAL,   STANDARD,  "block",   NOT_ORDERED, 20, 39, 40 }, // 02  PP - BUG: Doesn't send instantaneously (times >= 40)
                { "4x1", LOCAL,   STANDARD,  "wexec",   ORDERED,      1,  2,  3 }, // 03
                { "5x1", LOCAL,   STANDARD,  "wexec",   NOT_ORDERED, 20, 40, 60 }, // 04  PP
                { "6x1", LOCAL,   STANDARD,  "jexec",   ORDERED,      1,  2,  3 }, // 05
                { "7x1", LOCAL,   STANDARD,  "jexec",   NOT_ORDERED, 20, 40, 60 }, // 06  PP
                { "1x2", LOCAL,   WORKER,    "default", IGNORED,      1,  2,  3 }, // 07
                { "2x2", LOCAL,   WORKER,    "block",   ORDERED,      1,  2,  3 }, // 08
                { "3x2", LOCAL,   WORKER,    "block",   NOT_ORDERED, 20, 39, 40 }, // 09  PP - BUG: Doesn't send instantaneously (times >= 40)
                { "4x2", LOCAL,   WORKER,    "wexec",   ORDERED,      1,  2,  3 }, // 10
                { "5x2", LOCAL,   WORKER,    "wexec",   NOT_ORDERED, 20, 40, 60 }, // 11  PP
                { "6x2", LOCAL,   WORKER,    "jexec",   ORDERED,      1,  2,  3 }, // 12
                { "7x2", LOCAL,   WORKER,    "jexec",   NOT_ORDERED, 20, 40, 60 }, // 13  PP
                { "1x3", LOCAL,   WORKER_MT, "default", IGNORED,     20, 39, 40 }, // 14  PP - BUG: Doesn't send instantaneously (times >= 40)
                { "2x3", LOCAL,   WORKER_MT, "block",   ORDERED,      1,  2,  3 }, // 15
                { "3x3", LOCAL,   WORKER_MT, "block",   NOT_ORDERED, 20, 39, 40 }, // 16  PP - BUG: Doesn't send instantaneously (times >= 40)
                { "4x3", LOCAL,   WORKER_MT, "wexec",   ORDERED,      1,  2,  3 }, // 17
                { "5x3", LOCAL,   WORKER_MT, "wexec",   NOT_ORDERED, 20, 40, 60 }, // 18  PP
                { "6x3", LOCAL,   WORKER_MT, "jexec",   ORDERED,      1,  2,  3 }, // 19
                { "7x3", LOCAL,   WORKER_MT, "jexec",   NOT_ORDERED, 20, 40, 60 }, // 20  PP

                { "1x1", CLUSTER, STANDARD,  "default", IGNORED,      1,  2,  3 }, // 21     - BUG: Doesn't send instantaneously (times >= 2)
                { "2x1", CLUSTER, STANDARD,  "block",   ORDERED,      1,  2,  3 }, // 22
                { "3x1", CLUSTER, STANDARD,  "block",   NOT_ORDERED, 20, 40, 60 }, // 23  PP
                { "4x1", CLUSTER, STANDARD,  "wexec",   ORDERED,      1,  2,  3 }, // 24
                { "5x1", CLUSTER, STANDARD,  "wexec",   NOT_ORDERED, 20, 40, 60 }, // 25  PP
                { "6x1", CLUSTER, STANDARD,  "jexec",   ORDERED,      1,  2,  3 }, // 26
                { "7x1", CLUSTER, STANDARD,  "jexec",   NOT_ORDERED, 20, 40, 60 }, // 27  PP
                { "1x2", CLUSTER, WORKER,    "default", IGNORED,      1,  2,  3 }, // 28     - BUG: Doesn't send instantaneously (times >= 2)
                { "2x2", CLUSTER, WORKER,    "block",   ORDERED,      1,  2,  3 }, // 29     - BUG: Doesn't send instantaneously (times >= 2)
                { "3x2", CLUSTER, WORKER,    "block",   NOT_ORDERED, 20, 39, 40 }, // 30  PP - BUG: Doesn't send instantaneously (times >= 40)
                { "4x2", CLUSTER, WORKER,    "wexec",   ORDERED,      1,  2,  3 }, // 31
                { "5x2", CLUSTER, WORKER,    "wexec",   NOT_ORDERED, 20, 39, 40 }, // 32  PP
                { "6x2", CLUSTER, WORKER,    "jexec",   ORDERED,      1,  2,  3 }, // 33
                { "7x2", CLUSTER, WORKER,    "jexec",   NOT_ORDERED, 20, 39, 40 }, // 34  PP
                { "1x3", CLUSTER, WORKER_MT, "default", IGNORED,     20, 39, 40 }, // 35  PP - BUG: Doesn't send instantaneously (times >= 40)
                { "2x3", CLUSTER, WORKER_MT, "block",   ORDERED,      1,  2,  3 }, // 36     - BUG: Doesn't send instantaneously (times >= 2)
                { "3x3", CLUSTER, WORKER_MT, "block",   NOT_ORDERED, 20, 39, 40 }, // 37  PP - BUG: Doesn't send instantaneously (times >= 40)
                { "4x3", CLUSTER, WORKER_MT, "wexec",   ORDERED,      1,  2,  3 }, // 38
                { "5x3", CLUSTER, WORKER_MT, "wexec",   NOT_ORDERED, 20, 40, 60 }, // 39  PP
                { "6x3", CLUSTER, WORKER_MT, "jexec",   ORDERED,      1,  2,  3 }, // 40
                { "7x3", CLUSTER, WORKER_MT, "jexec",   NOT_ORDERED, 20, 40, 60 }, // 41  PP
        });
    }

    @Parameter(0) public String pos;
    @Parameter(1) public Named<Boolean> clustered;
    @Parameter(2) public Named<DeploymentOptions> options;
    @Parameter(3) public String mode;
    @Parameter(4) public Named<Boolean> ordered;
    @Parameter(5) public int times1st;
    @Parameter(6) public int times2nd;
    @Parameter(7) public int times3rd;

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
    }

    @After
    public void tearDown() throws Exception {
        pingVertx.close();
        pongVertx.close();
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
        if (size == 0) {
            return;
        }

        System.out.println(String.format("# Testing: [%s/%s/%s/%s]: ping> Send %s ball%s", clustered, options, mode, ordered, size, (size != 1 ? "s" : "")));
        Ping ping = new Ping(size);

        pingVertx.deploy(ping, WORKER.get());

        // Wait until received all answers
        while (ping.receivedTimeMap.size() < size) {
            Thread.sleep(CHECK_TIME);
        }

        // Calculating ball information
        List<Ball> balls = Stream.of(rangeMap.get(size))
                .map(ball -> new Ball(ball,
                        ping.receivedPosMap.get(ball),
                        pong.finishedProcessedTimeMap.get(ball),
                        ping.receivedTimeMap.get(ball)))
                .collect(toList());

        // Validating received order
        if (ordered != NOT_ORDERED && options != WORKER_MT) {
            // Sequential-Processing: Order guaranteed
            List<String> receivedBalls = balls.stream().sorted(comparingInt(ball -> ball.recvPos)).map(ball -> ball.name).collect(toList());
            assertThat(receivedBalls).containsExactly(rangeMap.get(size));
        } else if (ordered != NOT_ORDERED || options != WORKER_MT) {
            // Parallel-Processing: Slot order guaranteed (inside each slot of 20 - order not guaranteed)
            for (int index = 0; index < size; index += 20) {
                int i = index;
                List<String> receivedSlotBalls = balls.stream()
                        .filter(ball -> ball.recvPos > i && ball.recvPos <= i + 20)
                        .sorted(comparing(ball -> ball.name))
                        .map(ball -> ball.name).collect(toList());

                String[] expectedSlotOrder = balls.stream().skip(index).limit(20).map(ball -> ball.name).toArray(String[]::new);
                assertThat(receivedSlotBalls).containsExactly(expectedSlotOrder);
            }
        } else { // NOT_ORDERED && WORKER_MT
            // Parallel-Processing: Order not guaranteed at all
            List<String> allBalls = balls.stream().map(ball -> ball.name).collect(toList());
            assertThat(allBalls).containsOnly(rangeMap.get(size));
        }

        long maxDiff = balls.stream().mapToLong(Ball::delayedTime).max().orElse(0);
        System.out.println("Max delayed is " + maxDiff);

        // Validate timings between pong processing and ping received answer
        List<Ball> delayedBalls = balls.stream().filter(ball -> ball.delayedTime() > MAX_DELAY_TIME).collect(toList());
        assertThat(delayedBalls).isEmpty();

        // Test success: Print all ball's stat information
        balls.forEach(ball -> {
            System.out.println("# Ball: " + ball);
        });
    }
}
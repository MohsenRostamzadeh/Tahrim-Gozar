package ir.M_Rostamzadeh.Tahrim_Gozar;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import ir.M_Rostamzadeh.Tahrim_Gozar.traffic.TrafficStatsListener;
import ir.M_Rostamzadeh.Tahrim_Gozar.traffic.TrafficStatsTracker;

import static org.junit.Assert.*;

public class TrafficStatsTrackerTest {

    private TrafficStatsTracker tracker;

    @Before
    public void setUp() {
        tracker = new TrafficStatsTracker();
    }

    @Test
    public void testDefaultState() {
        assertFalse(tracker.isEnabled());
        assertEquals(0, tracker.getTotalRxBytes());
        assertEquals(0, tracker.getTotalTxBytes());
        assertEquals(0, tracker.getCurrentRxSpeed());
        assertEquals(0, tracker.getCurrentTxSpeed());
    }

    @Test
    public void testByteAccumulationWhenEnabled() {
        tracker.setEnabled(true);
        assertTrue(tracker.isEnabled());

        tracker.recordRx(1024);
        tracker.recordTx(512);

        assertEquals(1024, tracker.getTotalRxBytes());
        assertEquals(512, tracker.getTotalTxBytes());

        tracker.recordRx(2048);
        assertEquals(3072, tracker.getTotalRxBytes());
    }

    @Test
    public void testByteAccumulationIgnoredWhenDisabled() {
        tracker.setEnabled(false);
        tracker.recordRx(5000);
        tracker.recordTx(3000);

        assertEquals(0, tracker.getTotalRxBytes());
        assertEquals(0, tracker.getTotalTxBytes());
    }

    @Test
    public void testReset() {
        tracker.setEnabled(true);
        tracker.recordRx(4096);
        tracker.recordTx(2048);

        tracker.reset();
        assertEquals(0, tracker.getTotalRxBytes());
        assertEquals(0, tracker.getTotalTxBytes());
        assertEquals(0, tracker.getCurrentRxSpeed());
        assertEquals(0, tracker.getCurrentTxSpeed());
    }

    @Test
    public void testFormatBytes() {
        assertEquals("0 B", TrafficStatsTracker.formatBytes(0));
        assertEquals("500 B", TrafficStatsTracker.formatBytes(500));
        assertEquals("1.0 KB", TrafficStatsTracker.formatBytes(1024));
        assertEquals("1.5 KB", TrafficStatsTracker.formatBytes(1536));
        assertEquals("1.0 MB", TrafficStatsTracker.formatBytes(1024 * 1024));
        assertEquals("2.50 GB", TrafficStatsTracker.formatBytes((long) (2.5 * 1024 * 1024 * 1024)));
    }

    @Test
    public void testFormatSpeed() {
        assertEquals("0 B/s", TrafficStatsTracker.formatSpeed(0));
        assertEquals("512 B/s", TrafficStatsTracker.formatSpeed(512));
        assertEquals("2.0 KB/s", TrafficStatsTracker.formatSpeed(2048));
        assertEquals("10.00 MB/s", TrafficStatsTracker.formatSpeed(10 * 1024 * 1024));
    }

    @Test
    public void testListenerCallback() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong reportedRx = new AtomicLong();

        tracker.setListener((rxSpeed, txSpeed, totalRx, totalTx) -> {
            reportedRx.set(totalRx);
            latch.countDown();
        });

        tracker.setEnabled(true);
        tracker.recordRx(10240);

        // Ticker runs every 1 second, wait up to 2.5 seconds
        boolean received = latch.await(2500, TimeUnit.MILLISECONDS);
        tracker.setEnabled(false);

        assertTrue("Expected traffic stats listener to be triggered", received);
        assertEquals(10240, reportedRx.get());
    }
}

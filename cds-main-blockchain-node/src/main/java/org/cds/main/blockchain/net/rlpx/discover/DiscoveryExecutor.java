package org.cds.main.blockchain.net.rlpx.discover;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.cds.main.blockchain.net.rlpx.discover.table.KademliaOptions;

public class DiscoveryExecutor {

    ScheduledExecutorService discoverer = Executors.newSingleThreadScheduledExecutor();
    ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor();

    NodeManager nodeManager;

    public DiscoveryExecutor(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    public void start() {
        discoverer.scheduleWithFixedDelay(
                new DiscoverTask(nodeManager),
                1, KademliaOptions.DISCOVER_CYCLE, TimeUnit.SECONDS);

        refresher.scheduleWithFixedDelay(
                new RefreshTask(nodeManager),
                1, KademliaOptions.BUCKET_REFRESH, TimeUnit.MILLISECONDS);

    }

    public void close() {
        discoverer.shutdownNow();
        refresher.shutdownNow();
    }
}

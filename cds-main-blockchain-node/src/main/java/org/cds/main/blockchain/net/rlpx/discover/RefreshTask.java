package org.cds.main.blockchain.net.rlpx.discover;

import io.netty.channel.Channel;

import org.cds.main.blockchain.crypto.ECKey;
import org.cds.main.blockchain.net.rlpx.Node;
import org.cds.main.blockchain.net.rlpx.discover.table.NodeTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Random;

public class RefreshTask extends DiscoverTask {
    private static final Logger logger = LoggerFactory.getLogger("discover");

    public RefreshTask(NodeManager nodeManager) {
        super(nodeManager);
    }
//
//    RefreshTask(Channel channel, ECKey key, NodeTable table) {
//        super(getNodeId(), channel, key, table);
//    }

    public static byte[] getNodeId() {
        Random gen = new Random();
        byte[] id = new byte[64];
        gen.nextBytes(id);
        return id;
    }

    @Override
    public void run() {
        discover(getNodeId(), 0, new ArrayList<Node>());
    }
}

package org.cds.main.blockchain.shell.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Queue;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.cds.main.blockchain.config.SystemProperties;
import org.cds.main.blockchain.core.Block;
import org.cds.main.blockchain.core.Repository;
import org.cds.main.blockchain.facade.Ethereum;
import org.cds.main.blockchain.plot.PlotFile;
import org.cds.main.blockchain.shell.model.dto.MinerStatusDTO;
import org.cds.main.blockchain.shell.util.BlockUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class PrivateMinerService {
	Logger log = LoggerFactory.getLogger("harmony");
	
    public final static String MINER_TOPIC = "/topic/mineInfo";

    @Autowired
    Environment env;

    @Autowired
    Ethereum ethereum;

    @Autowired
    SystemProperties config;

    @Autowired
    public Repository repository;

    @Autowired
    private ClientMessageService clientMessageService;

    private final static int AVG_METRICS_BASE = 100;
    Queue<Block> latestBlocks = new CircularFifoQueue<>(AVG_METRICS_BASE);

    private MineStatus status = MineStatus.DISABLED;

    @PostConstruct
    public void init() throws IOException, InterruptedException {
        final boolean isPrivateNetwork = env.getProperty("networkProfile", "").equalsIgnoreCase("private");
        log.info("isPrivateNetwork: " + isPrivateNetwork);
        
        // WOW, how is stinks!
        // Overriding mine.start which was reset in {@link org.cds.main.blockchain.vm.shell.Application}
        SystemProperties.resetToDefault();
        config.overrideParams("mine.start", new Boolean(SystemProperties.getDefault().minerStart()).toString());
        if (config.minerStart()) {
            if (!config.isSyncEnabled()) {
           
            } else {
                this.status = MineStatus.AWAITING;
            }
        }
    }

    /**
     * @return average hash rate/second for our own mined blocks
     */
    public BigInteger calcAvgHashRate() {
        return BlockUtils.calculateHashRate(new ArrayList<>(latestBlocks));
    }

    /**
     * Pushes status change immediately to client application
     */
    private void pushStatus(MineStatus status) {
        clientMessageService.sendToTopic(MINER_TOPIC, new MinerStatusDTO(status.toString()));
    }

    public MineStatus getStatus() {
        return status;
    }

    public enum MineStatus {
        DISABLED,
        PLOTFILE_CHECK,
        PLOTFILE_CHECK_ERROR,
        PLOTFILE_GENERATE_START,
        PLOTFILE_READY,
        MINING,
        AWAITING // Mining is on, but we are on long sync, waiting for short sync
    }
}

package org.cds.main.blockchain;

import java.io.IOException;
import java.net.URISyntaxException;

import org.cds.main.blockchain.config.SystemProperties;
import org.cds.main.blockchain.facade.Ethereum;
import org.cds.main.blockchain.facade.EthereumFactory;

public class Start {

    public static void main(String args[]) throws IOException, URISyntaxException {
        //CLIInterface.call(args);
        final SystemProperties config = SystemProperties.getDefault();
        final boolean actionBlocksLoader = !config.blocksLoader().isEmpty();
        Ethereum ethereum = EthereumFactory.createEthereum();
        if (actionBlocksLoader) {
            ethereum.getBlockLoader().loadBlocks();
        }
    }

}

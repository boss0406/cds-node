package org.cds.main.blockchain.facade;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import org.cds.main.blockchain.core.Block;
import org.cds.main.blockchain.core.Transaction;
import org.cds.main.blockchain.db.BlockStore;

public interface Blockchain {

    /**
     * Get block by number from the best chain
     * @param number - number of the block
     * @return block by that number
     */
    Block getBlockByNumber(long number);

    /**
     * Get block by hash
     * @param hash - hash of the block
     * @return - bloc by that hash
     */
    Block getBlockByHash(byte[] hash);

    /**
     * Get total difficulty from the start
     * and until the head of the chain
     *
     * @return - total difficulty
     */
    BigInteger getTotalDifficulty();

    /**
     * Get the underlying BlockStore
     * @return Blockstore
     */
    BlockStore getBlockStore();


    /**
     * @return - last added block from blockchain
     */
    Block getBestBlock();

    /**
     * Flush the content of local storage objects to disk
     */
    void flush();
}

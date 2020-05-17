package org.cds.main.blockchain.facade;

import java.util.List;
import java.util.Set;

import org.cds.main.blockchain.core.*;

public interface PendingState {

    /**
     * @return pending state repository
     */
    org.cds.main.blockchain.core.Repository getRepository();

    /**
     * @return list of pending transactions
     */
    List<Transaction> getPendingTransactions();
}

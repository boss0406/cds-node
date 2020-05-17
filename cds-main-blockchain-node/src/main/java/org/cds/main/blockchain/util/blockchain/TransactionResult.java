package org.cds.main.blockchain.util.blockchain;

import org.cds.main.blockchain.core.TransactionExecutionSummary;
import org.cds.main.blockchain.core.TransactionReceipt;

public class TransactionResult {
    TransactionReceipt receipt;
    TransactionExecutionSummary executionSummary;

    public boolean isIncluded() {
        return receipt != null;
    }

    public TransactionReceipt getReceipt() {
        return receipt;
    }

    public TransactionExecutionSummary getExecutionSummary() {
        return executionSummary;
    }
}

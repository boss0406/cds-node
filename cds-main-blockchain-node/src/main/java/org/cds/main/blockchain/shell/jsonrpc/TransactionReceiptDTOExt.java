package org.cds.main.blockchain.shell.jsonrpc;

import static org.cds.main.blockchain.shell.jsonrpc.TypeConverter.toJsonHex;

import org.cds.main.blockchain.core.Block;
import org.cds.main.blockchain.core.TransactionInfo;

public class TransactionReceiptDTOExt extends TransactionReceiptDTO {

    public String returnData;
    public String error;

    public TransactionReceiptDTOExt(Block block, TransactionInfo txInfo) {
        super(block, txInfo);
        returnData = toJsonHex(txInfo.getReceipt().getExecutionResult());
        error = txInfo.getReceipt().getError();
    }
}

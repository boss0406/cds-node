package org.cds.main.blockchain.net.eth.message;

import org.cds.main.blockchain.net.message.Message;

public abstract class EthMessage extends Message {

    public EthMessage() {
    }

    public EthMessage(byte[] encoded) {
        super(encoded);
    }

    abstract public EthMessageCodes getCommand();
}

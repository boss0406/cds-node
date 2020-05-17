package org.cds.main.blockchain.net.shh;

import org.cds.main.blockchain.net.message.*;

public class ShhMessageFactory implements MessageFactory {

    @Override
    public Message create(byte code, byte[] encoded) {

        ShhMessageCodes receivedCommand = ShhMessageCodes.fromByte(code);
        switch (receivedCommand) {
            case STATUS:
                return new ShhStatusMessage(encoded);
            case MESSAGE:
                return new ShhEnvelopeMessage(encoded);
            case FILTER:
                return new ShhFilterMessage(encoded);
            default:
                throw new IllegalArgumentException("No such message");
        }
    }
}

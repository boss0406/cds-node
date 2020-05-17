package org.cds.main.blockchain.net.p2p;

import org.cds.main.blockchain.net.message.Message;
import org.cds.main.blockchain.net.message.MessageFactory;
import org.cds.main.blockchain.net.message.StaticMessages;

/**
 * P2P message factory
 *
 */
public class P2pMessageFactory implements MessageFactory {

    @Override
    public Message create(byte code, byte[] encoded) {

        P2pMessageCodes receivedCommand = P2pMessageCodes.fromByte(code);
        switch (receivedCommand) {
            case HELLO:
                return new HelloMessage(encoded);
            case DISCONNECT:
                return new DisconnectMessage(encoded);
            case PING:
                return StaticMessages.PING_MESSAGE;
            case PONG:
                return StaticMessages.PONG_MESSAGE;
            case GET_PEERS:
                return StaticMessages.GET_PEERS_MESSAGE;
            case PEERS:
                return new PeersMessage(encoded);
            default:
                throw new IllegalArgumentException("No such message");
        }
    }
}

package org.cds.main.blockchain.net.eth.message;

import static org.cds.main.blockchain.util.ByteUtil.toHexString;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cds.main.blockchain.core.BlockHeader;
import org.cds.main.blockchain.util.RLP;
import org.cds.main.blockchain.util.RLPList;

/**
 * Wrapper around an Ethereum BlockHeaders message on the network
 *
 * @see EthMessageCodes#BLOCK_HEADERS
 *
 */
public class BlockHeadersMessage extends EthMessage {

    /**
     * List of block headers from the peer
     */
    private List<BlockHeader> blockHeaders;

    public BlockHeadersMessage(byte[] encoded) {
        super(encoded);
    }

    public BlockHeadersMessage(List<BlockHeader> headers) {
        this.blockHeaders = headers;
        parsed = true;
    }

    private synchronized void parse() {
        if (parsed) return;
        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        blockHeaders = new ArrayList<>();
        for (int i = 0; i < paramsList.size(); ++i) {
            RLPList rlpData = ((RLPList) paramsList.get(i));
            blockHeaders.add(new BlockHeader(rlpData));
        }
        parsed = true;
    }

    private void encode() {
        List<byte[]> encodedElements = new ArrayList<>();
        for (BlockHeader blockHeader : blockHeaders)
            encodedElements.add(blockHeader.getEncoded());
        byte[][] encodedElementArray = encodedElements.toArray(new byte[encodedElements.size()][]);
        this.encoded = RLP.encodeList(encodedElementArray);
    }


    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public List<BlockHeader> getBlockHeaders() {
        parse();
        return blockHeaders;
    }

    @Override
    public EthMessageCodes getCommand() {
        return EthMessageCodes.BLOCK_HEADERS;
    }

    @Override
    public String toString() {
        parse();

        StringBuilder payload = new StringBuilder();

        payload.append("count( ").append(blockHeaders.size()).append(" )");

        if (logger.isTraceEnabled()) {
            payload.append(" ");
            for (BlockHeader header : blockHeaders) {
                payload.append(toHexString(header.getHash()).substring(0, 6)).append(" | ");
            }
            if (!blockHeaders.isEmpty()) {
                payload.delete(payload.length() - 3, payload.length());
            }
        } else {
            if (blockHeaders.size() > 0) {
                payload.append("#").append(blockHeaders.get(0).getNumber()).append(" (")
                        .append(toHexString(blockHeaders.get(0).getHash()).substring(0, 8)).append(")");
            }
            if (blockHeaders.size() > 1) {
                payload.append(" ... #").append(blockHeaders.get(blockHeaders.size() - 1).getNumber()).append(" (")
                        .append(toHexString(blockHeaders.get(blockHeaders.size() - 1).getHash()).substring(0, 8)).append(")");
            }
        }

        return "[" + getCommand().name() + " " + payload + "]";
    }
}

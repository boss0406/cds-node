package org.cds.main.blockchain.net.eth.message;

import static org.cds.main.blockchain.util.ByteUtil.byteArrayToInt;
import static org.cds.main.blockchain.util.ByteUtil.byteArrayToLong;

import java.math.BigInteger;

import org.cds.main.blockchain.core.BlockIdentifier;
import org.cds.main.blockchain.util.ByteUtil;
import org.cds.main.blockchain.util.RLP;
import org.cds.main.blockchain.util.RLPList;

/**
 * Wrapper around an Ethereum GetBlockHeaders message on the network
 *
 * @see EthMessageCodes#GET_BLOCK_HEADERS
 *
 */
public class GetBlockHeadersMessage extends EthMessage {

    private static final int DEFAULT_SIZE_BYTES = 32;
    /**
     * Block number from which to start sending block headers
     */
    private long blockNumber;

    /**
     * Block hash from which to start sending block headers <br>
     * Initial block can be addressed by either {@code blockNumber} or {@code blockHash}
     */
    private byte[] blockHash;

    /**
     * The maximum number of headers to be returned. <br>
     * <b>Note:</b> the peer could return fewer.
     */
    private int maxHeaders;

    /**
     * Blocks to skip between consecutive headers. <br>
     * Direction depends on {@code reverse} param.
     */
    private int skipBlocks;

    /**
     * The direction of headers enumeration. <br>
     * <b>false</b> is for rising block numbers. <br>
     * <b>true</b> is for falling block numbers.
     */
    private boolean reverse;

    public GetBlockHeadersMessage(byte[] encoded) {
        super(encoded);
    }

    public GetBlockHeadersMessage(long blockNumber, int maxHeaders) {
        this(blockNumber, null, maxHeaders, 0, false);
    }

    public GetBlockHeadersMessage(long blockNumber, byte[] blockHash, int maxHeaders, int skipBlocks, boolean reverse) {
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.maxHeaders = maxHeaders;
        this.skipBlocks = skipBlocks;
        this.reverse = reverse;

        parsed = true;
        encode();
    }

    private void encode() {
        byte[] maxHeaders  = RLP.encodeInt(this.maxHeaders);
        byte[] skipBlocks = RLP.encodeInt(this.skipBlocks);
        byte[] reverse  = RLP.encodeByte((byte) (this.reverse ? 1 : 0));

        if (this.blockHash != null) {
            byte[] hash = RLP.encodeElement(this.blockHash);
            this.encoded = RLP.encodeList(hash, maxHeaders, skipBlocks, reverse);
        } else {
            byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.blockNumber));
            this.encoded = RLP.encodeList(number, maxHeaders, skipBlocks, reverse);
        }
    }

    private synchronized void parse() {
        if (parsed) return;
        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        byte[] blockBytes = paramsList.get(0).getRLPData();

        // it might be either a hash or number
        if (blockBytes == null) {
            this.blockNumber = 0;
        } else if (blockBytes.length == DEFAULT_SIZE_BYTES) {
            this.blockHash = blockBytes;
        } else {
            this.blockNumber = byteArrayToLong(blockBytes);
        }

        byte[] maxHeaders = paramsList.get(1).getRLPData();
        this.maxHeaders = byteArrayToInt(maxHeaders);

        byte[] skipBlocks = paramsList.get(2).getRLPData();
        this.skipBlocks = byteArrayToInt(skipBlocks);

        byte[] reverse = paramsList.get(3).getRLPData();
        this.reverse = byteArrayToInt(reverse) == 1;

        parsed = true;
    }

    public long getBlockNumber() {
        parse();
        return blockNumber;
    }

    public byte[] getBlockHash() {
        parse();
        return blockHash;
    }

    public BlockIdentifier getBlockIdentifier() {
        parse();
        return new BlockIdentifier(blockHash, blockNumber);
    }

    public int getMaxHeaders() {
        parse();
        return maxHeaders;
    }

    public int getSkipBlocks() {
        parse();
        return skipBlocks;
    }

    public boolean isReverse() {
        parse();
        return reverse;
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    @Override
    public Class<BlockHeadersMessage> getAnswerMessage() {
        return BlockHeadersMessage.class;
    }

    @Override
    public EthMessageCodes getCommand() {
        return EthMessageCodes.GET_BLOCK_HEADERS;
    }

    @Override
    public String toString() {
        parse();
        return "[" + this.getCommand().name() +
                " blockNumber=" + String.valueOf(blockNumber) +
                " blockHash=" + ByteUtil.toHexString(blockHash) +
                " maxHeaders=" + maxHeaders +
                " skipBlocks=" + skipBlocks +
                " reverse=" + reverse + "]";
    }
}

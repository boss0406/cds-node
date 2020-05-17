package org.cds.main.blockchain.core;

import static org.cds.main.blockchain.util.ByteUtil.byteArrayToLong;
import static org.cds.main.blockchain.util.ByteUtil.toHexString;

import java.math.BigInteger;

import org.cds.main.blockchain.util.RLP;
import org.cds.main.blockchain.util.RLPList;


/**
 * Block identifier holds block hash and number <br>
 * This tuple is used in some places of the core,
 */
public class BlockIdentifier {

    /**
     * Block hash
     */
    private byte[] hash;

    /**
     * Block number
     */
    private long number;

    public BlockIdentifier(RLPList rlp) {
        this.hash = rlp.get(0).getRLPData();
        this.number = byteArrayToLong(rlp.get(1).getRLPData());
    }

    public BlockIdentifier(byte[] hash, long number) {
        this.hash = hash;
        this.number = number;
    }

    public byte[] getHash() {
        return hash;
    }

    public long getNumber() {
        return number;
    }

    public byte[] getEncoded() {
        byte[] hash = RLP.encodeElement(this.hash);
        byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.number));

        return RLP.encodeList(hash, number);
    }

    @Override
    public String toString() {
        return "BlockIdentifier {" +
                "hash=" + toHexString(hash) +
                ", number=" + number +
                '}';
    }
}

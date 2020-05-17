package org.cds.main.blockchain.net.rlpx;

import static org.cds.main.blockchain.util.ByteUtil.longToBytesNoLeadZeroes;
import static org.cds.main.blockchain.util.ByteUtil.toHexString;

import org.cds.main.blockchain.crypto.ECKey;
import org.cds.main.blockchain.util.ByteUtil;
import org.cds.main.blockchain.util.RLP;
import org.cds.main.blockchain.util.RLPItem;
import org.cds.main.blockchain.util.RLPList;

public class FindNodeMessage extends Message {

    byte[] target;
    long expires;

    @Override
    public void parse(byte[] data) {

        RLPList list = (RLPList) RLP.decode2OneItem(data, 0);

        RLPItem target = (RLPItem) list.get(0);
        RLPItem expires = (RLPItem) list.get(1);

        this.target = target.getRLPData();
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());
    }


    public static FindNodeMessage create(byte[] target, ECKey privKey) {

        long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        /* RLP Encode data */
        byte[] rlpToken = RLP.encodeElement(target);

        byte[] rlpExp = longToBytesNoLeadZeroes(expiration);
        rlpExp = RLP.encodeElement(rlpExp);

        byte[] type = new byte[]{3};
        byte[] data = RLP.encodeList(rlpToken, rlpExp);

        FindNodeMessage findNode = new FindNodeMessage();
        findNode.encode(type, data, privKey);
        findNode.target = target;
        findNode.expires = expiration;

        return findNode;
    }

    public byte[] getTarget() {
        return target;
    }

    public long getExpires() {
        return expires;
    }

    @Override
    public String toString() {

        long currTime = System.currentTimeMillis() / 1000;

        return String.format("[FindNodeMessage] \n target: %s \n expires in %d seconds \n %s\n",
                toHexString(target), (expires - currTime), super.toString());
    }

}

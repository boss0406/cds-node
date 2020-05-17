package org.cds.main.blockchain.net.rlpx;

import static org.cds.main.blockchain.util.ByteUtil.merge;
import static org.cds.main.blockchain.util.ByteUtil.toHexString;
import static org.spongycastle.util.BigIntegers.asUnsignedByteArray;

import org.cds.main.blockchain.crypto.ECKey;
import org.spongycastle.math.ec.ECPoint;

/**
 * Authentication initiation message, to be wrapped inside
 *
 */
public class AuthInitiateMessage {
    ECKey.ECDSASignature signature; // 65 bytes
    byte[] ephemeralPublicHash; // 32 bytes
    ECPoint publicKey; // 64 bytes - uncompressed and no type byte
    byte[] nonce; // 32 bytes
    boolean isTokenUsed; // 1 byte - 0x00 or 0x01

    public AuthInitiateMessage() {
    }

    public static int getLength() {
        return 65+32+64+32+1;
    }

    static AuthInitiateMessage decode(byte[] wire) {
        AuthInitiateMessage message = new AuthInitiateMessage();
        int offset = 0;
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(wire, offset, r, 0, 32);
        offset += 32;
        System.arraycopy(wire, offset, s, 0, 32);
        offset += 32;
        int v = wire[offset] + 27;
        offset += 1;
        message.signature = ECKey.ECDSASignature.fromComponents(r, s, (byte)v);
        message.ephemeralPublicHash = new byte[32];
        System.arraycopy(wire, offset, message.ephemeralPublicHash, 0, 32);
        offset += 32;
        byte[] bytes = new byte[65];
        System.arraycopy(wire, offset, bytes, 1, 64);
        offset += 64;
        bytes[0] = 0x04; // uncompressed
        message.publicKey = ECKey.CURVE.getCurve().decodePoint(bytes);
        message.nonce = new byte[32];
        System.arraycopy(wire, offset, message.nonce, 0, 32);
        offset += message.nonce.length;
        byte tokenUsed = wire[offset];
        offset += 1;
        if (tokenUsed != 0x00 && tokenUsed != 0x01)
            throw new RuntimeException("invalid boolean"); // TODO specific exception
        message.isTokenUsed = (tokenUsed == 0x01);
        return message;
    }

    public byte[] encode() {

        byte[] rsigPad = new byte[32];
        byte[] rsig = asUnsignedByteArray(signature.r);
        System.arraycopy(rsig, 0, rsigPad, rsigPad.length - rsig.length, rsig.length);

        byte[] ssigPad = new byte[32];
        byte[] ssig = asUnsignedByteArray(signature.s);
        System.arraycopy(ssig, 0, ssigPad, ssigPad.length - ssig.length, ssig.length);

        byte[] sigBytes = merge(rsigPad, ssigPad, new byte[]{EncryptionHandshake.recIdFromSignatureV(signature.v)});

        byte[] buffer = new byte[getLength()];
        int offset = 0;
        System.arraycopy(sigBytes, 0, buffer, offset, sigBytes.length);
        offset += sigBytes.length;
        System.arraycopy(ephemeralPublicHash, 0, buffer, offset, ephemeralPublicHash.length);
        offset += ephemeralPublicHash.length;
        byte[] publicBytes = publicKey.getEncoded(false);
        System.arraycopy(publicBytes, 1, buffer, offset, publicBytes.length - 1);
        offset += publicBytes.length - 1;
        System.arraycopy(nonce, 0, buffer, offset, nonce.length);
        offset += nonce.length;
        buffer[offset] = (byte)(isTokenUsed ? 0x01 : 0x00);
        offset += 1;
        return buffer;
    }

    @Override
    public String toString() {

        byte[] sigBytes = merge(asUnsignedByteArray(signature.r),
                asUnsignedByteArray(signature.s), new byte[]{EncryptionHandshake.recIdFromSignatureV(signature.v)});

        return "AuthInitiateMessage{" +
                "\n  sigBytes=" + toHexString(sigBytes) +
                "\n  ephemeralPublicHash=" + toHexString(ephemeralPublicHash) +
                "\n  publicKey=" + toHexString(publicKey.getEncoded(false)) +
                "\n  nonce=" + toHexString(nonce) +
                "\n}";
    }
}

package org.cds.main.blockchain.net.rlpx;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import org.cds.main.blockchain.net.message.ReasonCode;
import org.cds.main.blockchain.net.server.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.List;

/**
 * Snappy compression codec. <br>
 *
 * Check <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-706.md">EIP-706</a> for details
 *
 */
public class SnappyCodec extends MessageToMessageCodec<FrameCodec.Frame, FrameCodec.Frame> {

    private static final Logger logger = LoggerFactory.getLogger("net");

    private final static int SNAPPY_P2P_VERSION = 5;
    private final static int MAX_SIZE = 16 * 1024 * 1024; // 16 mb

    Channel channel;

    public SnappyCodec(Channel channel) {
        this.channel = channel;
    }

    public static boolean isSupported(int p2pVersion) {
        return p2pVersion >= SNAPPY_P2P_VERSION;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, FrameCodec.Frame msg, List<Object> out) throws Exception {

        // stay consistent with decoding party
        if (msg.size > MAX_SIZE) {
            logger.info("{}: outgoing frame size exceeds the limit ({} bytes), disconnect", channel, msg.size);
            channel.disconnect(ReasonCode.USELESS_PEER);
            return;
        }

        byte[] in = new byte[msg.size];
        msg.payload.read(in);

        byte[] compressed = Snappy.rawCompress(in, in.length);

        out.add(new FrameCodec.Frame((int) msg.type, compressed));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, FrameCodec.Frame msg, List<Object> out) throws Exception {

        byte[] in = new byte[msg.size];
        msg.payload.read(in);

        long uncompressedLength = Snappy.uncompressedLength(in) & 0xFFFFFFFFL;
        if (uncompressedLength > MAX_SIZE) {
            logger.info("{}: uncompressed frame size exceeds the limit ({} bytes), drop the peer", channel, uncompressedLength);
            channel.disconnect(ReasonCode.BAD_PROTOCOL);
            return;
        }

        byte[] uncompressed = new byte[(int) uncompressedLength];
        try {
            Snappy.rawUncompress(in, 0, in.length, uncompressed, 0);
        } catch (IOException e) {
            String detailMessage = e.getMessage();
            // 5 - error code for framed snappy
            if (detailMessage.startsWith("FAILED_TO_UNCOMPRESS") && detailMessage.contains("5")) {
                logger.info("{}: Snappy frames are not allowed in DEVp2p protocol, drop the peer", channel);
                channel.disconnect(ReasonCode.BAD_PROTOCOL);
                return;
            } else {
                throw e;
            }
        }

        out.add(new FrameCodec.Frame((int) msg.type, uncompressed));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (channel.isDiscoveryMode()) {
            logger.trace("SnappyCodec failed: " + cause);
        } else {
            if (cause instanceof IOException) {
                logger.debug("SnappyCodec failed: " + ctx.channel().remoteAddress() + ": " + cause);
            } else {
                logger.warn("SnappyCodec failed: ", cause);
            }
        }
        ctx.close();
    }
}

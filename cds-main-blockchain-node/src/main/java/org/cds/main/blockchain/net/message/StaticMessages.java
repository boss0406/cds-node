package org.cds.main.blockchain.net.message;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cds.main.blockchain.config.SystemProperties;
import org.cds.main.blockchain.net.client.Capability;
import org.cds.main.blockchain.net.client.ConfigCapabilities;
import org.cds.main.blockchain.net.p2p.DisconnectMessage;
import org.cds.main.blockchain.net.p2p.GetPeersMessage;
import org.cds.main.blockchain.net.p2p.HelloMessage;
import org.cds.main.blockchain.net.p2p.PingMessage;
import org.cds.main.blockchain.net.p2p.PongMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class contains static values of messages on the network. These message
 * will always be the same and therefore don't need to be created each time.
 *
 */
@Component
public class StaticMessages {
	private String blockName;
	
    @Autowired
    SystemProperties config;

    @Autowired
    ConfigCapabilities configCapabilities;

    public final static PingMessage PING_MESSAGE = new PingMessage();
    public final static PongMessage PONG_MESSAGE = new PongMessage();
    public final static GetPeersMessage GET_PEERS_MESSAGE = new GetPeersMessage();
    public final static DisconnectMessage DISCONNECT_MESSAGE = new DisconnectMessage(ReasonCode.REQUESTED);
    
    @Autowired
    public void initStaticMessages() {
    	blockName = config.getBlockchainConfig().getCommonConstants().getBlockName();
    }
    
    public HelloMessage createHelloMessage(String peerId) {
        return createHelloMessage(peerId, config.listenPort());
    }
    public HelloMessage createHelloMessage(String peerId, int listenPort) {

        String helloAnnouncement = buildHelloAnnouncement();
        byte p2pVersion = (byte) config.defaultP2PVersion();
        List<Capability> capabilities = configCapabilities.getConfigCapabilities();

        return new HelloMessage(p2pVersion, helloAnnouncement,
                capabilities, listenPort, peerId);
    }

    private String buildHelloAnnouncement() {
        String version = config.projectVersion();
        String numberVersion = version;
        Pattern pattern = Pattern.compile("^\\d+(\\.\\d+)*");
        Matcher matcher = pattern.matcher(numberVersion);
        if (matcher.find()) {
            numberVersion = numberVersion.substring(matcher.start(), matcher.end());
        }
        String system = System.getProperty("os.name");
        if (system.contains(" "))
            system = system.substring(0, system.indexOf(" "));
        if (System.getProperty("java.vm.vendor").contains("Android"))
            system = "Android";
        String phrase = config.helloPhrase();

        return String.format("%s(J)/v%s/%s/%s/Java/%s", blockName, numberVersion, system,
                config.projectVersionModifier().equalsIgnoreCase("release") ? "Release" : "Dev", phrase);
    }
}

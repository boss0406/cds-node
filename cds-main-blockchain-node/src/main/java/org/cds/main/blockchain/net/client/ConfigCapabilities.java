package org.cds.main.blockchain.net.client;

import org.cds.main.blockchain.config.SystemProperties;
import org.cds.main.blockchain.net.eth.EthVersion;
import org.cds.main.blockchain.net.shh.ShhHandler;
import org.cds.main.blockchain.net.swarm.bzz.BzzHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.cds.main.blockchain.net.client.Capability.*;
import static org.cds.main.blockchain.net.eth.EthVersion.fromCode;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

@Component
public class ConfigCapabilities {

    SystemProperties config;

    private SortedSet<Capability> AllCaps = new TreeSet<>();

    @Autowired
    public ConfigCapabilities(final SystemProperties config) {
        this.config = config;
        if (config.syncVersion() != null) {
            EthVersion eth = fromCode(config.syncVersion());
            if (eth != null) AllCaps.add(new Capability(ETH, eth.getCode()));
        } else {
            for (EthVersion v : EthVersion.supported())
                AllCaps.add(new Capability(ETH, v.getCode()));
        }

        AllCaps.add(new Capability(SHH, ShhHandler.VERSION));
        AllCaps.add(new Capability(BZZ, BzzHandler.VERSION));
    }

    /**
     * Gets the capabilities listed in 'peer.capabilities' config property
     * sorted by their names.
     */
    public List<Capability> getConfigCapabilities() {
        List<Capability> ret = new ArrayList<>();
        List<String> caps = config.peerCapabilities();
        for (Capability capability : AllCaps) {
            if (caps.contains(capability.getName())) {
                ret.add(capability);
            }
        }
        return ret;
    }

}

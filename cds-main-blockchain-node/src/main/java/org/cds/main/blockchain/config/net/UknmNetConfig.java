package org.cds.main.blockchain.config.net;

import org.cds.main.blockchain.config.blockchain.UknmConfig;

public class UknmNetConfig extends BaseNetConfig {
	
    public UknmNetConfig() {
        add(0, new UknmConfig());
    }
}

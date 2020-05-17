package org.cds.main.blockchain.config.blockchain;

import java.math.BigInteger;

import org.cds.main.blockchain.config.ConstantsAdapter;

public class CDSPhaseDoneConfig extends CDSMainConfig {
	
	public CDSPhaseDoneConfig() {
		constants = new ConstantsAdapter(super.getConstants()) {
			@Override
            public BigInteger getBLOCK_REWARD() {
            	return BigInteger.ZERO;
            }
		};
	}
	
	
}
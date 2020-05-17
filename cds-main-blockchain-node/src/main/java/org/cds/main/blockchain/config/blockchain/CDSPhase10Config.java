package org.cds.main.blockchain.config.blockchain;

import java.math.BigInteger;

import org.cds.main.blockchain.config.ConstantsAdapter;

public class CDSPhase10Config extends CDSMainConfig {
	
	public CDSPhase10Config() {
		constants = new ConstantsAdapter(super.getConstants()) {
			private final BigInteger BLOCK_REWARD = new BigInteger("3715991240000000000");
			
			@Override
            public BigInteger getBLOCK_REWARD() {
            	return BLOCK_REWARD;
            }
		};
	}
	
	
}
package org.cds.main.blockchain.config.blockchain;

import java.math.BigInteger;
import java.util.List;

import org.cds.main.blockchain.config.ConstantsAdapter;
import org.cds.main.blockchain.db.ByteArrayWrapper;

public class UknmConfig extends AbstractConfig {
	private static int UKNM_CHAIN_ID = 0;
	private static String BLOCK_NAME = "UKNM";
	private long equilibriumTime = 90;
	
	public UknmConfig() {
		constants = new ConstantsAdapter(super.getConstants()) {
			private final BigInteger BLOCK_REWARD = new BigInteger("360000000000000000000");
			@Override
			public String getBlockName() {
				return BLOCK_NAME;
			}
			
            @Override
            public int getMAX_CONTRACT_SZIE() {
                return 0x6000;
            }
            
            @Override
            public BigInteger getBLOCK_REWARD() {
            	return BLOCK_REWARD;
            }
        };
	}

	@Override
	public long getEquilibriumTime() {
		return equilibriumTime;
	}
	
	@Override
    public Integer getChainId() {
        return UKNM_CHAIN_ID;
    }

	@Override
	public List<ByteArrayWrapper> getMinerNodes() {
		return null;
	}
}
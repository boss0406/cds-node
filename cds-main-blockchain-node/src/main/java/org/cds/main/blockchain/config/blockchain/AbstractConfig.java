package org.cds.main.blockchain.config.blockchain;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cds.main.blockchain.config.BlockchainConfig;
import org.cds.main.blockchain.config.BlockchainNetConfig;
import org.cds.main.blockchain.config.Constants;
import org.cds.main.blockchain.core.Block;
import org.cds.main.blockchain.core.BlockHeader;
import org.cds.main.blockchain.core.Repository;
import org.cds.main.blockchain.core.Transaction;
import org.cds.main.blockchain.db.BlockStore;
import org.cds.main.blockchain.util.Utils;
import org.cds.main.blockchain.validator.BlockHeaderValidator;
import org.cds.main.blockchain.vm.DataWord;
import org.cds.main.blockchain.vm.GasCost;
import org.cds.main.blockchain.vm.OpCode;
import org.cds.main.blockchain.vm.program.Program;

/**
 * BlockchainForkConfig is also implemented by this class - its (mostly testing) purpose to represent
 * the specific config for all blocks on the chain (kinda constant config).
 */
public abstract class AbstractConfig implements BlockchainConfig, BlockchainNetConfig {
	
	static final BigInteger SECP256K1N_HALF = Constants.getSECP256K1N().divide(BigInteger.valueOf(2));
	
    protected Constants constants;
    private List<Pair<Long, BlockHeaderValidator>> headerValidators = new ArrayList<>();

    public AbstractConfig() {
        this(new Constants());
    }

    public AbstractConfig(Constants constants) {
        this.constants = constants;
    }
    
    @Override
    public Constants getConstants() {
        return constants;
    }

    @Override
    public BlockchainConfig getConfigForBlock(long blockHeader) {
        return this;
    }

    @Override
    public Constants getCommonConstants() {
        return getConstants();
    }

    @Override
    public BigInteger calcDifficulty(BlockHeader curBlock, BlockHeader parent) {
		if (curBlock.isGenesis()) {
			return curBlock.getDifficultyBI();
		} else {
			BigInteger result = parent.getDifficultyBI().add(new BigInteger("18446744073709551616")
					.divide(curBlock.getDeadLineBI()).divide(new BigInteger(curBlock.getBaseTarget()+"")));
			return BigInteger.ZERO.compareTo(result)==0?BigInteger.ONE:result;
		}
    }

    protected int getExplosion(BlockHeader curBlock, BlockHeader parent) {
        int periodCount = (int) (curBlock.getNumber() / getConstants().getEXP_DIFFICULTY_PERIOD());
        return periodCount - 2;
    }

    @Override
    public boolean acceptTransactionSignature(Transaction tx) {
        if (tx.getSignature() == null) return false;
        if (!tx.getSignature().validateComponents() || tx.getSignature().s.compareTo(SECP256K1N_HALF) > 0) return false;
        return  tx.getChainId() == null || Objects.equals(getChainId(), tx.getChainId());
    
    }

    @Override
    public String validateTransactionChanges(BlockStore blockStore, Block curBlock, Transaction tx,Repository repository) {
        return null;
    }

    @Override
    public void hardForkTransfers(Block block, Repository repo) {}

    @Override
    public byte[] getExtraData(byte[] minerExtraData, long blockNumber) {
        return minerExtraData;
    }

    @Override
    public List<Pair<Long, BlockHeaderValidator>> headerValidators() {
        return headerValidators;
    }


    @Override
    public GasCost getGasCost() {
        return new GasCost() {
        	public int getBALANCE()             {     return 400;     }
            public int getEXT_CODE_SIZE()       {     return 700;     }
            public int getEXT_CODE_COPY()       {     return 700;     }
            public int getSLOAD()               {     return 200;     }
            public int getCALL()                {     return 700;     }
            public int getSUICIDE()             {     return 5000;    }
            public int getNEW_ACCT_SUICIDE()    {     return 25000;   }
            public int getEXP_BYTE_GAS()        {     return 50;      }
        };
    }

    @Override
    public DataWord getCallGas(OpCode op, DataWord requestedGas, DataWord availableGas) throws Program.OutOfGasException {
        DataWord maxAllowed = Utils.allButOne64th(availableGas);
        return requestedGas.compareTo(maxAllowed) > 0 ? maxAllowed : requestedGas;
    }

    @Override
    public DataWord getCreateGas(DataWord availableGas) {
        return Utils.allButOne64th(availableGas);
    }
    
    @Override
	public long getTransactionCost(Transaction tx) {
        long nonZeroes = tx.nonZeroDataBytes();
        long zeroVals  = ArrayUtils.getLength(tx.getData()) - nonZeroes;
        return getGasCost().getTRANSACTION() + zeroVals * getGasCost().getTX_ZERO_DATA() +
                nonZeroes * getGasCost().getTX_NO_ZERO_DATA();
    }
    
    @Override
    public boolean eip161() {
        return true;
    }

    @Override
    public Integer getChainId() {
        return null;
    }

    @Override
    public boolean eip198() {
        return true;
    }

    @Override
    public boolean eip206() {
        return true;
    }

    @Override
    public boolean eip211() {
        return true;
    }

    @Override
    public boolean eip212() {
        return true;
    }

    @Override
    public boolean eip213() {
        return true;
    }

    @Override
    public boolean eip214() {
        return true;
    }

    @Override
    public boolean eip658() {
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}

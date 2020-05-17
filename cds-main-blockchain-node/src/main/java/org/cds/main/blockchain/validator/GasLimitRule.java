package org.cds.main.blockchain.validator;

import java.math.BigInteger;

import org.cds.main.blockchain.config.Constants;
import org.cds.main.blockchain.config.SystemProperties;
import org.cds.main.blockchain.core.BlockHeader;

/**
 * Checks {@link BlockHeader#gasLimit} against {@link Constants#getMIN_GAS_LIMIT}. <br>
 *
 * This check is NOT run in Frontier
 */
public class GasLimitRule extends BlockHeaderRule {

    private final int MIN_GAS_LIMIT;

    public GasLimitRule(SystemProperties config) {
        MIN_GAS_LIMIT = config.getBlockchainConfig().
                getCommonConstants().getMIN_GAS_LIMIT();
    }

    @Override
    public ValidationResult validate(BlockHeader header) {

        if (new BigInteger(1, header.getGasLimit()).compareTo(BigInteger.valueOf(MIN_GAS_LIMIT)) < 0) {
            return fault("header.getGasLimit() < MIN_GAS_LIMIT");
        }

        return Success;
    }
}

package org.cds.main.blockchain.validator;

import static org.cds.main.blockchain.util.BIUtil.isEqual;

import java.math.BigInteger;

import org.cds.main.blockchain.config.SystemProperties;
import org.cds.main.blockchain.core.BlockHeader;

/**
 * Checks block's difficulty against calculated difficulty value
 */

public class DifficultyRule extends DependentBlockHeaderRule {

    private final SystemProperties config;

    public DifficultyRule(SystemProperties config) {
        this.config = config;
    }

    @Override
    public boolean validate(BlockHeader header, BlockHeader parent) {

        errors.clear();

        BigInteger calcDifficulty = header.calcDifficulty(config.getBlockchainConfig(), parent);
        BigInteger difficulty = header.getDifficultyBI();
        
        if (!isEqual(difficulty, calcDifficulty)) {

            errors.add(String.format("#%d: difficulty != calcDifficulty", header.getNumber()));
            return false;
        }

        return true;
    }
}

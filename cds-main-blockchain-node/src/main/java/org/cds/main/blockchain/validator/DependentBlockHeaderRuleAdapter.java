package org.cds.main.blockchain.validator;

import org.cds.main.blockchain.core.BlockHeader;

public class DependentBlockHeaderRuleAdapter extends DependentBlockHeaderRule {

    @Override
    public boolean validate(BlockHeader header, BlockHeader dependency) {
        return true;
    }
}

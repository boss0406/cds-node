package org.cds.main.blockchain.util.blockchain;

import org.cds.main.blockchain.core.CallTransaction;

public interface SolidityFunction {

    SolidityContract getContract();

    CallTransaction.Function getInterface();
}

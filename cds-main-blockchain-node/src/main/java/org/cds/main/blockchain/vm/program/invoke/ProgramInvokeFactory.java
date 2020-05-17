package org.cds.main.blockchain.vm.program.invoke;

import java.math.BigInteger;

import org.cds.main.blockchain.core.Block;
import org.cds.main.blockchain.core.Repository;
import org.cds.main.blockchain.core.Transaction;
import org.cds.main.blockchain.db.BlockStore;
import org.cds.main.blockchain.vm.DataWord;
import org.cds.main.blockchain.vm.program.Program;

public interface ProgramInvokeFactory {

    ProgramInvoke createProgramInvoke(Transaction tx, Block block,
                                      Repository repository, BlockStore blockStore);

    ProgramInvoke createProgramInvoke(Program program, DataWord toAddress, DataWord callerAddress,
                                             DataWord inValue, DataWord inGas,
                                             BigInteger balanceInt, byte[] dataIn,
                                             Repository repository, BlockStore blockStore,
                                            boolean staticCall, boolean byTestingSuite);


}

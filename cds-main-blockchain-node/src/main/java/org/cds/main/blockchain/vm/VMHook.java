package org.cds.main.blockchain.vm;

import org.cds.main.blockchain.vm.program.Program;

public interface VMHook {
    void startPlay(Program program);
    void step(Program program, OpCode opcode);
    void stopPlay(Program program);
}

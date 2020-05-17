package org.cds.main.blockchain.config.net;

import org.cds.main.blockchain.config.blockchain.CDSPhase01Config;
import org.cds.main.blockchain.config.blockchain.CDSPhase02Config;
import org.cds.main.blockchain.config.blockchain.CDSPhase03Config;
import org.cds.main.blockchain.config.blockchain.CDSPhase04Config;
import org.cds.main.blockchain.config.blockchain.CDSPhase05Config;
import org.cds.main.blockchain.config.blockchain.CDSPhase06Config;
import org.cds.main.blockchain.config.blockchain.CDSPhase07Config;
import org.cds.main.blockchain.config.blockchain.CDSPhase08Config;
import org.cds.main.blockchain.config.blockchain.CDSPhase09Config;
import org.cds.main.blockchain.config.blockchain.CDSPhase10Config;
import org.cds.main.blockchain.config.blockchain.CDSPhaseDoneConfig;

public class CDSMainNetConfig extends BaseNetConfig {
	
    public CDSMainNetConfig() {
    	long blockNumPerYear = 262800L;
        add(blockNumPerYear * 0, new CDSPhase01Config());
        add(blockNumPerYear * 1, new CDSPhase02Config());
        add(blockNumPerYear * 2, new CDSPhase03Config());
        add(blockNumPerYear * 3, new CDSPhase04Config());
        add(blockNumPerYear * 4, new CDSPhase05Config());
        add(blockNumPerYear * 5, new CDSPhase06Config());
        add(blockNumPerYear * 6, new CDSPhase07Config());
        add(blockNumPerYear * 7, new CDSPhase08Config());
        add(blockNumPerYear * 8, new CDSPhase09Config());
        add(blockNumPerYear * 9, new CDSPhase10Config());
        add(blockNumPerYear * 10, new CDSPhaseDoneConfig());
    }
}
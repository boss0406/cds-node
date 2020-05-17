package org.cds.main.blockchain.config.blockchain;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.cds.main.blockchain.config.ConstantsAdapter;
import org.cds.main.blockchain.db.ByteArrayWrapper;
import org.cds.main.blockchain.util.ByteUtil;



public abstract class CDSMainConfig extends AbstractConfig {
	private String[] minerNodeAddr= {
		"0x6a01f85812af958021a4b64a467c1d954d867381",
		"0x81e5079ffe297d1366a43a414fafdd11ed9209cf",
		"0x10084c7eae3a87695a0a999ab392cbfce8875dc6",
		"0x8552ea6004a479e6bf0152ad5cf483c7fde5880c",
		"0x7ad2b39cd40cb4e97bb1a34cb524c7205ec55330",
		"0x8df1ab382301a2ba0acf94e24164f5a3e90ac951",
		"0xbd7677eb43948a4c44fd0891a348247ec4d59af3",
		"0x25492318fdde573f1ce57788ccb1968a2f4d4e8a",
		"0x0c45b4861828a41951d9e3efca6d5609d2a200b9",
		"0xf5a6de797bf5d3839992659b0e620ce4717d698a",
		"0xa3be08958d5496cc88e9546cecb33b710c2844ab",
		"0x80b13af605ccc7ebf688831da0cb13db42b40b98",
		"0xa47aeecb1bb5e46ca0d7baa71d5cee6bc7036b50",
		"0x461d016d1d831ceda8b3e825bba636e489ccd0b1",
		"0xc5918a9222a04a756092c3ef70fd86259ebfc0e7",
		"0x088706230fbc03fe302c48732abfc317d04f036b",
		"0x2b2473aaafa9547581d3cd145387af17ee5d0897",
		"0x7ca4ad97003fd5e050866126f72e130bcecac240",
		"0xee8e121b2642bdc2c37a14d4573f0cf7822013a9",
		"0xbfbeb9acee682bbbf0c2e318c54f4bde561676b8",
		"0xf3bfef9a29322ba32de5a9edd3ec1a1e7469c7f7",
		"0xa4a908fb89a5e636a27463eadcea56179ad5aa20",
		"0x26d79a3f5e84b6a408a285e65eb6ceee3ff75516",
		"0xb681758119f19a0d9a6988a9dc0d514b3414a474",
		"0xa4da6259bd6bfb31a1c823d8dcc64e6149b19a31",
		"0xc931e77cc3e3f4922395d1d0904322948743ec35",
		"0x712957a434e724fad84400de9a10efed333f88f7",
		"0xa9dc9d40fa152b98b8ef48ef9278b4d3841d8b1c",
		"0x0f10667f608fc3725ef015652bfa6d43eafaf3fd",
		"0x8ebb21ab80c4847ce37775c36ae8933e748ee0c9",
	};
	private static int CDS_CHAIN_ID = 1;
	private static String BLOCK_NAME = "CDS";
	private long equilibriumTime = 120;
	private List<ByteArrayWrapper> minerNodeWrapper;
	
	public CDSMainConfig() {
		constants = new ConstantsAdapter(super.getConstants()) {
			@Override
			public String getBlockName() {
				return BLOCK_NAME;
			}
			
            @Override
            public int getMAX_CONTRACT_SZIE() {
                return 0x6000;
            }
        };
        minerNodeWrapper = Arrays.asList(minerNodeAddr).stream()
        		.map(addr->new ByteArrayWrapper(ByteUtil.hexStringToBytes(addr)))
        		.collect(Collectors.toList());
	}

	@Override
	public long getEquilibriumTime() {
		return equilibriumTime;
	}
	
	@Override
    public Integer getChainId() {
        return CDS_CHAIN_ID;
    }

	@Override
	public List<ByteArrayWrapper> getMinerNodes() {
		return minerNodeWrapper;
	}
}
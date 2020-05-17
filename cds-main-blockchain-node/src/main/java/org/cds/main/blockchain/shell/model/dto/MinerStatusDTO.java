package org.cds.main.blockchain.shell.model.dto;

public class MinerStatusDTO {

    private final String status;

	public MinerStatusDTO(String status) {
		super();
		this.status = status;
	}

	public String getStatus() {
		return status;
	}
    
}

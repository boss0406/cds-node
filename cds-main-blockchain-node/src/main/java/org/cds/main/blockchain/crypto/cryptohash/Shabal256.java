package org.cds.main.blockchain.crypto.cryptohash;

public class Shabal256 extends ShabalGeneric {

	/**
	 * Create the engine.
	 */
	public Shabal256()
	{
		super(256);
	}

	/** @see ShabalGeneric */
	ShabalGeneric dup()
	{
		return new Shabal256();
	}
}

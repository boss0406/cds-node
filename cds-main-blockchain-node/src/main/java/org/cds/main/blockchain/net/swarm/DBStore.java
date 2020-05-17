package org.cds.main.blockchain.net.swarm;

import org.cds.main.blockchain.datasource.DbSource;

/**
 * ChunkStore backed up with KeyValueDataSource
 *
 */
public class DBStore implements ChunkStore {
    private DbSource<byte[]> db;

    public DBStore(DbSource db) {
        this.db = db;
    }

    @Override
    public void put(Chunk chunk) {
        db.put(chunk.getKey().getBytes(), chunk.getData());
    }

    @Override
    public Chunk get(Key key) {
        byte[] bytes = db.get(key.getBytes());
        return bytes == null ? null : new Chunk(key, bytes);
    }
}

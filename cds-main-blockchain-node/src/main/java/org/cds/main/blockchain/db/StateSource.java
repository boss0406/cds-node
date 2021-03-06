package org.cds.main.blockchain.db;

import org.cds.main.blockchain.config.CommonConfig;
import org.cds.main.blockchain.config.SystemProperties;
import org.cds.main.blockchain.datasource.*;
import org.springframework.beans.factory.annotation.Autowired;

public class StateSource extends SourceChainBox<byte[], byte[], byte[], byte[]>
        implements HashedKeySource<byte[], byte[]> {

    // for debug purposes
    public static StateSource INST;

    JournalSource<byte[]> journalSource;
    NoDeleteSource<byte[], byte[]> noDeleteSource;

    ReadCache<byte[], byte[]> readCache;
    AbstractCachedSource<byte[], byte[]> writeCache;

    public StateSource(Source<byte[], byte[]> src, boolean pruningEnabled) {
        super(src);
        INST = this;
        add(readCache = new ReadCache.BytesKey<>(src).withMaxCapacity(16 * 1024 * 1024 / 512)); // 512 - approx size of a node
        readCache.setFlushSource(true);
        writeCache = new AsyncWriteCache<byte[], byte[]>(readCache) {
            @Override
            protected WriteCache<byte[], byte[]> createCache(Source<byte[], byte[]> source) {
                WriteCache.BytesKey<byte[]> ret = new WriteCache.BytesKey<byte[]>(source, WriteCache.CacheType.SIMPLE);
                ret.withSizeEstimators(MemSizeEstimator.ByteArrayEstimator, MemSizeEstimator.ByteArrayEstimator);
                ret.setFlushSource(true);
                return ret;
            }
        }.withName("state");

        add(writeCache);

        if (pruningEnabled) {
            add(journalSource = new JournalSource<>(writeCache));
        } else {
            add(noDeleteSource = new NoDeleteSource<>(writeCache));
        }
    }

    @Autowired
    public void setConfig(SystemProperties config) {
        int size = config.getConfig().getInt("cache.stateCacheSize");
        readCache.withMaxCapacity(size * 1024 * 1024 / 512); // 512 - approx size of a node
    }

    @Autowired
    public void setCommonConfig(CommonConfig commonConfig) {
        if (journalSource != null) {
            journalSource.setJournalStore(commonConfig.cachedDbSource("journal"));
        }
    }

    public JournalSource<byte[]> getJournalSource() {
        return journalSource;
    }

    /**
     * Returns the source behind JournalSource
     */
    public Source<byte[], byte[]> getNoJournalSource() {
        return writeCache;
    }

    public AbstractCachedSource<byte[], byte[]> getWriteCache() {
        return writeCache;
    }

    public ReadCache<byte[], byte[]> getReadCache() {
        return readCache;
    }
}

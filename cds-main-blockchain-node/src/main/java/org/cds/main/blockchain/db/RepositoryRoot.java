package org.cds.main.blockchain.db;

import org.cds.main.blockchain.core.AccountState;
import org.cds.main.blockchain.core.Repository;
import org.cds.main.blockchain.datasource.*;
import org.cds.main.blockchain.trie.*;
import org.cds.main.blockchain.vm.DataWord;

public class RepositoryRoot extends RepositoryImpl {

    private static class StorageCache extends ReadWriteCache<DataWord, DataWord> {
        Trie<byte[]> trie;

        public StorageCache(Trie<byte[]> trie) {
            super(new SourceCodec<>(trie, Serializers.StorageKeySerializer, Serializers.StorageValueSerializer), WriteCache.CacheType.SIMPLE);
            this.trie = trie;
        }
    }

    private class MultiStorageCache extends MultiCache<StorageCache> {
        public MultiStorageCache() {
            super(null);
        }
        @Override
        protected synchronized StorageCache create(byte[] key, StorageCache srcCache) {
            AccountState accountState = accountStateCache.get(key);
            Serializer<byte[], byte[]> keyCompositor = new NodeKeyCompositor(key);
            Source<byte[], byte[]> composingSrc = new SourceCodec.KeyOnly<>(trieCache, keyCompositor);
            TrieImpl storageTrie = createTrie(composingSrc, accountState == null ? null : accountState.getStateRoot());
            return new StorageCache(storageTrie);
        }

        @Override
        protected synchronized boolean flushChild(byte[] key, StorageCache childCache) {
            if (super.flushChild(key, childCache)) {
                if (childCache != null) {
                    AccountState storageOwnerAcct = accountStateCache.get(key);
                    // need to update account storage root
                    childCache.trie.flush();
                    byte[] rootHash = childCache.trie.getRootHash();
                    accountStateCache.put(key, storageOwnerAcct.withStateRoot(rootHash));
                    return true;
                } else {
                    // account was deleted
                    return true;
                }
            } else {
                // no storage changes
                return false;
            }
        }
    }

    private Source<byte[], byte[]> stateDS;
    private CachedSource.BytesKey<byte[]> trieCache;
    private Trie<byte[]> stateTrie;

    public RepositoryRoot(Source<byte[], byte[]> stateDS) {
        this(stateDS, null);
    }

    /**
     * Building the following structure for snapshot Repository:
     *
     * stateDS --> trieCache --> stateTrie --> accountStateCodec --> accountStateCache
     *  \                 \
     *   \                 \-->>> storageKeyCompositor --> contractStorageTrie --> storageCodec --> storageCache
     *    \--> codeCache
     *
     *
     * @param stateDS
     * @param root
     */
    public RepositoryRoot(final Source<byte[], byte[]> stateDS, byte[] root) {
        this.stateDS = stateDS;

        trieCache = new WriteCache.BytesKey<>(stateDS, WriteCache.CacheType.COUNTING);
        stateTrie = new SecureTrie(trieCache, root);

        SourceCodec.BytesKey<AccountState, byte[]> accountStateCodec = new SourceCodec.BytesKey<>(stateTrie, Serializers.AccountStateSerializer);
        final ReadWriteCache.BytesKey<AccountState> accountStateCache = new ReadWriteCache.BytesKey<>(accountStateCodec, WriteCache.CacheType.SIMPLE);

        final MultiCache<StorageCache> storageCache = new MultiStorageCache();

        // counting as there can be 2 contracts with the same code, 1 can suicide
        Source<byte[], byte[]> codeCache = new WriteCache.BytesKey<>(stateDS, WriteCache.CacheType.COUNTING);

        init(accountStateCache, codeCache, storageCache);
    }

    @Override
    public synchronized void commit() {
        super.commit();

        stateTrie.flush();
        trieCache.flush();
    }

    @Override
    public synchronized byte[] getRoot() {
        storageCache.flush();
        accountStateCache.flush();

        return stateTrie.getRootHash();
    }

    @Override
    public synchronized void flush() {
        commit();
    }

    @Override
    public Repository getSnapshotTo(byte[] root) {
        return new RepositoryRoot(stateDS, root);
    }

    @Override
    public synchronized String dumpStateTrie() {
        return ((TrieImpl) stateTrie).dumpTrie();
    }

    @Override
    public synchronized void syncToRoot(byte[] root) {
        stateTrie.setRoot(root);
    }

    protected TrieImpl createTrie(Source<byte[], byte[]> trieCache, byte[] root) {
        return new SecureTrie(trieCache, root);
    }

}

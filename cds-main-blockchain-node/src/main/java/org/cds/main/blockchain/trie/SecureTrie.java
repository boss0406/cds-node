package org.cds.main.blockchain.trie;

import static org.cds.main.blockchain.crypto.HashUtil.sha3;
import static org.cds.main.blockchain.util.ByteUtil.EMPTY_BYTE_ARRAY;

import org.cds.main.blockchain.datasource.Source;
import org.cds.main.blockchain.util.Value;

public class SecureTrie extends TrieImpl {

    public SecureTrie(byte[] root) {
        super(root);
    }

    public SecureTrie(Source<byte[], byte[]> cache) {
        super(cache, null);
    }

    public SecureTrie(Source<byte[], byte[]> cache, byte[] root) {
        super(cache, root);
    }

    @Override
    public byte[] get(byte[] key) {
        return super.get(sha3(key));
    }

    @Override
    public void put(byte[] key, byte[] value) {
        super.put(sha3(key), value);
    }

    @Override
    public void delete(byte[] key) {
        put(key, EMPTY_BYTE_ARRAY);
    }
}

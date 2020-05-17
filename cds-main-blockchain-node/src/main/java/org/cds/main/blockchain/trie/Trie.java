package org.cds.main.blockchain.trie;

import org.cds.main.blockchain.datasource.Source;

public interface Trie<V> extends Source<byte[], V> {

    byte[] getRootHash();

    void setRoot(byte[] root);

    /**
     * Recursively delete all nodes from root
     */
    void clear();
}

package org.cds.main.blockchain.trie;

import org.cds.main.blockchain.util.Value;

public class CountAllNodes implements TrieImpl.ScanAction {

    int counted = 0;

    @Override
    public void doOnNode(byte[] hash, TrieImpl.Node node) {
        ++counted;
    }

    @Override
    public void doOnValue(byte[] nodeHash, TrieImpl.Node node, byte[] key, byte[] value) {}

    public int getCounted() {
        return counted;
    }
}

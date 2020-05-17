package org.cds.main.blockchain.contrdata.storage;


import java.util.List;

public class StoragePage
{
    private List<StorageEntry> entries;
    private int number;
    private int size;
    private int total;
    
    public List<StorageEntry> getEntries() {
        return this.entries;
    }
    
    public int getNumber() {
        return this.number;
    }
    
    public int getSize() {
        return this.size;
    }
    
    public int getTotal() {
        return this.total;
    }
    
    public StoragePage(final List<StorageEntry> entries, final int number, final int size, final int total) {
        this.entries = entries;
        this.number = number;
        this.size = size;
        this.total = total;
    }
}

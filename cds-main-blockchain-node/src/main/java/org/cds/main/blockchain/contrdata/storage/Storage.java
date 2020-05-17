package org.cds.main.blockchain.contrdata.storage;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.cds.main.blockchain.core.Repository;
import org.cds.main.blockchain.vm.DataWord;

public interface Storage
{
    int size(final byte[] p0);
    
    Map<DataWord, DataWord> entries(final byte[] p0, final List<DataWord> p1);
    
    Set<DataWord> keys(final byte[] p0);
    
    DataWord get(final byte[] p0, final DataWord p1);
    
    default Storage fromMap(final Map<DataWord, DataWord> map) {
        return new Storage() {
            @Override
            public int size(final byte[] address) {
                return map.size();
            }
            
            @Override
            public Map<DataWord, DataWord> entries(final byte[] address, final List<DataWord> keys) {
                final Map<DataWord, DataWord> result = new HashMap<DataWord, DataWord>();
                for (final DataWord key : keys) {
                    final DataWord value = map.get(key);
                    if (Objects.nonNull(value)) {
                        result.put(key, value);
                    }
                }
                return result;
            }
            
            @Override
            public Set<DataWord> keys(final byte[] address) {
                return map.keySet();
            }
            
            @Override
            public DataWord get(final byte[] address, final DataWord key) {
                return map.get(key);
            }
        };
    }
    
    default Storage fromRepo(final Repository repository) {
        return new Storage() {
            @Override
            public int size(final byte[] address) {
                return repository.getContractDetails(address).getStorageSize();
            }
            
            @Override
            public Map<DataWord, DataWord> entries(final byte[] address, final List<DataWord> keys) {
                return (Map<DataWord, DataWord>)repository.getContractDetails(address).getStorage(keys);
            }
            
            @Override
            public Set<DataWord> keys(final byte[] address) {
                return (Set<DataWord>)repository.getContractDetails(address).getStorageKeys();
            }
            
            @Override
            public DataWord get(final byte[] address, final DataWord key) {
                return repository.getContractDetails(address).get(key);
            }
        };
    }
}

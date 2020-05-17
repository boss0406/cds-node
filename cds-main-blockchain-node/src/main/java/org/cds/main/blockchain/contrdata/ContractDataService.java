package org.cds.main.blockchain.contrdata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.cds.main.blockchain.contrdata.contract.Ast;
import org.cds.main.blockchain.contrdata.contract.ContractData;
import org.cds.main.blockchain.contrdata.contract.Member;
import org.cds.main.blockchain.contrdata.contract.Members;
import org.cds.main.blockchain.contrdata.storage.Path;
import org.cds.main.blockchain.contrdata.storage.Storage;
import org.cds.main.blockchain.contrdata.storage.StorageEntry;
import org.cds.main.blockchain.contrdata.storage.StoragePage;
import org.cds.main.blockchain.contrdata.storage.dictionary.Layout;
import org.cds.main.blockchain.contrdata.storage.dictionary.StorageDictionary;
import org.cds.main.blockchain.contrdata.storage.dictionary.StorageDictionaryDb;
import org.cds.main.blockchain.datasource.Source;
import org.cds.main.blockchain.util.ByteUtil;
import org.cds.main.blockchain.vm.DataWord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ContractDataService {
	private static Logger log;
    @Autowired
    private StorageDictionaryDb dictionaryDb;
    @Autowired
    private Storage storage;
    
    public StoragePage getStorageEntries(byte[] address, int page, int size) {
        List<StorageEntry> entries = Collections.emptyList();
        int storageSize = this.storage.size(address);
        if (storageSize > 0) {
            int offset = page * size;
            int fromIndex = Math.max(offset, 0);
            int toIndex = Math.min(storageSize, offset + size);
            if (fromIndex < toIndex) {
                List<DataWord> keys = this.storage.keys(address).stream().sorted().collect(Collectors.toList()).subList(fromIndex, toIndex);
                entries = this.storage.entries(address, keys).entrySet().stream().map(StorageEntry::raw).sorted().collect(Collectors.toList());
            }
        }
        return new StoragePage(entries, page, size, storageSize);
    }
    
    private StorageDictionary getDictionary(byte[] address) {
        return this.dictionaryDb.getDictionaryFor(Layout.Lang.solidity, address);
    }
    
    private StorageDictionary getDictionary(byte[] address, Set<DataWord> hashFilter) {
        return this.getDictionary(address).getFiltered(hashFilter);
    }
    
    public StoragePage getStructuredStorageEntries(byte[] address, StorageDictionary dictionary, Path path, int page, int size) {
        try {
            StorageDictionary.PathElement pathElement = dictionary.getByPath(path.parts());
            List<StorageEntry> entries = pathElement.getChildren(page * size, size).stream().map(pe -> StorageEntry.structured(pe, key -> this.storage.get(address, key))).collect(Collectors.toList());
            return new StoragePage(entries, page, size, pathElement.getChildrenCount());
        }
        catch (Exception e) {
            ContractDataService.log.error(DetailedMsg.withTitle("Cannot build contract structured storage:", new Object[0]).add("address", address).add("path", path).add("storageDictionary", dictionary.dmp()).add("storage", this.storageEntries(address)).toJson());
            throw e;
        }
    }
    
    public Map<String, String> exportDictionary(byte[] address, Path path) {
        Map<String, String> result = new HashMap<String, String>();
        StorageDictionary.PathElement pathElement = this.getDictionary(address).getByPath(path.parts());
        StorageDictionary.dmp(pathElement, result);
        return result;
    }
    
    public void importDictionary(byte[] address, Map<String, String> toImport) {
        this.clearDictionary(address);
        StorageDictionary dictionary = this.dictionaryDb.getDictionaryFor(Layout.Lang.solidity, address);
        try {
            Source<byte[], byte[]> dataSource = dictionary.getStorageDb();
            toImport.forEach((key, value) -> dataSource.put(Hex.decode(key), Hex.decode(value)));
        }
        finally {
            this.dictionaryDb.flush();
        }
    }
    
    public void clearDictionary(byte[] address) {
        StorageDictionary dictionary = this.dictionaryDb.getDictionaryFor(Layout.Lang.solidity, address);
        try {
            Source<byte[], byte[]> source = dictionary.getStorageDb();
            dictionary.allKeys().forEach(key -> source.delete(key.getData()));
        }
        finally {
            this.dictionaryDb.flush();
        }
    }
    
    public StoragePage getStructuredStorageEntries(String address, Path path, int page, int size) {
        byte[] addr = Hex.decode(address);
        StorageDictionary dictionary = this.getDictionary(addr);
        return this.getStructuredStorageEntries(addr, dictionary, path, page, size);
    }
    
    public StoragePage getStructuredStorageDiffEntries(String transactionHash, String address, Path path, int page, int size) {
        byte[] contractAddress = Hex.decode(address);
        byte[] txHash = Hex.decode(transactionHash);
        StorageDictionary dictionary = this.getDictionary(contractAddress, this.storage.keys(txHash));
        return this.getStructuredStorageEntries(txHash, dictionary, path, page, size);
    }
    
    public StoragePage getContractData(byte[] address, ContractData contractData, boolean ignoreEmpty, Path path, int page, int size) {
        try {
            ContractData.Element element = contractData.elementByPath((Object[])path.parts());
            List<StorageEntry> entries = element.getChildren(page, size, ignoreEmpty).stream().map(el -> StorageEntry.smart(el, key -> this.storage.get(address, key))).collect(Collectors.toList());
            return new StoragePage(entries, page, size, element.getChildrenCount());
        }
        catch (Exception e) {
            ContractDataService.log.error(DetailedMsg.withTitle("Cannot build smart contract data:", new Object[0]).add("address", address).add("path", path).add("dataMembers", contractData.getContract()).add("storageDictionary", contractData.getDictionary().dmp()).add("storage", this.storageEntries(address)).toJson());
            throw e;
        }
    }
    
    public StoragePage getContractData(String address, String contractDataJson, Path path, int page, int size) {
        byte[] contractAddress = Hex.decode(address);
        StorageDictionary dictionary = this.getDictionary(contractAddress);
        ContractData contractData = ContractData.parse(contractDataJson, dictionary);
        return this.getContractData(contractAddress, contractData, false, path, page, size);
    }
    
    public void fillMissingKeys(ContractData contractData) {
        StorageDictionary.PathElement root = contractData.getDictionary().getByPath(new String[0]);
        fillKeys(contractData, root, contractData.getMembers(), 0);
    }
    
    private static void fillKeys(ContractData contractData, StorageDictionary.PathElement root, List<Member> members, int addition) {
        members.forEach(member -> {
            if (member.getType().isElementary()) {
            	StorageDictionary.PathElement pe = new StorageDictionary.PathElement();
                pe.type = StorageDictionary.PathElement.Type.StorageIndex;
                pe.key = String.valueOf(member.getStorageIndex() + addition);
                pe.storageKey = new DataWord(ByteUtil.intToBytes(member.getStorageIndex() + addition)).getData();
                root.addChild(pe);
            }
            else if (member.getType().isStruct()) {
            	Ast.Type.Struct struct = member.getType().asStruct();
            	Members structFields = contractData.getStructFields(struct);
                fillKeys(contractData, root, structFields, member.getStorageIndex() + addition);
            }
            else if (member.getType().isStaticArray()) {
            	Ast.Type.Array array = member.getType().asArray();
                for (int i = 0; i < array.getSize(); ++i) {
                	Ast.Variable variable = new Ast.Variable();
                    variable.setType(array.getElementType());
                    Member subMember = new Member(null, variable, contractData);
                    fillKeys(contractData, root, Arrays.asList(subMember), member.getStorageIndex() + i);
                }
            }
        });
    }
    
    public StoragePage getContractDataDiff(String transactionHash, String address, String contractDataJson, Path path, int page, int size) {
        byte[] contractAddress = Hex.decode(address);
        byte[] txHash = Hex.decode(transactionHash);
        StorageDictionary dictionary = this.getDictionary(contractAddress, this.storage.keys(txHash));
        ContractData contractData = ContractData.parse(contractDataJson, dictionary);
        return this.getContractData(txHash, contractData, true, path, page, size);
    }
    
    public Map<DataWord, DataWord> storageEntries(byte[] address) {
        Set<DataWord> keys = this.storage.keys(address);
        return this.storage.entries(address, new ArrayList<DataWord>(keys));
    }
    
    static {
        log = LoggerFactory.getLogger("contract-data");
    }
    
    private static class DetailedMsg extends LinkedHashMap<String, Object>
    {
		private static final long serialVersionUID = 1L;

		public static DetailedMsg withTitle(String title, Object... args) {
            return new DetailedMsg().add("title", String.format(title, args));
        }
        
        public DetailedMsg add(String title, Object value) {
            this.put(title, value);
            return this;
        }
        
        public DetailedMsg add(String title, byte[] bytes) {
            return this.add(title, ByteUtil.toHexString(bytes));
        }
        
        public String toJson() {
            try {
                return new ObjectMapper().writeValueAsString((Object)this);
            }
            catch (JsonProcessingException e) {
                return "Cannot format JSON message cause: " + e.getMessage();
            }
        }
        
        @Override
        public String toString() {
            return this.entrySet().stream().map(entry -> entry.getKey() + ": " + Objects.toString(entry.getValue())).collect(Collectors.joining("\n"));
        }
    }
}

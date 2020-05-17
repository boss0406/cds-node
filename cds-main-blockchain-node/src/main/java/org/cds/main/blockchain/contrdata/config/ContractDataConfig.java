package org.cds.main.blockchain.contrdata.config;

import org.cds.main.blockchain.config.SystemProperties;
import org.cds.main.blockchain.datasource.DbSource;
import org.cds.main.blockchain.datasource.leveldb.LevelDbDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({ "org.cds.main.blockchain.contrdata" })
public class ContractDataConfig
{
    @Bean
    public SystemProperties systemProperties() {
        return SystemProperties.getDefault();
    }
    
    @Bean
    public DbSource<byte[]> storageDict() {
        final DbSource<byte[]> dataSource = (DbSource<byte[]>)new LevelDbDataSource("storageDict");
        dataSource.init();
        return dataSource;
    }
}

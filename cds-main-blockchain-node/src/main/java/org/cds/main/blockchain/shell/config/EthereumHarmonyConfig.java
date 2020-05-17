package org.cds.main.blockchain.shell.config;

import org.cds.main.blockchain.config.CommonConfig;
import org.cds.main.blockchain.config.NoAutoscan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


/**
 * Override default blockchain config to apply custom configuration.
 * This is entry point for starting EthereumJ core beans.
 */
@Configuration
@ComponentScan(
        basePackages = "org.cds.main.blockchain",
        excludeFilters = @ComponentScan.Filter(NoAutoscan.class))
public class EthereumHarmonyConfig extends CommonConfig {
}

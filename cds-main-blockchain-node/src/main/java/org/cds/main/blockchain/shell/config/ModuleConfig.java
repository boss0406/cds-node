package org.cds.main.blockchain.shell.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.servlet.Filter;

import org.cds.main.blockchain.datasource.DbSource;
import org.cds.main.blockchain.datasource.leveldb.LevelDbDataSource;
import org.cds.main.blockchain.shell.service.ClientMessageService;
import org.cds.main.blockchain.shell.service.ClientMessageServiceDummy;
import org.cds.main.blockchain.shell.service.ClientMessageServiceImpl;
import org.cds.main.blockchain.shell.service.contracts.ContractsService;
import org.cds.main.blockchain.shell.service.contracts.ContractsServiceImpl;
import org.cds.main.blockchain.shell.service.contracts.DisabledContractService;
import org.cds.main.blockchain.shell.util.exception.Web3jSafeAnnotationsErrorResolver;
import org.apache.catalina.connector.Connector;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.HiddenHttpMethodFilter;

@Configuration
public class ModuleConfig {

    private Integer firstPort;
    private Integer secondPort = null;
    private HarmonyProperties props = HarmonyProperties.DEFAULT;

    private static final String ERROR_RESOLVER_KEY = "jsonrpc.web3jCompliantError";

    public ModuleConfig() {
        LinkedHashSet<Integer> portSet = new LinkedHashSet<>();

        if (props.webPort() != null) {
            portSet.add(props.webPort());
        }

        if (props.rpcPort() != null) {
            portSet.add(props.rpcPort());
        }

        // fallback
        if (portSet.isEmpty()) {
            portSet.add(8080);
        }

        List<Integer> ports = new ArrayList<>(portSet);

        firstPort = ports.get(0);
        if (ports.size() == 2) {
            secondPort = ports.get(1);
        }
    }

    // Common //

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return (container -> {
            container.setPort(firstPort);
        });
    }

    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
        if (secondPort != null) {
            Connector[] additionalConnectors = new Connector[1];
            Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
            connector.setScheme("http");
            connector.setPort(secondPort);
            additionalConnectors[0] = connector;
            tomcat.addAdditionalTomcatConnectors(additionalConnectors);
        }
        return tomcat;
    }

    /**
     * Configuration of filter which rejects requests to a web or rpc service
     * if called on the wrong port
     */
    @Bean
    public Filter modulePortFilter() {
        if (secondPort != null) {
            return new ModulePortFilter(HarmonyProperties.DEFAULT.rpcPort(), HarmonyProperties.DEFAULT.webPort());
        } else {
            return ModulePortFilter.DUMMY;
        }
    }

    // RPC //

    /**
     * Export bean which will find our json-rpc bean with @JsonRpcService and publish it.
     * https://github.com/briandilley/jsonrpc4j/issues/69
     */
    @Bean
    @Conditional(RpcEnabledCondition.class)
    @SuppressWarnings({"deprecation"})
    // full class path to avoid deprecation warning
    public com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceExporter exporter() {
        com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceExporter serviceExporter = new com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceExporter();

        if ("true".equalsIgnoreCase(System.getProperty(ERROR_RESOLVER_KEY, ""))) {
            serviceExporter.setErrorResolver(Web3jSafeAnnotationsErrorResolver.INSTANCE);
        }

        return serviceExporter;
    }

    /**
     * With this code we aren't required to pass explicit "Content-Type: application/json" in curl.
     * Found at https://github.com/spring-projects/spring-boot/issues/4782
     */
    @Bean
    @Conditional(RpcEnabledCondition.class)
    // full class path to avoid deprecation warning
    public FilterRegistrationBean registration(HiddenHttpMethodFilter filter) {
        FilterRegistrationBean registration = new FilterRegistrationBean(filter);
        registration.setEnabled(false);
        return registration;
    }

    // Web //

    @Bean
    public ClientMessageService clientMessageService() {
        if (WebEnabledCondition.matches()) {
            return new ClientMessageServiceImpl();
        } else {
            return new ClientMessageServiceDummy();
        }
    }

    // Contracts //

    @Bean("contractSettingsStorage")
    DbSource<byte[]> contractSettingsStorage() {
        DbSource<byte[]> settingsStorage = new LevelDbDataSource("settings");
        settingsStorage.init();

        return settingsStorage;
    }

    @Bean
    ContractsService contractsService() {
        if (props.isContractStorageEnabled()) {
            return new ContractsServiceImpl();
        } else {
            return new DisabledContractService(contractSettingsStorage());
        }
    }
}

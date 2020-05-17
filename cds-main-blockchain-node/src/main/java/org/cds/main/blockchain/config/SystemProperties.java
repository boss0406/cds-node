package org.cds.main.blockchain.config;

import static org.cds.main.blockchain.crypto.HashUtil.sha3;
import static org.cds.main.blockchain.util.ByteUtil.toHexString;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.cds.main.blockchain.config.net.UknmNetConfig;
import org.cds.main.blockchain.core.Genesis;
import org.cds.main.blockchain.core.genesis.GenesisJson;
import org.cds.main.blockchain.core.genesis.GenesisLoader;
import org.cds.main.blockchain.crypto.ECKey;
import org.cds.main.blockchain.net.p2p.P2pHandler;
import org.cds.main.blockchain.net.rlpx.MessageCodec;
import org.cds.main.blockchain.net.rlpx.Node;
import org.cds.main.blockchain.util.BuildInfo;
import org.cds.main.blockchain.util.ByteUtil;
import org.cds.main.blockchain.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigRenderOptions;

/**
 * Utility class to retrieve property values from the ethereumj.conf files
 *
 * The properties are taken from different sources and merged in the following order
 * (the config option from the next source overrides option from previous):
 * - resource blockchain.conf : normally used as a reference config with default values
 *          and shouldn't be changed
 * - system property : each config entry might be altered via -D VM option
 * - [user dir]/config/blockchain.conf
 * - config specified with the -Dethereumj.conf.file=[file.conf] VM option
 * - CLI options
 *
 */

public class SystemProperties {
    private static Logger logger = LoggerFactory.getLogger("general");

    public final static String PROPERTY_DB_DIR = "database.dir";
    public final static String PROPERTY_LISTEN_PORT = "peer.listen.port";
    public final static String PROPERTY_PEER_ACTIVE = "peer.active";
    public final static String PROPERTY_DB_RESET = "database.reset";
    public final static String PROPERTY_PEER_DISCOVERY_ENABLED = "peer.discovery.enabled";

    /* Testing */
    private final static Boolean DEFAULT_VMTEST_LOAD_LOCAL = false;
    private final static String DEFAULT_BLOCKS_LOADER = "";

    private static SystemProperties CONFIG;
    private static boolean useOnlySpringConfig = false;
    private String generatedNodePrivateKey;

    /**
     * Returns the static config instance. If the config is passed
     * as a Spring bean by the application this instance shouldn't
     * be used
     * This method is mainly used for testing purposes
     * (Autowired fields are initialized with this static instance
     * but when running within Spring context they replaced with the
     * bean config instance)
     */
    public static SystemProperties getDefault() {
        return useOnlySpringConfig ? null : getSpringDefault();
    }

    static SystemProperties getSpringDefault() {
        if (CONFIG == null) {
            CONFIG = new SystemProperties();
        }
        return CONFIG;
    }

    public static void resetToDefault() {
        CONFIG = null;
    }

    /**
     * Used mostly for testing purposes to ensure the application
     * refers only to the config passed as a Spring bean.
     * If this property is set to true {@link #getDefault()} returns null
     */
    public static void setUseOnlySpringConfig(boolean useOnlySpringConfig) {
        SystemProperties.useOnlySpringConfig = useOnlySpringConfig;
    }

    static boolean isUseOnlySpringConfig() {
        return useOnlySpringConfig;
    }

    /**
     * Marks config accessor methods which need to be called (for value validation)
     * upon config creation or modification
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ValidateMe {};


    private Config config;

    // mutable options for tests
    private String databaseDir = null;
    private Boolean databaseReset = null;
    private String projectVersion = null;
    private String projectVersionModifier = null;
    protected Integer databaseVersion = null;

    private String genesisInfo = null;

    private String bindIp = null;
    private String externalIp = null;

    private Boolean syncEnabled = null;
    private Boolean discoveryEnabled = null;

    private GenesisJson genesisJson;
    private BlockchainNetConfig blockchainConfig;
    private Genesis genesis;
    private Boolean vmTrace;
    private Boolean recordInternalTransactionsData;

    private final ClassLoader classLoader;

    private GenerateNodeIdStrategy generateNodeIdStrategy = null;

    public SystemProperties() {
        this(ConfigFactory.empty());
    }

    public SystemProperties(File configFile) {
        this(ConfigFactory.parseFile(configFile));
    }

    public SystemProperties(String configResource) {
        this(ConfigFactory.parseResources(configResource));
    }

    public SystemProperties(Config apiConfig) {
        this(apiConfig, SystemProperties.class.getClassLoader());
    }

    public SystemProperties(Config apiConfig, ClassLoader classLoader) {
        try {
            this.classLoader = classLoader;
            
            Config referenceConfig = ConfigFactory.parseResources("blockchain.conf");
            logger.info("Config (" + (referenceConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): default properties from resource 'blockchain.conf'");
            
            String res = System.getProperty("blockchain.conf.res");
            Config cmdLineConfigRes = res != null ? ConfigFactory.parseResources(res) : ConfigFactory.empty();
            logger.info("Config (" + (cmdLineConfigRes.entrySet().size() > 0 ? " yes " : " no  ") + "): user properties from -Dblockchain.conf.res resource '" + res + "'");
            
            Config userConfig = ConfigFactory.parseResources("user.conf");
            logger.info("Config (" + (userConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): user properties from resource 'user.conf'");
            
            String userConfRes = System.getProperty("user.conf.res");
            Config userResConfig = StringUtils.isNotBlank(userConfRes)?ConfigFactory.parseResources(userConfRes):ConfigFactory.empty();
            logger.info("Config (" + (userResConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): user properties from -Duser.conf.res '" + userConfRes + "'");
            
            File userDirFile = new File(System.getProperty("user.dir"), "/config/blockchain.conf");
            Config userDirConfig = ConfigFactory.parseFile(userDirFile);
            logger.info("Config (" + (userDirConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): user properties from file '" + userDirFile + "'");
            
            Config testConfig = ConfigFactory.parseResources("test-blockchain.conf");
            logger.info("Config (" + (testConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): test properties from resource 'test-user.conf'");
            
            Config testUserConfig = ConfigFactory.parseResources("test-user.conf");
            logger.info("Config (" + (testUserConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): test properties from resource 'test-user.conf'");
            
            String file = System.getProperty("blockchain.conf.file");
            Config cmdLineConfigFile = file != null ? ConfigFactory.parseFile(new File(file)) : ConfigFactory.empty();
            logger.info("Config (" + (cmdLineConfigFile.entrySet().size() > 0 ? " yes " : " no  ") + "): user properties from -Dblockchain.conf.file file '" + file + "'");
            
            logger.info("Config (" + (apiConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): config passed via constructor");
            config = apiConfig
                    .withFallback(cmdLineConfigFile)
                    .withFallback(testUserConfig)
                    .withFallback(testConfig)
                    .withFallback(userDirConfig)
                    .withFallback(userResConfig)
                    .withFallback(userConfig)
                    .withFallback(cmdLineConfigRes)
                    .withFallback(referenceConfig);
            logger.debug("Config trace: " + config.root().render(ConfigRenderOptions.defaults().setComments(false).setJson(false)));
            Config javaSystemProperties = ConfigFactory.load("no-such-resource-only-system-props");
            config = javaSystemProperties.withFallback(config).resolve();// substitute variables in config if any
            validateConfig();

            // There could be several files with the same name from other packages,
            // "version.properties" is a very common name
            List<InputStream> iStreams = loadResources("version.properties", this.getClass().getClassLoader());
          for (InputStream is : iStreams) {
            Properties props = new Properties();
            props.load(is);
            if (props.getProperty("versionNumber") == null || props.getProperty("databaseVersion") == null) {
              continue;
            }
            this.projectVersion = props.getProperty("versionNumber");
            this.projectVersion = this.projectVersion.replaceAll("'", "");

            if (this.projectVersion == null) this.projectVersion = "-.-.-";

            this.projectVersionModifier = "master".equals(BuildInfo.buildBranch) ? "RELEASE" : "SNAPSHOT";

            this.databaseVersion = Integer.valueOf(props.getProperty("databaseVersion"));

            this.generateNodeIdStrategy = new GetNodeIdFromPropsFile(databaseDir())
                .withFallback(new GenerateNodeIdRandomly(databaseDir()));
            break;
            }
        } catch (Exception e) {
            logger.error("Can't read config.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads resources using given ClassLoader assuming, there could be several resources
     * with the same name
     */
    public static List<InputStream> loadResources(
            final String name, final ClassLoader classLoader) throws IOException {
        final List<InputStream> list = new ArrayList<InputStream>();
        final Enumeration<URL> systemResources =
                (classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader)
                        .getResources(name);
        while (systemResources.hasMoreElements()) {
            list.add(systemResources.nextElement().openStream());
        }
        return list;
    }

    public Config getConfig() {
        return config;
    }

    /**
     * Puts a new config atop of existing stack making the options
     * in the supplied config overriding existing options
     * Once put this config can't be removed
     *
     * @param overrideOptions - atop config
     */
    public void overrideParams(Config overrideOptions) {
        config = overrideOptions.withFallback(config);
        validateConfig();
    }

    /**
     * Puts a new config atop of existing stack making the options
     * in the supplied config overriding existing options
     * Once put this config can't be removed
     *
     * @param keyValuePairs [name] [value] [name] [value] ...
     */
    public void overrideParams(String ... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) throw new RuntimeException("Odd argument number");
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        overrideParams(map);
    }

    /**
     * Puts a new config atop of existing stack making the options
     * in the supplied config overriding existing options
     * Once put this config can't be removed
     *
     * @param cliOptions -  command line options to take presidency
     */
    public void overrideParams(Map<String, ?> cliOptions) {
        Config cliConf = ConfigFactory.parseMap(cliOptions);
        overrideParams(cliConf);
    }

    private void validateConfig() {
        for (Method method : getClass().getMethods()) {
            try {
                if (method.isAnnotationPresent(ValidateMe.class)) {
                    method.invoke(this);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error validating config method: " + method, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
	public <T> T getProperty(String propName, T defaultValue) {
        if (!config.hasPath(propName)) return defaultValue;
        String string = config.getString(propName);
        if (string.trim().isEmpty()) return defaultValue;
        return (T) config.getAnyRef(propName);
    }
    
    public BlockchainNetConfig getBlockchainConfig() {
        if (blockchainConfig == null) {
        	String blockchainConfigName = config.hasPath("blockchain.config.name")?config.getString("blockchain.config.name"):null;
        	String blockchainConfigClass = config.hasPath("blockchain.config.class")?config.getString("blockchain.config.class"):null;
            if (StringUtils.isNotBlank(blockchainConfigName)
            		&& StringUtils.isNotBlank(blockchainConfigClass)) {
                throw new RuntimeException("Only one of two options should be defined: 'blockchain.config.name' and 'blockchain.config.class'");
            }
            if (StringUtils.isNotBlank(blockchainConfigName)) {
                switch (config.getString("blockchain.config.name")) {
	                case "UKNM":
	                    blockchainConfig = new UknmNetConfig();
	                    break;
	                default:
	                    throw new RuntimeException("Unknown value for 'blockchain.config.name': '" + config.getString("blockchain.config.name") + "'");
	            }
            } else {
                try {
                    Class<? extends BlockchainNetConfig> aClass = (Class<? extends BlockchainNetConfig>) classLoader.loadClass(blockchainConfigClass);
                    blockchainConfig = aClass.newInstance();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("The class specified via blockchain.config.class '" + blockchainConfigClass + "' not found", e);
                } catch (ClassCastException e) {
                    throw new RuntimeException("The class specified via blockchain.config.class '" + blockchainConfigClass + "' is not instance of org.cds.main.blockchain.config.BlockchainForkConfig", e);
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException("The class specified via blockchain.config.class '" + blockchainConfigClass + "' couldn't be instantiated (check for default constructor and its accessibility)", e);
                }
            }
        }
        return blockchainConfig;
    }
    
    public void setBlockchainConfig(BlockchainNetConfig config) {
        blockchainConfig = config;
    }

    @ValidateMe
    public boolean peerDiscovery() {
        return discoveryEnabled == null ? config.getBoolean("peer.discovery.enabled") : discoveryEnabled;
    }

    public void setDiscoveryEnabled(Boolean discoveryEnabled) {
        this.discoveryEnabled = discoveryEnabled;
    }

    @ValidateMe
    public boolean peerDiscoveryPersist() {
        return config.getBoolean("peer.discovery.persist");
    }

    @ValidateMe
    public int peerDiscoveryWorkers() {
        return config.getInt("peer.discovery.workers");
    }

    @ValidateMe
    public int peerDiscoveryTouchPeriod() {
        return config.getInt("peer.discovery.touchPeriod");
    }

    @ValidateMe
    public int peerDiscoveryTouchMaxNodes() {
        return config.getInt("peer.discovery.touchMaxNodes");
    }

    @ValidateMe
    public int peerConnectionTimeout() {
        return config.getInt("peer.connection.timeout") * 1000;
    }

    @ValidateMe
    public int defaultP2PVersion() {
        return config.hasPath("peer.p2p.version") ? config.getInt("peer.p2p.version") : P2pHandler.VERSION;
    }

    @ValidateMe
    public int rlpxMaxFrameSize() {
        return config.hasPath("peer.p2p.framing.maxSize") ? config.getInt("peer.p2p.framing.maxSize") : MessageCodec.NO_FRAMING;
    }


    @ValidateMe
    public int transactionApproveTimeout() {
        return config.getInt("transaction.approve.timeout") * 1000;
    }

    @ValidateMe
    public List<String> peerDiscoveryIPList() {
        return config.getStringList("peer.discovery.ip.list");
    }

    @ValidateMe
    public boolean databaseReset() {
        return databaseReset == null ? config.getBoolean("database.reset") : databaseReset;
    }

    public void setDatabaseReset(Boolean reset) {
        databaseReset = reset;
    }

    @ValidateMe
    public long databaseResetBlock() {
        return config.getLong("database.resetBlock");
    }

    @ValidateMe
    public boolean databaseFromBackup() {
        return config.getBoolean("database.fromBackup");
    }

    @ValidateMe
    public int databasePruneDepth() {
        return config.getBoolean("database.prune.enabled") ? config.getInt("database.prune.maxDepth") : -1;
    }

    @ValidateMe
    public List<Node> peerActive() {
        if (!config.hasPath("peer.active")) {
            return Collections.EMPTY_LIST;
        }
        List<Node> ret = new ArrayList<>();
        List<? extends ConfigObject> list = config.getObjectList("peer.active");
        for (ConfigObject configObject : list) {
            Node n;
            if (configObject.get("url") != null) {
                String url = configObject.toConfig().getString("url");
                n = new Node(url.startsWith("enode://") ? url : "enode://" + url);
            } else if (configObject.get("ip") != null) {
                String ip = configObject.toConfig().getString("ip");
                int port = configObject.toConfig().getInt("port");
                byte[] nodeId;
                if (configObject.toConfig().hasPath("nodeId")) {
                    nodeId = Hex.decode(configObject.toConfig().getString("nodeId").trim());
                    if (nodeId.length != 64) {
                        throw new RuntimeException("Invalid config nodeId '" + nodeId + "' at " + configObject);
                    }
                } else {
                    if (configObject.toConfig().hasPath("nodeName")) {
                        String nodeName = configObject.toConfig().getString("nodeName").trim();
                        // FIXME should be keccak-512 here ?
                        nodeId = ECKey.fromPrivate(sha3(nodeName.getBytes())).getNodeId();
                    } else {
                        throw new RuntimeException("Either nodeId or nodeName should be specified: " + configObject);
                    }
                }
                n = new Node(nodeId, ip, port);
            } else {
                throw new RuntimeException("Unexpected element within 'peer.active' config list: " + configObject);
            }
            ret.add(n);
        }
        return ret;
    }

    @ValidateMe
    public NodeFilter peerTrusted() {
        List<? extends ConfigObject> list = config.getObjectList("peer.trusted");
        NodeFilter ret = new NodeFilter();

        for (ConfigObject configObject : list) {
            byte[] nodeId = null;
            String ipMask = null;
            if (configObject.get("nodeId") != null) {
                nodeId = Hex.decode(configObject.toConfig().getString("nodeId").trim());
            }
            if (configObject.get("ip") != null) {
                ipMask = configObject.toConfig().getString("ip").trim();
            }
            ret.add(nodeId, ipMask);
        }
        return ret;
    }


    @ValidateMe
    public Integer blockQueueSize() {
        return config.getInt("cache.blockQueueSize") * 1024 * 1024;
    }
    @ValidateMe
    public Integer headerQueueSize() {
        return config.getInt("cache.headerQueueSize") * 1024 * 1024;
    }

    @ValidateMe
    public Integer peerChannelReadTimeout() {
        return config.getInt("peer.channel.read.timeout");
    }

    @ValidateMe
    public Integer traceStartBlock() {
        return config.getInt("trace.startblock");
    }

    @ValidateMe
    public boolean recordBlocks() {
        return config.getBoolean("record.blocks");
    }

    @ValidateMe
    public boolean dumpFull() {
        return config.getBoolean("dump.full");
    }

    @ValidateMe
    public String dumpDir() {
        return config.getString("dump.dir");
    }

    @ValidateMe
    public String dumpStyle() {
        return config.getString("dump.style");
    }

    @ValidateMe
    public int dumpBlock() {
        return config.getInt("dump.block");
    }

    @ValidateMe
    public String databaseDir() {
        return databaseDir == null ? config.getString("database.dir") : databaseDir;
    }

    public String ethashDir() {
        return config.hasPath("ethash.dir") ? config.getString("ethash.dir") : databaseDir();
    }

    public void setDataBaseDir(String dataBaseDir) {
        this.databaseDir = dataBaseDir;
    }

    @ValidateMe
    public boolean dumpCleanOnRestart() {
        return config.getBoolean("dump.clean.on.restart");
    }

    @ValidateMe
    public boolean playVM() {
        return config.getBoolean("play.vm");
    }

    @ValidateMe
    public boolean blockChainOnly() {
        return config.getBoolean("blockchain.only");
    }

    @ValidateMe
    public int syncPeerCount() {
        return config.getInt("sync.peer.count");
    }
    
    public List<String> syncForkTrust(){
    	return config.getStringList("sync.fork.trust");
    }
    
    public Integer syncVersion() {
        if (!config.hasPath("sync.version")) {
            return null;
        }
        return config.getInt("sync.version");
    }

    @ValidateMe
    public boolean exitOnBlockConflict() {
        return config.getBoolean("sync.exitOnBlockConflict");
    }

    @ValidateMe
    public String projectVersion() {
        return projectVersion;
    }

    @ValidateMe
    public Integer databaseVersion() {
        return databaseVersion;
    }

    @ValidateMe
    public String projectVersionModifier() {
        return projectVersionModifier;
    }

    @ValidateMe
    public String helloPhrase() {
        return config.getString("hello.phrase");
    }

    @ValidateMe
    public String rootHashStart() {
        return config.hasPath("root.hash.start") ? config.getString("root.hash.start") : null;
    }

    @ValidateMe
    public List<String> peerCapabilities() {
        return config.getStringList("peer.capabilities");
    }

    @ValidateMe
    public boolean vmTrace() {
        return vmTrace == null ? (vmTrace = config.getBoolean("vm.structured.trace")) : vmTrace;
    }

    @ValidateMe
    public boolean vmTraceCompressed() {
        return config.getBoolean("vm.structured.compressed");
    }

    @ValidateMe
    public int vmTraceInitStorageLimit() {
        return config.getInt("vm.structured.initStorageLimit");
    }

    @ValidateMe
    public int cacheFlushBlocks() {
        return config.getInt("cache.flush.blocks");
    }

    @ValidateMe
    public String vmTraceDir() {
        return config.getString("vm.structured.dir");
    }

    public String customSolcPath() {
        return config.hasPath("solc.path") ? config.getString("solc.path"): null;
    }

    public String privateKey() {
        if (config.hasPath("peer.privateKey")) {
            String key = config.getString("peer.privateKey");
            if (key.length() != 64 || !Utils.isHexEncoded(key)) {
                throw new RuntimeException("The peer.privateKey needs to be Hex encoded and 32 byte length");
            }
            return key;
        } else {
            return getGeneratedNodePrivateKey();
        }
    }

    private String getGeneratedNodePrivateKey() {
        if (generatedNodePrivateKey == null) {
            generatedNodePrivateKey = generateNodeIdStrategy.getNodePrivateKey();
        }
        return generatedNodePrivateKey;
    }

    public ECKey getMyKey() {
        return ECKey.fromPrivate(Hex.decode(privateKey()));
    }

    /**
     *  Home NodeID calculated from 'peer.privateKey' property
     */
    public byte[] nodeId() {
        return getMyKey().getNodeId();
    }

    @ValidateMe
    public int networkId() {
        return config.getInt("peer.networkId");
    }

    @ValidateMe
    public int maxActivePeers() {
        return config.getInt("peer.maxActivePeers");
    }

    @ValidateMe
    public boolean eip8() {
        return config.getBoolean("peer.p2p.eip8");
    }

    @ValidateMe
    public int listenPort() {
        return config.getInt("peer.listen.port");
    }


    /**
     * This can be a blocking call with long timeout (thus no ValidateMe)
     */
    public String bindIp() {
        if (!config.hasPath("peer.discovery.bind.ip") || config.getString("peer.discovery.bind.ip").trim().isEmpty()) {
            if (bindIp == null) {
                logger.info("Bind address wasn't set, Punching to identify it...");
                try {
                    Socket s = new Socket("www.google.com", 80);
                    bindIp = s.getLocalAddress().getHostAddress();
                    logger.info("UDP local bound to: {}", bindIp);
                } catch (IOException e) {
                    logger.warn("Can't get bind IP. Fall back to 0.0.0.0: " + e);
                    bindIp = "0.0.0.0";
                }
            }
            return bindIp;
        } else {
            return config.getString("peer.discovery.bind.ip").trim();
        }
    }

    /**
     * This can be a blocking call with long timeout (thus no ValidateMe)
     */
    public String externalIp() {
        if (!config.hasPath("peer.discovery.external.ip") || config.getString("peer.discovery.external.ip").trim().isEmpty()) {
            if (externalIp == null) {
                logger.info("External IP wasn't set, using checkip.amazonaws.com to identify it...");
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            new URL("http://checkip.amazonaws.com").openStream()));
                    externalIp = in.readLine();
                    if (externalIp == null || externalIp.trim().isEmpty()) {
                        throw new IOException("Invalid address: '" + externalIp + "'");
                    }
                    try {
                        InetAddress.getByName(externalIp);
                    } catch (Exception e) {
                        throw new IOException("Invalid address: '" + externalIp + "'");
                    }
                    logger.info("External address identified: {}", externalIp);
                } catch (IOException e) {
                    externalIp = bindIp();
                    logger.warn("Can't get external IP. Fall back to peer.bind.ip: " + externalIp + " :" + e);
                }
            }
            return externalIp;

        } else {
            return config.getString("peer.discovery.external.ip").trim();
        }
    }

    @ValidateMe
    public String getKeyValueDataSource() {
        return config.getString("keyvalue.datasource");
    }

    @ValidateMe
    public boolean isSyncEnabled() {
        return this.syncEnabled == null ? config.getBoolean("sync.enabled") : syncEnabled;
    }

    public void setSyncEnabled(Boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    @ValidateMe
    public boolean isFastSyncEnabled() {
        return isSyncEnabled() && config.getBoolean("sync.fast.enabled");
    }

    @ValidateMe
    public byte[] getFastSyncPivotBlockHash() {
        if (!config.hasPath("sync.fast.pivotBlockHash")) return null;
        byte[] ret = Hex.decode(config.getString("sync.fast.pivotBlockHash"));
        if (ret.length != 32) throw new RuntimeException("Invalid block hash length: " + toHexString(ret));
        return ret;
    }

    @ValidateMe
    public boolean fastSyncBackupState() {
        return config.getBoolean("sync.fast.backupState");
    }

    @ValidateMe
    public boolean fastSyncSkipHistory() {
        return config.getBoolean("sync.fast.skipHistory");
    }

    @ValidateMe
    public int makeDoneByTimeout() {
        return config.getInt("sync.makeDoneByTimeout");
    }


    @ValidateMe
    public boolean isPublicHomeNode() { return config.getBoolean("peer.discovery.public.home.node");}

    @ValidateMe
    public String genesisInfo() {
        return genesisInfo == null ? config.getString("genesis") : genesisInfo;
    }

    @ValidateMe
    public int txOutdatedThreshold() {
        return config.getInt("transaction.outdated.threshold");
    }

    public void setGenesisInfo(String genesisInfo){
        this.genesisInfo = genesisInfo;
    }

    @ValidateMe
    public boolean minerStart() {
        return config.getBoolean("mine.start");
    }
    
    @ValidateMe
    public String minerMode() {
    	return config.getString("mine.mode");
    }
    
    @ValidateMe
    public int minerEntrustPort() {
    	return config.getInt("mine.entrust.port");
    }
    
    @ValidateMe
    public List<String> minerEntrustAuthorizationCodes(){
    	return config.getStringList("mine.entrust.authorizationCodes");
    }

    @ValidateMe
    public byte[] getMinerCoinbase() {
        String sc = config.getString("mine.coinbase");
        byte[] c = ByteUtil.hexStringToBytes(sc);
        if (c.length != 20) throw new RuntimeException("mine.coinbase has invalid value: '" + sc + "'");
        return c;
    }

    @ValidateMe
    public byte[] getMineExtraData() {
        byte[] bytes;
        if (config.hasPath("mine.extraDataHex")) {
            bytes = Hex.decode(config.getString("mine.extraDataHex"));
        } else {
            bytes = config.getString("mine.extraData").getBytes();
        }
        if (bytes.length > 32) throw new RuntimeException("mine.extraData exceed 32 bytes length: " + bytes.length);
        return bytes;
    }

    @ValidateMe
    public BigInteger getMineMinGasPrice() {
        return new BigInteger(config.getString("mine.minGasPrice"));
    }

    @ValidateMe
    public long getMineMinBlockTimeoutMsec() {
        return config.getLong("mine.minBlockTimeoutMsec");
    }

    @ValidateMe
    public int getMineCpuThreads() {
        return config.getInt("mine.cpuMineThreads");
    }

    @ValidateMe
    public boolean isMineFullDataset() {
        return config.getBoolean("mine.fullDataSet");
    }
    
    @ValidateMe
    public String getMinePlotPath() {
        return config.getString("mine.plot.path");
    }
    
    @ValidateMe
    public int getMinePlotSize() {
        return config.getInt("mine.plot.size");
    }

    @ValidateMe
    public String getCryptoProviderName() {
        return config.getString("crypto.providerName");
    }

    @ValidateMe
    public boolean recordInternalTransactionsData() {
        if (recordInternalTransactionsData == null) {
            recordInternalTransactionsData = config.getBoolean("record.internal.transactions.data");
        }
        return recordInternalTransactionsData;
    }

    public void setRecordInternalTransactionsData(Boolean recordInternalTransactionsData) {
        this.recordInternalTransactionsData = recordInternalTransactionsData;
    }

    @ValidateMe
    public String getHash256AlgName() {
        return config.getString("crypto.hash.alg256");
    }

    @ValidateMe
    public String getHash512AlgName() {
        return config.getString("crypto.hash.alg512");
    }

    @ValidateMe
    public String getEthashMode() {
        return config.getString("sync.ethash");
    }
    
    private GenesisJson getGenesisJson() {
        if (genesisJson == null) {
            genesisJson = GenesisLoader.loadGenesisJson(this, classLoader);
        }
        return genesisJson;
    }

    public Genesis getGenesis() {
        if (genesis == null) {
            genesis = GenesisLoader.parseGenesis(getBlockchainConfig(), getGenesisJson());
        }
        return genesis;
    }

    /**
     * Method used in StandaloneBlockchain.
     */
    public Genesis useGenesis(InputStream inputStream) {
        genesisJson = GenesisLoader.loadGenesisJson(inputStream);
        genesis = GenesisLoader.parseGenesis(getBlockchainConfig(), getGenesisJson());
        return genesis;
    }

    public String dump() {
        return config.root().render(ConfigRenderOptions.defaults().setComments(false));
    }

    /*
     *
     * Testing
     *
     */
    public boolean vmTestLoadLocal() {
        return config.hasPath("GitHubTests.VMTest.loadLocal") ?
                config.getBoolean("GitHubTests.VMTest.loadLocal") : DEFAULT_VMTEST_LOAD_LOCAL;
    }

    public String blocksLoader() {
        return config.hasPath("blocks.loader") ?
                config.getString("blocks.loader") : DEFAULT_BLOCKS_LOADER;
    }

    public String githubTestsPath() {
        return config.hasPath("GitHubTests.testPath") ?
                config.getString("GitHubTests.testPath") : "";
    }

    public boolean githubTestsLoadLocal() {
        return config.hasPath("GitHubTests.testPath") &&
                !config.getString("GitHubTests.testPath").isEmpty();
    }

    void setGenerateNodeIdStrategy(GenerateNodeIdStrategy generateNodeIdStrategy) {
      this.generateNodeIdStrategy = generateNodeIdStrategy;
    }
}

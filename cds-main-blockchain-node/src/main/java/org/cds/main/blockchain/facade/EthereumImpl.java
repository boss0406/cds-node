package org.cds.main.blockchain.facade;

import static org.cds.main.blockchain.util.ByteUtil.toHexString;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.cds.main.blockchain.config.BlockchainConfig;
import org.cds.main.blockchain.config.CommonConfig;
import org.cds.main.blockchain.config.SystemProperties;
import org.cds.main.blockchain.core.Block;
import org.cds.main.blockchain.core.BlockSummary;
import org.cds.main.blockchain.core.CallTransaction;
import org.cds.main.blockchain.core.ImportResult;
import org.cds.main.blockchain.core.PendingState;
import org.cds.main.blockchain.core.Repository;
import org.cds.main.blockchain.core.Transaction;
import org.cds.main.blockchain.core.TransactionExecutionSummary;
import org.cds.main.blockchain.core.TransactionReceipt;
import org.cds.main.blockchain.crypto.ECKey;
import org.cds.main.blockchain.listener.CompositeEthereumListener;
import org.cds.main.blockchain.listener.EthereumListener;
import org.cds.main.blockchain.listener.EthereumListenerAdapter;
import org.cds.main.blockchain.listener.GasPriceTracker;
import org.cds.main.blockchain.manager.AdminInfo;
import org.cds.main.blockchain.manager.BlockLoader;
import org.cds.main.blockchain.manager.WorldManager;
import org.cds.main.blockchain.net.client.PeerClient;
import org.cds.main.blockchain.net.rlpx.Node;
import org.cds.main.blockchain.net.server.ChannelManager;
import org.cds.main.blockchain.net.shh.Whisper;
import org.cds.main.blockchain.net.submit.TransactionExecutor;
import org.cds.main.blockchain.net.submit.TransactionTask;
import org.cds.main.blockchain.sync.SyncManager;
import org.cds.main.blockchain.util.ByteUtil;
import org.cds.main.blockchain.vm.program.ProgramResult;
import org.cds.main.blockchain.vm.program.invoke.ProgramInvokeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.FutureAdapter;

@Component
public class EthereumImpl implements Ethereum, SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger("facade");
    private static final Logger gLogger = LoggerFactory.getLogger("general");

    @Autowired
    WorldManager worldManager;

    @Autowired
    AdminInfo adminInfo;

    @Autowired
    ChannelManager channelManager;

    @Autowired
    ApplicationContext ctx;

    @Autowired
    BlockLoader blockLoader;

    @Autowired
    ProgramInvokeFactory programInvokeFactory;

    @Autowired
    Whisper whisper;

    @Autowired
    PendingState pendingState;

    @Autowired
    SyncManager syncManager;

    @Autowired
    CommonConfig commonConfig = CommonConfig.getDefault();

    private SystemProperties config;

    private CompositeEthereumListener compositeEthereumListener;


    private GasPriceTracker gasPriceTracker = new GasPriceTracker();

    @Autowired
    public EthereumImpl(final SystemProperties config, final CompositeEthereumListener compositeEthereumListener) {
        this.compositeEthereumListener = compositeEthereumListener;
        this.config = config;
        System.out.println();
        this.compositeEthereumListener.addListener(gasPriceTracker);
        gLogger.info("node started: enode://" + toHexString(config.nodeId()) + "@" + config.externalIp() + ":" + config.listenPort());
    }

    @Override
    public void startPeerDiscovery() {
        worldManager.startPeerDiscovery();
    }

    @Override
    public void stopPeerDiscovery() {
        worldManager.stopPeerDiscovery();
    }

    @Override
    public void connect(InetAddress addr, int port, String remoteId) {
        connect(addr.getHostName(), port, remoteId);
    }

    @Override
    public void connect(final String ip, final int port, final String remoteId) {
        logger.debug("Connecting to: {}:{}", ip, port);
        worldManager.getActivePeer().connectAsync(ip, port, remoteId, false);
    }

    @Override
    public void connect(Node node) {
        connect(node.getHost(), node.getPort(), Hex.toHexString(node.getId()));
    }

    @Override
    public org.cds.main.blockchain.facade.Blockchain getBlockchain() {
        return (org.cds.main.blockchain.facade.Blockchain) worldManager.getBlockchain();
    }

    public ImportResult addNewMinedBlock(Block block) {
        ImportResult importResult = worldManager.getBlockchain().tryToConnect(block);
        if (importResult == ImportResult.IMPORTED_BEST) {
            channelManager.sendNewBlock(block);
        }
        return importResult;
    }

   /* @Override
    public BlockMiner getBlockMiner() {
        return ctx.getBean(BlockMiner.class);
    }*/

    @Override
    public void addListener(EthereumListener listener) {
        worldManager.addListener(listener);
    }

    @Override
    public void close() {
        logger.info("### Shutdown initiated ### ");
        ((AbstractApplicationContext) getApplicationContext()).close();
    }

    @Override
    public SyncStatus getSyncStatus() {
        return syncManager.getSyncStatus();
    }

    @Override
    public PeerClient getDefaultPeer() {
        return worldManager.getActivePeer();
    }

    @Override
    public boolean isConnected() {
        return worldManager.getActivePeer() != null;
    }

    @Override
    public Transaction createTransaction(BigInteger nonce,
                                         BigInteger gasPrice,
                                         BigInteger gas,
                                         byte[] receiveAddress,
                                         BigInteger value, byte[] data) {

        byte[] nonceBytes = ByteUtil.bigIntegerToBytes(nonce);
        byte[] gasPriceBytes = ByteUtil.bigIntegerToBytes(gasPrice);
        byte[] gasBytes = ByteUtil.bigIntegerToBytes(gas);
        byte[] valueBytes = ByteUtil.bigIntegerToBytes(value);

        return new Transaction(nonceBytes, gasPriceBytes, gasBytes,
                receiveAddress, valueBytes, data, getChainIdForNextBlock());
    }


    @Override
    public Future<Transaction> submitTransaction(Transaction transaction) {

        TransactionTask transactionTask = new TransactionTask(transaction, channelManager);

        final Future<List<Transaction>> listFuture =
                TransactionExecutor.instance.submitTransaction(transactionTask);

        pendingState.addPendingTransaction(transaction);

        return new FutureAdapter<Transaction, List<Transaction>>(listFuture) {
            @Override
            protected Transaction adapt(List<Transaction> adapteeResult) throws ExecutionException {
                return adapteeResult.get(0);
            }
        };
    }

    @Override
    public TransactionReceipt callConstant(Transaction tx, Block block) {
        if (tx.getSignature() == null) {
            tx.sign(ECKey.DUMMY);
        }
        return callConstantImpl(tx, block).getReceipt();
    }

    @Override
    public BlockSummary replayBlock(Block block) {
        List<TransactionReceipt> receipts = new ArrayList<>();
        List<TransactionExecutionSummary> summaries = new ArrayList<>();

        Block parent = worldManager.getBlockchain().getBlockByHash(block.getParentHash());

        if (parent == null) {
            logger.info("Failed to replay block #{}, its ancestor is not presented in the db", block.getNumber());
            return new BlockSummary(block, new HashMap<byte[], BigInteger>(), receipts, summaries);
        }

        Repository track = ((Repository) worldManager.getRepository())
                .getSnapshotTo(parent.getStateRoot());

        try {
            for (Transaction tx : block.getTransactionsList()) {

                Repository txTrack = track.startTracking();
                org.cds.main.blockchain.core.TransactionExecutor executor = new org.cds.main.blockchain.core.TransactionExecutor(
                        tx, block.getCoinbase(), txTrack, worldManager.getBlockStore(),
                        programInvokeFactory, block, worldManager.getListener(), 0)
                        .withCommonConfig(commonConfig);

                executor.init();
                executor.execute();
                executor.go();

                TransactionExecutionSummary summary = executor.finalization();

                txTrack.commit();

                TransactionReceipt receipt = executor.getReceipt();
                receipt.setPostTxState(track.getRoot());
                receipts.add(receipt);
                summaries.add(summary);
            }
        } finally {
            track.rollback();
        }

        return new BlockSummary(block, new HashMap<byte[], BigInteger>(), receipts, summaries);
    }

    private org.cds.main.blockchain.core.TransactionExecutor callConstantImpl(Transaction tx, Block block) {

        Repository repository = ((Repository) worldManager.getRepository())
                .getSnapshotTo(block.getStateRoot())
                .startTracking();

        try {
            org.cds.main.blockchain.core.TransactionExecutor executor = new org.cds.main.blockchain.core.TransactionExecutor
                    (tx, block.getCoinbase(), repository, worldManager.getBlockStore(),
                            programInvokeFactory, block, new EthereumListenerAdapter(), 0)
                    .withCommonConfig(commonConfig)
                    .setLocalCall(true);

            executor.init();
            executor.execute();
            executor.go();
            executor.finalization();

            return executor;
        } finally {
            repository.rollback();
        }
    }

    @Override
    public ProgramResult callConstantFunction(String receiveAddress,
                                              CallTransaction.Function function, Object... funcArgs) {
        return callConstantFunction(receiveAddress, ECKey.DUMMY, function, funcArgs);
    }

    @Override
    public ProgramResult callConstantFunction(String receiveAddress, ECKey senderPrivateKey,
                                              CallTransaction.Function function, Object... funcArgs) {
        Transaction tx = CallTransaction.createCallTransaction(0, 0, 100000000000000L,
                receiveAddress, 0, function, funcArgs);
        tx.sign(senderPrivateKey);
        Block bestBlock = worldManager.getBlockchain().getBestBlock();

        return callConstantImpl(tx, bestBlock).getResult();
    }

    @Override
    public org.cds.main.blockchain.facade.Repository getRepository() {
        return worldManager.getRepository();
    }

    @Override
    public org.cds.main.blockchain.facade.Repository getLastRepositorySnapshot() {
        return getSnapshotTo(getBlockchain().getBestBlock().getStateRoot());
    }

    @Override
    public org.cds.main.blockchain.facade.Repository getPendingState() {
        return worldManager.getPendingState().getRepository();
    }

    @Override
    public org.cds.main.blockchain.facade.Repository getSnapshotTo(byte[] root) {

        Repository repository = (Repository) worldManager.getRepository();
        org.cds.main.blockchain.facade.Repository snapshot = repository.getSnapshotTo(root);

        return snapshot;
    }

    @Override
    public AdminInfo getAdminInfo() {
        return adminInfo;
    }

    @Override
    public ChannelManager getChannelManager() {
        return channelManager;
    }


    @Override
    public List<Transaction> getWireTransactions() {
        return worldManager.getPendingState().getPendingTransactions();
    }

    @Override
    public List<Transaction> getPendingStateTransactions() {
        return worldManager.getPendingState().getPendingTransactions();
    }

    @Override
    public BlockLoader getBlockLoader() {
        return blockLoader;
    }

    @Override
    public Whisper getWhisper() {
        return whisper;
    }

    @Override
    public long getGasPrice() {
        return gasPriceTracker.getGasPrice();
    }

    @Override
    public Integer getChainIdForNextBlock() {
        BlockchainConfig nextBlockConfig = config.getBlockchainConfig().getConfigForBlock(getBlockchain()
                .getBestBlock().getNumber() + 1);
        return nextBlockConfig.getChainId();
    }

    public CompletableFuture<Void> switchToShortSync() {
        return syncManager.switchToShortSync();
    }

    @Override
    public void exitOn(long number) {
        worldManager.getBlockchain().setExitOn(number);
    }

    @Override
    public void initSyncing() {
        worldManager.initSyncing();
    }


    /**
     * For testing purposes and 'hackers'
     */
    public ApplicationContext getApplicationContext() {
        return ctx;
    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

    /**
     * Shutting down all app beans
     */
    @Override
    public void stop(Runnable callback) {
        logger.info("Shutting down Ethereum instance...");
        worldManager.close();
        callback.run();
    }

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public boolean isRunning() {
        return true;
    }

    /**
     * Called first on shutdown lifecycle
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}

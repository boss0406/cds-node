package org.cds.main.blockchain.listener;

import org.cds.main.blockchain.core.*;
import org.cds.main.blockchain.net.eth.message.StatusMessage;
import org.cds.main.blockchain.net.message.Message;
import org.cds.main.blockchain.net.p2p.HelloMessage;
import org.cds.main.blockchain.net.rlpx.Node;
import org.cds.main.blockchain.net.server.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompositeEthereumListener implements EthereumListener {

    private static abstract class RunnableInfo implements Runnable {
        private EthereumListener listener;
        private String info;

        public RunnableInfo(EthereumListener listener, String info) {
            this.listener = listener;
            this.info = info;
        }

        @Override
        public String toString() {
            return "RunnableInfo: " + info + " [listener: " + listener.getClass() + "]";
        }
    }

    @Autowired
    EventDispatchThread eventDispatchThread = EventDispatchThread.getDefault();
    
    protected List<EthereumListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(EthereumListener listener) {
        listeners.add(listener);
    }
    public void removeListener(EthereumListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void trace(final String output) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "trace") {
                @Override
                public void run() {
                    listener.trace(output);
                }
            });
        }
    }

    @Override
    public void onBlock(final BlockSummary blockSummary) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onBlock") {
                @Override
                public void run() {
                    listener.onBlock(blockSummary);
                }
            });
        }
    }

    @Override
    public void onBlock(final BlockSummary blockSummary, final boolean best) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onBlock") {
                @Override
                public void run() {
                    listener.onBlock(blockSummary, best);
                }
            });
        }
    }

    @Override
    public void onRecvMessage(final Channel channel, final Message message) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onRecvMessage") {
                @Override
                public void run() {
                    listener.onRecvMessage(channel, message);
                }
            });
        }
    }

    @Override
    public void onSendMessage(final Channel channel, final Message message) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onSendMessage") {
                @Override
                public void run() {
                    listener.onSendMessage(channel, message);
                }
            });
        }
    }

    @Override
    public void onPeerDisconnect(final String host, final long port) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onPeerDisconnect") {
                @Override
                public void run() {
                    listener.onPeerDisconnect(host, port);
                }
            });
        }
    }

    @Override
    public void onPendingTransactionsReceived(final List<Transaction> transactions) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onPendingTransactionsReceived") {
                @Override
                public void run() {
                    listener.onPendingTransactionsReceived(transactions);
                }
            });
        }
    }

    @Override
    public void onPendingStateChanged(final PendingState pendingState) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onPendingStateChanged") {
                @Override
                public void run() {
                    listener.onPendingStateChanged(pendingState);
                }
            });
        }
    }

    @Override
    public void onSyncDone(final SyncState state) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onSyncDone") {
                @Override
                public void run() {
                    listener.onSyncDone(state);
                }
            });
        }
    }

    @Override
    public void onNoConnections() {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onNoConnections") {
                @Override
                public void run() {
                    listener.onNoConnections();
                }
            });
        }
    }

    @Override
    public void onHandShakePeer(final Channel channel, final HelloMessage helloMessage) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onHandShakePeer") {
                @Override
                public void run() {
                    listener.onHandShakePeer(channel, helloMessage);
                }
            });
        }
    }

    @Override
    public void onVMTraceCreated(final String transactionHash, final String trace) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onVMTraceCreated") {
                @Override
                public void run() {
                    listener.onVMTraceCreated(transactionHash, trace);
                }
            });
        }
    }

    @Override
    public void onNodeDiscovered(final Node node) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onNodeDiscovered") {
                @Override
                public void run() {
                    listener.onNodeDiscovered(node);
                }
            });
        }
    }

    @Override
    public void onEthStatusUpdated(final Channel channel, final StatusMessage status) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onEthStatusUpdated") {
                @Override
                public void run() {
                    listener.onEthStatusUpdated(channel, status);
                }
            });
        }
    }

    @Override
    public void onTransactionExecuted(final TransactionExecutionSummary summary) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onTransactionExecuted") {
                @Override
                public void run() {
                    listener.onTransactionExecuted(summary);
                }
            });
        }
    }

    @Override
    public void onPeerAddedToSyncPool(final Channel peer) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onPeerAddedToSyncPool") {
                @Override
                public void run() {
                    listener.onPeerAddedToSyncPool(peer);
                }
            });
        }
    }

    @Override
    public void onPendingTransactionUpdate(final TransactionReceipt txReceipt, final PendingTransactionState state,
                                           final Block block) {
        for (final EthereumListener listener : listeners) {
            eventDispatchThread.invokeLater(new RunnableInfo(listener, "onPendingTransactionUpdate") {
                @Override
                public void run() {
                    listener.onPendingTransactionUpdate(txReceipt, state, block);
                }
            });
        }
    }
}

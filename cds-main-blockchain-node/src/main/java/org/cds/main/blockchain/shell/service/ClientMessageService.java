package org.cds.main.blockchain.shell.service;

public interface ClientMessageService {
    void sendToTopic(String topic, Object dto);
}

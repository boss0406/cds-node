package org.cds.main.blockchain.vm;

/**
 * Factory used to create {@link VMHook} objects
 */
public interface VMHookFactory {
    /**
     * Creates {@link VMHook}
     */
    VMHook create();
}

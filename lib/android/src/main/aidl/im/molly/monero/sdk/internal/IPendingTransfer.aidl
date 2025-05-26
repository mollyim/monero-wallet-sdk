package im.molly.monero.sdk.internal;

import im.molly.monero.sdk.internal.ITransferCallback;

interface IPendingTransfer {
    long getAmount();
    long getFee();
    int getTxCount();
    oneway void commitAndClose(in ITransferCallback callback);
    oneway void close();
}

package im.molly.monero.internal;

import im.molly.monero.internal.ITransferCallback;

interface IPendingTransfer {
    long getAmount();
    long getFee();
    int getTxCount();
    oneway void commitAndClose(in ITransferCallback callback);
    oneway void close();
}

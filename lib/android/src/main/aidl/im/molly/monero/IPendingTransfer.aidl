package im.molly.monero;

import im.molly.monero.ITransferCallback;

interface IPendingTransfer {
    long getAmount();
    long getFee();
    int getTxCount();
    oneway void commitAndClose(in ITransferCallback callback);
    oneway void close();
}

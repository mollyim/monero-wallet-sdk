package im.molly.monero;

import im.molly.monero.IBalanceListener;
import im.molly.monero.IRefreshCallback;

interface IWallet {
    String getPrimaryAccountAddress();
    void addBalanceListener(in IBalanceListener listener);
    void removeBalanceListener(in IBalanceListener listener);
    oneway void save(in ParcelFileDescriptor destination);
    oneway void resumeRefresh(boolean skipCoinbaseOutputs, in IRefreshCallback callback);
    oneway void cancelRefresh();
    oneway void setRefreshSince(long heightOrTimestamp);
    void close();
}

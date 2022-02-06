package im.molly.monero;

import im.molly.monero.IBalanceListener;
import im.molly.monero.IRefreshCallback;
import im.molly.monero.PublicAddress;

interface IWallet {
    String getPrimaryAccountAddress();
    void addBalanceListener(in IBalanceListener listener);
    void removeBalanceListener(in IBalanceListener listener);
    void save(in ParcelFileDescriptor destination);
    oneway void restartRefresh(boolean skipCoinbaseOutputs, in IRefreshCallback callback);
    oneway void stopRefresh();
    void setRefreshSince(long heightOrTimestamp);
    void close();
}

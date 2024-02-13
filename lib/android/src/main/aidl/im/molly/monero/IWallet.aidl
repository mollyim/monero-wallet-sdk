package im.molly.monero;

import im.molly.monero.IBalanceListener;
import im.molly.monero.IWalletCallbacks;

interface IWallet {
    String getAccountPrimaryAddress();
    void addBalanceListener(in IBalanceListener listener);
    void removeBalanceListener(in IBalanceListener listener);
    oneway void resumeRefresh(boolean skipCoinbase, in IWalletCallbacks callback);
    oneway void cancelRefresh();
    oneway void setRefreshSince(long heightOrTimestamp);
    oneway void commit(in IWalletCallbacks callback);
    oneway void requestFees(in IWalletCallbacks callback);
    void close();
}

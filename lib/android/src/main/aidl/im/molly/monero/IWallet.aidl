package im.molly.monero;

import im.molly.monero.IBalanceListener;
import im.molly.monero.ITransferRequestCallback;
import im.molly.monero.IWalletCallbacks;
import im.molly.monero.PaymentRequest;
import im.molly.monero.SweepRequest;

interface IWallet {
    String getAccountPrimaryAddress();
    void addBalanceListener(in IBalanceListener listener);
    void removeBalanceListener(in IBalanceListener listener);
    oneway void resumeRefresh(boolean skipCoinbase, in IWalletCallbacks callback);
    oneway void cancelRefresh();
    oneway void setRefreshSince(long heightOrTimestamp);
    oneway void commit(in IWalletCallbacks callback);
    oneway void createPayment(in PaymentRequest request, in ITransferRequestCallback callback);
    oneway void createSweep(in SweepRequest request, in ITransferRequestCallback callback);
    oneway void requestFees(in IWalletCallbacks callback);
    void close();
}

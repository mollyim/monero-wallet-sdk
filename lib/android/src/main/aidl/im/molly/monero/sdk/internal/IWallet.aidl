package im.molly.monero.sdk.internal;

import im.molly.monero.sdk.PaymentRequest;
import im.molly.monero.sdk.SweepRequest;
import im.molly.monero.sdk.internal.IBalanceListener;
import im.molly.monero.sdk.internal.ITransferCallback;
import im.molly.monero.sdk.internal.IWalletCallbacks;

interface IWallet {
    String getPublicAddress();
    void addBalanceListener(in IBalanceListener listener);
    void removeBalanceListener(in IBalanceListener listener);
    oneway void addDetachedSubAddress(int accountIndex, int subAddressIndex, in IWalletCallbacks callback);
    oneway void createAccount(in IWalletCallbacks callback);
    oneway void createSubAddressForAccount(int accountIndex, in IWalletCallbacks callback);
    oneway void getAddressesForAccount(int accountIndex, in IWalletCallbacks callback);
    oneway void getAllAddresses(in IWalletCallbacks callback);
    oneway void resumeRefresh(boolean skipCoinbase, in IWalletCallbacks callback);
    oneway void cancelRefresh();
    oneway void setRefreshSince(long heightOrTimestamp);
    oneway void commit(in ParcelFileDescriptor outputFd, in IWalletCallbacks callback);
    oneway void createPayment(in PaymentRequest request, in ITransferCallback callback);
    oneway void createSweep(in SweepRequest request, in ITransferCallback callback);
    oneway void requestFees(in IWalletCallbacks callback);
    void close();
}

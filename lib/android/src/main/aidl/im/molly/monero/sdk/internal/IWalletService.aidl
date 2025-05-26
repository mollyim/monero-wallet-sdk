package im.molly.monero.sdk.internal;

import im.molly.monero.sdk.SecretKey;
import im.molly.monero.sdk.internal.IHttpRpcClient;
import im.molly.monero.sdk.internal.IWalletServiceCallbacks;
import im.molly.monero.sdk.internal.IWalletServiceListener;
import im.molly.monero.sdk.internal.WalletConfig;

interface IWalletService {
    oneway void createWallet(in WalletConfig config, in IHttpRpcClient rpcClient, in IWalletServiceCallbacks callback);
    oneway void restoreWallet(in WalletConfig config, in IHttpRpcClient rpcClient, in IWalletServiceCallbacks callback, in SecretKey spendSecretKey, long restorePoint);
    oneway void openWallet(in WalletConfig config, in IHttpRpcClient rpcClient, in IWalletServiceCallbacks callback, in ParcelFileDescriptor inputFd);
    void setListener(in IWalletServiceListener listener);
    boolean isServiceIsolated();
}

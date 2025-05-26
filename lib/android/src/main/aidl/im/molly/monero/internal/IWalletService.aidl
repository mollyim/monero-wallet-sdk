package im.molly.monero.internal;

import im.molly.monero.SecretKey;
import im.molly.monero.internal.IHttpRpcClient;
import im.molly.monero.internal.IWalletServiceCallbacks;
import im.molly.monero.internal.IWalletServiceListener;
import im.molly.monero.internal.WalletConfig;

interface IWalletService {
    oneway void createWallet(in WalletConfig config, in IHttpRpcClient rpcClient, in IWalletServiceCallbacks callback);
    oneway void restoreWallet(in WalletConfig config, in IHttpRpcClient rpcClient, in IWalletServiceCallbacks callback, in SecretKey spendSecretKey, long restorePoint);
    oneway void openWallet(in WalletConfig config, in IHttpRpcClient rpcClient, in IWalletServiceCallbacks callback, in ParcelFileDescriptor inputFd);
    void setListener(in IWalletServiceListener listener);
    boolean isServiceIsolated();
}

package im.molly.monero;

import im.molly.monero.IRemoteNodeClient;
import im.molly.monero.IStorageAdapter;
import im.molly.monero.IWalletServiceCallbacks;
import im.molly.monero.IWalletServiceListener;
import im.molly.monero.SecretKey;
import im.molly.monero.WalletConfig;

interface IWalletService {
    oneway void createWallet(in WalletConfig config, in IStorageAdapter storage, in IRemoteNodeClient client, in IWalletServiceCallbacks callback);
    oneway void restoreWallet(in WalletConfig config, in IStorageAdapter storage, in IRemoteNodeClient client, in IWalletServiceCallbacks callback, in SecretKey spendSecretKey, long restorePoint);
    oneway void openWallet(in WalletConfig config, in IStorageAdapter storage, in IRemoteNodeClient client, in IWalletServiceCallbacks callback);
    void setListener(in IWalletServiceListener listener);
}

package im.molly.monero;

import im.molly.monero.IWalletServiceCallbacks;
import im.molly.monero.IWalletServiceListener;
import im.molly.monero.SecretKey;
import im.molly.monero.WalletConfig;

interface IWalletService {
    oneway void createWallet(in WalletConfig config, in IWalletServiceCallbacks callback);
    oneway void restoreWallet(in WalletConfig config, in IWalletServiceCallbacks callback, in SecretKey spendSecretKey, long accountCreationTimestamp);
    oneway void openWallet(in WalletConfig config, in IWalletServiceCallbacks callback);
    void setListener(in IWalletServiceListener listener);
}

package im.molly.monero;

import im.molly.monero.IRemoteNodeClient;
import im.molly.monero.IWallet;
import im.molly.monero.IWalletServiceListener;
import im.molly.monero.SecretKey;
import im.molly.monero.WalletConfig;

interface IWalletService {
    IWallet createWallet(in WalletConfig config, in IRemoteNodeClient client);
    IWallet restoreWallet(in WalletConfig config, in IRemoteNodeClient client, in SecretKey spendSecretKey, long accountCreationTimestamp);
    IWallet openWallet(in WalletConfig config, in IRemoteNodeClient client, in ParcelFileDescriptor source);
    void setListener(in IWalletServiceListener listener);
}

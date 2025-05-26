package im.molly.monero.sdk.internal;

import im.molly.monero.sdk.BlockchainTime;

oneway interface IWalletCallbacks {
    void onRefreshResult(in BlockchainTime blockchainTime, int status);
    void onCommitResult(boolean success);
    void onSubAddressReady(String subAddress);
    void onSubAddressListReceived(in String[] subAddresses);
    void onAccountNotFound(int accountIndex);
    void onFeesReceived(in long[] fees);
}

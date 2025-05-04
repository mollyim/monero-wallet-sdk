package im.molly.monero.internal;

import im.molly.monero.BlockchainTime;

oneway interface IWalletCallbacks {
    void onRefreshResult(in BlockchainTime blockchainTime, int status);
    void onCommitResult(boolean success);
    void onSubAddressReady(String subAddress);
    void onSubAddressListReceived(in String[] subAddresses);
    void onFeesReceived(in long[] fees);
}

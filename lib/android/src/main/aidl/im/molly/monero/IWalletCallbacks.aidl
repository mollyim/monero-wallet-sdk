package im.molly.monero;

import im.molly.monero.BlockchainTime;

oneway interface IWalletCallbacks {
    void onAddressReady(in String[] subAddresses);
    void onRefreshResult(in BlockchainTime blockchainTime, int status);
    void onCommitResult(boolean success);
    void onFeesReceived(in long[] fees);
}

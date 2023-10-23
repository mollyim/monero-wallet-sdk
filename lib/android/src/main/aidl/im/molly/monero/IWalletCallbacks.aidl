package im.molly.monero;

import im.molly.monero.BlockchainTime;

oneway interface IWalletCallbacks {
    void onRefreshResult(in BlockchainTime blockchainTime, int status);
    void onCommitResult(boolean success);
}

package im.molly.monero;

oneway interface IWalletCallbacks {
    void onRefreshResult(long blockHeight, int status);
    void onCommitResult(boolean success);
}

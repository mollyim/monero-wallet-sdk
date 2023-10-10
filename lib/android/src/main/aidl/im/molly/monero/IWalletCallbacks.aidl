package im.molly.monero;

oneway interface IWalletCallbacks {
    void onRefreshResult(int blockHeight, int status);
    void onCommitResult(boolean success);
}

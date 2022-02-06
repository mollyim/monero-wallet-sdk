package im.molly.monero;

oneway interface IRefreshCallback {
    void onResult(long blockHeight, int status);
}

package im.molly.monero.sdk.internal;

oneway interface IWalletServiceListener {
    void onLogMessage(int priority, String tag, String msg, String cause);
}

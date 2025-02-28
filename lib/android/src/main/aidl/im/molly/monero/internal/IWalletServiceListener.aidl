package im.molly.monero.internal;

oneway interface IWalletServiceListener {
    void onLogMessage(int priority, String tag, String msg, String cause);
}

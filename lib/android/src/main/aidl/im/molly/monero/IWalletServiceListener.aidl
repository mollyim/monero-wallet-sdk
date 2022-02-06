package im.molly.monero;

oneway interface IWalletServiceListener {
    void onLogMessage(int priority, String tag, String msg, String cause);
}

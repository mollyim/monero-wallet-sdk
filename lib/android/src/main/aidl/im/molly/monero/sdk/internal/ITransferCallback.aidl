package im.molly.monero.sdk.internal;

import im.molly.monero.sdk.internal.IPendingTransfer;

oneway interface ITransferCallback {
    void onTransferCreated(in IPendingTransfer pendingTransfer);
    void onTransferCommitted();
//    void onDaemonBusy();
//    void onNoConnectionToDaemon();
//    void onRPCError(String errorMessage);
//    void onFailedToGetOutputs();
//    void onNotEnoughUnlockedMoney(long available, long sentAmount);
//    void onNotEnoughMoney(long available, long sentAmount);
//    void onTransactionNotPossible(long available, long transactionAmount, long fee);
//    void onNotEnoughOutsToMix(int mixinCount, Map<Long, Long> scantyOuts);
//    void onTransactionNotConstructed();
//    void onTransactionRejected(String transactionHash, int status);
//    void onTransactionSumOverflow(String errorMessage);
//    void onZeroDestination();
//    void onTransactionTooBig();
//    void onTransferError(String errorMessage);
//    void onWalletInternalError(String errorMessage);
    void onUnexpectedError(String message);
}

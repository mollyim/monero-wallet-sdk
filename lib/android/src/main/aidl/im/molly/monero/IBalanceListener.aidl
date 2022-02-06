package im.molly.monero;

import im.molly.monero.OwnedTxOut;

oneway interface IBalanceListener {
    void onBalanceChanged(in List<OwnedTxOut> txOuts, long checkedAtBlockHeight);
    void onRefresh(long blockchainHeight);
}

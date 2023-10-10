package im.molly.monero;

import im.molly.monero.internal.TxInfo;

oneway interface IBalanceListener {
    void onBalanceChanged(in List<TxInfo> txHistory, int blockchainHeight);
    void onRefresh(int blockchainHeight);
}

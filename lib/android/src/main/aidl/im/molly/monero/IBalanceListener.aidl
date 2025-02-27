package im.molly.monero;

import im.molly.monero.BlockchainTime;
import im.molly.monero.internal.TxInfo;

oneway interface IBalanceListener {
    void onBalanceUpdateFinalized(in List<TxInfo> txBatch, in String[] allSubAddresses, in BlockchainTime blockchainTime);
    void onBalanceUpdateChunk(in List<TxInfo> txBatch);
    void onWalletRefreshed(in BlockchainTime blockchainTime);
    void onSubAddressListUpdated(in String[] allSubAddresses);
}

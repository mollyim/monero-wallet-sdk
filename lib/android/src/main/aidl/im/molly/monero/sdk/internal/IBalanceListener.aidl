package im.molly.monero.sdk.internal;

import im.molly.monero.sdk.BlockchainTime;
import im.molly.monero.sdk.internal.TxInfo;

oneway interface IBalanceListener {
    void onBalanceUpdateFinalized(in List<TxInfo> txBatch, in String[] allSubAddresses, in BlockchainTime blockchainTime);
    void onBalanceUpdateChunk(in List<TxInfo> txBatch);
    void onWalletRefreshed(in BlockchainTime blockchainTime);
    void onSubAddressListUpdated(in String[] allSubAddresses);
}

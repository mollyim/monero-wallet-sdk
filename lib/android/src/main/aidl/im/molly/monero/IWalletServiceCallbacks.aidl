package im.molly.monero;

import im.molly.monero.IWallet;

oneway interface IWalletServiceCallbacks {
    void onWalletResult(in IWallet wallet);
}

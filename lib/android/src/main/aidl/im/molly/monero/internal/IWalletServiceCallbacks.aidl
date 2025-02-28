package im.molly.monero.internal;

import im.molly.monero.IWallet;

oneway interface IWalletServiceCallbacks {
    void onWalletResult(in IWallet wallet);
}

package im.molly.monero.internal;

import im.molly.monero.internal.IWallet;

oneway interface IWalletServiceCallbacks {
    void onWalletResult(in IWallet wallet);
}

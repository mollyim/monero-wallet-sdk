package im.molly.monero.sdk.internal;

import im.molly.monero.sdk.internal.IWallet;

oneway interface IWalletServiceCallbacks {
    void onWalletResult(in IWallet wallet);
}

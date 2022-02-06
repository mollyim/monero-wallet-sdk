package im.molly.monero;

import im.molly.monero.IRemoteNodeClient;

interface IWalletClient {
    int getNetworkId();
    IRemoteNodeClient getRemoteNodeClient();
}

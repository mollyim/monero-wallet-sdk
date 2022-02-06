package im.molly.monero;

import im.molly.monero.HttpResponse;
import im.molly.monero.RemoteNode;

interface IRemoteNodeClient {
//    RemoteNode getRemoteNode();
    HttpResponse makeRequest(String method, String path, String header, in byte[] body);
}

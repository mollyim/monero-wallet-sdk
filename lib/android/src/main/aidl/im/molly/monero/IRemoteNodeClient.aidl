package im.molly.monero;

import im.molly.monero.IHttpRequestCallback;

interface IRemoteNodeClient {
    oneway void requestAsync(int requestId, String method, String path, String header, in byte[] bodyBytes, in IHttpRequestCallback callback);
    oneway void cancelAsync(int requestId);
}

package im.molly.monero.internal;

import im.molly.monero.internal.HttpRequest;
import im.molly.monero.internal.IHttpRequestCallback;

interface IHttpRpcClient {
    oneway void callAsync(in HttpRequest request, in IHttpRequestCallback callback, int callId);
    oneway void cancelAsync(int callId);
}

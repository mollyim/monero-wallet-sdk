package im.molly.monero.sdk.internal;

import im.molly.monero.sdk.internal.HttpRequest;
import im.molly.monero.sdk.internal.IHttpRequestCallback;

interface IHttpRpcClient {
    oneway void callAsync(in HttpRequest request, in IHttpRequestCallback callback, int callId);
    oneway void cancelAsync(int callId);
}

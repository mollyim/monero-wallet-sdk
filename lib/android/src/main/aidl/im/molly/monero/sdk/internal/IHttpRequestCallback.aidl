package im.molly.monero.sdk.internal;

import im.molly.monero.sdk.internal.HttpResponse;

oneway interface IHttpRequestCallback {
    void onResponse(in HttpResponse response);
    void onError();
    void onRequestCanceled();
}

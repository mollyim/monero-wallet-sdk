package im.molly.monero.internal;

import im.molly.monero.internal.HttpResponse;

oneway interface IHttpRequestCallback {
    void onResponse(in HttpResponse response);
    void onError();
    void onRequestCanceled();
}

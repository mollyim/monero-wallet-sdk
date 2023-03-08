package im.molly.monero;

oneway interface IHttpRequestCallback {
    void onResponse(int code, String contentType, in ParcelFileDescriptor body);
    void onFailure();
}

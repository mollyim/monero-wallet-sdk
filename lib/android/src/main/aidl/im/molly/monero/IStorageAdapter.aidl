package im.molly.monero;

interface IStorageAdapter {
    boolean writeAsync(in ParcelFileDescriptor pfd);
    void readAsync(in ParcelFileDescriptor pfd);
}

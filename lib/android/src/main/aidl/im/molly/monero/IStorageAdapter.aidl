package im.molly.monero;

interface IStorageAdapter {
    boolean writeAsync(in ParcelFileDescriptor pfd);
    oneway void readAsync(in ParcelFileDescriptor pfd);
}

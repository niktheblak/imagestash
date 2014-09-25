package imagestash.j;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RandomAccessFileInputStream extends InputStream {
    private final RandomAccessFile source;
    private final long amount;

    private long bytesRead;

    public RandomAccessFileInputStream(RandomAccessFile source) throws IOException {
        this(source, source.length() - source.getFilePointer());
    }

    public RandomAccessFileInputStream(RandomAccessFile source, long amount) throws IOException {
        long remaining = source.length() - source.getFilePointer();
        if (amount > remaining) {
            throw new IllegalArgumentException("Requested read amount larger than remaining data in source file");
        }
        this.source = source;
        this.amount = amount;
    }

    @Override
    public int read() throws IOException {
        if (bytesRead == amount) {
            return -1;
        }
        int b = source.read();
        if (b != -1) {
            ++bytesRead;
        }
        return b;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (bytesRead == amount) {
            return -1;
        }
        int amountToRead = (int)Math.min(len, remaining());
        int bytesRead = source.read(b, off, amountToRead);
        this.bytesRead += bytesRead;
        return bytesRead;
    }

    @Override
    public long skip(long n) throws IOException {
        int amountToSkip = (int)Math.min(n, remaining());
        int skipped = source.skipBytes(amountToSkip);
        bytesRead += skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return (int)remaining();
    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    public long remaining() {
        return amount - bytesRead;
    }
}

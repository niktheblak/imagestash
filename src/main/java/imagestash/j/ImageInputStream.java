package imagestash.j;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class ImageInputStream extends InputStream {
    private final RandomAccessFile source;
    private final long length;

    private long position;

    public ImageInputStream(RandomAccessFile source, long length) throws IOException {
        this.source = source;
        this.length = length;
    }

    @Override
    public int read() throws IOException {
        if (position == length) {
            return -1;
        }
        int b = source.read();
        if (b != -1) {
            ++position;
        }
        return b;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (position == length) {
            return -1;
        }
        int amountToRead = (int)Math.min(len, remaining());
        int bytesRead = source.read(b, off, amountToRead);
        position += bytesRead;
        return bytesRead;
    }

    @Override
    public long skip(long n) throws IOException {
        int skipped = source.skipBytes((int)n);
        position += skipped;
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
        return length - position;
    }
}

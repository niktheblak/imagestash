package imagestash.j;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class FileChannels {
    private FileChannels() { }

    public static FileChannel read(File file) throws IOException {
        Path path = file.toPath();
        return FileChannel.open(path, StandardOpenOption.READ);
    }

    public static FileChannel read(String filePath) throws IOException {
        Path path = new File(filePath).toPath();
        return FileChannel.open(path, StandardOpenOption.READ);
    }

    public static FileChannel append(File file) throws IOException {
        Path path = file.toPath();
        return FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}

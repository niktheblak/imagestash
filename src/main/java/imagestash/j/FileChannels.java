/* Copyright 2016 Niko Korhonen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

package imagestash.j;

import java.awt.image.BufferedImage;

public class Scalr {
    private Scalr() { }

    public static BufferedImage resize(BufferedImage source, int size) {
        return org.imgscalr.Scalr.resize(source, size);
    }
}

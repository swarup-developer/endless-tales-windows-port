// Makes a Windows .ico (256, 128, 64, 48, 32, 16 px, PNG-compressed entries) from any image ImageIO can read.
// Run: java -cp <imageio jars> tools/MakeIcon.java <in-image> <out.ico>

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MakeIcon {
    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        if (src == null) throw new IllegalArgumentException("Cannot read " + args[0]);
        int[] sizes = {256, 128, 64, 48, 32, 16};
        List<byte[]> pngs = new ArrayList<>();
        for (int s : sizes) {
            BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            var g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            double scale = Math.min((double) s / src.getWidth(), (double) s / src.getHeight());
            int w = (int) Math.round(src.getWidth() * scale), h = (int) Math.round(src.getHeight() * scale);
            g.drawImage(src, (s - w) / 2, (s - h) / 2, w, h, null);
            g.dispose();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bo);
            pngs.add(bo.toByteArray());
        }
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(args[1]))) {
            writeShort(out, 0);            // reserved
            writeShort(out, 1);            // type: icon
            writeShort(out, sizes.length); // count
            int offset = 6 + 16 * sizes.length;
            for (int i = 0; i < sizes.length; i++) {
                int s = sizes[i];
                out.writeByte(s >= 256 ? 0 : s);
                out.writeByte(s >= 256 ? 0 : s);
                out.writeByte(0);          // palette
                out.writeByte(0);          // reserved
                writeShort(out, 1);        // planes
                writeShort(out, 32);       // bits per pixel
                writeInt(out, pngs.get(i).length);
                writeInt(out, offset);
                offset += pngs.get(i).length;
            }
            for (byte[] p : pngs) out.write(p);
        }
        System.out.println("wrote " + args[1]);
    }

    private static void writeShort(DataOutputStream o, int v) throws Exception { o.writeByte(v & 0xFF); o.writeByte((v >> 8) & 0xFF); }
    private static void writeInt(DataOutputStream o, int v) throws Exception { writeShort(o, v & 0xFFFF); writeShort(o, (v >> 16) & 0xFFFF); }
}

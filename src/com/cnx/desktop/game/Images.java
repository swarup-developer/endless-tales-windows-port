package com.cnx.desktop.game;

import com.cnx.desktop.DesktopRuntime;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads game images (mostly .webp, decoded with the TwelveMonkeys ImageIO plugin) into JavaFX images.
 * Keeps a small most-recently-used cache.
 */
public final class Images {
    private static final int CACHE_SIZE = 48;
    private static final Map<String, Image> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Image> e) { return size() > CACHE_SIZE; }
    };

    private Images() {}

    /** An asset image, e.g. asset("locations", "locale_lisport_farms"). Missing images return null. */
    public static Image asset(String folder, String name) {
        if (name == null || name.isEmpty()) return null;
        String base = (folder == null || folder.isEmpty() ? "" : folder + "/") + stripExt(name);
        for (String ext : new String[]{".webp", ".jpg", ".png"}) {
            Path p = DesktopRuntime.assetsDir.resolve(base + ext);
            if (Files.isRegularFile(p)) return load(p);
        }
        return null;
    }

    /** An image from the decoded res/drawable* folders, e.g. drawable("icon_daytime"). */
    public static Image drawable(String name) {
        if (name == null || name.isEmpty()) return null;
        for (String dir : new String[]{"drawable", "drawable-xxhdpi", "drawable-xhdpi", "drawable-hdpi", "drawable-mdpi", "drawable-nodpi", "mipmap-xxxhdpi", "mipmap-xxhdpi"}) {
            for (String ext : new String[]{".png", ".webp", ".jpg"}) {
                Path p = DesktopRuntime.resDir.resolve(dir).resolve(name + ext);
                if (Files.isRegularFile(p)) return load(p);
            }
        }
        return null;
    }

    private static String stripExt(String n) {
        for (String ext : new String[]{".webp", ".jpg", ".png"}) if (n.endsWith(ext)) return n.substring(0, n.length() - ext.length());
        return n;
    }

    public static synchronized Image load(Path p) {
        String key = p.toString();
        Image cached = CACHE.get(key);
        if (cached != null) return cached;
        Image img;
        try {
            if (key.endsWith(".webp")) {
                BufferedImage bi = ImageIO.read(p.toFile());
                img = bi == null ? null : toFx(bi);
            } else {
                img = new Image(p.toUri().toString());
                if (img.isError()) img = null;
            }
        } catch (Exception e) {
            Log.warn("Cannot read image " + p + ": " + e);
            img = null;
        }
        if (img != null) CACHE.put(key, img);
        return img;
    }

    private static Image toFx(BufferedImage bi) {
        int w = bi.getWidth(), h = bi.getHeight();
        int[] argb = bi.getRGB(0, 0, w, h, null, 0, w);
        WritableImage out = new WritableImage(w, h);
        out.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), argb, 0, w);
        return out;
    }
}

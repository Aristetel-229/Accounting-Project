package tala.ui.theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * Backdrop
 * ------------------------------------------------------------
 * Paints the page background of the light design:
 *   linear-gradient(135deg, #ede6ff 0%, #f6f2ff 35%, #fbf9ff 60%, #fff8e3 100%)
 *   + a soft yellow glow in the top-right corner
 *   + a soft violet glow in the bottom-left corner
 *
 * The gradients are slow to draw, and the page background is behind
 * everything (it is redrawn while scrolling and on every hover), so it
 * is drawn ONCE into a picture at the screen's own resolution and that
 * picture is reused until the window size changes.
 */
public final class Backdrop {

    private Backdrop() { }

    /** A few ready pictures, by size (pages, the log-in window...) */
    private static final java.util.Map<String, java.awt.image.BufferedImage> CACHE =
            new java.util.LinkedHashMap<String, java.awt.image.BufferedImage>(8, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, java.awt.image.BufferedImage> eldest) {
                    return size() > 4;
                }
            };

    public static void paint(Graphics2D g, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        AffineTransform t = g.getTransform();
        double scale = t.getScaleX();
        boolean simple = t.getShearX() == 0 && t.getShearY() == 0 && scale == t.getScaleY() && scale > 0 && scale <= 4;
        if (!simple) {
            draw(g, w, h);   // printing or an unusual transform: draw it directly
            return;
        }
        int pw = (int) Math.ceil(w * scale), ph = (int) Math.ceil(h * scale);
        String key = pw + "x" + ph + "@" + scale;
        java.awt.image.BufferedImage img = CACHE.get(key);
        if (img == null) {
            java.awt.GraphicsConfiguration gc = g.getDeviceConfiguration();
            img = gc != null && gc.getDevice().getType() == java.awt.GraphicsDevice.TYPE_RASTER_SCREEN
                    ? gc.createCompatibleImage(pw, ph, java.awt.Transparency.OPAQUE)
                    : new java.awt.image.BufferedImage(pw, ph, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D ig = img.createGraphics();
            ig.scale(scale, scale);
            draw(ig, w, h);
            ig.dispose();
            CACHE.put(key, img);
        }
        Graphics2D g2 = (Graphics2D) g.create();
        // draw 1:1 in screen pixels (no resampling)
        g2.setTransform(AffineTransform.getTranslateInstance(t.getTranslateX(), t.getTranslateY()));
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
    }

    private static void draw(Graphics2D g, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setPaint(new LinearGradientPaint(0, 0, Math.max(1, w), Math.max(1, h),
                new float[] {0f, 0.35f, 0.60f, 1f},
                new Color[] {new Color(0xede6ff), new Color(0xf6f2ff), new Color(0xfbf9ff), new Color(0xfff8e3)}));
        g2.fillRect(0, 0, w, h);
        // radial-gradient(700px 420px at 100% 0%, rgba(253,221,68,.28), transparent 65%)
        glow(g2, w, 0, 700, 420, new Color(253, 221, 68, 71), w, h);
        // radial-gradient(800px 520px at 0% 100%, rgba(167,139,250,.30), transparent 65%)
        glow(g2, 0, h, 800, 520, new Color(167, 139, 250, 77), w, h);
        g2.dispose();
    }

    /** An oval glow (rx by ry) that fades out to nothing */
    private static void glow(Graphics2D g2, float cx, float cy, float rx, float ry, Color color, int w, int h) {
        float squash = ry / rx;
        AffineTransform t = AffineTransform.getScaleInstance(1, squash);
        Point2D center = new Point2D.Float(cx, cy / squash);
        Color clear = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
        g2.setPaint(new RadialGradientPaint(center, rx, center, new float[] {0f, 0.65f, 1f},
                new Color[] {color, clear, clear}, RadialGradientPaint.CycleMethod.NO_CYCLE,
                RadialGradientPaint.ColorSpaceType.SRGB, t));
        g2.fillRect(0, 0, w, h);
    }
}

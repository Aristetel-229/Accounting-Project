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
 */
public final class Backdrop {

    private Backdrop() { }

    public static void paint(Graphics2D g, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
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

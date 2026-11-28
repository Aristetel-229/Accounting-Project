package tala.ui.theme;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalScrollPaneUI;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * SmoothScrollPaneUI
 * ------------------------------------------------------------
 * Every scroll area in TALA uses this: the mouse wheel glides
 * instead of jumping. Each wheel step moves a "target", and the
 * content eases toward it over ~0.2 s (fast at first, then gently).
 *
 * When an inner area cannot scroll any further (or cannot scroll
 * up/down at all), the wheel is passed on to the area around it,
 * so the page keeps scrolling instead of getting stuck.
 * Shift + wheel scrolls sideways.
 */
public class SmoothScrollPaneUI extends MetalScrollPaneUI {

    private static final String ANIMATOR = "tala.smoothScroll";

    public static ComponentUI createUI(JComponent c) {
        return new SmoothScrollPaneUI();
    }

    @Override
    protected MouseWheelListener createMouseWheelListener() {
        return this::wheel;
    }

    private void wheel(MouseWheelEvent e) {
        JScrollPane pane = scrollpane;
        if (pane == null || !pane.isWheelScrollingEnabled()) {
            return;
        }
        double rotation = e.getPreciseWheelRotation();
        if (rotation == 0) {
            return;
        }
        boolean sideways = e.isShiftDown();
        JScrollBar bar = sideways ? pane.getHorizontalScrollBar() : pane.getVerticalScrollBar();
        if (!sideways && (bar == null || !bar.isVisible())) {
            // this area only scrolls sideways (or not at all): let the page scroll instead
            passOn(e);
            return;
        }
        if (bar == null || !bar.isVisible()) {
            passOn(e);
            return;
        }
        Animator a = (Animator) bar.getClientProperty(ANIMATOR);
        if (a == null) {
            a = new Animator(bar);
            bar.putClientProperty(ANIMATOR, a);
        }
        int max = bar.getMaximum() - bar.getVisibleAmount();
        double from = a.running() ? a.target : bar.getValue();
        boolean atEdge = (rotation < 0 && from <= bar.getMinimum()) || (rotation > 0 && from >= max);
        if (atEdge) {
            passOn(e);            // nothing more to scroll here: the outer area takes over
            return;
        }
        int unit = Math.max(16, bar.getUnitIncrement(rotation > 0 ? 1 : -1));
        double step = rotation * Math.max(56, unit * 3);
        a.scrollTo(Math.max(bar.getMinimum(), Math.min(max, from + step)));
        e.consume();
    }

    /** Hands the wheel event to the next scroll area outside this one */
    private void passOn(MouseWheelEvent e) {
        Component outer = SwingUtilities.getAncestorOfClass(JScrollPane.class, scrollpane);
        if (outer == null) {
            return;
        }
        Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), outer);
        outer.dispatchEvent(new MouseWheelEvent(outer, e.getID(), e.getWhen(), e.getModifiersEx(),
                p.x, p.y, e.getXOnScreen(), e.getYOnScreen(), e.getClickCount(), e.isPopupTrigger(),
                e.getScrollType(), e.getScrollAmount(), e.getWheelRotation(), e.getPreciseWheelRotation()));
    }

    /** Eases one scroll bar toward its target, about 120 times a second */
    private static final class Animator {
        private final JScrollBar bar;
        private final Timer timer;
        private double position, target;
        private int lastSet = Integer.MIN_VALUE;
        private long lastTick;

        Animator(JScrollBar bar) {
            this.bar = bar;
            this.timer = new Timer(8, e -> tick());
            this.timer.setCoalesce(true);
        }

        boolean running() {
            return timer.isRunning();
        }

        void scrollTo(double newTarget) {
            if (!timer.isRunning() || bar.getValue() != lastSet) {
                position = bar.getValue();        // start from where the bar really is
            }
            target = newTarget;
            lastTick = System.nanoTime();
            if (!timer.isRunning()) {
                timer.start();
            }
        }

        private void tick() {
            // the user dragged the bar meanwhile: stop gliding and let them have it
            if (lastSet != Integer.MIN_VALUE && bar.getValue() != lastSet && Math.abs(bar.getValue() - lastSet) > 1) {
                timer.stop();
                lastSet = Integer.MIN_VALUE;
                return;
            }
            long now = System.nanoTime();
            double dt = Math.min(50, (now - lastTick) / 1_000_000.0);
            lastTick = now;
            // frame-rate independent easing: ~90% of the way in about 0.16 s
            double k = 1 - Math.pow(0.0005, dt / 1000.0 * 4.0);
            position += (target - position) * k;
            if (Math.abs(target - position) < 0.6) {
                position = target;
            }
            lastSet = (int) Math.round(position);
            bar.setValue(lastSet);
            if (position == target) {
                timer.stop();
                lastSet = Integer.MIN_VALUE;
            }
        }
    }
}

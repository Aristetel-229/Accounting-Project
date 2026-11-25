package tala.ui.theme;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Hover
 * ------------------------------------------------------------
 * Smooth hover effects for any component.
 *
 *   Hover h = Hover.of(card);   // starts watching the mouse over "card"
 *   float t = h.value();         // 0 = mouse away ... 1 = mouse over (eased)
 *
 * The value glides from 0 to 1 in about 160 ms when the mouse comes in,
 * and back again when it leaves, and the component is repainted on every
 * step. Painting code mixes colors with Theme.mix(a, b, t), grows shadows
 * or lifts the component by t * 2px.
 *
 * One app-wide mouse listener decides what is under the mouse, so moving
 * onto a button INSIDE a card still counts as "over the card".
 */
public final class Hover {

    private static final Map<JComponent, Hover> ALL = new WeakHashMap<>();
    private static final List<Hover> MOVING = new ArrayList<>();
    private static final float STEP = 0.14f;   // per 15 ms frame
    private static Timer timer;
    private static boolean listening;

    private final JComponent owner;
    private float value;        // 0..1, what is painted
    private boolean over;       // is the mouse over it right now?
    private Runnable onChange;

    private Hover(JComponent owner) {
        this.owner = owner;
    }

    /** The hover state of a component (created the first time) */
    public static Hover of(JComponent component) {
        Hover hover = ALL.get(component);
        if (hover == null) {
            hover = new Hover(component);
            ALL.put(component, hover);
            listen();
        }
        return hover;
    }

    /** Eased 0..1 value for painting */
    public float value() {
        float t = value;
        return t * t * (3 - 2 * t);   // smoothstep
    }

    public boolean isOver() {
        return over;
    }

    /** Something to run on every animation step (besides repaint) */
    public Hover onChange(Runnable r) {
        this.onChange = r;
        return this;
    }

    private void setOver(boolean over) {
        if (this.over == over) {
            return;
        }
        this.over = over;
        if (!MOVING.contains(this)) {
            MOVING.add(this);
        }
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    /** One animation frame for every hover that is still gliding */
    private static void tick() {
        for (int i = MOVING.size() - 1; i >= 0; i--) {
            Hover h = MOVING.get(i);
            float target = h.over ? 1f : 0f;
            if (h.value < target) {
                h.value = Math.min(target, h.value + STEP);
            } else if (h.value > target) {
                h.value = Math.max(target, h.value - STEP);
            }
            if (h.value == target) {
                MOVING.remove(i);
            }
            h.owner.repaint();
            if (h.onChange != null) {
                h.onChange.run();
            }
        }
        if (MOVING.isEmpty()) {
            timer.stop();
        }
    }

    /** Installs the one app-wide mouse listener */
    private static void listen() {
        if (listening) {
            return;
        }
        listening = true;
        timer = new Timer(15, e -> tick());
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof MouseEvent)) {
                return;
            }
            MouseEvent me = (MouseEvent) event;
            int id = me.getID();
            if (id != MouseEvent.MOUSE_MOVED && id != MouseEvent.MOUSE_DRAGGED
                    && id != MouseEvent.MOUSE_EXITED && id != MouseEvent.MOUSE_ENTERED
                    && id != MouseEvent.MOUSE_WHEEL) {
                return;
            }
            Component source = me.getComponent();
            if (source == null || !source.isShowing()) {
                return;
            }
            Point screen = me.getLocationOnScreen();
            java.awt.Window window = SwingUtilities.getWindowAncestor(source);
            boolean leftWindow = id == MouseEvent.MOUSE_EXITED && source == window;
            for (Hover h : new ArrayList<>(ALL.values())) {
                JComponent c = h.owner;
                if (!c.isDisplayable()) {
                    // removed from its window: stop tracking it (Hover.of() makes a new one if it comes back)
                    ALL.remove(c);
                    MOVING.remove(h);
                    continue;
                }
                if (!c.isShowing()) {
                    h.setOver(false);
                    continue;
                }
                if (SwingUtilities.getWindowAncestor(c) != window) {
                    continue;
                }
                if (leftWindow) {
                    h.setOver(false);
                    continue;
                }
                Point p = new Point(screen);
                SwingUtilities.convertPointFromScreen(p, c);
                h.setOver(c.contains(p) && c.isEnabled());
            }
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
    }
}

package com.cnx.desktop.game;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * Keyboard navigation for the whole game, audio-game style.
 * The current screen (or the top pop-up) is a list of items; arrows move, Enter chooses,
 * Left/Right change a value, Escape goes back. Each item speaks only its own text — never role
 * words like "button" — plus "2 of 5" when position announcements are on.
 */
public final class Nav {
    /** Announce "2 of 5" after each item. */
    public static volatile boolean announcePositions = true;
    /** Short sound when moving between items. */
    public static volatile boolean menuSounds = true;

    public static final class Item {
        final Supplier<String> text;
        final Node node;
        Runnable activate;
        IntConsumer adjust;
        boolean heading;
        boolean enabled = true;

        public Item(Supplier<String> text, Node node) { this.text = text; this.node = node; }
        public Item(String text, Node node) { this(() -> text, node); }

        public Item onActivate(Runnable r) { activate = r; return this; }
        public Item onAdjust(IntConsumer c) { adjust = c; return this; }
        public Item heading() { heading = true; return this; }
        public Item enabled(boolean e) { enabled = e; return this; }

        String speak() {
            String t = text.get();
            return enabled ? t : t + ", " + Tr.t("unavailable");
        }
    }

    public static final class Context {
        final String title;
        final List<Item> items;
        int index = -1;
        Runnable onBack;
        /** Text read when the pop-up opened (repeated with R). */
        String intro = "";

        public Context(String title, List<Item> items, Runnable onBack) {
            this.title = title;
            this.items = items;
            this.onBack = onBack;
        }

        Item current() { return index >= 0 && index < items.size() ? items.get(index) : null; }
    }

    private Context base = new Context(null, new ArrayList<>(), null);
    private final Deque<Context> modals = new ArrayDeque<>();
    private Node highlighted;
    /** Called for Escape when the current screen has no back action (e.g. opens the game menu). */
    Runnable defaultEscape;
    /** Text input in progress (character name). */
    private TextEdit edit;

    Context active() { return modals.isEmpty() ? base : modals.peek(); }

    // ---------------------------------------------------------------- contexts

    /** Replaces the screen's list (the choice menu). {@code announce}: speak the first item after current speech. */
    void setBase(String title, List<Item> items, Runnable onBack, boolean afterSceneText) {
        base = new Context(title, items, onBack);
        if (modals.isEmpty() && !items.isEmpty()) {
            base.index = 0;
            highlight(items.get(0).node);
            String s = (title != null && !title.isBlank() ? title + ". " : "") + describe(base);
            // after new scene text: wait for it; a submenu opened by a key press: say it now
            Speech.say(s, !afterSceneText);
        }
    }

    void clearBase() { base = new Context(null, new ArrayList<>(), null); }

    /** The screen's current items (to add Left/Right behaviour after Ui.setChoices). */
    public List<Item> baseItems() { return base.items; }

    /** Moves the focus to an item of the screen's list without speaking. */
    public void focus(int index) {
        if (index < 0 || index >= base.items.size()) return;
        base.index = index;
        highlight(base.items.get(index).node);
    }

    /** Puts the keyboard on item {@code index} of the top pop-up; {@code speak}: say it now. */
    public void focusModal(int index, boolean speak) {
        Context c = active();
        if (c == base || index < 0 || index >= c.items.size()) return;
        c.index = index;
        highlight(c.current().node);
        if (speak) Speech.say((c.title != null ? c.title + ". " : "") + describe(c), true);
    }

    /** Puts the keyboard, silently, on the first item of the top pop-up with this text. */
    public void focusModalText(String text) {
        Context c = active();
        if (c == base) return;
        for (int i = 0; i < c.items.size(); i++) if (text.equals(c.items.get(i).text.get())) { focusModal(i, false); return; }
    }

    /** Index of the current item of the top pop-up (-1 if none). */
    public int modalIndex() { return modals.isEmpty() ? -1 : modals.peek().index; }

    /** The current item of the active list, spoken after current speech (after a pop-up closed). */
    void announceCurrent() {
        Context c = active();
        if (c.current() != null) Speech.say(describe(c), false);
    }

    Context activeContext() { return active(); }

    /**
     * A pop-up opened: its items take the keyboard until it closes.
     * Short pop-ups (a message, a reward) read their text at once and then sit on the first thing to
     * press; that text leaves the list so "1 of N" counts only what is left. Long pop-ups (character
     * sheet, help) start at the top so you can read them line by line.
     */
    void push(Context c) {
        modals.push(c);
        if (!c.items.isEmpty() && c.title != null && c.items.get(0).heading && c.title.equals(c.items.get(0).text.get())) c.items.remove(0);
        int first = firstActionable(c);
        StringBuilder intro = new StringBuilder();
        int n = 0;
        boolean hasSections = false;
        for (int i = 0; i < first; i++) {
            sentence(intro, c.items.get(i).text.get());
            if (c.items.get(i).heading) hasSections = true;
        }
        // short, plain pop-ups are read at once; anything with sections stays a list to read line by line
        if (first > 0 && intro.length() <= 450 && !hasSections) {
            for (n = 0; n < first; n++) c.items.remove(0);
            c.intro = intro.toString();
        }
        if (c.items.isEmpty()) c.index = -1;
        else if (!c.intro.isEmpty()) c.index = 0;                    // text was read: sit on the first button
        else {
            int f = firstActionable(c);
            c.index = f >= 0 && f <= 2 ? f : 0;                       // e.g. Options: start on the first setting
        }
        StringBuilder sb = new StringBuilder();
        if (c.title != null && !c.title.isBlank()) sentence(sb, c.title);
        if (!c.intro.isEmpty()) sb.append(c.intro);
        if (c.current() != null) { highlight(c.current().node); sb.append(describe(c)); }
        Speech.say(sb.toString(), true);
    }

    /** Appends text as a sentence (adds ". " unless it already ends with punctuation). */
    static void sentence(StringBuilder sb, String t) {
        if (t == null || t.isBlank()) return;
        t = t.strip();
        sb.append(t);
        char last = t.charAt(t.length() - 1);
        sb.append(last == '.' || last == '!' || last == '?' || last == ':' ? " " : ". ");
    }

    /** R inside a pop-up: its title, intro text and the current item. Returns false outside pop-ups. */
    boolean repeatPopup() {
        if (modals.isEmpty()) return false;
        Context c = modals.peek();
        StringBuilder sb = new StringBuilder();
        if (c.title != null) sentence(sb, c.title);
        sb.append(c.intro);
        sb.append(describe(c));
        Speech.say(sb.toString(), true);
        return true;
    }

    void pop(Context c) {
        boolean wasTop = modals.peek() == c;
        modals.remove(c);
        if (wasTop) {
            Context a = active();
            if (a.current() != null) highlight(a.current().node);
        }
    }

    void clearModals() { modals.clear(); }

    private static int firstActionable(Context c) {
        for (int i = 0; i < c.items.size(); i++) if (c.items.get(i).activate != null || c.items.get(i).adjust != null) return i;
        return -1;
    }

    private String describe(Context c) {
        Item it = c.current();
        if (it == null) return "";
        String s = it.speak();
        if (announcePositions && c.items.size() > 1) s += ", " + Tr.f("%d of %d", c.index + 1, c.items.size());
        return s;
    }

    // ---------------------------------------------------------------- keys

    /** Handles a key press; returns true when it was used. */
    boolean handle(KeyEvent e) {
        if (edit != null) return edit.handle(e);
        Context c = active();
        KeyCode k = e.getCode();
        switch (k) {
            case DOWN -> { move(c, 1); return true; }
            case UP -> { move(c, -1); return true; }
            case TAB -> { move(c, e.isShiftDown() ? -1 : 1); return true; }
            case HOME -> { jump(c, 0); return true; }
            case END -> { jump(c, c.items.size() - 1); return true; }
            case PAGE_DOWN -> { heading(c, 1); return true; }
            case PAGE_UP -> { heading(c, -1); return true; }
            case LEFT, RIGHT -> {
                Item it = c.current();
                if (it != null && it.adjust != null && it.enabled) {
                    it.adjust.accept(k == KeyCode.RIGHT ? 1 : -1);
                    // if the change rebuilt the list, the screen has already said the new value
                    if (active() == c && c.items.contains(it)) Speech.say(it.speak(), true);
                    return true;
                }
                return false;
            }
            case ENTER, SPACE -> {
                Item it = c.current();
                if (it == null) return false;
                if (!it.enabled) { Speech.say(it.speak(), true); return true; }
                if (it.activate != null) { it.activate.run(); return true; }
                if (it.adjust != null) { it.adjust.accept(1); if (active() == c && c.items.contains(it)) Speech.say(it.speak(), true); return true; }
                Speech.say(it.speak(), true);
                return true;
            }
            case ESCAPE, BACK_SPACE -> {
                if (c.onBack != null) c.onBack.run();
                else if (c == base && defaultEscape != null) defaultEscape.run();
                return true;
            }
            default -> {
                String ch = e.getText();
                if (ch != null && ch.length() == 1 && java.lang.Character.isLetterOrDigit(ch.charAt(0)) && !e.isControlDown() && !e.isAltDown() && e.isShiftDown()) {
                    return letter(c, ch.charAt(0));
                }
                return false;
            }
        }
    }

    private void move(Context c, int d) {
        if (c.items.isEmpty()) { Speech.say(Tr.t("Nothing here"), true); return; }
        int n = c.items.size();
        int i = c.index < 0 ? (d > 0 ? 0 : n - 1) : (c.index + d + n) % n;
        jump(c, i);
    }

    private void jump(Context c, int i) {
        if (c.items.isEmpty() || i < 0) return;
        c.index = Math.min(i, c.items.size() - 1);
        if (menuSounds) Sound.tick();
        highlight(c.current().node);
        Speech.say(describe(c), true);
    }

    private void heading(Context c, int d) {
        int n = c.items.size();
        for (int s = 1; s <= n; s++) {
            int i = (c.index + d * s + n * 2) % n;
            if (c.items.get(i).heading) { jump(c, i); return; }
        }
        Speech.say(Tr.t("No more sections"), true);
    }

    /** Shift + letter: next item starting with that letter. */
    private boolean letter(Context c, char ch) {
        int n = c.items.size();
        for (int s = 1; s <= n; s++) {
            int i = (c.index + s) % n;
            String t = c.items.get(i).text.get();
            if (!t.isEmpty() && java.lang.Character.toLowerCase(t.charAt(0)) == java.lang.Character.toLowerCase(ch)) { jump(c, i); return true; }
        }
        return false;
    }

    /** Re-reads the current item (after a value changed). */
    public void rereadCurrent() {
        Item it = active().current();
        if (it != null) Speech.say(it.speak(), true);
    }

    // ---------------------------------------------------------------- visual focus

    private void highlight(Node n) {
        if (highlighted != null) highlighted.getStyleClass().remove("nav-focus");
        highlighted = n;
        if (n == null) return;
        n.getStyleClass().add("nav-focus");
        ensureVisible(n);
    }

    private static void ensureVisible(Node n) {
        Node p = n.getParent();
        while (p != null && !(p instanceof ScrollPane)) p = p.getParent();
        if (!(p instanceof ScrollPane sp) || sp.getContent() == null) return;
        Bounds content = sp.getContent().getLayoutBounds();
        Bounds b = sp.getContent().sceneToLocal(n.localToScene(n.getLayoutBounds()));
        double viewport = sp.getViewportBounds().getHeight();
        double extra = content.getHeight() - viewport;
        if (extra <= 0 || b == null) return;
        double top = sp.getVvalue() * extra;
        if (b.getMinY() < top) sp.setVvalue(b.getMinY() / extra);
        else if (b.getMaxY() > top + viewport) sp.setVvalue((b.getMaxY() - viewport) / extra);
    }

    // ---------------------------------------------------------------- building a list from a pop-up

    /** Key on a node's properties: a ready-made Item for it (used by choices, stat rows ...). */
    public static final String ITEM = "nav.item";

    /** Walks a pop-up's nodes in reading order and makes an item for everything a player needs. */
    static List<Item> collect(Node root) {
        List<Item> out = new ArrayList<>();
        walk(root, out);
        return out;
    }

    private static void walk(Node n, List<Item> out) {
        if (n == null || !n.isVisible()) return;
        Object pre = n.getProperties().get(ITEM);
        if (pre instanceof Item it) { out.add(it); return; }
        if (n.getProperties().containsKey("nav.skip")) return;
        if (n instanceof Button b) {
            out.add(new Item(b::getText, b).onActivate(b::fire).enabled(!b.isDisabled()));
            return;
        }
        if (n instanceof CheckBox cb) {
            out.add(new Item(() -> cb.getText() + ", " + Tr.t(cb.isSelected() ? "on" : "off"), cb).onAdjust(d -> cb.setSelected(!cb.isSelected())));
            return;
        }
        if (n instanceof Slider s) {
            String label = String.valueOf(s.getProperties().getOrDefault("nav.label", ""));
            Object st = s.getProperties().get("nav.step");
            double step = st instanceof Double dd ? dd : Math.max(1, (s.getMax() - s.getMin()) / 20);
            @SuppressWarnings("unchecked")
            java.util.function.DoubleFunction<String> fmt = s.getProperties().get("nav.format") instanceof java.util.function.DoubleFunction<?> f
                    ? (java.util.function.DoubleFunction<String>) f : v -> String.valueOf(Math.round(v));
            out.add(new Item(() -> label + ", " + fmt.apply(s.getValue()), s).onAdjust(d -> {
                double v = Math.max(s.getMin(), Math.min(s.getMax(), Math.round((s.getValue() + d * step) / step) * step));
                s.setValue(v);
            }));
            return;
        }
        if (n instanceof ComboBox<?> cbx) {
            String label = String.valueOf(cbx.getProperties().getOrDefault("nav.label", ""));
            out.add(new Item(() -> label + ", " + cbx.getSelectionModel().getSelectedItem(), cbx).onAdjust(d -> {
                int size = cbx.getItems().size();
                if (size == 0) return;
                int i = Math.max(0, cbx.getSelectionModel().getSelectedIndex());
                cbx.getSelectionModel().select((i + d + size) % size);
            }));
            return;
        }
        if (n instanceof Label l) {
            if (l.getProperties().containsKey("nav.labelFor")) return; // read as part of its control
            String t = l.getText();
            if (t != null && !t.isBlank()) {
                Item it = new Item(t, l);
                if (l.getStyleClass().contains("panel-title") || l.getStyleClass().contains("section")) it.heading();
                out.add(it);
            }
            return;
        }
        if (n instanceof ImageView iv) {
            Object alt = iv.getProperties().get("nav.alt");
            if (alt != null) out.add(new Item(alt.toString(), iv));
            return;
        }
        if (n instanceof ScrollPane sp) { walk(sp.getContent(), out); return; }
        if (n instanceof Parent p) for (Node c : p.getChildrenUnmodifiable()) walk(c, out);
    }

    // ---------------------------------------------------------------- accessible text input

    /** Starts typing into a text field: letters are echoed, Backspace says what was removed, Enter finishes. */
    public void editText(TextField field, String label, Runnable done) {
        edit = new TextEdit(field, label, done);
        Speech.say(Tr.f("Editing %s.", label) + " " + (field.getText().isEmpty() ? Tr.t("Empty") + "." : field.getText()) + " " + Tr.t("Type, then press Enter."), true);
    }

    private final class TextEdit {
        final TextField field;
        final String label;
        final Runnable done;

        TextEdit(TextField f, String l, Runnable d) { field = f; label = l; done = d; }

        boolean handle(KeyEvent e) {
            KeyCode k = e.getCode();
            if (k == KeyCode.ENTER || k == KeyCode.ESCAPE || k == KeyCode.TAB) {
                edit = null;
                Speech.say(label + ": " + (field.getText().isEmpty() ? "empty" : field.getText()), true);
                if (done != null) done.run();
                return true;
            }
            if (k == KeyCode.BACK_SPACE) {
                String t = field.getText();
                if (t.isEmpty()) { Speech.say(Tr.t("Empty"), true); return true; }
                String removed = t.substring(t.length() - 1);
                field.setText(t.substring(0, t.length() - 1));
                Speech.say(removed.equals(" ") ? Tr.t("space") : removed, true);
                return true;
            }
            if (k == KeyCode.UP || k == KeyCode.DOWN || (k == KeyCode.R && e.isControlDown())) {
                Speech.say(field.getText().isEmpty() ? Tr.t("Empty") : field.getText(), true);
                return true;
            }
            String ch = e.getText();
            if (ch != null && !ch.isEmpty() && !e.isControlDown() && !e.isAltDown() && ch.charAt(0) >= ' ') {
                if (field.getText().length() >= 20) { Speech.say(Tr.t("Maximum 20 letters"), true); return true; }
                field.setText(field.getText() + ch);
                Speech.say(ch.equals(" ") ? Tr.t("space") : ch, true);
                return true;
            }
            return true; // swallow everything else while typing
        }
    }

    boolean editing() { return edit != null; }

    /** For panes the walker should not enter (decorations). */
    public static void skip(Node n) { n.getProperties().put("nav.skip", true); }
    /** A text alternative for an image. */
    public static void alt(ImageView iv, String text) { iv.getProperties().put("nav.alt", text); }
    /** The spoken name of a slider / combo box. */
    public static void label(Control c, String text) { c.getProperties().put("nav.label", text); }
    /** How a slider's value is spoken, and how far Left/Right move it. */
    public static void format(Slider s, java.util.function.DoubleFunction<String> f, double step) {
        s.getProperties().put("nav.format", f);
        s.getProperties().put("nav.step", step);
    }
    /** Marks a label as already spoken by the control it names. */
    public static void labelFor(Label l) { l.getProperties().put("nav.labelFor", true); }

    @SuppressWarnings("unused")
    private static boolean isPane(Node n) { return n instanceof Pane; }
}

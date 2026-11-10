package com.cnx.desktop.game;

import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.entities.Character;
import com.cnx.endlesstalestwo.enums.Enums;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The visual-novel stage: a fixed 1280x720 surface scaled to the window, with layers
 * (back to front): background art, portrait, HUD, text box, choice menu, modal panel, toasts, input blocker.
 */
public final class Ui {
    public static final double W = 1280, H = 720;
    private static Ui instance;

    public static Ui get() { return instance; }

    /** One button in a choice menu. */
    public record Choice(String label, String detail, Image icon, boolean enabled, Runnable action) {
        public Choice(String label, Runnable action) { this(label, null, null, true, action); }
        public Choice(String label, Image icon, Runnable action) { this(label, null, icon, true, action); }
    }

    final StackPane surface = new StackPane();
    private final ImageView background = new ImageView();
    private final ColorAdjust dayNight = new ColorAdjust();
    private final Pane portraitLayer = new Pane();
    /** Screen-specific visuals (battle enemies, shop goods) between the art and the HUD. */
    private final StackPane sceneLayer = new StackPane();
    private final ImageView portrait = new ImageView();
    private final ImageView cg = new ImageView();
    private final StackPane cgFrame = new StackPane(cg);
    private final Hud hud = new Hud();
    private final VBox textBox = new VBox(6);
    private final Label speaker = new Label();
    private final Label subLine = new Label();
    private final Text bodyText = new Text();
    private final VBox choiceBox = new VBox(8);
    private final ScrollPane choiceScroll = new ScrollPane(choiceBox);
    private final Label choiceTitle = new Label();
    private final VBox choicePanel = new VBox(8, choiceTitle, choiceScroll);
    private final StackPane modalLayer = new StackPane();
    private final VBox toastLayer = new VBox(8);
    private final Pane blocker = new Pane();
    private Timeline typewriter;
    private String fullText = "";
    private Runnable afterText;
    private int busyCount;
    /** Keyboard navigation + speech for the current screen and pop-ups. */
    public final Nav nav = new Nav();
    /** The last scene text (location description, dialogue line, event text) for the R key. */
    private String lastSceneSpeech = "";

    Ui(Scene scene, StackPane root) {
        instance = this;
        surface.setPrefSize(W, H);
        surface.setMinSize(W, H);
        surface.setMaxSize(W, H);
        surface.getStyleClass().add("surface");
        surface.setClip(new Rectangle(W, H));

        background.setFitWidth(W);
        background.setFitHeight(H);
        background.setEffect(dayNight);
        Region vignette = new Region();
        vignette.getStyleClass().add("vignette");
        vignette.setMouseTransparent(true);

        portrait.setPreserveRatio(true);
        portrait.setFitHeight(560);
        portrait.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.7)));
        cg.setPreserveRatio(true);
        cg.setFitHeight(400);
        cg.setFitWidth(820);
        cgFrame.getStyleClass().add("cg-frame");
        cgFrame.setLayoutY(78);
        cgFrame.setVisible(false);
        portraitLayer.getChildren().addAll(portrait, cgFrame);
        portraitLayer.setMouseTransparent(true);

        speaker.getStyleClass().add("speaker");
        subLine.getStyleClass().add("subline");
        subLine.setWrapText(true);
        bodyText.getStyleClass().add("body-text");
        bodyText.setWrappingWidth(W - 470);
        TextFlow flow = new TextFlow(bodyText);
        ScrollPane bodyScroll = new ScrollPane(flow);
        bodyScroll.setFitToWidth(true);
        bodyScroll.getStyleClass().add("clear-scroll");
        bodyScroll.setPrefViewportHeight(150);
        textBox.getChildren().addAll(speaker, subLine, bodyScroll);
        textBox.getStyleClass().add("textbox");
        textBox.setPrefWidth(W - 420);
        textBox.setMaxWidth(W - 420);
        textBox.setMaxHeight(230);
        textBox.setOnMouseClicked(e -> { if (e.getButton() == MouseButton.PRIMARY) skipText(); });
        StackPane.setAlignment(textBox, Pos.BOTTOM_LEFT);
        StackPane.setMargin(textBox, new Insets(0, 0, 18, 18));

        choiceTitle.getStyleClass().add("choice-title");
        choiceBox.setFillWidth(true);
        choiceBox.setPadding(new Insets(2, 6, 2, 2));
        choiceScroll.setFitToWidth(true);
        choiceScroll.getStyleClass().add("clear-scroll");
        choiceScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        choicePanel.getStyleClass().add("choice-panel");
        choicePanel.setPrefWidth(372);
        choicePanel.setMaxWidth(372);
        choicePanel.setMaxHeight(H - 110);
        StackPane.setAlignment(choicePanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(choicePanel, new Insets(0, 18, 18, 0));

        StackPane.setAlignment(hud, Pos.TOP_CENTER);

        modalLayer.setPickOnBounds(false);
        toastLayer.setAlignment(Pos.TOP_CENTER);
        toastLayer.setPadding(new Insets(80, 0, 0, 0));
        toastLayer.setMouseTransparent(true);
        toastLayer.setPickOnBounds(false);
        blocker.setVisible(false);
        blocker.setStyle("-fx-background-color: transparent;");

        sceneLayer.setPickOnBounds(false);
        surface.getChildren().addAll(background, vignette, portraitLayer, sceneLayer, hud, textBox, choicePanel, modalLayer, toastLayer, blocker);

        // scale the 1280x720 surface to the window, keeping the aspect ratio
        Group group = new Group(surface);
        root.getChildren().add(group);
        Scale scale = new Scale(1, 1, 0, 0);
        surface.getTransforms().add(scale);
        Runnable fit = () -> {
            double s = Math.min(scene.getWidth() / W, scene.getHeight() / H);
            scale.setX(s);
            scale.setY(s);
        };
        scene.widthProperty().addListener((o, a, b) -> fit.run());
        scene.heightProperty().addListener((o, a, b) -> fit.run());
        fit.run();

        // controls never take keyboard focus: the scene-level handler (Keys) drives everything,
        // so the screen reader hears only what the game speaks
        surface.setFocusTraversable(true);
        clearScene();
    }

    /** Repeats the scene text (R key). */
    public void repeatScene() {
        Speech.say(lastSceneSpeech.isEmpty() ? Tr.t("Nothing to repeat") : lastSceneSpeech, true);
    }

    /** True while a typewriter animation is running; Enter / click finishes it first. */
    boolean finishTextIfTyping() {
        if (typewriter == null) return false;
        skipText();
        return true;
    }

    private static final class Group extends javafx.scene.Group {
        Group(Node n) { super(n); }
    }

    // ---------------------------------------------------------------- scene parts

    /** Clears portrait, text box, choices and HUD visibility (background stays). */
    public void clearScene() {
        sceneLayer.getChildren().clear();
        setPortrait(null, true);
        setCg(null);
        textBox.setVisible(false);
        choicePanel.setVisible(false);
        hud.setVisible(false);
        stopTypewriter();
    }

    /** Shows art as a full-screen background (square art is center-cropped), tinted for the time of day. */
    public void setBackground(Image img, Enums.DayPeriod period) {
        if (img == null) img = Images.drawable("game_bg");
        if (img != background.getImage()) {
            background.setImage(img);
            if (img != null) {
                double iw = img.getWidth(), ih = img.getHeight();
                double scale = Math.max(W / iw, H / ih);
                double vw = W / scale, vh = H / scale;
                background.setViewport(new Rectangle2D((iw - vw) / 2, (ih - vh) / 2.6, vw, vh));
            }
            FadeTransition f = new FadeTransition(Duration.millis(350), background);
            f.setFromValue(0.35);
            f.setToValue(1);
            f.play();
        }
        setDayPeriod(period);
    }

    public void setDayPeriod(Enums.DayPeriod period) {
        if (period == Enums.DayPeriod.NIGHT) { dayNight.setSaturation(-0.45); dayNight.setBrightness(-0.32); dayNight.setHue(-0.04); }
        else if (period == Enums.DayPeriod.DAWN || period == Enums.DayPeriod.NIGHTFALL) { dayNight.setSaturation(-0.35); dayNight.setBrightness(-0.16); dayNight.setHue(0.02); }
        else { dayNight.setSaturation(0); dayNight.setBrightness(0); dayNight.setHue(0); }
    }

    public void dimBackground(boolean dim) {
        background.setOpacity(dim ? 0.55 : 1);
    }

    /** A character portrait standing on the left or right of the scene. */
    public void setPortrait(Image img, boolean right) {
        portrait.setImage(img);
        portrait.setVisible(img != null);
        if (img == null) return;
        double h = portrait.getFitHeight();
        double w = img.getWidth() * h / img.getHeight();
        portrait.setLayoutY(H - h - 6);
        portrait.setLayoutX(right ? W - 410 - w * 0.8 : 40);
        FadeTransition f = new FadeTransition(Duration.millis(300), portrait);
        f.setFromValue(0);
        f.setToValue(1);
        f.play();
    }

    /** A framed illustration (event art) above the text box. */
    public void setCg(Image img) {
        cg.setImage(img);
        cgFrame.setVisible(img != null);
        if (img == null) return;
        double scale = Math.min(cg.getFitWidth() / img.getWidth(), cg.getFitHeight() / img.getHeight());
        double w = img.getWidth() * scale;
        cgFrame.setLayoutX(18 + ((W - 420) - w) / 2 - 8);
        FadeTransition f = new FadeTransition(Duration.millis(400), cgFrame);
        f.setFromValue(0);
        f.setToValue(1);
        f.play();
    }

    public void showHud(boolean show) {
        hud.setVisible(show);
        if (show) hud.refresh();
    }

    public void refreshHud() { if (hud.isVisible()) hud.refresh(); }

    /** Like {@link #say(String, String, String, Runnable)} but speaks {@code spoken} instead of the full text. */
    public void say(String who, String sub, String text, Runnable done, String spoken) {
        speechOverride = spoken;
        say(who, sub, text, done);
    }

    private String speechOverride;
    /** True between a scene text and its menu: the menu then waits for the text instead of cutting it off. */
    private boolean sceneTextJustSpoken;

    /** Shows a line of text with a typewriter effect. Click / Space shows it all at once. */
    public void say(String who, String sub, String text, Runnable done) {
        textBox.setVisible(true);
        speaker.setText(who == null ? "" : who);
        speaker.setVisible(who != null && !who.isEmpty());
        speaker.setManaged(speaker.isVisible());
        subLine.setText(sub == null ? "" : sub);
        subLine.setVisible(sub != null && !sub.isEmpty());
        subLine.setManaged(subLine.isVisible());
        stopTypewriter();
        fullText = text == null ? "" : text;
        afterText = done;
        // name first, then the line (what blind VN players expect); sub line (job, age, your answer) after
        StringBuilder spoken = new StringBuilder();
        if (who != null && !who.isEmpty()) spoken.append(who).append(": ");
        spoken.append(fullText);
        if (sub != null && !sub.isEmpty()) spoken.append(". ").append(sub);
        lastSceneSpeech = spoken.toString();
        nav.clearBase();
        String now = speechOverride != null ? speechOverride : lastSceneSpeech;
        speechOverride = null;
        Speech.say(now, true);
        sceneTextJustSpoken = true;
        // with speech on, the choices appear at once (the voice already reads the text)
        if (fullText.isEmpty() || Settings.textSpeed <= 0 || Speech.enabled()) { bodyText.setText(fullText); finishText(); return; }
        bodyText.setText("");
        final int[] i = {0};
        typewriter = new Timeline(new KeyFrame(Duration.millis(Settings.textSpeed), e -> {
            i[0] = Math.min(fullText.length(), i[0] + 2);
            bodyText.setText(fullText.substring(0, i[0]));
            if (i[0] >= fullText.length()) { stopTypewriter(); finishText(); }
        }));
        typewriter.setCycleCount(Timeline.INDEFINITE);
        typewriter.play();
    }

    public void hideText() { stopTypewriter(); textBox.setVisible(false); }

    /** Shows text in the text box without speaking it (the caller speaks what is new). */
    public void showTextQuiet(String who, String sub, String text) {
        stopTypewriter();
        textBox.setVisible(true);
        speaker.setText(who == null ? "" : who);
        speaker.setVisible(who != null && !who.isEmpty());
        speaker.setManaged(speaker.isVisible());
        subLine.setText(sub == null ? "" : sub);
        subLine.setVisible(sub != null && !sub.isEmpty());
        subLine.setManaged(subLine.isVisible());
        fullText = text == null ? "" : text;
        bodyText.setText(fullText);
    }

    /** Puts screen-specific visuals (e.g. battle enemies) above the background art. */
    public void setSceneNode(Node n) {
        sceneLayer.getChildren().clear();
        if (n != null) sceneLayer.getChildren().add(n);
    }

    /** The next menu waits for current speech instead of interrupting it. */
    public void queueMenuAfterSpeech() { sceneTextJustSpoken = true; }

    /** Changes the text R repeats, without speaking it now. */
    public void setSceneSpeech(String s) { lastSceneSpeech = s == null ? "" : s; }

    private void skipText() {
        if (typewriter != null) { stopTypewriter(); bodyText.setText(fullText); finishText(); }
    }

    private void stopTypewriter() { if (typewriter != null) { typewriter.stop(); typewriter = null; } }

    private void finishText() {
        Runnable r = afterText;
        afterText = null;
        if (r != null) r.run();
    }

    /** Replaces the choice menu. */
    public void setChoices(String title, List<Choice> choices) { setChoices(title, choices, null); }

    /**
     * Replaces the choice menu. {@code onBack} runs for Escape (null: the screen's default, e.g. the game menu).
     * The menu becomes the keyboard list; its first item is spoken after the scene text.
     */
    public void setChoices(String title, List<Choice> choices, Runnable onBack) {
        choiceBox.getChildren().clear();
        choiceTitle.setText(title == null ? "" : title);
        choiceTitle.setVisible(title != null && !title.isEmpty());
        choiceTitle.setManaged(choiceTitle.isVisible());
        List<Nav.Item> items = new ArrayList<>();
        for (Choice c : choices) {
            HBox node = choiceButton(c);
            choiceBox.getChildren().add(node);
            items.add((Nav.Item) node.getProperties().get(Nav.ITEM));
        }
        nav.setBase(title, items, onBack, sceneTextJustSpoken);
        sceneTextJustSpoken = false;
        choicePanel.setVisible(!choices.isEmpty());
        choiceScroll.setVvalue(0);
        FadeTransition f = new FadeTransition(Duration.millis(200), choicePanel);
        f.setFromValue(0.2);
        f.setToValue(1);
        f.play();
    }

    public void hideChoices() { choicePanel.setVisible(false); }

    /**
     * One choice: a plain clickable row (a Button measures a wrapped-label graphic badly and makes
     * every choice ~200 px tall). It carries a {@link Nav.Item} that speaks "label, detail" only.
     */
    public static HBox choiceButton(Choice c) {
        HBox b = new HBox(10);
        b.getStyleClass().add("choice");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        double textWidth = c.icon() != null ? 280 : 320;
        VBox lines = new VBox(1);
        Label main = new Label(c.label());
        main.getStyleClass().add("choice-label");
        main.setWrapText(true);
        main.setPrefWidth(textWidth);
        main.setMaxWidth(textWidth);
        main.setMinHeight(Region.USE_PREF_SIZE);
        lines.getChildren().add(main);
        if (c.detail() != null && !c.detail().isEmpty()) {
            Label d = new Label(c.detail());
            d.getStyleClass().add("choice-detail");
            d.setWrapText(true);
            d.setPrefWidth(textWidth);
            d.setMaxWidth(textWidth);
            d.setMinHeight(Region.USE_PREF_SIZE);
            lines.getChildren().add(d);
        }
        if (c.icon() != null) {
            ImageView iv = new ImageView(c.icon());
            iv.setFitWidth(30);
            iv.setFitHeight(30);
            iv.setPreserveRatio(true);
            b.getChildren().add(iv);
        }
        b.getChildren().add(lines);
        if (!c.enabled()) b.getStyleClass().add("choice-disabled");
        Runnable fire = () -> {
            if (!c.enabled()) return;
            Sound.click();
            try { c.action().run(); } catch (Throwable t) { Log.error("Choice failed: " + c.label(), t); Ui.get().toast(Tr.t("Error: ") + t, true); }
        };
        String spoken = c.detail() == null || c.detail().isEmpty() ? c.label() : c.label() + ", " + c.detail();
        b.getProperties().put(Nav.ITEM, new Nav.Item(spoken, b).onActivate(fire).enabled(c.enabled()));
        b.setOnMouseClicked(e -> { if (e.getButton() == MouseButton.PRIMARY) fire.run(); });
        return b;
    }

    // ---------------------------------------------------------------- modal panels

    /** Shows a centered panel over a dimmed scene. Returns a handle that closes it. */
    public Runnable showModal(Node content, boolean dimClickCloses, Runnable onClose) {
        Region dim = new Region();
        dim.getStyleClass().add("modal-dim");
        StackPane holder = new StackPane(dim, content);
        StackPane.setAlignment(content, Pos.CENTER);
        modalLayer.getChildren().add(holder);
        FadeTransition f = new FadeTransition(Duration.millis(180), holder);
        f.setFromValue(0);
        f.setToValue(1);
        f.play();
        final boolean[] closed = {false};
        final Nav.Context[] ctx = new Nav.Context[1];
        Runnable close = () -> {
            if (closed[0]) return;
            closed[0] = true;
            boolean wasTop = nav.activeContext() == ctx[0];
            long said = Speech.spoken;
            modalLayer.getChildren().remove(holder);
            nav.pop(ctx[0]);
            if (onClose != null) onClose.run();
            // back where you were: say it, unless what happened next already spoke (a new screen, a pop-up)
            if (wasTop) javafx.application.Platform.runLater(() -> { if (Speech.spoken == said) nav.announceCurrent(); });
        };
        if (dimClickCloses) dim.setOnMouseClicked(e -> close.run());

        // keyboard + speech: the panel's contents become the list; Escape = close / its first (cancel) button
        List<Nav.Item> items = Nav.collect(content);
        String title = null;
        if (!items.isEmpty() && items.get(0).heading && items.get(0).node instanceof Label l && l.getStyleClass().contains("panel-title")) {
            title = l.getText();
        }
        Object esc = content.getProperties().get("nav.escape");
        Runnable back = content.getProperties().containsKey("nav.noEscape") ? nav::rereadCurrent
                : dimClickCloses || !(esc instanceof Button) ? close : ((Button) esc)::fire;
        ctx[0] = new Nav.Context(title, items, back);
        nav.push(ctx[0]);
        return close;
    }

    /** Text items before the first thing you can press, read when a short pop-up opens. */
    private static String leadingText(List<Nav.Item> items) {
        StringBuilder sb = new StringBuilder();
        for (Nav.Item it : items) {
            if (it.activate != null || it.adjust != null) break;
            if (it.heading && it.node instanceof Label l && l.getStyleClass().contains("panel-title")) continue;
            sb.append(it.text.get()).append(". ");
        }
        return sb.length() > 900 ? "" : sb.toString();
    }

    public int modalCount() { return modalLayer.getChildren().size(); }

    public boolean hasModal() { return !modalLayer.getChildren().isEmpty(); }

    public void closeAllModals() { modalLayer.getChildren().clear(); nav.clearModals(); }

    /** A styled panel with a title, scrollable body and buttons. */
    public static VBox panel(String title, Node body, double width, Button... buttons) {
        VBox p = new VBox(12);
        p.getStyleClass().add("panel");
        p.setMaxWidth(width);
        p.setPrefWidth(width);
        p.setMaxHeight(H - 80);
        if (title != null) {
            Label t = new Label(title);
            t.getStyleClass().add("panel-title");
            t.setWrapText(true);
            p.getChildren().add(t);
        }
        if (body != null) {
            ScrollPane sp = new ScrollPane(body);
            sp.setFitToWidth(true);
            sp.getStyleClass().add("clear-scroll");
            sp.setMaxHeight(H - 230);
            VBox.setVgrow(sp, Priority.ALWAYS);
            p.getChildren().add(sp);
        }
        if (buttons.length > 0) {
            HBox row = new HBox(10, buttons);
            row.setAlignment(Pos.CENTER_RIGHT);
            p.getChildren().add(row);
            p.getProperties().put("nav.escape", buttons[0]); // Escape = first button (Back / No / OK / Close)
        }
        return p;
    }

    public static Button button(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("btn");
        b.setFocusTraversable(false); // keys are handled by Nav, focus stays on the game surface
        b.setOnAction(e -> { Sound.click(); action.run(); });
        return b;
    }

    public static Label para(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.getStyleClass().add("para");
        return l;
    }

    /** Shows a message panel with an OK button. */
    public void message(String title, String text, Image img, Runnable onOk) {
        VBox body = new VBox(12);
        body.setAlignment(Pos.TOP_CENTER);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setPreserveRatio(true);
            iv.setFitHeight(180);
            body.getChildren().add(iv);
        }
        body.getChildren().add(para(text));
        final Runnable[] close = new Runnable[1];
        Button ok = button("OK", () -> close[0].run());
        ok.setDefaultButton(true);
        close[0] = showModal(panel(title, body, 560, ok), false, onOk);
    }

    /** Yes/no question. */
    public void confirm(String title, String text, Consumer<Boolean> answer) {
        final Runnable[] close = new Runnable[1];
        final boolean[] result = {false};
        Button yes = button(Game.t("Yes").equals("Yes") ? Tr.t("Yes") : Game.t("Yes"), () -> { result[0] = true; close[0].run(); });
        Button no = button(Game.t("No").equals("No") ? Tr.t("No") : Game.t("No"), () -> close[0].run());
        close[0] = showModal(panel(title, para(text), 520, no, yes), false, () -> answer.accept(result[0]));
    }

    // ---------------------------------------------------------------- toasts, busy

    public void toast(String text, boolean error) {
        if (text == null || text.isBlank()) return;
        Speech.say(text, error); // errors interrupt (they answer what you just did), info waits its turn
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(640);
        l.getStyleClass().addAll("toast", error ? "toast-error" : "toast-info");
        toastLayer.getChildren().add(l);
        PauseTransition stay = new PauseTransition(Duration.millis(2600 + Math.min(4000, text.length() * 30L)));
        stay.setOnFinished(e -> {
            FadeTransition f = new FadeTransition(Duration.millis(400), l);
            f.setToValue(0);
            f.setOnFinished(x -> toastLayer.getChildren().remove(l));
            f.play();
        });
        stay.play();
    }

    /** Blocks input while the game is busy (setScreenTouchable(false) / lockScreenTouches(true)). */
    public boolean isBusy() { return blocker.isVisible(); }

    public void busy(boolean b) {
        busyCount = b ? 1 : 0;
        blocker.setVisible(busyCount > 0);
    }

    /** A full-screen fade to black and back, with a caption in the middle. */
    public void transition(String caption, long millis, Runnable atMiddle) {
        StackPane black = new StackPane();
        black.setStyle("-fx-background-color: black;");
        Label l = new Label(caption);
        l.getStyleClass().add("transition-caption");
        l.setWrapText(true);
        black.getChildren().add(l);
        black.setOpacity(0);
        modalLayer.getChildren().add(black);
        Speech.say(caption, true);
        busy(true);
        FadeTransition in = new FadeTransition(Duration.millis(350), black);
        in.setToValue(1);
        in.setOnFinished(e -> {
            PauseTransition hold = new PauseTransition(Duration.millis(Math.max(200, millis)));
            hold.setOnFinished(x -> {
                try { if (atMiddle != null) atMiddle.run(); }
                catch (Throwable t) { Log.error("Transition step failed", t); }
                FadeTransition out = new FadeTransition(Duration.millis(400), black);
                out.setToValue(0);
                out.setOnFinished(y -> { modalLayer.getChildren().remove(black); busy(false); });
                out.play();
            });
            hold.play();
        });
        in.play();
    }

    // ---------------------------------------------------------------- HUD

    /** Top bar: portrait, name, class/level, HP/MP, XP, gold, day and time. */
    static final class Hud extends HBox {
        private final ImageView face = new ImageView();
        private final Label name = new Label(), cls = new Label(), gold = new Label(), day = new Label(), time = new Label();
        private final ProgressBar hp = new ProgressBar(), mp = new ProgressBar(), xp = new ProgressBar();
        private final Label hpText = new Label(), mpText = new Label();
        private final ImageView dayIcon = new ImageView();

        Hud() {
            super(16);
            getStyleClass().add("hud");
            setAlignment(Pos.CENTER_LEFT);
            setMaxHeight(64);
            setPrefWidth(W);
            face.setFitWidth(48);
            face.setFitHeight(48);
            face.setPreserveRatio(true);
            name.getStyleClass().add("hud-name");
            cls.getStyleClass().add("hud-small");
            VBox who = new VBox(0, name, cls);
            who.setAlignment(Pos.CENTER_LEFT);
            who.setMinWidth(170);
            hp.getStyleClass().add("bar-hp");
            mp.getStyleClass().add("bar-mp");
            xp.getStyleClass().add("bar-xp");
            for (ProgressBar b : new ProgressBar[]{hp, mp}) b.setPrefWidth(150);
            xp.setPrefWidth(150);
            hpText.getStyleClass().add("hud-small");
            mpText.getStyleClass().add("hud-small");
            VBox bars = new VBox(3, row("icon_hp_color", hp, hpText), row("icon_mp_color", mp, mpText), row("icon_experience", xp, new Label()));
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            gold.getStyleClass().add("hud-value");
            day.getStyleClass().add("hud-value");
            time.getStyleClass().add("hud-small");
            dayIcon.setFitWidth(28);
            dayIcon.setFitHeight(28);
            VBox clock = new VBox(0, day, time);
            clock.setAlignment(Pos.CENTER_RIGHT);
            getChildren().addAll(face, who, bars, spacer, icon("icon_coins", 26), gold, dayIcon, clock);
            setPadding(new Insets(8, 20, 8, 14));
        }

        private static HBox row(String icon, ProgressBar bar, Label text) {
            HBox r = new HBox(6, icon(icon, 16), bar, text);
            r.setAlignment(Pos.CENTER_LEFT);
            return r;
        }

        static ImageView icon(String name, double size) {
            ImageView iv = new ImageView(Images.drawable(name));
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true);
            return iv;
        }

        void refresh() {
            try {
                Character c = App.Shell == null ? null : App.Shell.character;
                if (c == null) return;
                face.setImage(Images.asset("characters", c.imgFile));
                name.setText(c.getName());
                cls.setText(c.getCharClassName() + "  |  " + Game.t("Level") + " " + c.level);
                int maxHp = Math.max(1, c.attributesManager.getMaxHp()), maxMp = Math.max(1, c.attributesManager.getMaxMp());
                float curHp = c.attributesManager.getCurrent(Enums.AttributeName.HP), curMp = c.attributesManager.getCurrent(Enums.AttributeName.MP);
                hp.setProgress(curHp / maxHp);
                mp.setProgress(curMp / maxMp);
                hpText.setText(Math.round(curHp) + "/" + maxHp);
                mpText.setText(Math.round(curMp) + "/" + maxMp);
                int lo = com.cnx.endlesstalestwo.enums.ExperienceTable.get(c.level), hi = com.cnx.endlesstalestwo.enums.ExperienceTable.get(c.level + 1);
                xp.setProgress(hi > lo ? (double) (c.experience - lo) / (hi - lo) : 1);
                gold.setText(String.valueOf(c.gold));
                day.setText(Game.t("Day") + " " + App.Shell.flowManager.days);
                time.setText(App.Shell.flowManager.getHoursForDisplay());
                dayIcon.setImage(Images.drawable(App.Shell.flowManager.isDayLight ? "icon_daytime" : "icon_nighttime"));
            } catch (Throwable t) {
                Log.error("HUD refresh failed", t);
            }
        }
    }

    /** Small helper used by panels: "icon  label  value" row. */
    public static HBox statRow(String icon, String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox r = new HBox(8);
        if (icon != null) r.getChildren().add(Hud.icon(icon, 20));
        r.getChildren().addAll(l, sp, v);
        r.setAlignment(Pos.CENTER_LEFT);
        r.getProperties().put(Nav.ITEM, new Nav.Item(label + ": " + value, r));
        return r;
    }

    /** A section heading inside a panel; Page Up / Page Down jump between them. */
    public static Label section(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("section", "panel-subtitle");
        return l;
    }

    public static List<Choice> list() { return new ArrayList<>(); }
}

package com.cnx.desktop.game.screens;

import com.cnx.desktop.game.*;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.GameEngine;
import com.cnx.endlesstalestwo.data.locations.LocationsIds;
import com.cnx.endlesstalestwo.data.regions.RegionsIds;
import com.cnx.endlesstalestwo.entities.*;
import com.cnx.endlesstalestwo.libs.LibBestiary;
import com.cnx.endlesstalestwo.libs.LibInventory;
import com.cnx.endlesstalestwo.managers.DataManager;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Region maps (port of ViewMap). Shows the original map picture with your pin, and — so it is
 * useful without sight — a text map: every place in the region, where you are, and the roads from
 * here with direction and travel time. Also the bestiary (creatures here and in the region).
 */
public final class MapScreen implements Screens.Screen {
    private final List<RegionsIds> regions = new ArrayList<>();
    private int index;

    @Override
    public void show() {
        regions.clear();
        for (Region r : DataManager.REGIONS) regions.add(r.getUId());
        Location here = App.Shell.flowManager.currentLocation;
        index = here == null ? 0 : Math.max(0, regions.indexOf(here.regionId));
        Ui.get().clearScene();
        Ui.get().showHud(true);
        Ui.get().dimBackground(true);
        render(true);
    }

    @Override public void refresh() { render(false); }

    private Region region() { return App.DataManager.getRegion(regions.get(index)); }

    private boolean hasMap(Region r) { return r != null && r.mapItemUid != null && LibInventory.checkHasItemAmount(r.mapItemUid, 1, App.getPlayerChar()); }

    private void render(boolean speak) {
        Region r = region();
        Location here = App.Shell.flowManager.currentLocation;
        boolean mine = hasMap(r);
        boolean inRegion = here != null && here.regionId == regions.get(index);
        String name = mine ? r.getName(App.CURRENT_LANGUAGE) : Tr.t("Unknown region");

        // the picture, with your pin when you are in this region (ViewMap.setCurrentMap)
        ImageView pic = new ImageView();
        if (mine) {
            String base = r.getMapImageName();
            javafx.scene.image.Image img = null;
            if (inRegion) {
                LocationsIds pinId = here instanceof Place p && p.localeId != null ? p.localeId : here.getUId();
                img = Images.asset("maps", base + "_pin_" + pinId.name().toLowerCase());
            }
            if (img == null) img = Images.asset("maps", base);
            pic.setImage(img);
        }
        pic.setFitHeight(470);
        pic.setFitWidth(820);
        pic.setPreserveRatio(true);
        Ui.get().setSceneNode(pic);

        StringBuilder text = new StringBuilder();
        if (!mine) {
            text.append(Tr.t("You do not have the map of this region."));
        } else {
            if (inRegion) text.append(Tr.t("You are at ")).append(here.getName(App.CURRENT_LANGUAGE)).append(". ");
            List<String> places = new ArrayList<>();
            for (LocationsIds id : r.localesIds) {
                Locale l = App.DataManager.getLocale(id);
                if (l != null) places.add(l.getName(App.CURRENT_LANGUAGE));
            }
            text.append(places.size()).append(Tr.t(" places: ")).append(String.join(", ", places)).append(".");
        }
        if (speak) Ui.get().say(name, Tr.t("Region ") + (index + 1) + Tr.t(" of ") + regions.size(), text.toString(), null);
        else { Ui.get().showTextQuiet(name, Tr.t("Region ") + (index + 1) + Tr.t(" of ") + regions.size(), text.toString()); Speech.say(name + ". " + text, true); }

        List<Ui.Choice> c = Ui.list();
        c.add(new Ui.Choice(Tr.t("Region: ") + name, Tr.t("Left and Right to change region"), Icons.get("icon_map"), true, () -> {}));
        if (mine && inRegion && here instanceof Locale l && l.destinationLocales != null) {
            for (LocationTravelReference ref : l.destinationLocales) {
                Locale dest = App.DataManager.getLocale(ref.toLocationId);
                if (dest == null) continue;
                String dir = ref.direction == null ? "" : ExploreScreen.pretty(ref.direction.name()) + ", ";
                c.add(new Ui.Choice(Tr.t("Road to ") + dest.getName(App.CURRENT_LANGUAGE), dir + ExploreScreen.formatMinutes(GameEngine.calculateTravelTime(ref)),
                        Icons.locationType(dest.type), true, () -> Speech.say(Tr.t("To travel, use Destinations on the main screen."), true)));
            }
        }
        c.add(new Ui.Choice(Tr.t("Bestiary"), Tr.t("creatures of this region"), Icons.get("icon_battle_begin"), mine || inRegion, this::bestiary));
        c.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), Screens::backToGameplay));
        Ui.get().queueMenuAfterSpeech();
        Ui.get().setChoices(Tr.t("Map"), c, Screens::backToGameplay);
        Ui.get().nav.baseItems().get(0).onAdjust(d -> { index = (index + d + regions.size()) % regions.size(); render(false); });
    }

    /** ViewMap.loadBestiary() */
    private void bestiary() {
        Region r = region();
        Location here = App.Shell.flowManager.currentLocation;
        boolean inRegion = here != null && here.regionId == regions.get(index);
        List<Enemy> local = inRegion ? LibBestiary.getLocalCreatures(here) : new ArrayList<>();
        List<Enemy> regional = hasMap(r) ? LibBestiary.getRegionCreaturesFiltered(r, local) : new ArrayList<>();
        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(6);
        if (inRegion) body.getChildren().add(Ui.section(Tr.t("Here: ") + here.getName(App.CURRENT_LANGUAGE)));
        for (Enemy e : local) body.getChildren().add(Ui.para(e.getName(App.CURRENT_LANGUAGE)));
        if (hasMap(r)) body.getChildren().add(Ui.section(Tr.t("Elsewhere in ") + r.getName(App.CURRENT_LANGUAGE)));
        for (Enemy e : regional) body.getChildren().add(Ui.para(e.getName(App.CURRENT_LANGUAGE)));
        if (local.isEmpty() && regional.isEmpty()) body.getChildren().add(Ui.para(Tr.t("No known creatures.")));
        final Runnable[] close = new Runnable[1];
        close[0] = Ui.get().showModal(Ui.panel(Tr.t("Bestiary"), body, 560, Ui.button(Tr.t("Close"), () -> close[0].run())), true, null);
    }
}

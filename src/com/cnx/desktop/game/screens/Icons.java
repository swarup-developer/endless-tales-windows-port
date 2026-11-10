package com.cnx.desktop.game.screens;

import com.cnx.desktop.game.Images;
import com.cnx.endlesstalestwo.enums.Enums;
import javafx.scene.image.Image;

/** Menu icons taken from the original game's drawables. */
public final class Icons {
    private Icons() {}

    public static Image get(String name) { return Images.drawable(name); }

    public static Image talk() {
        for (String n : new String[]{"icon_talk", "icon_conversation", "icon_persons", "icon_npc", "icon_horn"}) {
            Image i = Images.drawable(n);
            if (i != null) return i;
        }
        return null;
    }

    public static Image locationType(Enums.LocationTypes t) {
        if (t == null) return get("icon_place");
        Image i = Images.drawable("icon_location_type_" + t.name().toLowerCase());
        return i != null ? i : get("icon_place");
    }
}

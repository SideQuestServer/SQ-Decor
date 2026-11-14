package com.sqdecor.lamp;

import java.util.Locale;

import net.minecraft.network.chat.Component;

public enum LampPart {
    BULB("bulb", 0),
    CASE("case", 1),
    CASE2("case2", 2),
    FIXED("fixed", -1);

    public static final LampPart[] PAINTABLE = {BULB, CASE, CASE2};

    public final String id;
    public final int tintIndex;

    LampPart(String id, int tintIndex) {
        this.id = id;
        this.tintIndex = tintIndex;
    }

    public static LampPart of(String name) {
        String[] words = name.toLowerCase(Locale.ROOT).strip().split("[\\s_.-]+");
        String first = words[0];
        if (first.startsWith("bulb")) return BULB;
        if (first.equals("case2")) return CASE2;
        if (first.equals("case") && words.length >= 3 && words[1].equals("2")) return CASE2;
        if (first.startsWith("case")) return CASE;
        return FIXED;
    }

    public static LampPart byTintIndex(int tintIndex) {
        for (LampPart part : PAINTABLE) {
            if (part.tintIndex == tintIndex) return part;
        }
        return FIXED;
    }

    public Component displayName() {
        return Component.translatable("part.sqdecor." + id);
    }
}

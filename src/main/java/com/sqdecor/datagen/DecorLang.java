package com.sqdecor.datagen;

import java.util.function.BiConsumer;

public final class DecorLang {
    private DecorLang() {}

    public static void english(BiConsumer<String, String> add) {
        add.accept("block.sqdecor.emergency_stop", "Emergency Stop Button");
        add.accept("block.sqdecor.frame_grid", "Tiled Frame Panel");
        add.accept("block.sqdecor.frame_inset", "Inset Frame Panel");
        add.accept("block.sqdecor.frame_panel", "Framed Panel");
        add.accept("block.sqdecor.frame_riveted", "Riveted Frame Panel");
        add.accept("block.sqdecor.fuse_box", "Fuse Box");
        add.accept("block.sqdecor.grating_walkway", "Grating Walkway");
        add.accept("block.sqdecor.industrial_lever", "Industrial Lever");
        add.accept("block.sqdecor.rotary_switch", "Rotary Switch");
        add.accept("block.sqdecor.switch_cabinet", "Switch Cabinet");
        add.accept("compendium.sqdecor.frames.text", "Blocks with a frame that joins up: put the same panel next to each other and the frame only runs around the outside, so many blocks look like one big plate. Works on walls, floors and ceilings.\n\n- Four looks: plain frame, riveted, inset and tiles.\n- Dye colors the block; differently colored panels still join.\n- 8 iron ingots around an iron nugget, iron ingot, smooth stone or stone bricks: 8 panels.");
        add.accept("compendium.sqdecor.frames.title", "Frame Panels");
        add.accept("compendium.sqdecor.industrial_controls.text", "Industrial switches that give redstone like a lever, and cabinets with a door.\n\n- Industrial lever and rotary switch: right-click switches on and off.\n- Emergency stop: a short redstone pulse like a button.\n- Switch cabinet (standing) and fuse box (on the wall): right-click opens the door, inside are rows of breakers.\n- Dye colors the housing (not the emergency stop).");
        add.accept("compendium.sqdecor.industrial_controls.title", "Switches and Cabinets");
        add.accept("compendium.sqdecor.walkway.text", "A thin grating to walk on. Placed on top of a block it lies at the bottom, placed under a ceiling or on the top half of a side it sits flush at the top.\n\n- Open edges get a railing on their own; next to another walkway or a wall it goes away.\n- Shift + right-click with an empty hand near an edge turns the railing there on or off.\n- Dye colors it.\n- 2 iron ingots over 3 iron bars: 6 walkways.");
        add.accept("compendium.sqdecor.walkway.title", "Grating Walkway");
        add.accept("compendium.sqdecor.workshop.text", "Furniture for workshops and offices: workbench with vise, tool wall, tool cabinet, desk, office chair and filing cabinet. They face you when placed.\n\n- Dye colors the parts one after another, like the other decor.\n- Made in the stonecutter from diamond plate.");
        add.accept("compendium.sqdecor.workshop.title", "Workshop and Office");
        add.accept("item.sqdecor.header_building", "Frames & Walkway");
        add.accept("item.sqdecor.header_cables", "Cables & Caution Tape");
        add.accept("item.sqdecor.header_ceiling", "Panels, Ceiling & Wall Lights");
        add.accept("item.sqdecor.header_controls", "Switches & Control Panels");
        add.accept("item.sqdecor.header_industrial", "Industrial Blocks");
        add.accept("item.sqdecor.header_leds", "LEDs & Light Strips");
        add.accept("item.sqdecor.header_misc", "Other");
        add.accept("item.sqdecor.header_props", "Props, Furniture & Barriers");
        add.accept("item.sqdecor.header_screens", "Screens & Marquees");
        add.accept("item.sqdecor.header_signs", "Signs & Signals");
        add.accept("item.sqdecor.header_spots", "Spotlights & Work Lights");
        add.accept("item.sqdecor.header_style_blocks", "Fences");
        add.accept("item.sqdecor.header_style_sets", "Style Sets");
        add.accept("item.sqdecor.header_tools", "Tools");
        add.accept("item.sqdecor.header_warning", "Warning & Beacon Lights");
        add.accept("item.sqdecor.tab_spacer", " ");
    }

    public static void german(BiConsumer<String, String> add) {
        add.accept("block.sqdecor.emergency_stop", "Not-Aus-Taster");
        add.accept("block.sqdecor.frame_grid", "Rahmenfliesen");
        add.accept("block.sqdecor.frame_inset", "Kassettenplatte");
        add.accept("block.sqdecor.frame_panel", "Rahmenplatte");
        add.accept("block.sqdecor.frame_riveted", "Genietete Rahmenplatte");
        add.accept("block.sqdecor.fuse_box", "Sicherungskasten");
        add.accept("block.sqdecor.grating_walkway", "Gitterrost-Laufsteg");
        add.accept("block.sqdecor.industrial_lever", "Industrie-Hebel");
        add.accept("block.sqdecor.rotary_switch", "Drehschalter");
        add.accept("block.sqdecor.switch_cabinet", "Schaltschrank");
        add.accept("compendium.sqdecor.frames.text", "Blöcke mit einem Rahmen, der sich verbindet: gleiche Platten nebeneinander haben nur noch außen herum einen Rahmen, viele Blöcke sehen aus wie eine große Platte. Geht an Wand, Boden und Decke.\n\n- Vier Arten: einfacher Rahmen, genietet, Kassette und Fliesen.\n- Farbstoff färbt den Block, verschieden gefärbte Platten verbinden sich trotzdem.\n- 8 Eisenbarren um einen Eisenklumpen, Eisenbarren, glatten Stein oder Steinziegel: 8 Platten.");
        add.accept("compendium.sqdecor.frames.title", "Rahmenplatten");
        add.accept("compendium.sqdecor.industrial_controls.text", "Industrie-Schalter, die Redstone geben wie ein Hebel, und Schränke mit Tür.\n\n- Industrie-Hebel und Drehschalter: Rechtsklick schaltet an und aus.\n- Not-Aus: ein kurzer Redstone-Impuls wie ein Knopf.\n- Schaltschrank (stehend) und Sicherungskasten (an der Wand): Rechtsklick öffnet die Tür, innen sind Reihen mit Sicherungen.\n- Farbstoff färbt das Gehäuse (nicht beim Not-Aus).");
        add.accept("compendium.sqdecor.industrial_controls.title", "Schalter und Schränke");
        add.accept("compendium.sqdecor.walkway.text", "Ein dünner Gitterrost zum Drüberlaufen. Oben auf einen Block gesetzt liegt er unten im Block, unter die Decke oder an die obere Hälfte einer Seite gesetzt sitzt er oben bündig.\n\n- Offene Kanten bekommen von selbst ein Geländer; neben einem anderen Laufsteg oder einer Wand verschwindet es.\n- Shift + Rechtsklick mit leerer Hand nahe einer Kante schaltet das Geländer dort an oder aus.\n- Farbstoff färbt ihn.\n- 2 Eisenbarren über 3 Eisengittern: 6 Laufstege.");
        add.accept("compendium.sqdecor.walkway.title", "Gitterrost-Laufsteg");
        add.accept("compendium.sqdecor.workshop.text", "Möbel für Werkstatt und Büro: Werkbank mit Schraubstock, Werkzeugwand, Werkzeugwagen, Schreibtisch, Bürostuhl und Aktenschrank. Beim Setzen zeigen sie zu dir.\n\n- Farbstoff färbt die Teile nacheinander, wie bei der anderen Deko.\n- Im Steinschneider aus Riffelblech.");
        add.accept("compendium.sqdecor.workshop.title", "Werkstatt und Büro");
        add.accept("item.sqdecor.header_building", "Rahmen & Laufsteg");
        add.accept("item.sqdecor.header_cables", "Kabel & Flatterband");
        add.accept("item.sqdecor.header_ceiling", "Panels, Decken- & Wandleuchten");
        add.accept("item.sqdecor.header_controls", "Schalter & Bedienfelder");
        add.accept("item.sqdecor.header_industrial", "Industrie-Blöcke");
        add.accept("item.sqdecor.header_leds", "LEDs & Leuchtbänder");
        add.accept("item.sqdecor.header_misc", "Sonstiges");
        add.accept("item.sqdecor.header_props", "Deko, Möbel & Absperrung");
        add.accept("item.sqdecor.header_screens", "Bildschirme & Laufschriften");
        add.accept("item.sqdecor.header_signs", "Schilder & Signale");
        add.accept("item.sqdecor.header_spots", "Strahler & Arbeitslicht");
        add.accept("item.sqdecor.header_style_blocks", "Zäune");
        add.accept("item.sqdecor.header_style_sets", "Stil-Sets");
        add.accept("item.sqdecor.header_tools", "Werkzeuge");
        add.accept("item.sqdecor.header_warning", "Warn- & Rundumleuchten");
        add.accept("item.sqdecor.tab_spacer", " ");
    }

}

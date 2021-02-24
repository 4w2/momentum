package me.linus.momentum.util.render;

import me.linus.momentum.Momentum;
import me.linus.momentum.managers.ModuleManager;
import me.linus.momentum.mixin.MixinInterface;
import me.linus.momentum.module.modules.client.ClientFont;
import me.linus.momentum.util.client.font.FontRender;
import me.linus.momentum.util.world.Timer;

import java.awt.*;
import java.io.InputStream;

/**
 * @author linustouchtips
 * @since 11/29/2020
 */

public class FontUtil implements MixinInterface {

    public static FontRender verdana = null;
    public static FontRender lato = null;
    public static FontRender ubuntu = null;
    public static FontRender comfortaa = null;
    public static FontRender comicsans = null;

    public static Timer cursorTimer = new Timer();
    public static boolean blink = false;

    public void load() {
        try {
            this.lato = new FontRender(FontUtil.getFont("Lato.ttf", (float) ClientFont.scale.getValue()));
            this.comfortaa = new FontRender(FontUtil.getFont("comfortaa.ttf", (float) ClientFont.scale.getValue()));
            this.comicsans = new FontRender(FontUtil.getFont("comic-sans.ttf", (float) ClientFont.scale.getValue()));
            this.verdana = new FontRender(FontUtil.getFont("Verdana.ttf", (float) ClientFont.scale.getValue()));
            this.ubuntu = new FontRender(FontUtil.getFont("Ubuntu.ttf", (float) ClientFont.scale.getValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Font getFont(String fontName, float size) {
        try {
            InputStream inputStream = FontUtil.class.getResourceAsStream("/assets/momentum/fonts/" + fontName);
            Font awtClientFont = Font.createFont(0, inputStream);
            awtClientFont = awtClientFont.deriveFont(0, size);
            inputStream.close();

            return awtClientFont;
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("default", 0, (int) size);
        }
    }

    public FontRender getCustomFont() {
        switch (ClientFont.family.getValue()) {
            case 0:
                return this.lato;
            case 1:
                return this.ubuntu;
            case 2:
                return this.verdana;
            case 3:
                return this.comfortaa;
            case 4:
                return this.comicsans;
        }

        return this.lato;
    }

    public static void drawString(String text, float x, float y, int color) {
        if (ModuleManager.getModuleByName("Font").isEnabled()) {
            if (ClientFont.shadow.getValue())
                Momentum.fontManager.getCustomFont().drawStringWithShadow(text, x, y, color);
            else
                Momentum.fontManager.getCustomFont().drawString(text, (int) x, (int) y, color);
        }

        else {
            if (ClientFont.shadow.getValue())
                mc.fontRenderer.drawStringWithShadow(text, x, y, color);
            else
                mc.fontRenderer.drawString(text, (int) x, (int) y, color);
        }
    }

    public static int getString(String text, float x, float y, int color) {
        if (ModuleManager.getModuleByName("Font").isEnabled()) {
            if (ClientFont.shadow.getValue())
                return Momentum.fontManager.getCustomFont().drawStringWithShadow(text, x, y, color);
            else
                return Momentum.fontManager.getCustomFont().drawString(text, (int) x, (int) y, color);
        }

        else {
            if (ClientFont.shadow.getValue())
                return mc.fontRenderer.drawStringWithShadow(text, x, y, color);
            else
                return mc.fontRenderer.drawString(text, (int) x, (int) y, color);
        }
    }

    public static float getStringWidth(String text) {
        if (ModuleManager.getModuleByName("Font").isEnabled())
            return Momentum.fontManager.getCustomFont().getStringWidth(text);
        else
            return mc.fontRenderer.getStringWidth(text) + 4;
    }

    public static float getFontHeight() {
        if (ModuleManager.getModuleByName("Font").isEnabled())
            return Momentum.fontManager.getCustomFont().FONT_HEIGHT;
        else
            return mc.fontRenderer.FONT_HEIGHT;
    }

    public static String insertionPoint() {
        if (cursorTimer.passed(500, Timer.Format.System)) {
            cursorTimer.reset();
            blink = !blink;
        }

        if (blink)
            return ModuleManager.getModuleByName("Font").isEnabled() ? "|" : "｜";
        else
            return "";
    }
}
package me.linus.momentum.gui.hud.components;

import me.linus.momentum.gui.hud.HUDComponent;
import me.linus.momentum.gui.theme.ThemeColor;
import me.linus.momentum.util.client.ColorUtil;
import me.linus.momentum.util.client.friend.FriendManager;
import me.linus.momentum.util.combat.EnemyUtil;
import me.linus.momentum.util.render.FontUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.text.TextFormatting;

public class TextRadar extends HUDComponent {
    public TextRadar() {
        super("TextRadar", 2, 80);
    }

    int count = 0;
    int screenWidth = new ScaledResolution(mc).getScaledWidth();

    @Override
    public void renderComponent() {
        mc.world.playerEntities.forEach(entityPlayer -> {
            int screenWidthScaled = new ScaledResolution(mc).getScaledWidth();
            float modWidth = FontUtil.getStringWidth(ColorUtil.getHealthText(EnemyUtil.getHealth(entityPlayer)) + String.valueOf(EnemyUtil.getHealth(entityPlayer)) + (FriendManager.isFriend(entityPlayer.getName()) ? TextFormatting.AQUA : TextFormatting.RESET) + entityPlayer.getName() + TextFormatting.WHITE + mc.player.getDistance(entityPlayer));
            String modText = ColorUtil.getHealthText(EnemyUtil.getHealth(entityPlayer)) + String.valueOf(EnemyUtil.getHealth(entityPlayer)) + (FriendManager.isFriend(entityPlayer.getName()) ? TextFormatting.AQUA : TextFormatting.RESET) + entityPlayer.getName() + TextFormatting.WHITE + mc.player.getDistance(entityPlayer);

            if (this.x < (screenWidthScaled / 2))
                FontUtil.drawString(modText, this.x - 2, this.y + (10 * count), ThemeColor.BRIGHT);
            else
                FontUtil.drawString(modText, this.x, this.y + (10 * count), ThemeColor.BRIGHT);

            count++;

            if (this.x < (screenWidth / 2))
                width = (int) (modWidth + 5);
            else
                width = (int) (modWidth - 5);

        });

        height = ((mc.fontRenderer.FONT_HEIGHT + 1) * count);
    }
}
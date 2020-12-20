package me.linus.momentum.module.modules.render;

import me.linus.momentum.event.events.render.Render3DEvent;
import me.linus.momentum.module.Module;
import me.linus.momentum.setting.checkbox.Checkbox;
import me.linus.momentum.setting.slider.Slider;
import me.linus.momentum.setting.slider.SubSlider;
import me.linus.momentum.util.render.RenderUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * @author linustouchtips
 * @since 11/30/2020
 */

public class BurrowESP extends Module {
    public BurrowESP() {
        super("BurrowESP", Category.RENDER, "Highlights players that are burrowed");
    }

    public static Checkbox color = new Checkbox("Color", true);
    public static SubSlider r = new SubSlider(color, "Red", 0.0D, 255.0D, 255.0D, 0);
    public static SubSlider g = new SubSlider(color, "Green", 0.0D, 0.0D, 255.0D, 0);
    public static SubSlider b = new SubSlider(color, "Blue", 0.0D, 0.0D, 255.0D, 0);
    public static SubSlider a = new SubSlider(color, "Alpha", 0.0D, 30.0D, 255.0D, 0);

    public static Slider range = new Slider("Range", 0.0D, 7.0D, 10.0D, 0);

    @Override
    public void setup() {
        addSetting(color);
        addSetting(range);
    }

    private final List<BlockPos> burrowList = new ArrayList<>();

    @Override
    public void onUpdate() {
        burrowList.clear();

        for (EntityPlayer player : mc.world.playerEntities) {
            BlockPos blockPos = new BlockPos(player.posX, player.posY, player.posZ);
            
            if (mc.world.getBlockState(blockPos).getBlock().equals(Blocks.OBSIDIAN) && mc.player.getDistanceSq(blockPos) <= range.getValue()) 
                burrowList.add(blockPos);
        }
    }

    @Override
    public void onRender3D(Render3DEvent eventRender) {
        for (BlockPos burrowPos : burrowList) {
            RenderUtil.drawVanillaBoxFromBlockPos(burrowPos, (float) r.getValue() / 255f, (float) g.getValue() / 255f, (float) b.getValue() / 255f, (float) a.getValue() / 255f);
        }
    }
}

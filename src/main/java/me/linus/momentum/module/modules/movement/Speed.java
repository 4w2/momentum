package me.linus.momentum.module.modules.movement;

import me.linus.momentum.event.events.packet.PacketReceiveEvent;
import me.linus.momentum.event.events.player.MoveEvent;
import me.linus.momentum.module.Module;
import me.linus.momentum.setting.checkbox.Checkbox;
import me.linus.momentum.setting.checkbox.SubCheckbox;
import me.linus.momentum.setting.mode.Mode;
import me.linus.momentum.setting.slider.Slider;
import me.linus.momentum.setting.slider.SubSlider;
import me.linus.momentum.util.client.MathUtil;
import me.linus.momentum.util.client.Timer;
import me.linus.momentum.util.world.PlayerUtil;
import net.minecraft.init.MobEffects;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class Speed extends Module {
    public Speed() {
        super("Speed", Category.MOVEMENT, "Allows you to go faster");
    }

    private static Mode mode = new Mode("Mode", "SmoothHop");
    private static SubCheckbox strict = new SubCheckbox(mode, "Strict", false);

    public static Slider multiplier = new Slider("Multiplier", 0.0D, 0.03D, 0.3D, 3);

    private static Checkbox useTimer = new Checkbox("Use Timer", false);
    public static SubSlider timerTicks = new SubSlider(useTimer, "Timer Speed", 0.0D, 1.12D, 2.0D, 2);

    public static Slider speed = new Slider("Speed", 0.0D, 0.21D, 1.0D, 2);

    private static Checkbox jump = new Checkbox("Jump", true);

    private static Checkbox disable = new Checkbox("Disable", true);
    private static SubCheckbox inWater = new SubCheckbox(disable, "Disable in Liquid", true);
    private static SubCheckbox rubberband = new SubCheckbox(disable, "Disable on Rubberband", false);

    @Override
    public void setup() {
        addSetting(mode);
        addSetting(multiplier);
        addSetting(useTimer);
        addSetting(speed);
        addSetting(jump);
        addSetting(disable);
    }

    private double moveSpeed = 0.0;
    private int level = 4;
    private double lastDist = 0.0;
    private int timerDelay;

    @Override
    public void onUpdate() {
        if (nullCheck())
            return;

        if ((mc.player.isInWater() || mc.player.isInLava() && inWater.getValue())) {
            disable();
            return;
        }

        double xDist = mc.player.posX - mc.player.prevPosX;
        double zDist = mc.player.posZ - mc.player.prevPosZ;
        lastDist = Math.sqrt(xDist * xDist + zDist * zDist);

        if (jump.getValue() && mc.player.onGround && PlayerUtil.isMoving())
            mc.player.jump();
    }

    @Override
    public void onDisable() {
        mc.timer.tickLength = 50;
    }

    @SubscribeEvent
    public void onPacketRecieve(PacketReceiveEvent event) {
        if (event.getPacket() instanceof SPacketPlayerPosLook) {
            if (rubberband.getValue()) {
                this.disable();
                return;
            }

            this.moveSpeed = 0.2873;
            this.lastDist = 0.0;
            this.moveSpeed = 0.0;
            this.level = 4;
            this.timerDelay = 0;
        }
    }

    @SubscribeEvent
    public void onMove(MoveEvent event) {
        timerDelay++;
        timerDelay %= 5;

        if (timerDelay != 0) {
            mc.timer.tickLength = 50;
        } else if (PlayerUtil.hasMotion()) {
            if (useTimer.getValue())
                mc.timer.tickLength = (float) (50 / timerTicks.getValue());

            mc.player.motionX *= 1.02f;
            mc.player.motionZ *= 1.02f;
        }

        if (mc.player.onGround && PlayerUtil.hasMotion()) {
            level = 2;
        }

        if (MathUtil.round(mc.player.posY - (double)((int)mc.player.posY)) == MathUtil.round(0.138)) {
            mc.player.motionY -= 0.08;
            event.setY(event.getY() - 0.09316090325960147);
            mc.player.posY -= 0.09316090325960147;
        }

        if (level == 1 && (mc.player.moveForward != 0.0f || mc.player.moveStrafing != 0.0f)) {
            level = 2;
            moveSpeed = 1.38 * speed.getValue() - 0.01;
        } else if (level == 2) {
            level = 3;

            if (jump.getValue()) {
                mc.player.motionY = 0.3994f;
                event.setY(0.3994f);
            }

            moveSpeed *= 2.149;
        } else if (level == 3) {
            level = 4;
            double difference = 0.66 * (lastDist - speed.getValue());
            moveSpeed = lastDist - difference;
        } else {
            if (mc.world.getCollisionBoxes(mc.player, mc.player.getEntityBoundingBox().offset(0.0, mc.player.motionY, 0.0)).size() > 0 || mc.player.collidedVertically) {
                level = 1;
            }
            moveSpeed = lastDist - lastDist / 159.0;
        }

        moveSpeed = Math.max(moveSpeed, speed.getValue());
        float forward = mc.player.movementInput.moveForward;
        float strafe = mc.player.movementInput.moveStrafe;
        float yaw = mc.player.rotationYaw;

        if (forward == 0.0f && strafe == 0.0f) {
            event.setX(0.0);
            event.setZ(0.0);
        } else if (forward != 0.0f) {
            if (strafe >= 1.0f) {
                yaw += (float)(forward > 0.0f ? -45 : 45);
                strafe = 0.0f;
            } else if (strafe <= -1.0f) {
                yaw += (float)(forward > 0.0f ? 45 : -45);
                strafe = 0.0f;
            }

            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }

        double mx = Math.cos(Math.toRadians(yaw + 90.0f));
        double mz = Math.sin(Math.toRadians(yaw + 90.0f));
        event.setX((double)forward * moveSpeed * mx + (double)strafe * moveSpeed * mz);
        event.setZ((double)forward * moveSpeed * mz - (double)strafe * moveSpeed * mx);
        mc.player.stepHeight = 0.6f;

        if (forward == 0.0f && strafe == 0.0f) {
            event.setX(0.0);
            event.setZ(0.0);
        }
    }

    @Override
    public String getHUDData() {
        return mode.getMode(mode.getValue());
    }
}



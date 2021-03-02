package me.linus.momentum.module.modules.combat;

import me.linus.momentum.event.events.packet.PacketReceiveEvent;
import me.linus.momentum.event.events.packet.PacketSendEvent;
import me.linus.momentum.managers.CrystalManager;
import me.linus.momentum.managers.ModuleManager;
import me.linus.momentum.managers.RotationManager;
import me.linus.momentum.managers.notification.Notification;
import me.linus.momentum.managers.notification.Notification.Type;
import me.linus.momentum.managers.notification.NotificationManager;
import me.linus.momentum.module.Module;
import me.linus.momentum.setting.checkbox.Checkbox;
import me.linus.momentum.setting.checkbox.SubCheckbox;
import me.linus.momentum.setting.color.ColorPicker;
import me.linus.momentum.setting.keybind.SubKeybind;
import me.linus.momentum.setting.mode.SubMode;
import me.linus.momentum.setting.slider.SubSlider;
import me.linus.momentum.util.client.MathUtil;
import me.linus.momentum.util.combat.EnemyUtil;
import me.linus.momentum.util.combat.crystal.Crystal;
import me.linus.momentum.util.combat.crystal.CrystalPosition;
import me.linus.momentum.util.combat.crystal.CrystalUtil;
import me.linus.momentum.util.player.InventoryUtil;
import me.linus.momentum.util.player.PlayerUtil;
import me.linus.momentum.util.player.rotation.Rotation;
import me.linus.momentum.util.player.rotation.RotationPriority;
import me.linus.momentum.util.player.rotation.RotationUtil;
import me.linus.momentum.util.render.RenderUtil;
import me.linus.momentum.util.render.builder.RenderBuilder.RenderMode;
import me.linus.momentum.util.world.HoleUtil;
import me.linus.momentum.util.world.RaytraceUtil;
import me.linus.momentum.util.world.Timer;
import me.linus.momentum.util.world.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author linustouchtips
 * @since 11/24/2020
 */

public class AutoCrystal extends Module {
    public AutoCrystal() {
        super("AutoCrystal", Category.COMBAT, "Automatically places and explodes crystals");
    }

    public static Checkbox explode = new Checkbox("Break", true);
    public static SubMode breakMode = new SubMode(explode, "Mode", "All", "Smart");
    public static SubMode breakHand = new SubMode(explode, "BreakHand", "OffHand", "MainHand", "Both", "MultiSwing");
    public static SubSlider breakRange = new SubSlider(explode, "Break Range", 0.0D, 5.0D, 7.0D, 1);
    public static SubSlider breakDelay = new SubSlider(explode, "Break Delay", 0.0D, 80.0D, 200.0D, 0);
    public static SubSlider minBreakDamage = new SubSlider(explode, "Break Damage", 0.0D, 4.0D, 36.0D, 0);
    public static SubSlider breakAttempts = new SubSlider(explode, "Break Attempts", 0.0D, 1.0D, 5.0D, 0);
    public static SubMode sync = new SubMode(explode, "Sync", "None", "Instant", "Attack", "Sound", "Unsafe");
    public static SubCheckbox packetBreak = new SubCheckbox(explode, "Packet Break", true);
    public static SubCheckbox walls = new SubCheckbox(explode, "Through Walls", true);
    public static SubCheckbox inhibit = new SubCheckbox(explode, "Inhibit", true);
    public static SubCheckbox antiWeakness = new SubCheckbox(explode, "Anti-Weakness", false);

    public static Checkbox place = new Checkbox("Place", true);
    public static SubSlider placeRange = new SubSlider(place, "Place Range", 0.0D, 5.0D, 7.0D, 1);
    public static SubSlider enemyRange = new SubSlider(place, "Enemy Range", 0.0D, 10.0D, 15.0D, 1);
    public static SubSlider wallRange = new SubSlider(place, "Walls Range", 0.0D, 5.0D, 7.0D, 1);
    public static SubSlider placeDelay = new SubSlider(place, "Place Delay", 0.0D, 0.0D, 500.0D, 0);
    public static SubSlider minDamage = new SubSlider(place, "Minimum Damage", 0.0D, 7.0D, 36.0D, 0);
    public static SubSlider maxLocalDamage = new SubSlider(place, "Maximum Local Damage", 0.0D, 8.0D, 36.0D, 0);
    public static SubSlider threshold = new SubSlider(place, "Threshold", 0.0D, 5.0D, 10.0D, 0);
    public static SubMode autoSwitch = new SubMode(place, "Switch", "None", "Normal", "Packet");
    public static SubMode rayTrace = new SubMode(place, "Ray-Trace", "Normal", "Quill-Trace", "None");
    public static SubCheckbox packetPlace = new SubCheckbox(place, "Packet Place", true);
    public static SubCheckbox boundary = new SubCheckbox(place, "Boundaries", true);
    public static SubCheckbox prediction = new SubCheckbox(place, "Prediction", true);
    public static SubCheckbox multiPlace = new SubCheckbox(place, "MultiPlace", false);

    public static Checkbox rotate = new Checkbox("Rotate", true);
    public static SubMode rotateDuring = new SubMode(rotate, "When", "Break", "Place", "Both");
    public static SubMode rotateMode = new SubMode(rotate, "Type", "Packet", "Legit", "None");
    public static SubSlider rotateStep = new SubSlider(rotate, "Rotation Step", 0.0D, 180.0D, 360.0D, 0);
    public static SubCheckbox onlyInViewFrustrum = new SubCheckbox(rotate, "Only In View Frustrum", false);
    public static SubCheckbox rubberband = new SubCheckbox(rotate, "Rubberband Detect", false);
    public static SubCheckbox randomRotate = new SubCheckbox(rotate, "Random Rotations", false);
    public static SubCheckbox strict = new SubCheckbox(rotate, "NCP Strict", false);

    public static Checkbox pause = new Checkbox("Pause", true);
    public static SubMode pauseMode = new SubMode(pause, "Pause Mode", "Place", "Break", "Both");
    public static SubSlider pauseHealth = new SubSlider(pause, "Pause Health", 0.0D, 7.0D, 36.0D, 0);
    public static SubCheckbox friendProtect = new SubCheckbox(pause, "Friend Protect", false);
    public static SubCheckbox whenMining = new SubCheckbox(pause, "When Mining", false);
    public static SubCheckbox whenEating = new SubCheckbox(pause, "When Eating", false);
    public static SubCheckbox closePlacements = new SubCheckbox(pause, "Close Placements", false);

    public static Checkbox facePlace = new Checkbox("FacePlace", true);
    public static SubSlider facePlaceHealth = new SubSlider(facePlace, "FacePlace Health", 0.0D, 16.0D, 36.0D, 0);
    public static SubCheckbox armorBreaker = new SubCheckbox(facePlace, "Armor Breaker", false);
    public static SubSlider armorScale = new SubSlider(facePlace, "Armor Scale", 0.0D, 15.0D, 100.0D, 0);
    public static SubCheckbox facePlaceHole = new SubCheckbox(facePlace, "HoleCampers", false);
    public static SubKeybind forceFaceplace = new SubKeybind(facePlace, "Force FacePlace", Keyboard.KEY_O);

    public static Checkbox calculations = new Checkbox("Calculations", true);
    public static SubMode timing = new SubMode(calculations, "Timing", "Linear", "Synchronous", "Tick");
    public static SubMode tick = new SubMode(calculations, "Tick", "Client", "Server", "Taiwan");
    public static SubMode heuristic = new SubMode(calculations, "Heuristic", "Damage", "MiniMax", "Atomic");
    public static SubSlider offset = new SubSlider(calculations, "Offset", 0.0D, 1.0D, 1.0D, 2);
    public static SubCheckbox serverConfirm = new SubCheckbox(calculations, "Server Confirm", true);
    public static SubCheckbox verifyPlace = new SubCheckbox(calculations, "Verify Placements", false);

    public static Checkbox logic = new Checkbox("Logic", true);
    public static SubMode logicMode = new SubMode(logic, "Crystal Logic", "Break -> Place", "Place -> Break");
    public static SubMode blockCalc = new SubMode(logic, "Block Logic", "Normal", "1.13+");
    public static SubMode enemyLogic = new SubMode(logic, "Enemy Logic", "Closest", "LowestHealth", "LowestArmor");

    public static Checkbox renderCrystal = new Checkbox("Render", true);
    public static ColorPicker colorPicker = new ColorPicker(renderCrystal, "Color Picker", new Color(250, 0, 250, 50));
    public static SubMode renderMode = new SubMode(renderCrystal, "Render Mode", "Fill", "Outline", "Both", "Claw");
    public static SubCheckbox renderDamage = new SubCheckbox(renderCrystal, "Render Damage", true);

    @Override
    public void setup() {
        addSetting(explode);
        addSetting(place);
        addSetting(rotate);
        addSetting(facePlace);
        addSetting(pause);
        addSetting(calculations);
        addSetting(logic);
        addSetting(renderCrystal);
    }

    public static Timer breakTimer = new Timer();
    public static Timer placeTimer = new Timer();
    public static EntityPlayer crystalTarget = null;
    public static Rotation crystalRotation = null;
    public static Crystal crystal = null;
    public static CrystalPosition crystalPosition = new CrystalPosition(BlockPos.ORIGIN, 0, 0);

    @Override
    public void onEnable() {
        if (nullCheck())
            return;

        super.onEnable();
        CrystalManager.resetCount();
        CrystalManager.resetLists();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        CrystalManager.resetCount();
        CrystalManager.resetLists();
    }

    @Override
    public void onUpdate() {
        if (nullCheck()) {
            this.disable();
            return;
        }

        crystalTarget = WorldUtil.getTarget(enemyRange.getValue(), enemyLogic.getValue());

        if (tick.getValue() == 0)
            autoCrystal();
    }

    @Override
    public void onFastUpdate() {
        if (nullCheck())
            return;

        if (tick.getValue() == 2)
            autoCrystal();
    }

    @Override
    public void onServerUpdate() {
        if (nullCheck())
            return;

        if (tick.getValue() == 1)
            autoCrystal();
    }

    public void autoCrystal() {
        switch (logicMode.getValue()) {
            case 0:
                breakCrystal();
                placeCrystal();
                break;
            case 1:
                placeCrystal();
                breakCrystal();
                break;
        }
    }

    public void breakCrystal() {
        if (handlePause() && (pauseMode.getValue() == 1 || pauseMode.getValue() == 2))
            return;

        crystal = new Crystal((EntityEnderCrystal) mc.world.loadedEntityList.stream().filter(CrystalUtil::attackCheck).min(Comparator.comparing(crystal -> mc.player.getDistance(crystal))).orElse(null), 0, 0);
        if (crystal.getCrystal() != null && explode.getValue() && crystalTarget != null) {
            if (crystal.getCrystal().getDistance(mc.player) > breakRange.getValue())
                return;

            if (!RaytraceUtil.raytraceEntity(crystal.getCrystal()) && !walls.getValue())
                return;

            if (antiWeakness.getValue() && mc.player.isPotionActive(MobEffects.WEAKNESS))
                InventoryUtil.switchToSlot(Items.DIAMOND_SWORD);

            if (breakTimer.passed(CrystalManager.skipTick ? 200 : (long) breakDelay.getValue(), Timer.Format.System)) {
                if (rotateDuring.getValue() == 0 || rotateDuring.getValue() == 2)
                    handleRotations();

                for (int i = 0; i < breakAttempts.getValue(); i++) {
                    CrystalUtil.attackCrystal(crystal.getCrystal(), packetBreak.getValue());
                    CrystalUtil.swingArm(breakHand.getValue());
                    CrystalManager.updateSwings();
                }

                if (sync.getValue() == 1)
                    crystal.getCrystal().setDead();

                breakTimer.reset();
            }

            if (!serverConfirm.getValue())
                CrystalManager.placedCrystals.removeIf(removePosition -> removePosition.getCrystalPosition().getDistance((int) crystal.getCrystal().posX, (int) crystal.getCrystal().posY, (int) crystal.getCrystal().posZ) <= 6);
        }
    }

    public void placeCrystal() {
        if (handlePause() && (pauseMode.getValue() == 0 || pauseMode.getValue() == 2))
            return;

        List<CrystalPosition> crystalPositions = new ArrayList<>();
        CrystalPosition tempPosition;

        for (BlockPos calculatedPosition : CrystalUtil.crystalBlocks(mc.player, placeRange.getValue(), prediction.getValue(), !multiPlace.getValue(), blockCalc.getValue())) {
            if (handleRaytrace(calculatedPosition))
                continue;

            if (verifyPlace.getValue() && mc.player.getDistanceSq(calculatedPosition) > Math.pow(breakRange.getValue(), 2))
                continue;

            double calculatedTargetDamage = CrystalUtil.calculateDamage(calculatedPosition.getX() + 0.5, calculatedPosition.getY() + offset.getValue(), calculatedPosition.getZ() + 0.5, crystalTarget);
            double calculatedSelfDamage = mc.player.capabilities.isCreativeMode ? 0 : CrystalUtil.calculateDamage(calculatedPosition.getX() + 0.5, calculatedPosition.getY() + offset.getValue(), calculatedPosition.getZ() + 0.5, mc.player);

            if (calculatedTargetDamage < minDamage.getValue() && handleMinDamage())
                continue;

            if (calculatedSelfDamage > maxLocalDamage.getValue())
                continue;

            crystalPositions.add(new CrystalPosition(calculatedPosition, calculatedTargetDamage, calculatedSelfDamage));
        }

        tempPosition = crystalPositions.stream().max(Comparator.comparing(idealCrystalPosition -> CrystalUtil.getHeuristic(idealCrystalPosition, heuristic.getValue()))).orElse(null);

        if (tempPosition == null) {
            crystalTarget = null;
            crystalPosition = null;
            crystalRotation.restoreRotation();
            return;
        }

        crystalPosition = tempPosition;

        switch (autoSwitch.getValue()) {
            case 1:
                InventoryUtil.switchToSlot(Items.END_CRYSTAL);
                break;
            case 2:
                InventoryUtil.switchToSlotGhost(Items.END_CRYSTAL);
                break;
        }

        if (placeTimer.passed((long) placeDelay.getValue(), Timer.Format.System) && place.getValue() && InventoryUtil.getHeldItem(Items.END_CRYSTAL) && crystalPosition.getCrystalPosition() != BlockPos.ORIGIN) {
            if (rotateDuring.getValue() == 1 || rotateDuring.getValue() == 2)
                handleRotations();

            CrystalUtil.placeCrystal(crystalPosition.getCrystalPosition(), CrystalUtil.getEnumFacing(boundary.getValue(), crystalPosition.getCrystalPosition()), packetPlace.getValue());
            placeTimer.reset();
        }

        if (timing.getValue() == 1) {
            for (Entity crystal : mc.world.loadedEntityList.stream().filter(CrystalUtil::attackCheck).collect(Collectors.toList())) {
                if (crystalPosition.getCrystalPosition().getDistance((int) crystal.posX, (int) crystal.posY, (int) crystal.posZ) < 2) {
                    CrystalUtil.attackCrystal((EntityEnderCrystal) crystal, packetBreak.getValue());
                    CrystalUtil.swingArm(breakHand.getValue());
                    CrystalManager.updateSwings();
                }
            }
        }
    }

    public boolean handlePause() {
        if (ModuleManager.getModuleByName("Surround").isEnabled() && !Surround.hasPlaced || ModuleManager.getModuleByName("AutoTrap").isEnabled() && !AutoTrap.hasPlaced || ModuleManager.getModuleByName("SelfTrap").isEnabled() && !SelfTrap.hasPlaced || ModuleManager.getModuleByName("HoleFill").isEnabled() && HoleFill.processing)
            return true;

        if (crystalTarget == null)
            return true;

        if (friendProtect.getValue()) {
            for (EntityPlayer friend : Objects.requireNonNull(WorldUtil.getNearbyFriends(placeRange.getValue()))) {
                if (EnemyUtil.getHealth(friend) - (CrystalUtil.calculateDamage(crystal.crystal.posX + 0.5, crystal.crystal.posY + 1, crystal.crystal.posZ + 0.5, friend)) <= pauseHealth.getValue())
                    return true;
            }
        }

        if (PlayerUtil.getHealth() < pauseHealth.getValue())
            return true;
        else if (PlayerUtil.isEating() && whenEating.getValue() || PlayerUtil.isMining() && whenMining.getValue())
            return true;
        else if (closePlacements.getValue() && mc.player.getDistance(crystal.crystal) < 1.5)
            return true;
        else if (CrystalManager.swings > 50 && inhibit.getValue()) {
            CrystalManager.updateTicks(true);

            NotificationManager.addNotification(new Notification("AutoCrystal Frozen! Pausing for 1 tick!", Notification.Type.Warning));

            crystal = null;
            if (sync.getValue() == 4)
                crystal.getCrystal().setDead();

            return false;
        }

        else
            return false;
    }

    public boolean handleRaytrace(BlockPos calculatedPosition) {
        if (mc.player.getDistanceSq(calculatedPosition) > Math.pow(wallRange.getValue(), 2)) {
            switch (rayTrace.getValue()) {
                case 0:
                    return !RaytraceUtil.raytraceBlock(calculatedPosition, offset.getValue());
                case 1:
                    return !RaytraceUtil.raytraceQuill(calculatedPosition, offset.getValue());
                case 2:
                    return true;
            }
        }

        return false;
    }

    public boolean handleMinDamage() {
        if (crystalTarget != null) {
            if (EnemyUtil.getHealth(crystalTarget) < facePlaceHealth.getValue())
                return false;
            else if (EnemyUtil.getArmor(crystalTarget, armorBreaker.getValue(), armorScale.getValue()))
                return false;
            else if (facePlaceHole.getValue() && HoleUtil.isInHole(crystalTarget))
                return false;
            else if (Keyboard.isKeyDown(forceFaceplace.getKey()))
                return false;
        }

        return true;
    }

    public void handleRotations() {
        if (!RaytraceUtil.raytraceEntity(crystal.getCrystal()) && onlyInViewFrustrum.getValue())
            return;

        float[] rotations = null;

        switch (rotateDuring.getValue()) {
            case 0:
                rotations = randomRotate.getValue() ? new float[] {(float) new Random().nextGaussian(), (float) new Random().nextGaussian()} : RotationUtil.getAngles(crystal.getCrystal());
                break;
            case 1:
            case 2:
                rotations = randomRotate.getValue() ? new float[] {(float) new Random().nextGaussian(), (float) new Random().nextGaussian()} : RotationUtil.getPositionAngles(crystalPosition.getCrystalPosition());
                break;
        }

        switch (rotateMode.getValue()) {
            case 0:
                assert rotations != null;
                Rotation packetRotation = new Rotation(rotations[0], rotations[1], Rotation.RotationMode.Packet, RotationPriority.Highest);
                crystalRotation = strict.getValue() ? RotationUtil.rotationStep(RotationManager.serverRotation, packetRotation, (float) rotateStep.getValue()) : packetRotation;
                break;
            case 1:
                assert rotations != null;
                Rotation legitRotation = new Rotation(rotations[0], rotations[1], Rotation.RotationMode.Legit, RotationPriority.Highest);
                crystalRotation = strict.getValue() ? RotationUtil.rotationStep(RotationManager.serverRotation, legitRotation, (float) rotateStep.getValue()) : legitRotation;
                break;
        }

        RotationManager.rotationQueue.add(crystalRotation);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent eventRender) {
        try {
            if (renderCrystal.getValue() && crystalPosition.getCrystalPosition() != BlockPos.ORIGIN && crystalPosition != null && crystalPosition.getCrystalPosition() != null && crystalTarget != null) {
                switch (renderMode.getValue()) {
                    case 0:
                        RenderUtil.drawBoxBlockPos(crystalPosition.getCrystalPosition(), 0, colorPicker.getColor(), RenderMode.Fill);
                        break;
                    case 1:
                        RenderUtil.drawBoxBlockPos(crystalPosition.getCrystalPosition(), 0, colorPicker.getColor(), RenderMode.Outline);
                        break;
                    case 2:
                        RenderUtil.drawBoxBlockPos(crystalPosition.getCrystalPosition(), 0, colorPicker.getColor(), RenderMode.Both);
                        break;
                    case 3:
                        RenderUtil.drawBoxBlockPos(crystalPosition.getCrystalPosition(), 0, colorPicker.getColor(), RenderMode.Claw);
                        break;
                }

                if (renderDamage.getValue())
                    RenderUtil.drawNametagFromBlockPos(crystalPosition.getCrystalPosition(), 0.5f, String.valueOf(MathUtil.roundAvoid(crystalPosition.getTargetDamage() / 2.16667, 1)));
            }
        } catch (Exception ignored) {

        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof SPacketSpawnObject && ((SPacketSpawnObject) event.getPacket()).getType() == 51 && timing.getValue() == 0 && explode.getValue()) {
            if (mc.player.getDistance(((SPacketSpawnObject) event.getPacket()).getX(), ((SPacketSpawnObject) event.getPacket()).getY(), ((SPacketSpawnObject) event.getPacket()).getZ()) > breakRange.getValue())
                return;

            CrystalUtil.attackCrystal(((SPacketSpawnObject) event.getPacket()).getEntityID());
        }

        if (event.getPacket() instanceof SPacketPlayerPosLook && rubberband.getValue()) {
            NotificationManager.addNotification(new Notification("Rubberband detected! Reset Rotations!", Type.Warning));
            crystalRotation.restoreRotation();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacket() instanceof CPacketUseEntity && ((CPacketUseEntity) event.getPacket()).getAction() == CPacketUseEntity.Action.ATTACK && ((CPacketUseEntity) event.getPacket()).getEntityFromWorld(mc.world) instanceof EntityEnderCrystal) {
            if (sync.getValue() == 2)
                Objects.requireNonNull(((CPacketUseEntity) event.getPacket()).getEntityFromWorld(mc.world)).setDead();
        }
    }

    @Override
    public String getHUDData() {
        return crystalTarget != null ? " " + crystalTarget.getName() : " None";
    }
}
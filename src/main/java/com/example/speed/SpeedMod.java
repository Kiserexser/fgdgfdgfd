package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Random;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean killaura = false;
    private static boolean lastR = false;
    private static Entity target = null;
    private static long lastAttackTime = 0;
    private static final Random random = new Random();

    // Настройки
    private static final float RANGE = 4.2f;
    private static final long MIN_DELAY = 750L;
    private static final long MAX_DELAY = 850L;
    private static final float ROTATION_SPEED = 0.4f;   // 0.4 = 40% от разницы за тик (плавно, без рывков)
    private static final float MAX_ANGLE_DELTA = 15f;   // атакуем если цель в пределах 15° от центра

    @Override
    public void onInitialize() {
        new Thread(() -> {
            while (true) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                if (mc.player == null) continue;
                long window = mc.getWindow().getHandle();
                boolean currentR = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
                if (currentR && !lastR) {
                    killaura = !killaura;
                    mc.player.sendMessage(Text.literal(killaura ? "§aKillaura ON" : "§cKillaura OFF"), true);
                    if (!killaura) target = null;
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (killaura) tick();
            }
        }).start();
    }

    private void tick() {
        updateTarget();
        if (target == null) return;

        if (mc.player.squaredDistanceTo(target) > RANGE * RANGE) return;
        if (!mc.player.canSee(target)) return;

        // Идеальные углы на цель
        Vec3d eye = mc.player.getEyePos();
        Vec3d to = target.getBoundingBox().getCenter().subtract(eye);
        double hyp = Math.hypot(to.x, to.z);
        float idealYaw = wrap((float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90));
        float idealPitch = clamp((float) -Math.toDegrees(Math.atan2(to.y, hyp)), -89, 89);

        // Плавное изменение текущих углов к идеальным (интерполяция)
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        float newYaw = lerpAngle(currentYaw, idealYaw, ROTATION_SPEED);
        float newPitch = lerp(currentPitch, idealPitch, ROTATION_SPEED);
        newPitch = clamp(newPitch, -89, 89);

        // Применяем поворот
        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
        mc.player.headYaw = newYaw;
        mc.player.bodyYaw = newYaw;

        // Проверка, атакуем ли
        float deltaYaw = wrap(idealYaw - newYaw);
        float deltaPitch = idealPitch - newPitch;
        boolean canAttack = Math.abs(deltaYaw) < MAX_ANGLE_DELTA && Math.abs(deltaPitch) < MAX_ANGLE_DELTA;

        long now = System.currentTimeMillis();
        long delay = MIN_DELAY + (long)(random.nextDouble() * (MAX_DELAY - MIN_DELAY));
        if (now - lastAttackTime >= delay && canAttack) {
            boolean wasSprinting = mc.player.isSprinting();
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            if (wasSprinting) mc.player.setSprinting(true);
            mc.player.setSprinting(true);
            lastAttackTime = now;
        }
    }

    private void updateTarget() {
        if (target != null && target.isAlive() && mc.player.squaredDistanceTo(target) <= RANGE * RANGE) {
            return;
        }
        Entity best = null;
        double closest = RANGE * RANGE;
        Box box = mc.player.getBoundingBox().expand(RANGE);
        List<Entity> entities = mc.world.getOtherEntities(mc.player, box,
                e -> e instanceof LivingEntity && e != mc.player && ((LivingEntity) e).isAlive());
        for (Entity e : entities) {
            if (e instanceof PlayerEntity && mc.player.isTeammate((PlayerEntity) e)) continue;
            double dist = mc.player.squaredDistanceTo(e);
            if (dist < closest && mc.player.canSee(e)) {
                closest = dist;
                best = e;
            }
        }
        target = best;
    }

    // Линейная интерполяция углов (учитывает переход через 360)
    private static float lerpAngle(float from, float to, float factor) {
        float diff = wrap(to - from);
        return from + diff * factor;
    }

    private static float lerp(float from, float to, float factor) {
        return from + (to - from) * factor;
    }

    private static float wrap(float v) {
        v %= 360f;
        if (v >= 180f) v -= 360f;
        if (v < -180f) v += 360f;
        return v;
    }
    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}

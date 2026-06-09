package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static int tickCounter = 0;
    private static final double CLIMB_SPEED = 0.35;      // сила подъёма
    private static final double WALL_PUSH = 0.1;         // прижимание к стене

    @Override
    public void onInitialize() {
        new Thread(() -> {
            while (true) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                if (mc.player == null) continue;
                long window = mc.getWindow().getHandle();
                boolean currentR = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
                if (currentR && !lastR) {
                    enabled = !enabled;
                    mc.player.sendMessage(Text.literal(enabled ? "§aSpider ON" : "§cSpider OFF"), true);
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled) tick();
            }
        }).start();
    }

    private void tick() {
        if (mc.player == null) return;
        tickCounter++;

        // Проверяем, зажат ли прыжок
        if (!mc.options.jumpKey.isPressed()) return;

        // Проверяем, есть ли блок перед игроком (стена)
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVector();
        double reach = 3.0;
        Vec3d end = eyePos.add(lookVec.multiply(reach));
        BlockHitResult hit = mc.world.raycast(new net.minecraft.world.RaycastContext(eyePos, end, net.minecraft.world.RaycastContext.ShapeType.OUTLINE, net.minecraft.world.RaycastContext.FluidHandling.NONE, mc.player));
        boolean isTouchingWall = hit.getType() == HitResult.Type.BLOCK && hit.getPos().distanceTo(eyePos) < 2.0;

        if (!isTouchingWall) return;

        // Каждые 2 тика даём импульс вверх и к стене
        if (tickCounter % 2 == 0) {
            // Вертикальный импульс
            mc.player.addVelocity(0, CLIMB_SPEED, 0);
            // Импульс в сторону стены (прижимание)
            Vec3d wallDir = hit.getPos().subtract(eyePos).normalize();
            mc.player.addVelocity(wallDir.x * WALL_PUSH, 0, wallDir.z * WALL_PUSH);
        }

        // Автоспринт не нужен, но можно включить
        mc.player.setSprinting(true);
    }
}

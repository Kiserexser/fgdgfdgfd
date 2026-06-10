package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;

    // Настройки скоростей (можно менять прямо здесь)
    private static final double UP_SPEED = 0.8;       // скорость подъёма (пробел)
    private static final double DOWN_SPEED = 0.6;     // скорость спуска (Shift или назад?)
    private static final double HORIZONTAL_SPEED = 1.2; // горизонтальная скорость

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
                    if (!enabled && mc.player.getVehicle() instanceof BoatEntity boat) {
                        // При выключении возвращаем нормальное поведение лодке
                        boat.noClip = false;
                        boat.setNoGravity(false);
                    }
                    mc.player.sendMessage(Text.literal(enabled ? "§aBoatNoClip ON" : "§cBoatNoClip OFF"), true);
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled) tick();
            }
        }).start();
    }

    private static void tick() {
        if (mc.player == null || mc.world == null) return;
        if (!(mc.player.getVehicle() instanceof BoatEntity boat)) return;

        // Включаем noClip и отключаем гравитацию
        boat.noClip = true;
        boat.setNoGravity(true);

        // Синхронизируем поворот лодки с игроком
        boat.setYaw(mc.player.getYaw());
        boat.prevYaw = boat.getYaw();

        double motionX = 0, motionY = 0, motionZ = 0;
        float yaw = boat.getYaw();
        double rad = Math.toRadians(yaw);

        // Управление
        if (mc.options.jumpKey.isPressed()) {
            motionY += UP_SPEED;
        }
        if (mc.options.sneakKey.isPressed()) {
            motionY -= DOWN_SPEED;
        }
        if (mc.options.forwardKey.isPressed()) {
            motionX -= MathHelper.sin((float) rad) * HORIZONTAL_SPEED;
            motionZ += MathHelper.cos((float) rad) * HORIZONTAL_SPEED;
        }
        if (mc.options.backKey.isPressed()) {
            motionX += MathHelper.sin((float) rad) * HORIZONTAL_SPEED;
            motionZ -= MathHelper.cos((float) rad) * HORIZONTAL_SPEED;
        }
        if (mc.options.leftKey.isPressed()) {
            motionX -= MathHelper.cos((float) rad) * HORIZONTAL_SPEED;
            motionZ -= MathHelper.sin((float) rad) * HORIZONTAL_SPEED;
        }
        if (mc.options.rightKey.isPressed()) {
            motionX += MathHelper.cos((float) rad) * HORIZONTAL_SPEED;
            motionZ += MathHelper.sin((float) rad) * HORIZONTAL_SPEED;
        }

        // Применяем движение
        boat.setVelocity(motionX, motionY, motionZ);

        // Принудительное оставание в лодке (на случай выхода)
        if (!mc.player.hasVehicle()) {
            mc.player.startRiding(boat, true);
        }
    }
}

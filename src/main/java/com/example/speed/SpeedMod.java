package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static int tickCounter = 0;
    private static final Random random = new Random();

    // Настройки
    private static final double SPEED_MULTIPLIER = 1.30;   // +30% к скорости спринта
    private static final int GHOST_INTERVAL = 5;          // создаём призрачный блок каждые N тиков

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
                    mc.player.sendMessage(Text.literal(enabled ? "§aGhostSpeed ON" : "§cGhostSpeed OFF"), true);
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled) tick();
            }
        }).start();
    }

    private static void tick() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        tickCounter++;

        // Работаем только когда бежим вперёд
        if (mc.player.input.movementForward <= 0) return;

        // Призрачный блок: отправляем пакет с флагом onGround = true, даже если игрок в воздухе
        if (tickCounter % GHOST_INTERVAL == 0) {
            Vec3d pos = mc.player.getPos();
            // Немного сдвигаем позицию вниз, чтобы имитировать "блок" под ногами
            Vec3d fakePos = new Vec3d(pos.x, pos.y - 0.001, pos.z);
            PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(fakePos.x, fakePos.y, fakePos.z, true, false);
            mc.getNetworkHandler().sendPacket(packet);
        }

        // Ускорение: увеличиваем горизонтальную скорость (прямое)
        float yaw = mc.player.getYaw();
        double rad = Math.toRadians(yaw);
        double baseSpeed = 0.3; // спринт
        double newSpeed = baseSpeed * SPEED_MULTIPLIER;
        double vx = -Math.sin(rad) * newSpeed;
        double vz = Math.cos(rad) * newSpeed;
        mc.player.setVelocity(vx, mc.player.getVelocity().y, vz);
        mc.player.setSprinting(true);
    }
}

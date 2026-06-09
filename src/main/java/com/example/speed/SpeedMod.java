package com.example.speed;

import net.fabricc.api.ModInitializer;
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

    // Настройки (можно менять)
    private static final double EXTRA_SPEED = 0.15;      // сила ускорения (0.15 – безопасно)
    private static final int PACKET_INTERVAL = 3;       // отправка каждые N тиков
    private static final float SEND_CHANCE = 0.3f;      // вероятность отправки (30%)

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
                    mc.player.sendMessage(Text.literal(enabled ? "§aPacketSpeed ON" : "§cPacketSpeed OFF"), true);
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

        // Работаем только когда игрок идёт вперёд
        if (mc.player.input.movementForward <= 0) return;

        // Отправляем фейковый пакет не каждый тик, а с интервалом и шансом
        if (tickCounter % PACKET_INTERVAL == 0 && random.nextFloat() < SEND_CHANCE) {
            Vec3d realPos = mc.player.getPos();
            float yaw = mc.player.getYaw();
            double rad = Math.toRadians(yaw);
            double offsetX = -Math.sin(rad) * EXTRA_SPEED;
            double offsetZ = Math.cos(rad) * EXTRA_SPEED;
            Vec3d fakePos = new Vec3d(realPos.x + offsetX, realPos.y, realPos.z + offsetZ);
            // Конструктор PositionAndOnGround требует 5 параметров (x, y, z, onGround, hasHorizontalCollision)
            PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(fakePos.x, fakePos.y, fakePos.z, mc.player.isOnGround(), false);
            mc.getNetworkHandler().sendPacket(packet);
        }

        // Автоспринт
        mc.player.setSprinting(true);
    }
}

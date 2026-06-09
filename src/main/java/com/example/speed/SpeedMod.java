package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static int tickCounter = 0;
    private static final double EXTRA_SPEED = 0.2; // безопасное значение для ReallyWorld

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

    private void tick() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        tickCounter++;

        if (mc.player.input.movementForward <= 0) return;

        if (tickCounter % 2 == 0) {
            Vec3d realPos = mc.player.getPos();
            float yaw = mc.player.getYaw();
            double rad = Math.toRadians(yaw);
            double offsetX = -Math.sin(rad) * EXTRA_SPEED;
            double offsetZ = Math.cos(rad) * EXTRA_SPEED;
            Vec3d fakePos = new Vec3d(realPos.x + offsetX, realPos.y, realPos.z + offsetZ);
            PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(fakePos.x, fakePos.y, fakePos.z, mc.player.isOnGround());
            mc.getNetworkHandler().sendPacket(packet);
        }

        mc.player.setSprinting(true);
    }
}

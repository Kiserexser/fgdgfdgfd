package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    // Настройка: 0.36 = +20%, 0.39 = +30%, 0.42 = +40% (рискованно)
    private static final double SPEED_BOOST = 0.36;

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
                    mc.player.sendMessage(Text.literal(enabled ? "§aSafeSpeed ON" : "§cSafeSpeed OFF"), true);
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled && mc.player.isOnGround() && mc.player.input.movementForward > 0) {
                    float yaw = mc.player.getYaw();
                    double rad = Math.toRadians(yaw);
                    double vx = -Math.sin(rad) * SPEED_BOOST;
                    double vz = Math.cos(rad) * SPEED_BOOST;
                    mc.player.setVelocity(vx, mc.player.getVelocity().y, vz);
                    mc.player.setSprinting(true);
                }
            }
        }).start();
    }
}

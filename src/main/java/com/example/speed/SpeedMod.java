package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static int ticks = 0;
    private static int groundTicks = 0;
    private static float originalTickLength = 50.0f;

    @Override
    public void onInitialize() {
        if (mc.timer != null) originalTickLength = mc.timer.tickLength;
        System.out.println("[SpeedModule] Loaded. Press R to toggle.");

        new Thread(() -> {
            while (true) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                if (mc.player == null) continue;
                long window = mc.getWindow().getHandle();
                boolean currentR = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
                if (currentR && !lastR) {
                    enabled = !enabled;
                    mc.player.sendMessage(Text.literal(enabled ? "§aSpeedModule ON" : "§cSpeedModule OFF"), true);
                    if (!enabled) {
                        if (mc.timer != null) mc.timer.tickLength = originalTickLength;
                        ticks = 0;
                        groundTicks = 0;
                    }
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled) {
                    onMovePost();
                    onMoveInput();
                    onPostMotion();
                }
            }
        }).start();
    }

    private void setTimer(float factor) {
        if (mc.timer != null) mc.timer.tickLength = originalTickLength / factor;
    }

    // ========== EventOnMovePost ==========
    private void onMovePost() {
        setTimer(1.7F);

        if (ticks > 3) {
            double bst = 0.03;
            if (ticks % 2 == 0) {
                mc.player.addVelocity(0, 0.03F, 0);
                bst = mc.player.isOnGround() ? 0.085 : 0.03;
            }
            float dir = getDirection();
            if (dir != -1.0F) {
                double yaw = Math.toRadians(dir);
                double xt = -Math.sin(yaw);
                double zt = Math.cos(yaw);
                mc.player.addVelocity(xt * bst, 0, zt * bst);
            }
        }
        ticks++;
    }

    // ========== EventMoveInput (прыжки на земле) ==========
    private void onMoveInput() {
        if (mc.player.verticalCollision) groundTicks++;
        else groundTicks = 0;

        if (groundTicks >= 1) mc.player.jump();
    }

    // ========== EventPostMotion (элитра) ==========
    private void onPostMotion() {
        if (ticks % 2 == 0) {
            setTimer(0.3F);
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }
    }

    // ========== Обработка пакета PlayerPositionLookS2CPacket (будет вызвана из миксина) ==========
    public static void onPlayerPositionLook() {
        if (enabled) {
            if (ticks % 2 == 1) {
                ticks++;
            }
            if (mc.timer != null) mc.timer.tickLength = originalTickLength;
        }
    }

    private float getDirection() {
        float yaw = mc.player.getYaw();
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        if (forward == 0 && strafe == 0) return -1.0f;
        float angle = yaw + (strafe > 0 ? -90 : 90) * (strafe != 0 ? 1 : 0);
        if (forward < 0) angle += 180;
        return angle;
    }
}

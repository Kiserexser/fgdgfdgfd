package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static int tickCounter = 0;
    private static final double WEB_SPEED = 0.85;    // горизонтальное ускорение
    private static final double WEB_UP = 0.85;       // вертикальный подъём

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
                    mc.player.sendMessage(Text.literal(enabled ? "§aWebFly+Speed ON" : "§cWebFly+Speed OFF"), true);
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled) tick();
            }
        }).start();
    }

    private static void tick() {
        if (mc.player == null) return;
        tickCounter++;

        boolean inWeb = mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.COBWEB ||
                        mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() == Blocks.COBWEB;
        if (!inWeb) return;

        // Вертикальный полёт (при зажатом прыжке)
        if (mc.options.jumpKey.isPressed()) {
            if (tickCounter % (1 + (int)(Math.random() * 2)) == 0) {
                mc.player.addVelocity(0, WEB_UP, 0);
            }
        }

        // Горизонтальное ускорение (при любом движении)
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        if (forward != 0 || strafe != 0) {
            float yaw = mc.player.getYaw();
            double rad = Math.toRadians(yaw);
            double vx = -Math.sin(rad) * forward * WEB_SPEED;
            double vz = Math.cos(rad) * forward * WEB_SPEED;
            if (strafe != 0) {
                double strafeRad = Math.toRadians(yaw + (strafe > 0 ? -90 : 90));
                vx += -Math.sin(strafeRad) * strafe * WEB_SPEED;
                vz += Math.cos(strafeRad) * strafe * WEB_SPEED;
            }
            mc.player.setVelocity(vx, mc.player.getVelocity().y, vz);
        }

        mc.player.setSprinting(true);
    }
}

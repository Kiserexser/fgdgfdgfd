package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static int tickCounter = 0;

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
                    mc.player.sendMessage(Text.literal(enabled ? "§aFenceBoost ON" : "§cFenceBoost OFF"), true);
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled) tick();
            }
        }).start();
    }

    private void tick() {
        if (mc.player == null) return;
        BlockPos below = mc.player.getBlockPos().down();
        boolean onFence = mc.world.getBlockState(below).getBlock() == Blocks.OAK_FENCE ||
                          mc.world.getBlockState(below).getBlock() == Blocks.NETHER_BRICK_FENCE;
        if (onFence && mc.options.jumpKey.isPressed()) {
            // Добавляем вертикальный импульс
            mc.player.setVelocity(mc.player.getVelocity().x, 0.6, mc.player.getVelocity().z);
            mc.player.jump();
        }
    }
}

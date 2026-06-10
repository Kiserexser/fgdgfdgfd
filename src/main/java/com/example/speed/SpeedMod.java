package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static final double BOOST_STRENGTH = 2.0;

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
                    mc.player.sendMessage(Text.literal(enabled ? "§aShulkerBoost ON" : "§cShulkerBoost OFF"), true);
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled) tick();
            }
        }).start();
    }

    private static void tick() {
        if (mc.player == null || mc.world == null) return;

        // Радиус проверки – 4 блока вокруг игрока
        BlockPos playerPos = mc.player.getBlockPos();
        int radius = 4;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockEntity be = mc.world.getBlockEntity(pos);
                    if (be instanceof ShulkerBoxBlockEntity shulker) {
                        double dx = mc.player.getX() - (pos.getX() + 0.5);
                        double dz = mc.player.getZ() - (pos.getZ() + 0.5);
                        double dy = mc.player.getY() - (pos.getY() + 0.5);
                        double horizDist = Math.sqrt(dx * dx + dz * dz);

                        if (horizDist <= 1.0 && Math.abs(dy) <= 2.0) {
                            float progress = shulker.getAnimationProgress(1.0f);
                            if (progress > 0.0f && progress != 1.0f) {
                                mc.player.setVelocity(mc.player.getVelocity().x, BOOST_STRENGTH, mc.player.getVelocity().z);
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static final double BOOST_STRENGTH = 2.0; // "моушен" = 2

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

        // Перебираем все блоки-сущности в мире
        for (BlockEntity be : mc.world.blockEntities) {
            if (be instanceof ShulkerBoxBlockEntity shulker) {
                // Расстояние до игрока (центр блока + 0.5)
                double dx = mc.player.getX() - (shulker.getPos().getX() + 0.5);
                double dz = mc.player.getZ() - (shulker.getPos().getZ() + 0.5);
                double dy = mc.player.getY() - (shulker.getPos().getY() + 0.5);
                double horizDist = Math.sqrt(dx * dx + dz * dz);

                // Проверка: горизонтально ≤ 1 блок, вертикально ≤ 2 блока (по аналогии с оригиналом)
                if (horizDist <= 1.0 && Math.abs(dy) <= 2.0) {
                    // Прогресс анимации открытия (0..1, 0=закрыт, 1=открыт)
                    float progress = shulker.getAnimationProgress(1.0f);
                    if (progress > 0.0f && progress != 1.0f) {
                        // Подбрасываем игрока вверх
                        mc.player.setVelocity(mc.player.getVelocity().x, BOOST_STRENGTH, mc.player.getVelocity().z);
                        // Можно добавить задержку, чтобы не спамило, но в оригинале так.
                    }
                }
            }
        }
    }
}

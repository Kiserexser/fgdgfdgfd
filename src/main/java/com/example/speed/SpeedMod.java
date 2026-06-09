package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static long lastJumpTime = 0;
    private static final long JUMP_DELAY_MS = 400;
    private static final double JUMP_VELOCITY = 0.6;

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
                    mc.player.sendMessage(Text.literal(enabled ? "§aAirJump ON" : "§cAirJump OFF"), true);
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled) tick();
            }
        }).start();
    }

    private void tick() {
        if (mc.player == null || mc.player.isOnGround()) {
            // Если на земле, сбрасываем таймер
            lastJumpTime = 0;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastJumpTime >= JUMP_DELAY_MS && canAirJump()) {
            mc.player.setVelocity(mc.player.getVelocity().x, JUMP_VELOCITY, mc.player.getVelocity().z);
            lastJumpTime = now;
        }
    }

    private boolean canAirJump() {
        // Режим "Polar Block Collision": проверяем наличие коллизии под игроком или вокруг
        Box playerBox = mc.player.getBoundingBox();
        // Смещаем немного вниз для проверки (как в оригинале)
        Box checkBox = new Box(
                playerBox.minX, playerBox.minY - 0.2,
                playerBox.minZ, playerBox.maxX, playerBox.minY + 0.4,
                playerBox.maxZ
        );
        // Перебираем все блоки в этой области
        for (BlockPos pos : BlockPos.iterate(
                (int) Math.floor(checkBox.minX), (int) Math.floor(checkBox.minY), (int) Math.floor(checkBox.minZ),
                (int) Math.floor(checkBox.maxX), (int) Math.floor(checkBox.maxY), (int) Math.floor(checkBox.maxZ)
        )) {
            var state = mc.world.getBlockState(pos);
            var shape = state.getCollisionShape(mc.world, pos);
            if (!shape.isEmpty()) {
                // Проверяем, пересекается ли коллизия с нашим боксом
                var boxes = shape.getBoundingBoxes();
                for (var box : boxes) {
                    Box blockBox = new Box(pos.getX() + box.minX, pos.getY() + box.minY, pos.getZ() + box.minZ,
                            pos.getX() + box.maxX, pos.getY() + box.maxY, pos.getZ() + box.maxZ);
                    if (blockBox.intersects(checkBox)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

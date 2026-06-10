package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;

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
                    mc.player.sendMessage(Text.literal(enabled ? "§aSpider ON" : "§cSpider OFF"), true);
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled) tick();
            }
        }).start();
    }

    private static void tick() {
        if (mc.player == null) return;

        // Ищем ведро с водой в инвентаре (первые 9 слотов)
        int bucketSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.WATER_BUCKET) {
                bucketSlot = i;
                break;
            }
        }
        if (bucketSlot == -1) return;

        // Если игрок касается стены (горизонтальная коллизия)
        if (mc.player.horizontalCollision) {
            // Переключаемся на слот с ведром, если не в руке
            if (mc.player.getInventory().selectedSlot != bucketSlot) {
                mc.player.getInventory().selectedSlot = bucketSlot;
            }
            // Используем правый клик (ставить воду)
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            // Даём вертикальный импульс
            mc.player.setVelocity(mc.player.getVelocity().x, 0.36, mc.player.getVelocity().z);
        }
    }
}

package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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
                    mc.player.sendMessage(Text.literal(enabled ? "§aWallWaterFly ON" : "§cWallWaterFly OFF"), true);
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled && mc.options.jumpKey.isPressed()) {
                    tick();
                }
            }
        }).start();
    }

    private void tick() {
        if (mc.player == null || mc.world == null) return;

        // Проверяем наличие ведра с водой в инвентаре
        boolean hasBucket = mc.player.getInventory().contains(Items.WATER_BUCKET.getDefaultStack());
        if (!hasBucket) return;

        // Вычисляем блок перед игроком (на расстоянии 0.5 блока)
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVector();
        BlockPos frontBlock = BlockPos.ofFloored(eyePos.add(lookVec.multiply(0.6))); // перед лицом

        // Убедимся, что этот блок – воздух (иначе не поставить воду)
        if (!mc.world.getBlockState(frontBlock).isAir()) return;

        // Находим слот с ведром воды
        int bucketSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.WATER_BUCKET) {
                bucketSlot = i;
                break;
            }
        }
        if (bucketSlot == -1) return;

        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = bucketSlot;

        // Ставим воду на стену (блок перед игроком)
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(frontBlock), Direction.UP, frontBlock, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);

        // Возвращаем слот
        mc.player.getInventory().selectedSlot = prevSlot;

        // Прыгаем, чтобы начать подъём в воде
        mc.player.jump();
    }
}

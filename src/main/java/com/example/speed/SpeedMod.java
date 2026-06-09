package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
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
                    mc.player.sendMessage(Text.literal(enabled ? "§aWebTower ON" : "§cWebTower OFF"), true);
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled) tick();
            }
        }).start();
    }

    private static void tick() {
        if (mc.player == null || mc.world == null) return;
        tickCounter++;

        // Поиск паутины в инвентаре (горячая панель)
        int webSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() == Blocks.COBWEB) {
                webSlot = i;
                break;
            }
        }
        if (webSlot == -1) return;

        // Сохраняем текущий слот
        int prevSlot = mc.player.getInventory().selectedSlot;

        // Определяем позицию для установки паутины: на 1 блок выше текущей позиции игрока
        // Игрок стоит на блоке, ставим паутину над его головой? Лучше ставить на уровне ног + 1 блок вверх?
        // Чтобы столб рос вверх, ставим паутину на позиции игрока (там уже может быть паутина) и над ним.
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos webPos = playerPos; // блок, где стоит игрок
        // Если на позиции игрока уже паутина, ставим выше
        if (mc.world.getBlockState(webPos).getBlock() == Blocks.COBWEB) {
            webPos = webPos.up();
        }
        // Если и там паутина, то выше и т.д. – но для простоты ставим на 1 блок выше текущей позиции игрока
        // Также можно ставить под ноги, чтобы подниматься снизу.

        // Более надёжный способ: ставим паутину над головой (на 1 блок выше глаз), чтобы игрок всегда оказывался внутри.
        // И также ставим под ноги для устойчивости.
        BlockPos under = playerPos.down();
        BlockPos above = playerPos.up();

        // Переключаемся на слот с паутиной
        mc.player.getInventory().selectedSlot = webSlot;

        // Ставим паутину под ноги (если там воздух или не паутина)
        if (mc.world.getBlockState(under).isAir()) {
            placeBlock(under);
        }
        // Ставим паутину на уровне головы (над игроком)
        if (mc.world.getBlockState(above).isAir()) {
            placeBlock(above);
        }
        // Можно ставить ещё выше, чтобы столб рос быстрее
        if (tickCounter % 2 == 0) {
            BlockPos above2 = above.up();
            if (mc.world.getBlockState(above2).isAir()) {
                placeBlock(above2);
            }
        }

        // Возвращаем слот
        mc.player.getInventory().selectedSlot = prevSlot;

        // Если игрок зажал прыжок, даём импульс вверх (ускорение)
        if (mc.options.jumpKey.isPressed()) {
            mc.player.addVelocity(0, 0.25, 0);
            mc.player.setSprinting(true);
        }
    }

    private static void placeBlock(BlockPos pos) {
        if (mc.interactionManager == null) return;
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        // Небольшая задержка, чтобы не спамить
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
    }
}

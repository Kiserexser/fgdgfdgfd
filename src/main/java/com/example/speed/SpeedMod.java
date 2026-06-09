package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
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
    private static Thread buildThread = null;
    private static volatile boolean running = false;
    private static BlockPos lastPlaced = null;

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
                    if (enabled) startBuilding();
                    else stopBuilding();
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
            }
        }).start();
    }

    private static void startBuilding() {
        if (buildThread != null && buildThread.isAlive()) return;
        running = true;
        lastPlaced = null;
        buildThread = new Thread(() -> {
            while (running && enabled) {
                try {
                    if (mc.player != null && mc.world != null) {
                        buildStep();
                    }
                    Thread.sleep(500); // строим каждые 0.5 сек
                } catch (InterruptedException e) { break; }
            }
        });
        buildThread.start();
    }

    private static void stopBuilding() {
        running = false;
        if (buildThread != null) {
            buildThread.interrupt();
            buildThread = null;
        }
        lastPlaced = null;
    }

    private static void buildStep() {
        int slot = findWebSlot();
        if (slot == -1) {
            mc.player.sendMessage(Text.literal("§cНет паутины в горячей панели!"), true);
            stopBuilding();
            return;
        }

        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos targetPos;

        if (lastPlaced == null) {
            // Первая паутина – ставим на позицию ног игрока или под ним?
            // Чтобы игрок оказался внутри паутины, ставим на его позицию (если там воздух) или под ним.
            if (mc.world.getBlockState(playerPos).isAir()) {
                targetPos = playerPos;
            } else {
                targetPos = playerPos.down();
            }
        } else {
            // Строим выше
            targetPos = lastPlaced.up();
        }

        if (targetPos.getY() - playerPos.getY() > 20) {
            // Слишком высоко, останавливаемся
            stopBuilding();
            return;
        }

        if (mc.world.getBlockState(targetPos).isAir()) {
            placeWeb(targetPos);
            lastPlaced = targetPos;
        }

        mc.player.getInventory().selectedSlot = prevSlot;
    }

    private static void placeWeb(BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 0));
    }

    private static int findWebSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.COBWEB) {
                return i;
            }
        }
        return -1;
    }
}

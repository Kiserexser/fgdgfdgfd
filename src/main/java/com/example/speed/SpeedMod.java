package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static Thread farmThread = null;
    private static volatile boolean running = false;
    private static final Set<BlockPos> brokenBlocks = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, Long> blockBreakingTimes = new ConcurrentHashMap<>();
    private static final int RADIUS = 4;
    private static final long COOLDOWN_MS = 400;

    @Override
    public void onInitialize() {
        System.out.println("[FarmCarrot] Module loaded. Press R to toggle.");
        new Thread(() -> {
            while (true) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                if (mc.player == null) continue;
                long window = mc.getWindow().getHandle();
                boolean currentR = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
                if (currentR && !lastR) {
                    enabled = !enabled;
                    mc.player.sendMessage(Text.literal(enabled ? "§aFarmCarrot ON" : "§cFarmCarrot OFF"), true);
                    if (enabled) startFarming();
                    else stopFarming();
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
            }
        }).start();
    }

    private static void startFarming() {
        if (farmThread != null && farmThread.isAlive()) return;
        running = true;
        farmThread = new Thread(() -> {
            while (running && enabled) {
                try {
                    if (mc.player != null && mc.world != null) farm();
                    Thread.sleep(1);
                } catch (InterruptedException e) { break; }
            }
        });
        farmThread.start();
    }

    private static void stopFarming() {
        running = false;
        if (farmThread != null) {
            farmThread.interrupt();
            farmThread = null;
        }
        brokenBlocks.clear();
        blockBreakingTimes.clear();
    }

    private static void farm() {
        BlockPos playerPos = mc.player.getBlockPos();
        List<BlockPos> targets = new ArrayList<>();

        // Поиск моркови в радиусе
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (block == Blocks.CARROTS) {
                        Block below = mc.world.getBlockState(pos.down()).getBlock();
                        if (below == Blocks.FARMLAND) {
                            targets.add(pos);
                        }
                    }
                }
            }
        }

        long now = System.currentTimeMillis();
        brokenBlocks.removeIf(p -> now - blockBreakingTimes.getOrDefault(p, now) >= COOLDOWN_MS);
        targets.removeIf(brokenBlocks::contains);
        if (targets.isEmpty()) return;

        targets.sort(Comparator.comparingDouble(p -> p.getSquaredDistance(playerPos)));
        BlockPos target = targets.get(0);
        try {
            // Ломаем морковь
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, target, Direction.UP));
            Thread.sleep(3);
            brokenBlocks.add(target);
            blockBreakingTimes.put(target, now);

            // Сажаем новую морковь, если есть в левой руке
            if (mc.player.getOffHandStack().getItem() == net.minecraft.item.Items.CARROT) {
                BlockPos dirtPos = target.down();
                BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(dirtPos), Direction.UP, dirtPos, false);
                // ✅ Исправленный конструктор с параметром sequence = 0
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, hitResult, 0));
                Thread.sleep(15);
            }
            Thread.sleep(40);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

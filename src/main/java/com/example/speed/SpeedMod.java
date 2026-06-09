package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarrotsBlock;
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
    private static final long COOLDOWN_MS = 500;

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
                    if (mc.player != null && mc.world != null) farmTick();
                    Thread.sleep(50); // 20 проверок в секунду
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

    private static void farmTick() {
        BlockPos playerPos = mc.player.getBlockPos();
        List<BlockPos> targets = new ArrayList<>();

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.getBlock() == Blocks.CARROTS) {
                        // Проверяем возраст: 7 = зрелая
                        int age = state.get(CarrotsBlock.AGE);
                        if (age >= 7) {
                            // Проверяем, что под блоком грядка
                            if (mc.world.getBlockState(pos.down()).getBlock() == Blocks.FARMLAND) {
                                targets.add(pos);
                            }
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
            Thread.sleep(80); // даём серверу время обработать
            brokenBlocks.add(target);
            blockBreakingTimes.put(target, now);

            // Проверяем, что блок действительно исчез (упрощённо)
            if (mc.world.getBlockState(target).getBlock() == Blocks.AIR) {
                // Сажаем новую морковь, если есть в левой руке
                if (mc.player.getOffHandStack().getItem() == net.minecraft.item.Items.CARROT) {
                    BlockPos dirtPos = target.down();
                    BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(dirtPos), Direction.UP, dirtPos, false);
                    mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, hit));
                    Thread.sleep(50);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

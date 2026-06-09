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
    private static final double WEB_SPEED = 0.8;   // горизонтальная скорость в паутине
    private static final double WEB_UP_FORCE = 0.8; // сила подъёма при прыжке

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
                    mc.player.sendMessage(Text.literal(enabled ? "§aWebTower+Speed ON" : "§cWebTower+Speed OFF"), true);
                    if (enabled) startBuilding();
                    else stopBuilding();
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled) {
                    handleWebMovement(); // ускорение в паутине
                }
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
                    Thread.sleep(400); // строим каждые 0.4 сек
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
            mc.player.sendMessage(Text.literal("§cНет паутины в инвентаре!"), true);
            stopBuilding();
            return;
        }
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos targetPos;

        if (lastPlaced == null) {
            // Ставим паутину на 1 блок НИЖЕ игрока (под ногами)
            targetPos = playerPos.down();
        } else {
            // Строим выше
            targetPos = lastPlaced.up();
        }

        if (targetPos.getY() - playerPos.getY() > 15) {
            stopBuilding(); // лимит высоты
            return;
        }

        if (mc.world.getBlockState(targetPos).isAir()) {
            placeWeb(targetPos);
            lastPlaced = targetPos;
        }

        mc.player.getInventory().selectedSlot = prevSlot;
    }

    private static void handleWebMovement() {
        if (mc.player == null) return;
        // Проверка, находится ли игрок в паутине
        boolean inWeb = mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.COBWEB ||
                        mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() == Blocks.COBWEB;
        if (!inWeb) return;

        // Горизонтальное ускорение (0.8)
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        if (forward != 0 || strafe != 0) {
            float yaw = mc.player.getYaw();
            double rad = Math.toRadians(yaw);
            double vx = -Math.sin(rad) * forward * WEB_SPEED;
            double vz = Math.cos(rad) * forward * WEB_SPEED;
            if (strafe != 0) {
                double strafeRad = Math.toRadians(yaw + (strafe > 0 ? -90 : 90));
                vx += -Math.sin(strafeRad) * strafe * WEB_SPEED;
                vz += Math.cos(strafeRad) * strafe * WEB_SPEED;
            }
            mc.player.setVelocity(vx, mc.player.getVelocity().y, vz);
            mc.player.setSprinting(true);
        }

        // Вертикальный подъём при зажатом прыжке (0.8)
        if (mc.options.jumpKey.isPressed()) {
            mc.player.setVelocity(mc.player.getVelocity().x, WEB_UP_FORCE, mc.player.getVelocity().z);
        }
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

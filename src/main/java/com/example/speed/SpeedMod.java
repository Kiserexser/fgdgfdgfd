package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static int ticks = 0;

    // Режим: 0 = Vanilla, 1 = Grim, 2 = Spooky, 3 = FT228
    private static final int MODE = 3;

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
                    mc.player.sendMessage(Text.literal(enabled ? "§aNoSlow ON" : "§cNoSlow OFF"), true);
                    ticks = 0;
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled) tick();
            }
        }).start();
    }

    private static void tick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (!mc.player.isUsingItem()) {
            ticks = 0;
            return;
        }

        // Режим FT228
        if (MODE == 3) {
            if (mc.player.getActiveHand() == Hand.MAIN_HAND && mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            }
            return;
        }

        if (MODE == 2) ticks++;

        // Для режимов Grim (1) и Spooky (2)
        if (MODE == 1 || MODE == 2) {
            boolean isBlockingOrEating = (mc.player.getMainHandStack().getUseAction() == UseAction.BLOCK ||
                                          mc.player.getMainHandStack().getUseAction() == UseAction.EAT) &&
                                          mc.player.getActiveHand() == Hand.MAIN_HAND;
            if (!isBlockingOrEating && mc.player.isUsingItem()) {
                mc.player.setSprinting(true);
                if (mc.player.getActiveHand() == Hand.MAIN_HAND && MODE != 2) {
                    // Отправка пакета для оффхенда
                    if (mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.OFF_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
                    }
                } else {
                    Hand hand = mc.player.getActiveHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
                    if (MODE != 2 && mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(hand, 0, mc.player.getYaw(), mc.player.getPitch()));
                    }
                    if (ticks >= 2 || MODE == 1) {
                        ticks = 0;
                    }
                }
            }
        }

        // Всегда включаем спринт
        if (mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
        }
    }
}

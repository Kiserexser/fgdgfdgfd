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

    // Выберите режим: 0 = Vanilla (просто не замедляться), 1 = Grim, 2 = Spooky, 3 = FT228
    private static final int MODE = 3; // 3 = FT228 (как в твоём коде)

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

        // Если игрок не использует предмет – сбрасываем счётчик и выходим
        if (!mc.player.isUsingItem()) {
            ticks = 0;
            return;
        }

        // Режим FT228 (отправка пакета смены слота)
        if (MODE == 3) {
            if (mc.player.getActiveHand() == Hand.MAIN_HAND) {
                // Отправляем пакет смены слота на текущий слот (обманываем сервер)
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            }
            return; // дальше не обрабатываем другие режимы
        }

        // Увеличиваем счётчик тиков для режимов Spooky/HolyWorld
        if (MODE == 2) ticks++;

        // Режимы: Vanilla (0) – ничего не делаем, просто не даём замедляться
        // Режимы Spooky (2) – используем доп. логику
        if (MODE == 2 || MODE == 1) {
            // Определяем, используется ли предмет в главной руке и есть ли замедляющее действие
            boolean isBlockingOrEating = (mc.player.getMainHandStack().getUseAction() == UseAction.BLOCK ||
                                          mc.player.getMainHandStack().getUseAction() == UseAction.EAT) &&
                                          mc.player.getActiveHand() == Hand.MAIN_HAND;
            if (!isBlockingOrEating && mc.player.isUsingItem()) {
                mc.player.setSprinting(true);
                if (mc.player.getActiveHand() == Hand.MAIN_HAND && MODE != 2) {
                    // Отправляем пакет взаимодействия с предметом в оффхенд
                    mc.interactionManager.sendSequencedPacket(mc.world, seq -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, seq, mc.player.getYaw(), mc.player.getPitch()));
                } else {
                    Hand hand = mc.player.getActiveHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
                    if (MODE != 2) {
                        mc.interactionManager.sendSequencedPacket(mc.world, seq -> new PlayerInteractItemC2SPacket(hand, seq, mc.player.getYaw(), mc.player.getPitch()));
                    }
                    if (ticks >= 2 || MODE == 1) {
                        ticks = 0;
                        // Событие замедления отменяется (мы просто выходим, ничего не делая)
                    }
                }
            }
        }

        // Для всех режимов: принудительно включаем спринт, чтобы не было замедления
        if (mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
        }
    }
}

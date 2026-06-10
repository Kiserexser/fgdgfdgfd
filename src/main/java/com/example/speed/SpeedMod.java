package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
    private static boolean menuOpen = false;
    private static int mode = 3; // 0=Vanilla, 1=Grim, 2=Spooky, 3=FT228

    @Override
    public void onInitialize() {
        // Поток для отслеживания правого Shift (открытие меню)
        new Thread(() -> {
            while (true) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                if (mc.player != null && mc.currentScreen == null) {
                    long window = mc.getWindow().getHandle();
                    boolean rShift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                    if (rShift && !menuOpen) {
                        menuOpen = true;
                        mc.execute(() -> mc.setScreen(new NoSlowMenu()));
                        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                    }
                }
            }
        }).start();

        // Поток для включения/выключения модуля по R
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
        if (mode == 3) {
            if (mc.player.getActiveHand() == Hand.MAIN_HAND) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            }
            return;
        }

        // Режим Spooky
        if (mode == 2) ticks++;

        // Режимы Grim (1) и Spooky (2) используют схожую логику
        if (mode == 1 || mode == 2) {
            boolean isBlockingOrEating = (mc.player.getMainHandStack().getUseAction() == UseAction.BLOCK ||
                                          mc.player.getMainHandStack().getUseAction() == UseAction.EAT) &&
                                          mc.player.getActiveHand() == Hand.MAIN_HAND;
            if (!isBlockingOrEating && mc.player.isUsingItem()) {
                mc.player.setSprinting(true);
                if (mc.player.getActiveHand() == Hand.MAIN_HAND && mode != 2) {
                    // Отправляем пакет взаимодействия с оффхендом
                    mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.OFF_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
                } else {
                    Hand hand = mc.player.getActiveHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
                    if (mode != 2) {
                        mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(hand, 0, mc.player.getYaw(), mc.player.getPitch()));
                    }
                    if (ticks >= 2 || mode == 1) {
                        ticks = 0;
                    }
                }
            }
        }

        // Принудительный спринт для всех режимов
        if (mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
        }
    }

    // ==================== GUI выбора режима ====================
    static class NoSlowMenu extends Screen {
        protected NoSlowMenu() {
            super(Text.literal("NoSlow Mode Select"));
        }

        @Override
        protected void init() {
            super.init();
            int cx = width / 2;
            int y = height / 2 - 60;

            String[] modes = {"Vanilla (0)", "Grim (1)", "Spooky (2)", "FT228 (3)"};
            for (int i = 0; i < modes.length; i++) {
                final int m = i;
                addDrawableChild(ButtonWidget.builder(Text.literal(modes[i] + (mode == m ? " ✓" : "")), btn -> {
                    mode = m;
                    menuOpen = false;
                    close();
                }).dimensions(cx - 75, y + i * 25, 150, 20).build());
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> {
                menuOpen = false;
                close();
            }).dimensions(cx - 75, y + modes.length * 25 + 10, 150, 20).build());
        }

        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            ctx.fill(0, 0, width, height, 0xCC000000);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Select NoSlow Mode"), width / 2, height / 2 - 85, 0xFFFFFF);
            super.render(ctx, mx, my, delta);
        }

        @Override
        public boolean keyPressed(int keyCode, int scan, int mods) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                menuOpen = false;
                close();
                return true;
            }
            return super.keyPressed(keyCode, scan, mods);
        }
        @Override public boolean shouldPause() { return false; }
    }
}

package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static boolean menuOpen = false;
    private static int mode = 0; // 0-6
    private static final String[] MODES = {"Vanilla", "Grim", "Matrix", "Polar", "AAC", "HighPing", "VelocitySpam"};
    private static Vec3d frozenPos = Vec3d.ZERO;
    private static int tickCounter = 0;
    private static final Random random = new Random();

    @Override
    public void onInitialize() {
        new Thread(() -> {
            while (true) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                if (mc.player == null) continue;
                long window = mc.getWindow().getHandle();
                boolean rShift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                if (rShift && !menuOpen) {
                    menuOpen = true;
                    mc.execute(() -> mc.setScreen(new ModeMenu()));
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                boolean currentR = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
                if (currentR && !lastR) {
                    enabled = !enabled;
                    if (enabled) {
                        frozenPos = mc.player.getPos();
                        mc.player.sendMessage(Text.literal("§aFreeze [" + MODES[mode] + "] ON"), true);
                    } else {
                        mc.player.sendMessage(Text.literal("§cFreeze OFF"), true);
                    }
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (enabled) tick();
            }
        }).start();
    }

    private static void tick() {
        if (mc.player == null) return;
        tickCounter++;

        switch (mode) {
            case 0: // Vanilla
                mc.player.setPosition(frozenPos);
                mc.player.setVelocity(Vec3d.ZERO);
                mc.player.input.movementForward = 0;
                mc.player.input.movementSideways = 0;
                break;
            case 1: // Grim (пакеты onGround)
                if (tickCounter % 2 == 0) {
                    PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(
                            frozenPos.x, frozenPos.y, frozenPos.z, true, false);
                    mc.getNetworkHandler().sendPacket(packet);
                }
                mc.player.setVelocity(Vec3d.ZERO);
                break;
            case 2: // Matrix (телепортация + обнуление пакетов)
                mc.player.setPosition(frozenPos);
                mc.player.setVelocity(Vec3d.ZERO);
                if (tickCounter % 3 == 0) {
                    PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(
                            frozenPos.x, frozenPos.y, frozenPos.z, false, false);
                    mc.getNetworkHandler().sendPacket(packet);
                }
                break;
            case 3: // Polar (только позиция, скорость не обнуляем)
                mc.player.setPosition(frozenPos);
                break;
            case 4: // AAC (с задержкой)
                if (tickCounter % 5 == 0) {
                    mc.player.setPosition(frozenPos);
                    mc.player.setVelocity(Vec3d.ZERO);
                }
                break;
            case 5: // HighPing (симуляция высокого пинга)
                if (tickCounter % 10 == 0) {
                    // Отправляем пакет с задержкой (имитация лага)
                    mc.player.setPosition(frozenPos);
                    mc.player.setVelocity(Vec3d.ZERO);
                    // Не отправляем пакеты движения несколько тиков
                }
                // Блокируем движение
                mc.player.input.movementForward = 0;
                mc.player.input.movementSideways = 0;
                break;
            case 6: // VelocitySpam (спам пакетами скорости)
                if (tickCounter % 2 == 0) {
                    mc.player.setVelocity(0, 0, 0);
                }
                mc.player.setPosition(frozenPos);
                break;
        }
        // Дополнительно отключаем прыжок и приседание (через options)
        if (mode != 1 && mode != 2) { // не для Grim/Matrix
            mc.options.jumpKey.setPressed(false);
            mc.options.sneakKey.setPressed(false);
        }
    }

    static class ModeMenu extends Screen {
        protected ModeMenu() { super(Text.literal("Freeze Mode")); }
        @Override
        protected void init() {
            super.init();
            int cx = width / 2;
            int y = height / 2 - 80;
            for (int i = 0; i < MODES.length; i++) {
                final int idx = i;
                addDrawableChild(ButtonWidget.builder(
                        Text.literal(MODES[i] + (mode == idx ? " ✓" : "")),
                        btn -> { mode = idx; menuOpen = false; close(); }
                ).dimensions(cx - 100, y + i * 24, 200, 20).build());
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> { menuOpen = false; close(); })
                    .dimensions(cx - 50, y + MODES.length * 24 + 10, 100, 20).build());
        }
        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            ctx.fill(0, 0, width, height, 0xCC000000);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Select Freeze Mode"), width / 2, height / 2 - 100, 0xFFFFFF);
            super.render(ctx, mx, my, delta);
        }
        @Override public boolean keyPressed(int keyCode, int scan, int mods) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                menuOpen = false; close(); return true;
            }
            return super.keyPressed(keyCode, scan, mods);
        }
        @Override public void close() { client.setScreen(null); }
        @Override public boolean shouldPause() { return false; }
    }
}

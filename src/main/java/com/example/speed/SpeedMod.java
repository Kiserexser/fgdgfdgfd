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
    private static int mode = 0; // 0=Vanilla,1=Grim,2=Matrix,3=Polar,4=AAC,5=Grim2,6=Matrix2
    private static final String[] MODES = {"Vanilla", "Grim", "Matrix", "Polar", "AAC", "Grim2", "Matrix2"};
    private static Vec3d frozenPos = Vec3d.ZERO;
    private static int tickCounter = 0;
    private static final Random random = new Random();
    private static long lastPacketTime = 0;

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
                        tickCounter = 0;
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
        long now = System.currentTimeMillis();

        switch (mode) {
            case 0: // Vanilla – принудительная позиция и обнуление скорости
                mc.player.setPosition(frozenPos);
                mc.player.setVelocity(Vec3d.ZERO);
                mc.player.input.movementForward = 0;
                mc.player.input.movementSideways = 0;
                break;

            case 1: // Grim – отправка пакетов onGround=true каждые 2 тика
                if (tickCounter % 2 == 0) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(frozenPos.x, frozenPos.y, frozenPos.z, true, false));
                }
                mc.player.setPosition(frozenPos);
                break;

            case 2: // Matrix – заморозка через комбинацию setPosition + блокировка движения
                mc.player.setPosition(frozenPos);
                mc.player.setVelocity(0, 0, 0);
                if (tickCounter % 3 == 0) {
                    mc.player.setPosition(frozenPos.add(0, 0.001, 0));
                    mc.player.setPosition(frozenPos);
                }
                break;

            case 3: // Polar – использование случайного шума и отправка пакетов с задержкой
                if (now - lastPacketTime > 200) {
                    double noise = random.nextDouble() * 0.01;
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(frozenPos.x + noise, frozenPos.y, frozenPos.z + noise, true, false));
                    lastPacketTime = now;
                }
                mc.player.setPosition(frozenPos);
                break;

            case 4: // AAC – телепортация с обнулением вертикальной скорости каждые 2 тика
                if (tickCounter % 2 == 0) {
                    mc.player.setVelocity(0, -0.01, 0);
                }
                mc.player.setPosition(frozenPos);
                break;

            case 5: // Grim2 – более агрессивная отправка пакетов + блокировка ввода
                mc.player.setPosition(frozenPos);
                mc.player.setVelocity(0, 0, 0);
                for (int i = 0; i < 2; i++) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(frozenPos.x, frozenPos.y, frozenPos.z, true, false));
                }
                break;

            case 6: // Matrix2 – чередование позиции + отправка пакетов с onGround=false
                if (tickCounter % 2 == 0) {
                    mc.player.setPosition(frozenPos.add(0, 0.02, 0));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(frozenPos.x, frozenPos.y + 0.02, frozenPos.z, false, false));
                } else {
                    mc.player.setPosition(frozenPos);
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(frozenPos.x, frozenPos.y, frozenPos.z, true, false));
                }
                break;
        }
    }

    // ========== GUI для выбора режима ==========
    static class ModeMenu extends Screen {
        protected ModeMenu() {
            super(Text.literal("Freeze Mode"));
        }

        @Override
        protected void init() {
            super.init();
            int cx = width / 2;
            int y = height / 2 - (MODES.length * 12);
            for (int i = 0; i < MODES.length; i++) {
                final int idx = i;
                addDrawableChild(ButtonWidget.builder(Text.literal(MODES[i] + (mode == idx ? " ✓" : "")), btn -> {
                    mode = idx;
                    menuOpen = false;
                    close();
                }).dimensions(cx - 75, y + i * 24, 150, 20).build());
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> {
                menuOpen = false;
                close();
            }).dimensions(cx - 75, y + MODES.length * 24 + 10, 150, 20).build());
        }

        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            ctx.fill(0, 0, width, height, 0xCC000000);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Select Freeze Mode"), width / 2, height / 2 - 70, 0xFFFFFF);
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

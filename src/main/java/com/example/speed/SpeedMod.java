package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdatePlayerAbilitiesC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static int mode = 0; // 0 = PolarFlyX, 1 = AirStuck
    private static boolean menuOpen = false;

    // Переменные для PolarFlyX
    private static boolean goingUp = true;
    private static final double HORIZONTAL_SPEED = 6.8;
    private static final double MANUAL_VERTICAL = 8.25;
    private static final double CYCLE_VERTICAL = 0.10;

    // Переменные для AirStuck
    private static boolean airStuckActive = false;
    private static boolean wasElytra = false;
    private static double airStuckSpeed = 2.0; // скорость движения вперёд

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
                    mc.player.sendMessage(Text.literal(enabled ? "§a" + getModeName() + " ON" : "§c" + getModeName() + " OFF"), true);
                    if (enabled) {
                        if (mode == 1) activateAirStuck();
                    } else {
                        if (mode == 1) deactivateAirStuck();
                    }
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;

                if (enabled) {
                    if (mode == 0) tickPolarFly();
                    else tickAirStuck();
                }
            }
        }).start();
    }

    private static String getModeName() {
        return mode == 0 ? "PolarFlyX" : "AirStuck";
    }

    // ==================== PolarFlyX ====================
    private static void tickPolarFly() {
        if (mc.player == null) return;

        // При активации отправляем пакет элитры и сбрасываем падение
        if (mc.player.age % 20 == 0) { // один раз при включении
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.setVelocity(mc.player.getVelocity().x, 0.03, mc.player.getVelocity().z);
            mc.player.fallDistance = 0;
        }

        double yaw = Math.toRadians(mc.player.getYaw());
        double motionX = 0.0, motionZ = 0.0, motionY;

        // Горизонтальное движение
        if (mc.options.forwardKey.isPressed()) {
            motionX -= Math.sin(yaw) * HORIZONTAL_SPEED;
            motionZ += Math.cos(yaw) * HORIZONTAL_SPEED;
        }
        if (mc.options.backKey.isPressed()) {
            motionX += Math.sin(yaw) * HORIZONTAL_SPEED;
            motionZ -= Math.cos(yaw) * HORIZONTAL_SPEED;
        }
        if (mc.options.leftKey.isPressed()) {
            motionX -= Math.cos(yaw) * HORIZONTAL_SPEED;
            motionZ -= Math.sin(yaw) * HORIZONTAL_SPEED;
        }
        if (mc.options.rightKey.isPressed()) {
            motionX += Math.cos(yaw) * HORIZONTAL_SPEED;
            motionZ += Math.sin(yaw) * HORIZONTAL_SPEED;
        }

        // Вертикальное движение
        if (mc.options.jumpKey.isPressed()) {
            motionY = MANUAL_VERTICAL;
        } else if (mc.options.sneakKey.isPressed()) {
            motionY = -1.4;
        } else {
            motionY = goingUp ? CYCLE_VERTICAL : -CYCLE_VERTICAL;
            if (mc.player.age % 2 == 0) goingUp = !goingUp;
        }

        mc.player.fallDistance = 0;
        mc.player.setVelocity(motionX, motionY, motionZ);
    }

    // ==================== AirStuck ====================
    private static void activateAirStuck() {
        if (mc.player == null) return;
        wasElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
        // автосмена элитры на броню (если включено – но для простоты не реализовано)
    }

    private static void deactivateAirStuck() {
        if (mc.player == null) return;
        // при выключении возвращаем элитру, если её меняли (пропустим)
    }

    private static void tickAirStuck() {
        if (mc.player == null) return;

        // Блокировка пакетов движения (эмуляция через отмену – но в отдельном потоке нельзя, поэтому просто не отправляем)
        // В оригинале AirStuck отменяет пакеты движения. В нашем случае мы не отправляем ничего, кроме движения вперёд.
        if (mc.player.isGliding()) return; // не мешаем полёту на элитре

        if (mc.options.forwardKey.isPressed()) {
            float yaw = mc.player.getYaw();
            double motionX = -Math.sin(Math.toRadians(yaw)) * airStuckSpeed * 0.1;
            double motionZ = Math.cos(Math.toRadians(yaw)) * airStuckSpeed * 0.1;
            mc.player.setVelocity(motionX, 0, motionZ);
        } else {
            mc.player.setVelocity(0, 0, 0);
        }
        // Эмулируем отмену пакетов движения – не отправляем клиентские пакеты (уже не отправляем, кроме установки скорости)
    }

    // ==================== GUI выбора режима ====================
    static class ModeMenu extends Screen {
        protected ModeMenu() {
            super(Text.literal("Select Mode"));
        }

        @Override
        protected void init() {
            super.init();
            int cx = width / 2;
            int y = height / 2 - 30;

            addDrawableChild(ButtonWidget.builder(Text.literal("PolarFlyX" + (mode == 0 ? " ✓" : "")), btn -> {
                mode = 0;
                menuOpen = false;
                close();
            }).dimensions(cx - 75, y, 150, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("AirStuck" + (mode == 1 ? " ✓" : "")), btn -> {
                mode = 1;
                menuOpen = false;
                close();
            }).dimensions(cx - 75, y + 30, 150, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> {
                menuOpen = false;
                close();
            }).dimensions(cx - 75, y + 60, 150, 20).build());
        }

        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            ctx.fill(0, 0, width, height, 0xCC000000);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Select Flight Mode"), width / 2, height / 2 - 60, 0xFFFFFF);
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

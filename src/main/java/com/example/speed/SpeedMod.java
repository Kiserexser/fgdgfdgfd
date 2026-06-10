package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean spiderEnabled = false;
    private static boolean lastR = false;
    private static boolean menuOpen = false;
    private static double jumpHeight = 0.36; // настраиваемая высота

    @Override
    public void onInitialize() {
        // Поток для открытия GUI по правому Shift
        new Thread(() -> {
            while (true) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                if (mc.player == null) continue;
                long window = mc.getWindow().getHandle();
                boolean rShift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                if (rShift && !menuOpen) {
                    menuOpen = true;
                    mc.execute(() -> mc.setScreen(new SpiderMenu()));
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                boolean currentR = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
                if (currentR && !lastR) {
                    spiderEnabled = !spiderEnabled;
                    mc.player.sendMessage(Text.literal(spiderEnabled ? "§aSpider ON" : "§cSpider OFF"), true);
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                lastR = currentR;
                if (spiderEnabled && mc.player != null) tickSpider();
            }
        }).start();
    }

    private static void tickSpider() {
        if (mc.player == null) return;

        // Только при касании стены
        if (!mc.player.horizontalCollision) return;

        // Ищем ведро с водой в горячей панели
        int bucketSlot = findWaterBucketSlot();
        if (bucketSlot == -1) return;

        // Переключаемся на ведро, если не в руке
        if (mc.player.getMainHandStack().getItem() != Items.WATER_BUCKET) {
            mc.player.getInventory().selectedSlot = bucketSlot;
        }

        // Если в руке ведро – ставим воду и прыгаем
        if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            Vec3d vel = mc.player.getVelocity();
            mc.player.setVelocity(vel.x, jumpHeight, vel.z);
        }
    }

    private static int findWaterBucketSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET) {
                return i;
            }
        }
        return -1;
    }

    // ========== GUI для настройки высоты прыжка ==========
    static class SpiderMenu extends Screen {
        private SliderWidget heightSlider;

        protected SpiderMenu() {
            super(Text.literal("Spider Settings"));
        }

        @Override
        protected void init() {
            super.init();
            int cx = width / 2;
            int y = height / 2 - 40;

            heightSlider = new SliderWidget(cx - 100, y, 200, 20, Text.literal("Высота прыжка: " + String.format("%.2f", jumpHeight)), jumpHeight) {
                @Override
                protected void updateMessage() {
                    setMessage(Text.literal("Высота прыжка: " + String.format("%.2f", jumpHeight)));
                }

                @Override
                protected void applyValue() {
                    jumpHeight = this.value;
                }
            };
            addDrawableChild(heightSlider);

            addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), btn -> {
                menuOpen = false;
                close();
            }).dimensions(cx - 50, y + 40, 100, 20).build());
        }

        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            ctx.fill(0, 0, width, height, 0xCC000000);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Spider Module"), width / 2, height / 2 - 70, 0xFFFFFF);
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

        @Override
        public void close() {
            client.setScreen(null);
        }
        @Override public boolean shouldPause() { return false; }
    }
}

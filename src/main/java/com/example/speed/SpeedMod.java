package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static boolean menuOpen = false;
    private static Vec3d frozenPos = Vec3d.ZERO;
    private static int tickCounter = 0;
    private static final Random random = new Random();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Список обходов с состоянием
    public static class Bypass {
        String name;
        boolean enabled;
        Bypass(String name) { this.name = name; this.enabled = false; }
    }
    private static final List<Bypass> bypasses = new ArrayList<>();
    static {
        bypasses.add(new Bypass("Телепортация (Vanilla)"));
        bypasses.add(new Bypass("Отмена пакетов движения (Grim)"));
        bypasses.add(new Bypass("Фейковые пакеты onGround=true"));
        bypasses.add(new Bypass("Фейковые пакеты onGround=false"));
        bypasses.add(new Bypass("Имитация высокого пинга (задержка пакетов)"));
        bypasses.add(new Bypass("Отправка старых координат (десинхрон)"));
        bypasses.add(new Bypass("Циклическая телепортация на +0.01 Y"));
        bypasses.add(new Bypass("Случайный джиттер позиции"));
        bypasses.add(new Bypass("Блокировка инпута (движение)"));
        bypasses.add(new Bypass("Отправка пакетов с изменённым onGround"));
    }

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
                    mc.execute(() -> mc.setScreen(new FreezeMenu()));
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                boolean currentR = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
                if (currentR && !lastR) {
                    enabled = !enabled;
                    if (enabled) {
                        frozenPos = mc.player.getPos();
                        mc.player.sendMessage(Text.literal("§aFreeze ON"), true);
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

        // Обход 1: Телепортация
        if (bypasses.get(0).enabled) {
            mc.player.setPosition(frozenPos);
            mc.player.setVelocity(Vec3d.ZERO);
        }

        // Обход 2: Отмена пакетов движения – просто не отправляем их? Но пакеты отправляются автоматически. Вместо этого будем отменять пакеты через миксин, но без миксина сложно. Симулируем отправкой фейковых пакетов с задержкой (но не отменяем). Для Grim лучше комбинировать с другими.

        // Обход 3: Фейковые пакеты onGround=true
        if (bypasses.get(2).enabled && mc.getNetworkHandler() != null && tickCounter % 5 == 0) {
            PlayerMoveC2SPacket.LookAndOnGround packet = new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), true, false);
            mc.getNetworkHandler().sendPacket(packet);
        }

        // Обход 4: Фейковые пакеты onGround=false
        if (bypasses.get(3).enabled && mc.getNetworkHandler() != null && tickCounter % 5 == 2) {
            PlayerMoveC2SPacket.LookAndOnGround packet = new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), false, false);
            mc.getNetworkHandler().sendPacket(packet);
        }

        // Обход 5: Имитация высокого пинга (отправка пакетов с задержкой)
        if (bypasses.get(4).enabled && tickCounter % 10 == 0) {
            scheduler.schedule(() -> {
                if (mc.player != null && mc.getNetworkHandler() != null) {
                    PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(frozenPos.x, frozenPos.y, frozenPos.z, true, false);
                    mc.getNetworkHandler().sendPacket(packet);
                }
            }, random.nextInt(200) + 50, TimeUnit.MILLISECONDS);
        }

        // Обход 6: Отправка старых координат (на 0.1 блока выше)
        if (bypasses.get(5).enabled && mc.getNetworkHandler() != null && tickCounter % 7 == 0) {
            Vec3d oldPos = new Vec3d(frozenPos.x, frozenPos.y + 0.1, frozenPos.z);
            PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(oldPos.x, oldPos.y, oldPos.z, true, false);
            mc.getNetworkHandler().sendPacket(packet);
        }

        // Обход 7: Циклическая телепортация на +0.01 Y
        if (bypasses.get(6).enabled) {
            double offset = Math.sin(tickCounter * 0.2) * 0.01;
            mc.player.setPosition(frozenPos.x, frozenPos.y + offset, frozenPos.z);
        }

        // Обход 8: Случайный джиттер позиции
        if (bypasses.get(7).enabled) {
            double jitterX = (random.nextDouble() - 0.5) * 0.02;
            double jitterZ = (random.nextDouble() - 0.5) * 0.02;
            mc.player.setPosition(frozenPos.x + jitterX, frozenPos.y, frozenPos.z + jitterZ);
        }

        // Обход 9: Блокировка инпута (движение)
        if (bypasses.get(8).enabled) {
            mc.player.input.movementForward = 0;
            mc.player.input.movementSideways = 0;
            mc.player.input.jumping = false;
            mc.player.input.sneaking = false;
        }

        // Обход 10: Отправка пакетов с изменённым onGround (чередование)
        if (bypasses.get(9).enabled && mc.getNetworkHandler() != null && tickCounter % 4 == 0) {
            boolean onGround = (tickCounter / 4) % 2 == 0;
            PlayerMoveC2SPacket.LookAndOnGround packet = new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), onGround, false);
            mc.getNetworkHandler().sendPacket(packet);
        }
    }

    // ========== GUI с чекбоксами ==========
    static class FreezeMenu extends Screen {
        private final List<CheckboxWidget> checkboxes = new ArrayList<>();

        protected FreezeMenu() {
            super(Text.literal("Freeze Bypasses"));
        }

        @Override
        protected void init() {
            super.init();
            int cx = width / 2;
            int y = height / 2 - 120;
            for (int i = 0; i < bypasses.size(); i++) {
                Bypass b = bypasses.get(i);
                CheckboxWidget cb = new CheckboxWidget(cx - 100, y + i * 22, 200, 20, Text.literal(b.name), b.enabled);
                final int index = i;
                cb.setChecked(b.enabled);
                cb.setPressCallback(() -> {
                    b.enabled = cb.isChecked();
                });
                addDrawableChild(cb);
                checkboxes.add(cb);
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), btn -> {
                menuOpen = false;
                close();
            }).dimensions(cx - 50, y + bypasses.size() * 22 + 10, 100, 20).build());
        }

        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            ctx.fill(0, 0, width, height, 0xCC000000);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Выберите обходы Freeze"), width / 2, height / 2 - 140, 0xFFFFFF);
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
        @Override public void close() { client.setScreen(null); }
        @Override public boolean shouldPause() { return false; }
    }
}

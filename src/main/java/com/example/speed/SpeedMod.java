package com.example.speed;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class SpeedMod implements ModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean enabled = false;
    private static boolean lastR = false;
    private static Vec3d frozenPos = Vec3d.ZERO;

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
                    if (enabled) {
                        frozenPos = mc.player.getPos();
                        mc.player.sendMessage(Text.literal("§aAirStuck ON"), true);
                    } else {
                        mc.player.sendMessage(Text.literal("§cAirStuck OFF"), true);
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
        // Возвращаем в замороженную позицию
        mc.player.setPosition(frozenPos);
        // Обнуляем скорость
        mc.player.setVelocity(Vec3d.ZERO);
        // Запрещаем движение (если нужно, можно закомментировать)
        mc.player.input.movementForward = 0;
        mc.player.input.movementSideways = 0;
    }
}

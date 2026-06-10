package com.example.speed.mixin;

import com.example.speed.SpeedMod;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class SpeedModMixin {
    @Inject(method = "updateVelocity", at = @At("TAIL"))
    private void onUpdateVelocity(float speed, Vec3d movementInput, CallbackInfo ci) {
        // Здесь можно было бы вызвать EventOnMovePost, но мы уже реализовали его в основном классе.
        // Этот миксин не обязателен, если вы не используете EventManager. Оставлен как заглушка.
    }
}

// Отдельный миксин для перехвата пакета PlayerPositionLookS2CPacket
@Mixin(ClientPlayNetworkHandler.class)
public abstract class PlayerPositionLookMixin {
    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"))
    private void onPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        SpeedMod.onPlayerPositionLook();
    }
}

package com.example.healthbarsnametagfix.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// renders any entity's persistent name tag the same way EntityCulling renders occluded name tags:
// directly at the entity-iteration level, bypassing HealthBars' render() hook entirely
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void healthbarsnametagfix$renderPersistentNameTag(
            Entity entity,
            double camX,
            double camY,
            double camZ,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CallbackInfo callback
    ) {
        if (!(entity instanceof LivingEntity livingEntity)
                || !livingEntity.isCustomNameVisible()
                || !healthbarsnametagfix$isHealthBarsNotHandling(livingEntity)) {
            return;
        }

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (entity == dispatcher.crosshairPickEntity) {
            // crosshair-picking makes the vanilla path draw the name itself; avoid a double render
            return;
        }

        EntityRenderer<? super Entity> renderer = dispatcher.getRenderer(entity);
        if (!(renderer instanceof EntityRendererInvoker invoker)) {
            return;
        }

        Vec3 renderOffset = renderer.getRenderOffset(entity, partialTick);
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX()) - camX + renderOffset.x;
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) - camY + renderOffset.y;
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - camZ + renderOffset.z;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        invoker.healthbarsnametagfix$renderNameTag(
                entity,
                entity.getDisplayName(),
                poseStack,
                bufferSource,
                dispatcher.getPackedLightCoords(entity, partialTick),
                partialTick
        );
        poseStack.popPose();
    }

    // mirrors HealthBars' own draw decision so we only step in when it renders nothing for this entity;
    // uses reflection so this mod doesn't need HealthBars as a compile-time dependency
    private static boolean healthbarsnametagfix$isHealthBarsNotHandling(LivingEntity entity) {
        try {
            Class<?> healthBarsClass = Class.forName("fuzs.healthbars.HealthBars");
            Object configHolder = healthBarsClass.getField("CONFIG").get(null);
            Class<?> clientConfigClass = Class.forName("fuzs.healthbars.config.ClientConfig");
            Object config = configHolder.getClass()
                    .getMethod("get", Class.class)
                    .invoke(configHolder, clientConfigClass);

            Object anyRendering = clientConfigClass.getField("anyRendering").get(config);
            boolean anyRenderingActive = (Boolean) anyRendering.getClass().getMethod("get").invoke(anyRendering);
            boolean levelRenderingActive = clientConfigClass.getField("levelRendering").getBoolean(config);
            boolean isEntityAllowed = (Boolean) clientConfigClass
                    .getMethod("isEntityAllowed", LivingEntity.class)
                    .invoke(config, entity);

            return !(anyRenderingActive && levelRenderingActive) || !isEntityAllowed;
        } catch (ReflectiveOperationException | ClassCastException exception) {
            return true;
        }
    }
}

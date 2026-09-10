package com.example.healthbarsnametagfix.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fuzs.healthbars.HealthBars;
import fuzs.healthbars.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// renders MineColonies' persistent name tag the same way EntityCulling renders occluded name tags:
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
                || !healthbarsnametagfix$isMineColoniesCitizenOrVisitor(entity)
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

    private static boolean healthbarsnametagfix$isMineColoniesCitizenOrVisitor(Entity entity) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return "minecolonies".equals(entityId.getNamespace())
                && ("citizen".equals(entityId.getPath()) || "visitor".equals(entityId.getPath()));
    }

    // mirrors HealthBars' own draw decision so we only step in when it renders nothing for this entity
    private static boolean healthbarsnametagfix$isHealthBarsNotHandling(LivingEntity entity) {
        ClientConfig config = HealthBars.CONFIG.get(ClientConfig.class);
        boolean healthBarsActive = config.anyRendering.get() && config.levelRendering;
        return !healthBarsActive || !config.isEntityAllowed(entity);
    }
}

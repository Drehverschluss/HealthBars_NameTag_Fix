package com.example.healthbarsnametagfix.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderer.class)
public interface EntityRendererInvoker {
    @Invoker("shouldShowName")
    boolean healthbarsnametagfix$shouldShowName(Entity entity);

    @Invoker("renderNameTag")
    void healthbarsnametagfix$renderNameTag(
            Entity entity,
            Component nameTag,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float partialTick
    );
}
package com.kalioupo.equipment.mixin.client;

import com.kalioupo.equipment.client.ShieldSpecialRenderer;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers our shield special model renderer.
 *
 * The special model renderer table is built inside {@code bootstrap()}; the codec it exposes is
 * late bound, so adding our entry at the end of that method still makes
 * {@code "type": "kalioupo_equipment:shield"} resolve in item model json.
 */
@Mixin(SpecialModelRenderers.class)
public class SpecialModelRenderersMixin {
    @Shadow
    private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void kalioupo$registerShieldRenderer(CallbackInfo ci) {
        ID_MAPPER.put(Identifier.fromNamespaceAndPath("kalioupo_equipment", "shield"),
                ShieldSpecialRenderer.Unbaked.CODEC);
    }
}

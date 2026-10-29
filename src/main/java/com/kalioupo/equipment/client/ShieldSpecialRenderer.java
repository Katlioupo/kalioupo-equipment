package com.kalioupo.equipment.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Draws a shield with our own texture, the same way vanilla draws its shield.
 *
 * Vanilla shields go through a {@code SpecialModelRenderer}, which draws the model in centred
 * model space; a normal item model instead gets shifted by half a block right before its quads
 * are submitted. Going through the same path means the vanilla display transforms apply as is,
 * so the shield sits on the hand exactly like a vanilla shield does.
 */
public class ShieldSpecialRenderer implements SpecialModelRenderer<Unit> {
    private static final Vector3fc[] EXTENTS = {
            new Vector3f(-0.375f, -0.6875f, -0.125f),
            new Vector3f(0.375f, -0.6875f, -0.125f),
            new Vector3f(0.375f, 0.6875f, -0.125f),
            new Vector3f(-0.375f, 0.6875f, -0.125f),
            new Vector3f(-0.375f, -0.6875f, -0.0625f),
            new Vector3f(0.375f, -0.6875f, -0.0625f),
            new Vector3f(0.375f, 0.6875f, -0.0625f),
            new Vector3f(-0.375f, 0.6875f, -0.0625f),
    };

    private final Identifier texture;

    public ShieldSpecialRenderer(Identifier texture) {
        this.texture = texture;
    }

    @Override
    public void submit(Unit unit, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay,
                       boolean foil, int outline) {
        RenderType type = foil ? RenderTypes.entitySolidGlint(this.texture) : RenderTypes.entitySolid(this.texture);

        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            for (float[] quad : ShieldGeometry.QUADS) {
                this.emit(buffer, pose, quad, light, overlay, false);
                // the same quad wound the other way, so back face culling can never hide a face
                this.emit(buffer, pose, quad, light, overlay, true);
            }
        });
    }

    private void emit(VertexConsumer buffer, PoseStack.Pose pose, float[] quad, int light, int overlay,
                      boolean reversed) {
        for (int i = 0; i < 4; i++) {
            int corner = reversed ? 3 - i : i;
            int o = 3 + corner * 5;

            buffer.addVertex(pose, quad[o], quad[o + 1], quad[o + 2])
                    .setColor(255, 255, 255, 255)
                    .setUv(quad[o + 3], quad[o + 4])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, quad[0], quad[1], quad[2]);
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        for (Vector3fc extent : EXTENTS) {
            consumer.accept(extent);
        }
    }

    @Override
    public Unit extractArgument(ItemStack stack) {
        return Unit.INSTANCE;
    }

    public record Unbaked(Identifier texture) implements SpecialModelRenderer.Unbaked<Unit> {
        public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<Unit> bake(SpecialModelRenderer.BakingContext context) {
            return new ShieldSpecialRenderer(this.texture);
        }
    }
}

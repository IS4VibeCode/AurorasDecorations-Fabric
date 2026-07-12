/*
 * Copyright (c) 2021 LambdAurora <email@lambdaurora.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.aurorasdeco.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * {@code renderLeash} moved from {@code MobEntityRenderer}/{@code LivingEntityRenderer} up to the
 * base {@code EntityRenderer<T>} class in 1.21 (java/CLAUDE.md §3o), alongside the broader
 * {@code Leashable} interface rework -- confirmed by checking every class in
 * {@code MobEntityRenderer}'s real inheritance chain for the method.
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityRenderer.class)
public interface MobEntityRendererAccessor<T extends Entity> {
	@Invoker("renderLeash")
	<E extends Entity> void aurorasdeco$renderLeash(T entity, float tickDelta,
			MatrixStack matrices, VertexConsumerProvider provider,
			E holdingEntity);
}

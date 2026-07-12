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

import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

/**
 * {@code modelsToBake} is now keyed by {@link ModelIdentifier} rather than a bare {@code Identifier}
 * (java/CLAUDE.md §3o, confirmed against the real 1.21.1 ModelLoader class), and {@code putModel} was
 * renamed {@code addModelToBake} with the same key-type change -- callers now need to wrap a plain
 * part-model {@code Identifier} into a {@link ModelIdentifier} before calling either.
 */
@Environment(EnvType.CLIENT)
@Mixin(value = ModelLoader.class)
public interface ModelLoaderAccessor {
	@Accessor
	Map<ModelIdentifier, UnbakedModel> getModelsToBake();

	@Invoker("addModelToBake")
	void invokePutModel(ModelIdentifier id, UnbakedModel unbakedModel);
}

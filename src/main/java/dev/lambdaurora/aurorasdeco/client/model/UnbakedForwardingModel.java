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

package dev.lambdaurora.aurorasdeco.client.model;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Collection;
import java.util.function.Function;

/**
 * Represents an unbaked model that forwards another unbaked model.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
@Environment(EnvType.CLIENT)
public record UnbakedForwardingModel(UnbakedModel baseModel, Function<BakedModel, BakedModel> factory) implements AuroraUnbakedModel {
	@Override
	public Collection<Identifier> getModelDependencies() {
		return this.baseModel.getModelDependencies();
	}

	@Override
	public void setParents(Function<Identifier, UnbakedModel> models) {
		this.baseModel.setParents(models);
	}

	@Override
	public BakedModel bake(
			Baker loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer
	) {
		return this.factory.apply(this.baseModel.bake(loader, textureGetter, rotationContainer));
	}
}

/*
 * Copyright (c) 2023 LambdAurora <email@lambdaurora.dev>
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

package dev.lambdaurora.aurorasdeco.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lambdaurora.aurorasdeco.mixin.TransformSmithingRecipeAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SmithingTransformRecipe;

import java.util.stream.Stream;

/**
 * Represents a transform smithing recipe but that's actually good as it allows to dismiss the template item.
 * <p>
 * I just wanted to wax my blackboards Mojang.
 *
 * @author LambdAurora
 * @version 1.0.0-beta.13
 * @since 1.0.0-beta.13
 */
public class ActuallyGoodTransformSmithingRecipe extends SmithingTransformRecipe {
	public static final Serializer SERIALIZER = new Serializer();

	public ActuallyGoodTransformSmithingRecipe(Ingredient base, Ingredient addition, ItemStack result) {
		super(Ingredient.EMPTY, base, addition, result);
	}

	@Override
	public boolean isEmpty() {
		return Stream.of(((TransformSmithingRecipeAccessor) this).getBase(), ((TransformSmithingRecipeAccessor) this).getAddition())
				.anyMatch(Ingredient::isEmpty);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	public static class Serializer implements RecipeSerializer<ActuallyGoodTransformSmithingRecipe> {
		public static final MapCodec<ActuallyGoodTransformSmithingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Ingredient.ALLOW_EMPTY_CODEC.fieldOf("base")
						.forGetter(recipe -> ((TransformSmithingRecipeAccessor) recipe).getBase()),
				Ingredient.ALLOW_EMPTY_CODEC.fieldOf("addition")
						.forGetter(recipe -> ((TransformSmithingRecipeAccessor) recipe).getAddition()),
				ItemStack.VALIDATED_CODEC.fieldOf("result")
						.forGetter(recipe -> ((TransformSmithingRecipeAccessor) recipe).getResult())
		).apply(instance, ActuallyGoodTransformSmithingRecipe::new));

		public static final PacketCodec<net.minecraft.network.RegistryByteBuf, ActuallyGoodTransformSmithingRecipe> PACKET_CODEC = PacketCodec.tuple(
				Ingredient.PACKET_CODEC, recipe -> ((TransformSmithingRecipeAccessor) recipe).getBase(),
				Ingredient.PACKET_CODEC, recipe -> ((TransformSmithingRecipeAccessor) recipe).getAddition(),
				ItemStack.PACKET_CODEC, recipe -> ((TransformSmithingRecipeAccessor) recipe).getResult(),
				ActuallyGoodTransformSmithingRecipe::new
		);

		@Override
		public MapCodec<ActuallyGoodTransformSmithingRecipe> codec() {
			return CODEC;
		}

		@Override
		public PacketCodec<net.minecraft.network.RegistryByteBuf, ActuallyGoodTransformSmithingRecipe> packetCodec() {
			return PACKET_CODEC;
		}
	}
}

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

package dev.lambdaurora.aurorasdeco.recipe;

import dev.lambdaurora.aurorasdeco.blackboard.Blackboard;
import dev.lambdaurora.aurorasdeco.registry.AurorasDecoRegistry;
import dev.lambdaurora.aurorasdeco.registry.AurorasDecoTags;
import dev.lambdaurora.aurorasdeco.util.AuroraUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * Represents the blackboard clone recipe.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class BlackboardCloneRecipe extends SpecialCraftingRecipe {
	private static final Ingredient INPUT = Ingredient.fromTag(AurorasDecoTags.BLACKBOARD_ITEMS);
	private static final Ingredient OUTPUT = Ingredient.ofItems(
			AurorasDecoRegistry.BLACKBOARD_BLOCK,
			AurorasDecoRegistry.CHALKBOARD_BLOCK,
			AurorasDecoRegistry.GLASSBOARD_BLOCK
	);

	public BlackboardCloneRecipe(CraftingRecipeCategory craftingCategory) {
		super(craftingCategory);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		boolean hasInput = false, hasOutput = false;
		int count = 0;

		for (int slot = 0; slot < input.getSize(); ++slot) {
			var stack = input.getStackInSlot(slot);

			if (INPUT.test(stack)) {
				if (OUTPUT.test(stack) && !this.isInput(stack))
					hasOutput = true;
				else if (this.isInput(stack))
					hasInput = true;
				count++;
			}
		}
		return hasInput && hasOutput && count == 2;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registryLookup) {
		Blackboard blackboard = null;
		ItemStack output = null;
		Text customName = null;

		for (int slot = 0; slot < input.getSize(); ++slot) {
			var craftStack = input.getStackInSlot(slot);
			if (!craftStack.isEmpty()) {
				if (OUTPUT.test(craftStack) && !this.isInput(craftStack)) {
					output = craftStack;
				} else if (this.isInput(craftStack)) {
					var nbt = AuroraUtil.getBlockEntityNbt(craftStack);
					blackboard = Blackboard.fromNbt(nbt);
					var stackCustomName = craftStack.get(DataComponentTypes.CUSTOM_NAME);
					if (stackCustomName != null)
						customName = stackCustomName;
				}
			}
		}


		var out = output.copy();
		out.setCount(1);
		var nbt = AuroraUtil.getOrCreateBlockEntityNbt(out, AurorasDecoRegistry.BLACKBOARD_BLOCK_ENTITY_TYPE);
		blackboard.writeNbt(nbt);

		if (customName != null)
			out.set(DataComponentTypes.CUSTOM_NAME, customName);

		return out;
	}

	private boolean isInput(ItemStack stack) {
		var nbt = AuroraUtil.getBlockEntityNbt(stack);
		if (nbt != null) {
			if (nbt.contains("pixels", NbtElement.BYTE_ARRAY_TYPE)) {
				byte[] pixels = nbt.getByteArray("pixels");
				for (byte pixel : pixels) {
					if (pixel != 0) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public DefaultedList<ItemStack> getRemainder(CraftingRecipeInput input) {
		DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY);

		for (int i = 0; i < defaultedList.size(); ++i) {
			ItemStack invStack = input.getStackInSlot(i);
			if (!invStack.isEmpty()) {
				if (invStack.getItem().hasRecipeRemainder()) {
					defaultedList.set(i, new ItemStack(invStack.getItem().getRecipeRemainder()));
				} else if (this.isInput(invStack)) {
					ItemStack remainder = invStack.copy();
					remainder.setCount(1);
					defaultedList.set(i, remainder);
				}
			}
		}

		return defaultedList;
	}


	@Override
	public boolean fits(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AurorasDecoRegistry.BLACKBOARD_CLONE_RECIPE_SERIALIZER;
	}
}

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

package dev.lambdaurora.aurorasdeco.block;

import dev.lambdaurora.aurorasdeco.registry.AurorasDecoSounds;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Supplier;

@SuppressWarnings("deprecation")
public class FloweringAzaleaLogBlock extends PillarBlock {
	private final Supplier<Block> normalSupplier;

	public FloweringAzaleaLogBlock(Supplier<Block> normal, Settings settings) {
		super(settings);
		this.normalSupplier = normal;
	}

	/* Interaction */

	@Override
	public ItemActionResult onUseWithItem(ItemStack heldStack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (heldStack.isEmpty()) {
			return ItemActionResult.FAIL;
		}

		Item held = heldStack.getItem();
		if (!(held instanceof ShearsItem)) {
			return ItemActionResult.FAIL;
		}

		if (this.normalSupplier != null) {
			world.playSound(player, pos, AurorasDecoSounds.FLOWERING_SHEAR_SOUND_EVENT, SoundCategory.BLOCKS, 1.f, 1.f);

			if (player instanceof ServerPlayerEntity) {
				Criteria.ITEM_USED_ON_BLOCK.trigger((ServerPlayerEntity) player, pos, heldStack);
			}

			world.setBlockState(pos, this.normalSupplier.get().getDefaultState().with(PillarBlock.AXIS, state.get(PillarBlock.AXIS)), 11);

			heldStack.damage(1, player, LivingEntity.getSlotForHand(hand));

			return ItemActionResult.SUCCESS;
		}

		return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}
}

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

package dev.lambdaurora.aurorasdeco.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/**
 * Quilt's {@code BlockEntityType} kept a mutable {@code blocks} set with an
 * {@code addSupportedBlock(Block)} convenience -- needed by {@code WoodType.registerWoodTypeModificationCallback}
 * (java/CLAUDE.md §3b/§3c) to extend an already-built block entity type (sign posts, shelves) as new
 * wood types register. The real Yarn {@code BlockEntityType.blocks} is a plain {@code Set<Block>} with
 * no mutator, and confirmed via bytecode disassembly of {@code BlockEntityType.Builder.create} to hold
 * an immutable Guava {@code ImmutableSet}, so this accessor replaces the field with a fresh mutable set
 * rather than attempting to mutate the immutable one in place.
 */
@Mixin(BlockEntityType.class)
public interface BlockEntityTypeAccessor {
	@Accessor("blocks")
	Set<Block> getBlocks();

	@Accessor("blocks")
	@Mutable
	void setBlocks(Set<Block> blocks);
}

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

package dev.lambdaurora.aurorasdeco.mixin.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * A handful of previously-public AbstractBlock methods (getSoundGroup, getOutlineShape,
 * hasRandomTicks, randomTick, onEntityCollision, onBlockAdded, onStateReplaced,
 * emitsRedstonePower) became protected in 1.21.1, breaking every "wrapper block" in this codebase
 * (WallLanternBlock/SignPostBlock/BigPottedProxyBlock/etc.) that forwards behavior to a wrapped
 * Block instance outside its own class hierarchy -- confirmed via the real compiler errors, not
 * javap alone, since protected-vs-public is invisible to javap's default output.
 */
@Mixin(AbstractBlock.class)
public interface AbstractBlockAccessor {
	@Accessor
	AbstractBlock.Settings getSettings();

	@Mutable
	@Accessor
	void setSettings(AbstractBlock.Settings settings);

	@Invoker("getSoundGroup")
	BlockSoundGroup aurorasdeco$getSoundGroup(BlockState state);

	@Invoker("getOutlineShape")
	VoxelShape aurorasdeco$getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context);

	@Invoker("hasRandomTicks")
	boolean aurorasdeco$hasRandomTicks(BlockState state);

	@Invoker("randomTick")
	void aurorasdeco$randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random);

	@Invoker("onEntityCollision")
	void aurorasdeco$onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity);

	@Invoker("onBlockAdded")
	void aurorasdeco$onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify);

	@Invoker("onStateReplaced")
	void aurorasdeco$onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved);

	@Invoker("emitsRedstonePower")
	boolean aurorasdeco$emitsRedstonePower(BlockState state);

	@Invoker("onUse")
	net.minecraft.util.ActionResult aurorasdeco$onUse(BlockState state, World world, BlockPos pos,
			net.minecraft.entity.player.PlayerEntity player, net.minecraft.util.hit.BlockHitResult hit);
}

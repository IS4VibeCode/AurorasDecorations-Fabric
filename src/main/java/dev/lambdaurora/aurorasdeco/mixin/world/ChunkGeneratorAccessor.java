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

package dev.lambdaurora.aurorasdeco.mixin.world;

import com.mojang.datafixers.util.Pair;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

/**
 * Under Quilt Mappings these two private methods were named {@code findStructures}/{@code method_41522}
 * (matching the old {@code Holder}/{@code StructureFeature}/{@code StructureManager} naming family).
 * Under Yarn they're {@code locateConcentricRingsStructure}/{@code locateStructure}, and the equivalent
 * types are {@code RegistryEntry}/{@code net.minecraft.world.gen.structure.Structure}/
 * {@code StructureAccessor} — confirmed directly against the real 1.20.1 Yarn jar via javap, not guessed.
 */
@Mixin(ChunkGenerator.class)
public interface ChunkGeneratorAccessor {
	@Invoker("locateConcentricRingsStructure")
	@Nullable Pair<BlockPos, RegistryEntry<Structure>> invokeFindStructures(
			Set<RegistryEntry<Structure>> structures,
			ServerWorld world,
			StructureAccessor structureAccessor,
			BlockPos pos,
			boolean skipExistingChunks,
			ConcentricRingsStructurePlacement placement
	);

	@Invoker("locateStructure")
	@Nullable
	static Pair<BlockPos, RegistryEntry<Structure>> invokeLocateStructure(
			Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, boolean skipExistingChunks,
			StructurePlacement placement, ChunkPos pos
	) {
		throw new IllegalStateException("Mixin injection failed.");
	}
}

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

import net.minecraft.block.BlockState;
import net.minecraft.world.poi.PointOfInterestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/**
 * Quilt's {@code PointOfInterestHelper.addBlockStates} let a mod extend an already-registered POI type
 * (used here to make sleeping bags count as beds for the vanilla {@code minecraft:home} POI). No real
 * Yarn equivalent exists -- {@code PointOfInterestType} is a record with a plain, non-mutable
 * {@code Set<BlockState> blockStates} field, so this replaces the field with a fresh mutable set,
 * same approach as {@link BlockEntityTypeAccessor}.
 */
@Mixin(PointOfInterestType.class)
public interface PointOfInterestTypeAccessor {
	@Accessor("blockStates")
	@Mutable
	void setBlockStates(Set<BlockState> blockStates);
}

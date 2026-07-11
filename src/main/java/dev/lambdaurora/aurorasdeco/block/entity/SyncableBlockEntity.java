/*
 * Copyright (c) 2021 - 2023 LambdAurora <email@lambdaurora.dev>
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

package dev.lambdaurora.aurorasdeco.block.entity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.Objects;

/**
 * Replaces QSL's {@code QuiltBlockEntity} (a bare convenience interface, no Fabric equivalent
 * needed) -- provides the same easy-syncing default method it did.
 */
public interface SyncableBlockEntity {
	/**
	 * Attempts to synchronize the block entity data to the client.
	 *
	 * @throws IllegalStateException if called on the logical client
	 * @throws NullPointerException  if there's no world associated with the block entity
	 */
	default void sync() {
		if (this instanceof BlockEntity blockEntity) {
			World world = blockEntity.getWorld();

			Objects.requireNonNull(world); // Maintain distinct failure case from below.
			if (world instanceof ServerWorld serverWorld) {
				serverWorld.getChunkManager().markForUpdate(blockEntity.getPos());
			} else {
				throw new UnsupportedOperationException("Cannot call sync() on the logical client!");
			}
		} else {
			throw new IllegalStateException("SyncableBlockEntity has been implemented onto a non-BlockEntity class, please override sync().");
		}
	}
}

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

package dev.lambdaurora.aurorasdeco.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lambdaurora.aurorasdeco.mixin.entity.FoxEntityAccessor;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.predicate.BlockPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Criterion conditions moved from manual JSON parsing (getId()/conditionsFromJson()) to a codec-based
 * shape in 1.21 (java/CLAUDE.md §3o) -- AbstractCriterion<T> now just needs getConditionsCodec(), and
 * T is a record implementing AbstractCriterion.Conditions, matching the shape of every real vanilla
 * criterion (e.g. UsedTotemCriterion.Conditions) checked directly against the real 1.21.1 classes.
 */
public class PetUsePetBedCriterion extends AbstractCriterion<PetUsePetBedCriterion.Conditions> {
	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(PathAwareEntity entity, ServerWorld world, BlockPos pos) {
		if (entity instanceof TameableEntity tameable) {
			LivingEntity player = tameable.getOwner();
			if (player != null)
				this.trigger((ServerPlayerEntity) player, world, pos);
		} else if (entity instanceof FoxEntityAccessor fox) { // Foxes <3
			List<UUID> trusted = fox.aurorasdeco$getTrustedUuids();
			if (!trusted.isEmpty()) {
				var player = world.getPlayerByUuid(trusted.get(0));
				if (player != null)
					this.trigger((ServerPlayerEntity) player, world, pos);
			}
		}
	}

	public void trigger(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		this.trigger(player, conditions -> conditions.matches(world, pos));
	}

	public record Conditions(Optional<LootContextPredicate> player, BlockPredicate block)
			implements AbstractCriterion.Conditions {
		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				LootContextPredicate.CODEC.optionalFieldOf("player").forGetter(Conditions::player),
				BlockPredicate.CODEC.fieldOf("block").forGetter(Conditions::block)
		).apply(instance, Conditions::new));

		public boolean matches(ServerWorld world, BlockPos pos) {
			return this.block.test(world, pos);
		}
	}
}

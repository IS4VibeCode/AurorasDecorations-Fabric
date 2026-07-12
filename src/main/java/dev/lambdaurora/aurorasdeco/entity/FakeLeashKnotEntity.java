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

package dev.lambdaurora.aurorasdeco.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Represents a fake leash knot entity that can be leashed to real leash knot.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class FakeLeashKnotEntity extends MobEntity {
	private int obstructionCheckCounter;

	public FakeLeashKnotEntity(EntityType<? extends MobEntity> entityType, World world) {
		super(entityType, world);

		this.setPersistent();
		this.setAiDisabled(true);
	}

	/* Serialization */

	/*
	 * MobEntityAccessor's raw "leashNbt" compound field is gone in 1.21 -- leash state was formalized
	 * into the Leashable interface (java/CLAUDE.md §3o), which MobEntity now implements directly, with
	 * public getLeashData()/setLeashData() (no accessor mixin needed at all) and default
	 * readLeashDataFromNbt(NbtCompound)/writeLeashDataToNbt(NbtCompound, LeashData) methods replacing
	 * this class's old hand-rolled resolved/unresolved-holder NBT logic outright -- confirmed against
	 * the real 1.21.1 Leashable class that these defaults already handle both the "still unresolved,
	 * read from disk" and "resolved to a live entity" cases the old code branched on manually.
	 */
	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		if (nbt.contains(Leashable.LEASH_NBT_KEY)) {
			this.setLeashData(this.readLeashDataFromNbt(nbt));
		}
		this.setPersistent();
		this.setAiDisabled(true);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		if (this.getLeashData() != null) {
			this.writeLeashDataToNbt(nbt, this.getLeashData());
		}
	}

	/* Ticking */

	@Override
	public void tick() {
		super.tick();

		if (!this.getWorld().isClient()) {
			if (this.obstructionCheckCounter++ == 100) {
				this.obstructionCheckCounter = 0;

				var pos = this.getPos();
				double decimal = pos.y - (int) pos.y;
				double target = 0.375;
				double epsilon = 0.01;
				if (Math.abs(decimal - target) < epsilon) {
					this.setPosition(pos.x, ((int) pos.y) + target, pos.z);
				}

				if (!this.canStayAttached()) {
					this.breakAndDiscard(true);
				} else {
					var holding = this.getLeashHolder();
					if (holding == null || !holding.isAlive()) {
						this.breakAndDiscard(true);
					}
				}
			}
		}
	}

	/* Interaction */

	@Override
	public boolean damage(DamageSource source, float amount) {
		this.breakAndDiscard(!source.isSourceCreativePlayer());
		return true;
	}

	@Override
	public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
		return ActionResult.success(this.getWorld().isClient());
	}

	private void breakAndDiscard(boolean drop) {
		this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_LEASH_KNOT_BREAK, SoundCategory.BLOCKS,
				1.f, 1.f);
		if (this.isAlive() && this.getLeashHolder() != null && drop && !this.getWorld().isClient())
			this.dropItem(Items.LEAD, 1);
		this.discard();

		var holding = this.getLeashHolder();
		if (holding instanceof LeashKnotEntity)
			holding.discard();
	}

	public boolean canStayAttached() {
		var state = this.getWorld().getBlockState(this.getBlockPos());
		return state.isIn(BlockTags.FENCES);
	}
}

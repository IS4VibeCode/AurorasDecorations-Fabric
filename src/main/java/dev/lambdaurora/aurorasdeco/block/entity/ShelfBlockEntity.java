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

package dev.lambdaurora.aurorasdeco.block.entity;

import dev.lambdaurora.aurorasdeco.block.PartType;
import dev.lambdaurora.aurorasdeco.block.ShelfBlock;
import dev.lambdaurora.aurorasdeco.registry.AurorasDecoRegistry;
import dev.lambdaurora.aurorasdeco.screen.ShelfScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/**
 * Represents the shelf block entity.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class ShelfBlockEntity extends LootableContainerBlockEntity
		implements ExtendedScreenHandlerFactory<PartType>, SyncableBlockEntity {
	private DefaultedList<ItemStack> inventory;
	private boolean locked;

	public ShelfBlockEntity(BlockPos pos, BlockState state) {
		super(AurorasDecoRegistry.SHELF_BLOCK_ENTITY_TYPE, pos, state);
		this.inventory = DefaultedList.ofSize(8, ItemStack.EMPTY);
	}

	public boolean isLocked() {
		return this.locked;
	}

	@Override
	protected Text getContainerName() {
		return Text.translatable(this.getCachedState().getBlock().getTranslationKey());
	}

	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return new ShelfScreenHandler(syncId, playerInventory, this, this.getCachedState().get(ShelfBlock.TYPE));
	}

	/* Serialization */

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
		if (!this.readLootTable(nbt)) {
			Inventories.readNbt(nbt, this.inventory, registryLookup);
		}

		this.locked = nbt.getBoolean("locked");
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		if (!this.writeLootTable(nbt)) {
			Inventories.writeNbt(nbt, this.inventory, registryLookup);
		}

		nbt.putBoolean("locked", this.locked);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
		return this.createNbt(registryLookup);
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	/* Inventory */

	@Override
	public int size() {
		return 8;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		super.setStack(slot, stack);

		if (this.world != null && !this.world.isClient())
			this.sync();
	}

	@Override
	protected DefaultedList<ItemStack> getHeldStacks() {
		return this.inventory;
	}

	@Override
	protected void setHeldStacks(DefaultedList<ItemStack> list) {
		this.inventory = list;
	}

	@Override
	public boolean isValid(int slot, ItemStack stack) {
		return false;
	}

	@Override
	public PartType getScreenOpeningData(ServerPlayerEntity player) {
		return this.getCachedState().get(ShelfBlock.TYPE);
	}

	@Override
	public boolean checkUnlocked(PlayerEntity player) {
		return !this.locked && super.checkUnlocked(player);
	}
}

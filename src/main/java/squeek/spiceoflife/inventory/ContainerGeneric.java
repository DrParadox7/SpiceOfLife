package squeek.spiceoflife.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public abstract class ContainerGeneric extends Container {

    protected IInventory inventory;
    protected int nextSlotIndex = 0;
    protected boolean allowShiftClickToMultipleSlots = false;

    public ContainerGeneric(IInventory inventory) {
        this.inventory = inventory;
    }

    protected void addSlot(IInventory inventory, int xStart, int yStart) {
        addSlotOfType(Slot.class, inventory, xStart, yStart);
    }

    protected void addSlotOfType(Class<? extends Slot> slotClass, IInventory inventory, int xStart, int yStart) {
        addSlotsOfType(slotClass, inventory, xStart, yStart, 1, 1);
    }

    protected void addSlotsOfType(Class<? extends Slot> slotClass, IInventory inventory, int xStart, int yStart,
        int numSlots, int rows) {
        int numSlotsPerRow = numSlots / rows;
        for (int i = 0, col = 0, row = 0; i < numSlots; ++i, ++col) {
            if (col >= numSlotsPerRow) {
                row++;
                col = 0;
            }

            try {
                this.addSlotToContainer(
                    slotClass.getConstructor(IInventory.class, int.class, int.class, int.class)
                        .newInstance(inventory, getNextSlotIndex(), xStart + col * 18, yStart + row * 18));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected int getNextSlotIndex() {
        nextSlotIndex++;
        return nextSlotIndex - 1;
    }

    protected void addSlots(IInventory inventory, int xStart, int yStart) {
        addSlotsOfType(Slot.class, inventory, xStart, yStart, 1);
    }

    protected void addSlotsOfType(Class<? extends Slot> slotClass, IInventory inventory, int xStart, int yStart,
        int rows) {
        addSlotsOfType(slotClass, inventory, xStart, yStart, inventory.getSizeInventory(), rows);
    }

    protected void addSlots(IInventory inventory, int xStart, int yStart, int rows) {
        addSlotsOfType(Slot.class, inventory, xStart, yStart, rows);
    }

    protected void addSlots(IInventory inventory, int xStart, int yStart, int numSlots, int rows) {
        addSlotsOfType(Slot.class, inventory, xStart, yStart, numSlots, rows);
    }

    protected void addSlotsOfType(Class<? extends Slot> slotClass, IInventory inventory, int xStart, int yStart) {
        addSlotsOfType(slotClass, inventory, xStart, yStart, inventory.getSizeInventory(), 1);
    }

    protected void addPlayerInventorySlots(InventoryPlayer playerInventory, int yStart) {
        addPlayerInventorySlots(playerInventory, 8, yStart);
    }

    protected void addPlayerInventorySlots(InventoryPlayer playerInventory, int xStart, int yStart) {
        // inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(
                    new Slot(playerInventory, col + row * 9 + 9, xStart + col * 18, yStart + row * 18));
            }
        }

        // hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlotToContainer(new Slot(playerInventory, col, xStart + col * 18, yStart + 58));
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotNum) {
        Slot slot = (Slot) this.inventorySlots.get(slotNum);

        if (slot != null && slot.getHasStack()) {
            final ItemStack stackToTransfer = slot.getStack();

            // transferring from the container to the player inventory
            if (slotNum < this.inventory.getSizeInventory()) {
                if (!this.mergeItemStack(
                    stackToTransfer,
                    this.inventory.getSizeInventory(),
                    this.inventorySlots.size(),
                    true)) {
                    return null;
                }
            }
            // transferring from the player inventory into the container
            else {
                if (!this.mergeItemStack(stackToTransfer, 0, this.inventory.getSizeInventory(), false)) {
                    return null;
                }
            }

            if (stackToTransfer.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }

            // returning the remainder will attempt to fill any other valid slots with it
            if (allowShiftClickToMultipleSlots) return stackToTransfer;
        }

        // returning null stops it from attempting to fill consecutive slots with the remaining stack
        return null;
    }

    public int getEffectiveMaxStackSizeForSlot(int slotNum, ItemStack itemStack) {
        int effectiveMaxStackSize = itemStack.getMaxStackSize();
        if (slotNum < inventory.getSizeInventory())
            effectiveMaxStackSize = Math.min(effectiveMaxStackSize, this.inventory.getInventoryStackLimit());
        return effectiveMaxStackSize;
    }

    @Override
    public ItemStack slotClick(int slotNum, int mouseButton, int modifier, EntityPlayer player) {
        // Don't allow the swap if the slot isn't valid for the swapped item
        if (modifier == 2) {
            Slot slot1 = (Slot) this.inventorySlots.get(slotNum);
            ItemStack stack2 = player.inventory.getStackInSlot(mouseButton);

            if (stack2 != null && !slot1.isItemValid(stack2)) return slot1.getStack();
        }
        return super.slotClick(slotNum, mouseButton, modifier, player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return inventory.isUseableByPlayer(player);
    }

    @Override
    protected boolean mergeItemStack(ItemStack itemStack, int startSlotNum, int endSlotNum, boolean checkBackwards) {
        boolean didMerge = false;
        int k = startSlotNum;

        if (checkBackwards) {
            k = endSlotNum - 1;
        }

        Slot slot;
        ItemStack itemstack1;

        if (itemStack.isStackable()) {
            while (itemStack.stackSize > 0
                && (!checkBackwards && k < endSlotNum || checkBackwards && k >= startSlotNum)) {
                slot = (Slot) this.inventorySlots.get(k);
                itemstack1 = slot.getStack();

                if (itemstack1 != null && itemstack1.getItem() == itemStack.getItem()
                    && (!itemStack.getHasSubtypes() || itemStack.getItemDamage() == itemstack1.getItemDamage())
                    && ItemStack.areItemStackTagsEqual(itemStack, itemstack1)
                    && slot.isItemValid(itemStack)) {
                    int l = itemstack1.stackSize + itemStack.stackSize;
                    int effectiveMaxStackSize = getEffectiveMaxStackSizeForSlot(k, itemStack);

                    if (l <= effectiveMaxStackSize) {
                        itemStack.stackSize = 0;
                        itemstack1.stackSize = l;
                        slot.onSlotChanged();
                        didMerge = true;
                        break;
                    } else if (itemstack1.stackSize < effectiveMaxStackSize) {
                        itemStack.stackSize -= effectiveMaxStackSize - itemstack1.stackSize;
                        itemstack1.stackSize = effectiveMaxStackSize;
                        slot.onSlotChanged();
                        didMerge = true;
                        break;
                    }
                }

                if (checkBackwards) {
                    --k;
                } else {
                    ++k;
                }
            }
        }

        if (itemStack.stackSize > 0) {
            if (checkBackwards) {
                k = endSlotNum - 1;
            } else {
                k = startSlotNum;
            }

            while (!checkBackwards && k < endSlotNum || checkBackwards && k >= startSlotNum) {
                slot = (Slot) this.inventorySlots.get(k);
                itemstack1 = slot.getStack();

                if (itemstack1 == null && slot.isItemValid(itemStack)) {
                    int effectiveMaxStackSize = getEffectiveMaxStackSizeForSlot(k, itemStack);
                    ItemStack transferedStack = itemStack.copy();
                    if (transferedStack.stackSize > effectiveMaxStackSize)
                        transferedStack.stackSize = effectiveMaxStackSize;
                    slot.putStack(transferedStack);
                    slot.onSlotChanged();
                    itemStack.stackSize = itemStack.stackSize - transferedStack.stackSize;
                    didMerge = true;
                    break;
                }

                if (checkBackwards) {
                    --k;
                } else {
                    ++k;
                }
            }
        }

        return didMerge;
    }
}

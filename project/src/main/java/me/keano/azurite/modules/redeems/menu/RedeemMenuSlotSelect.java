package me.keano.azurite.modules.redeems.menu;

import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import me.keano.azurite.modules.redeems.EditorState;
import me.keano.azurite.modules.redeems.Redeem;
import me.keano.azurite.modules.redeems.RedeemManager;
import me.keano.azurite.utils.CC;
import me.keano.azurite.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RedeemMenuSlotSelect extends Menu {

    private final RedeemManager redeemManager;
    private final Redeem redeem;

    public RedeemMenuSlotSelect(MenuManager manager, Player player, RedeemManager redeemManager, Redeem redeem) {
        super(manager, player, CC.t("&8Select Slot (Shift + Right Click)"), redeemManager.getMenuSize(), false);
        this.redeemManager = redeemManager;
        this.redeem = redeem;
        this.fillEnabled = true;
        this.filler = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 0)
                .setName("&7Shift + Right Click to assign &e" + CC.t(redeem.getName()))
                .toItemStack();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        // Find occupied slots (other redeems that already have a slot assigned)
        Set<Integer> occupiedSlots = new HashSet<>();
        for (Redeem r : redeemManager.getRedeemsList()) {
            if (r.getId().equals(redeem.getId())) continue;
            if (r.getSlot() >= 0 && r.getSlot() < size) {
                occupiedSlots.add(r.getSlot());
            }
        }

        for (int i = 0; i < size; i++) {
            final int slot = i;
            final boolean occupied = occupiedSlots.contains(slot);

            if (occupied) {
                // Show occupied slot with a different appearance
                ItemStack occupiedItem = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 14)
                        .setName("&cOccupied Slot")
                        .setLore("&7This slot is already used by another redeem")
                        .toItemStack();

                buttons.put(slot + 1, new Button() {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        e.setCancelled(true);
                        if (e.getClick() == ClickType.SHIFT_RIGHT) {
                            player.sendMessage(CC.t("&cThis slot is already occupied by another redeem!"));
                        }
                    }

                    @Override
                    public ItemStack getItemStack() {
                        return occupiedItem.clone();
                    }
                });
            } else {
                buttons.put(slot + 1, new Button() {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        e.setCancelled(true);
                        if (e.getClick() == ClickType.SHIFT_RIGHT) {
                            redeem.setSlot(slot);
                            redeemManager.saveRedeem(redeem);
                            player.sendMessage(CC.t("&aSlot set to: &e" + slot));
                            player.closeInventory();
                            EditorState state = redeemManager.getEditorState(player.getUniqueId());
                            state.reset();
                            new RedeemIndividualMenu(getManager(), player, redeemManager, redeem).open();
                        }
                    }

                    @Override
                    public ItemStack getItemStack() {
                        return filler.clone();
                    }
                });
            }
        }

        return buttons;
    }
}

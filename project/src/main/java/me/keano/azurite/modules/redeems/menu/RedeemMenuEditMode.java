package me.keano.azurite.modules.redeems.menu;

import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import me.keano.azurite.modules.redeems.Redeem;
import me.keano.azurite.modules.redeems.RedeemManager;
import me.keano.azurite.utils.CC;
import me.keano.azurite.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RedeemMenuEditMode extends Menu {

    private final RedeemManager redeemManager;
    private final Set<Integer> lockedSlots;

    public RedeemMenuEditMode(MenuManager manager, Player player, RedeemManager redeemManager) {
        super(manager, player, CC.t(redeemManager.getMenuName()), redeemManager.getMenuSize(), false);
        this.redeemManager = redeemManager;
        this.allowInteract = true;
        this.lockedSlots = new HashSet<>();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        // Locked redeem slots - show the redeem items as they appear in /redeem
        for (Redeem redeem : redeemManager.getRedeemsList()) {
            if (redeem.getSlot() < 0 || redeem.getSlot() >= size) continue;

            final int slot = redeem.getSlot();
            lockedSlots.add(slot);

            ItemStack item = redeemManager.createRedeemItem(redeem);
            ItemBuilder builder = new ItemBuilder(item);
            builder.setName(CC.t(redeem.getName()));
            List<String> lore = new ArrayList<>();
            if (redeem.getLore() != null && !redeem.getLore().isEmpty()) {
                for (String line : redeem.getLore()) {
                    lore.add(line.replace("%uses%", String.valueOf(redeem.getUses())));
                }
            }
            builder.setLore(lore);

            buttons.put(slot + 1, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    e.setCancelled(true);
                    player.sendMessage(CC.t("&cThis redeem item is locked. Edit it through the Individual Redeem Editor."));
                }

                @Override
                public ItemStack getItemStack() {
                    return builder.toItemStack().clone();
                }
            });
        }

        // Load previously saved decorative items so they appear in edit mode
        for (Map.Entry<Integer, ItemStack> entry : redeemManager.getMenuItems().entrySet()) {
            int slot = entry.getKey();
            if (slot < 0 || slot >= size) continue;
            if (lockedSlots.contains(slot)) continue;

            final ItemStack savedItem = entry.getValue();
            buttons.put(slot + 1, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    // Allow free interaction with saved items
                }

                @Override
                public ItemStack getItemStack() {
                    return savedItem.clone();
                }
            });
        }

        return buttons;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() != null && e.getClickedInventory() != player.getInventory()) {
            int slot = e.getSlot();
            if (lockedSlots.contains(slot)) {
                e.setCancelled(true);
                player.sendMessage(CC.t("&cThis redeem item is locked. Edit it through the Individual Redeem Editor."));
                return;
            }
        }
    }

    @Override
    public void onClickOwn(InventoryClickEvent e) {
    }

    @Override
    public void onClose() {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (lockedSlots.contains(i)) continue;

            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                redeemManager.saveMenuItem(i, item);
            } else {
                if (redeemManager.getMenuItems().containsKey(i)) {
                    redeemManager.saveMenuItem(i, null);
                }
            }
        }
    }
}

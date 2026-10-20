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

public class RedeemMenu extends Menu {

    private final RedeemManager redeemManager;

    public RedeemMenu(MenuManager manager, Player player, RedeemManager redeemManager) {
        super(manager, player, CC.t(redeemManager.getMenuName()), redeemManager.getMenuSize(), false);
        this.redeemManager = redeemManager;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        // Place saved menu items first (from Edit Redeem Menu)
        for (Map.Entry<Integer, ItemStack> entry : redeemManager.getMenuItems().entrySet()) {
            int slot = entry.getKey();
            if (slot < 0 || slot >= size) continue;
            final ItemStack savedItem = entry.getValue();

            buttons.put(slot + 1, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    e.setCancelled(true);
                }

                @Override
                public ItemStack getItemStack() {
                    return savedItem.clone();
                }
            });
        }

        // Place redeem items
        for (Redeem redeem : redeemManager.getRedeemsList()) {
            if (redeem.getSlot() < 0 || redeem.getSlot() >= size) continue;

            boolean alreadyRedeemed = redeemManager.hasAnyRedeemed(player.getUniqueId());
            boolean redeemedThis = redeemManager.hasRedeemedSpecific(player.getUniqueId(), redeem.getId());
            ItemStack item = buildRedeemItem(redeem, alreadyRedeemed, redeemedThis);

            buttons.put(redeem.getSlot() + 1, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    e.setCancelled(true);

                    if (redeemedThis) {
                        String msg = CC.t(redeem.getAlreadyClaimedMessage()
                                .replace("%player%", player.getName())
                                .replace("%redeemname%", CC.t(redeem.getName())));
                        player.sendMessage(msg);
                        return;
                    }

                    if (alreadyRedeemed) {
                        player.sendMessage(CC.t("&cYou have already redeemed a redeem! You cannot redeem another unless an admin resets your redeems."));
                        return;
                    }

                    player.closeInventory();
                    redeemManager.executeRedeem(player, redeem);
                }

                @Override
                public ItemStack getItemStack() {
                    return item.clone();
                }
            });
        }

        return buttons;
    }

    @SuppressWarnings("deprecation")
    private ItemStack buildRedeemItem(Redeem redeem, boolean alreadyRedeemed, boolean redeemedThis) {
        ItemStack base = redeemManager.createRedeemItem(redeem);
        ItemBuilder builder = new ItemBuilder(base);

        builder.setName(CC.t(redeem.getName()));

        // Only show the lore exactly as configured, with %uses% replaced
        List<String> lore = new ArrayList<>();
        if (redeem.getLore() != null && !redeem.getLore().isEmpty()) {
            for (String line : redeem.getLore()) {
                lore.add(line.replace("%uses%", String.valueOf(redeem.getUses())));
            }
        }
        builder.setLore(lore);

        return builder.toItemStack();
    }
}

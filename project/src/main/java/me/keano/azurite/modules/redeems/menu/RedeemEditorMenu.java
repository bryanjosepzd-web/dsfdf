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
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RedeemEditorMenu extends Menu {

    private final RedeemManager redeemManager;

    private static final int[] DECORATION_SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35,
            36, 37, 38, 42, 43, 44
    };

    public RedeemEditorMenu(MenuManager manager, Player player, RedeemManager redeemManager) {
        super(manager, player, CC.t("&8Redeem Editor"), 45, false);
        this.redeemManager = redeemManager;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        ItemStack decoration = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 15)
                .setName("&7")
                .toItemStack();

        for (int slot : DECORATION_SLOTS) {
            final int s = slot;
            buttons.put(slot + 1, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    e.setCancelled(true);
                }

                @Override
                public ItemStack getItemStack() {
                    return decoration.clone();
                }
            });
        }

        buttons.put(5, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                player.closeInventory();
                EditorState state = redeemManager.getEditorState(player.getUniqueId());
                state.reset();
                state.setState(EditorState.State.CREATING_REDEEM);
                player.sendMessage(CC.t("&ePlease type a name for this redeem, or type &c\"cancel\" &eto cancel"));
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.NETHER_STAR)
                        .setName("&aCreate a Redeem")
                        .toItemStack();
            }
        });

        int[] redeemSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        List<Redeem> list = redeemManager.getRedeemsList();
        for (int i = 0; i < list.size() && i < redeemSlots.length; i++) {
            Redeem redeem = list.get(i);
            final int slot = redeemSlots[i];
            ItemStack item = buildRedeemEditorItem(redeem);

            buttons.put(slot + 1, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    e.setCancelled(true);
                    player.closeInventory();
                    new RedeemIndividualMenu(getManager(), player, redeemManager, redeem).open();
                }

                @Override
                public ItemStack getItemStack() {
                    return item.clone();
                }
            });
        }

        // Slot 41 - Edit Redeem Menu (anvil)
        buttons.put(42, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                player.closeInventory();
                player.sendMessage(CC.t("&eEdit Redeem Menu mode is now open. Place items freely."));
                new RedeemMenuEditMode(getManager(), player, redeemManager).open();
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.ANVIL)
                        .setName("&e&lEdit Redeem Menu")
                        .setLore(
                                "&7",
                                "&6Current Rows: &f" + redeemManager.getMenuRows(),
                                "&7",
                                "&eClick to edit."
                        )
                        .toItemStack();
            }
        });

        // Slot 39 - Edit Redeem Menu Name (paper)
        buttons.put(40, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                player.closeInventory();
                EditorState state = redeemManager.getEditorState(player.getUniqueId());
                state.setState(EditorState.State.ENTERING_MENU_NAME);
                player.sendMessage(CC.t("&aPlease type value."));
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.PAPER)
                        .setName("&e&lEdit Redeem Menu Name")
                        .setLore(
                                "&7",
                                "&6Current Name: &f" + CC.t(redeemManager.getMenuName()),
                                "&7",
                                "&eClick to edit."
                        )
                        .toItemStack();
            }
        });

        // Slot 40 - Edit Redeem Menu Rows (sign)
        buttons.put(41, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                player.closeInventory();
                EditorState state = redeemManager.getEditorState(player.getUniqueId());
                state.setState(EditorState.State.ENTERING_MENU_ROWS);
                player.sendMessage(CC.t("&aPlease input a value &f(1-6)"));
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.SIGN)
                        .setName("&e&lEdit Redeem Menu Rows")
                        .setLore(
                                "&7",
                                "&eClick to edit",
                                "&7"
                        )
                        .toItemStack();
            }
        });

        return buttons;
    }

    @SuppressWarnings("deprecation")
    private ItemStack buildRedeemEditorItem(Redeem redeem) {
        ItemStack base;
        if (redeem.isTextureEnabled() && !redeem.getTexture().isEmpty()) {
            base = redeemManager.createRedeemItem(redeem);
        } else {
            base = redeemManager.createRedeemItem(redeem);
        }

        ItemBuilder builder = new ItemBuilder(base);
        builder.setName(CC.t(redeem.getName()));

        List<String> lore = new ArrayList<>();
        if (redeem.getLore() != null && !redeem.getLore().isEmpty()) {
            for (String line : redeem.getLore()) {
                lore.add(line.replace("%uses%", String.valueOf(redeem.getUses())));
            }
        }
        lore.add("&7");
        lore.add("&eClick to edit");
        builder.setLore(lore);

        return builder.toItemStack();
    }
}

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

public class RedeemIndividualMenu extends Menu {

    private final RedeemManager redeemManager;
    private final Redeem redeem;

    public RedeemIndividualMenu(MenuManager manager, Player player, RedeemManager redeemManager, Redeem redeem) {
        super(manager, player, CC.t("&8Editing - " + CC.t(redeem.getName())), 27, false);
        this.redeemManager = redeemManager;
        this.redeem = redeem;
        this.fillEnabled = true;
        this.filler = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 15)
                .setName("&7")
                .toItemStack();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        EditorState state = redeemManager.getEditorState(player.getUniqueId());

        // Slot 10 - Edit Item (Texture Editor)
        buttons.put(11, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                if (e.getClick() == ClickType.MIDDLE) {
                    player.closeInventory();
                    state.setState(EditorState.State.SELECTING_ITEM);
                    state.setEditingRedeemId(redeem.getId());
                    player.sendMessage(CC.t("&aPlease Left Click the item you want to place..."));
                } else if (e.getClick() == ClickType.RIGHT) {
                    redeem.setTextureEnabled(!redeem.isTextureEnabled());
                    redeemManager.saveRedeem(redeem);
                    player.sendMessage(CC.t("&aTexture status: " + (redeem.isTextureEnabled() ? "&aYes" : "&cNo")));
                    update();
                } else if (e.getClick() == ClickType.LEFT) {
                    if (!redeem.isTextureEnabled()) {
                        player.sendMessage(CC.t("&cTo set a value, Texture Status must be enabled"));
                        return;
                    }
                    player.closeInventory();
                    state.setState(EditorState.State.ENTERING_TEXTURE);
                    state.setEditingRedeemId(redeem.getId());
                    state.setTextureBuffer(new StringBuilder());
                    player.sendMessage(CC.t("&aType value. Remember that there is a Chat Limit."));
                    player.sendMessage(CC.t("&aPut as much as you can and then put the rest. At the end, put Done."));
                }
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.SIGN)
                        .setName("&e&lEdit Item")
                        .setLore(
                                "&7",
                                "&6Texture Status: " + (redeem.isTextureEnabled() ? "&aYes" : "&cNo"),
                                "&7",
                                "&eRight Click to toggle texture",
                                "&eLeft Click to set a value",
                                "&bMiddle click to set another item"
                        )
                        .toItemStack();
            }
        });

        // Slot 11 - Redeem Slot
        buttons.put(12, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                player.closeInventory();
                state.setState(EditorState.State.SELECTING_SLOT);
                state.setEditingRedeemId(redeem.getId());
                player.sendMessage(CC.t("&aShift + Right Click an empty slot in the Redeem Menu to assign this redeem."));
                new RedeemMenuSlotSelect(getManager(), player, redeemManager, redeem).open();
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.ANVIL)
                        .setName("&e&lRedeem Slot")
                        .setLore(
                                "&7",
                                "&6Current Slot: &f" + (redeem.getSlot() < 0 ? "None" : redeem.getSlot()),
                                "&7",
                                "&eClick to edit."
                        )
                        .toItemStack();
            }
        });

        // Slot 12 - Commands
        buttons.put(13, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                if (e.getClick() == ClickType.SHIFT_LEFT) {
                    player.closeInventory();
                    state.setState(EditorState.State.ENTERING_COMMAND);
                    state.setEditingRedeemId(redeem.getId());
                    player.sendMessage(CC.t("&aPlease input a value"));
                } else if (e.getClick() == ClickType.SHIFT_RIGHT) {
                    if (!redeem.getCommands().isEmpty()) {
                        int idx = state.getSelectedCommandIndex();
                        if (idx >= 0 && idx < redeem.getCommands().size()) {
                            redeem.getCommands().remove(idx);
                            redeemManager.saveRedeem(redeem);
                            if (state.getSelectedCommandIndex() >= redeem.getCommands().size()) {
                                state.setSelectedCommandIndex(Math.max(0, redeem.getCommands().size() - 1));
                            }
                            player.sendMessage(CC.t("&cCommand removed."));
                            update();
                        }
                    }
                } else if (e.getClick() == ClickType.LEFT) {
                    if (!redeem.getCommands().isEmpty()) {
                        int idx = state.getSelectedCommandIndex() - 1;
                        if (idx < 0) idx = redeem.getCommands().size() - 1;
                        state.setSelectedCommandIndex(idx);
                        update();
                    }
                } else if (e.getClick() == ClickType.RIGHT) {
                    if (!redeem.getCommands().isEmpty()) {
                        int idx = state.getSelectedCommandIndex() + 1;
                        if (idx >= redeem.getCommands().size()) idx = 0;
                        state.setSelectedCommandIndex(idx);
                        update();
                    }
                }
            }

            @Override
            public ItemStack getItemStack() {
                List<String> lore = new ArrayList<>();
                lore.add("&7");
                lore.add("&7&lPlaceholders");
                lore.add("&7");
                lore.add("&6%player%");
                lore.add("&7");
                if (redeem.getCommands().isEmpty()) {
                    lore.add("&7No Commands");
                } else {
                    for (int i = 0; i < redeem.getCommands().size(); i++) {
                        String prefix = (i == state.getSelectedCommandIndex()) ? "&a" : "&7";
                        lore.add(prefix + i + ". &f" + redeem.getCommands().get(i));
                    }
                }
                lore.add("&7");
                lore.add("&eShift Left Click to add a new command");
                lore.add("&cShift Right Click to remove a command");
                lore.add("&eLeft and Right Click to Scroll");
                return new ItemBuilder(Material.COMMAND)
                        .setName("&e&lCommands")
                        .setLore(lore)
                        .toItemStack();
            }
        });

        // Slot 13 - Lore Text
        buttons.put(14, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                if (e.getClick() == ClickType.SHIFT_LEFT) {
                    player.closeInventory();
                    state.setState(EditorState.State.ENTERING_LORE);
                    state.setEditingRedeemId(redeem.getId());
                    player.sendMessage(CC.t("&aPlease input a value"));
                } else if (e.getClick() == ClickType.SHIFT_RIGHT) {
                    if (!redeem.getLore().isEmpty()) {
                        int idx = state.getSelectedLoreIndex();
                        if (idx >= 0 && idx < redeem.getLore().size()) {
                            redeem.getLore().remove(idx);
                            redeemManager.saveRedeem(redeem);
                            if (state.getSelectedLoreIndex() >= redeem.getLore().size()) {
                                state.setSelectedLoreIndex(Math.max(0, redeem.getLore().size() - 1));
                            }
                            player.sendMessage(CC.t("&cLore line removed."));
                            update();
                        }
                    }
                } else if (e.getClick() == ClickType.LEFT) {
                    if (!redeem.getLore().isEmpty()) {
                        int idx = state.getSelectedLoreIndex() - 1;
                        if (idx < 0) idx = redeem.getLore().size() - 1;
                        state.setSelectedLoreIndex(idx);
                        update();
                    }
                } else if (e.getClick() == ClickType.RIGHT) {
                    if (!redeem.getLore().isEmpty()) {
                        int idx = state.getSelectedLoreIndex() + 1;
                        if (idx >= redeem.getLore().size()) idx = 0;
                        state.setSelectedLoreIndex(idx);
                        update();
                    }
                } else if (e.getClick() == ClickType.MIDDLE) {
                    if (!redeem.getLore().isEmpty()) {
                        int idx = state.getSelectedLoreIndex();
                        if (idx >= 0 && idx < redeem.getLore().size()) {
                            player.closeInventory();
                            state.setState(EditorState.State.EDITING_LORE_LINE);
                            state.setEditingRedeemId(redeem.getId());
                            player.sendMessage(CC.t("&aType the new lore line or &fClick this message to copy current text"));
                        }
                    }
                }
            }

            @Override
            public ItemStack getItemStack() {
                List<String> lore = new ArrayList<>();
                lore.add("&7");
                lore.add("&7&lPlaceholders");
                lore.add("&6%uses%");
                lore.add("&7");
                if (redeem.getLore().isEmpty()) {
                    lore.add("&7No Lore");
                } else {
                    for (int i = 0; i < redeem.getLore().size(); i++) {
                        String prefix = (i == state.getSelectedLoreIndex()) ? "&a" : "&7";
                        lore.add(prefix + i + ". &e" + redeem.getLore().get(i));
                    }
                }
                lore.add("&7");
                lore.add("&aShift Left Click to add a new lore line");
                lore.add("&cShift Right Click to delete a lore line");
                lore.add("&eLeft and Right Click to Scroll");
                lore.add("&bMiddle Click to edit text");
                return new ItemBuilder(Material.SKULL_ITEM, 1, (byte) 1)
                        .setName("&e&lLore Text")
                        .setLore(lore)
                        .toItemStack();
            }
        });

        // Slot 14 - Item Name
        buttons.put(15, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                player.closeInventory();
                state.setState(EditorState.State.ENTERING_ITEM_NAME);
                state.setEditingRedeemId(redeem.getId());
                player.sendMessage(CC.t("&aType the new name or &fClick this message to copy current name"));
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.NAME_TAG)
                        .setName("&e&lItem Name")
                        .setLore(
                                "&7",
                                "&6Current Name: &f" + CC.t(redeem.getName()),
                                "&7",
                                "&eClick to edit"
                        )
                        .toItemStack();
            }
        });

        // Slot 15 - Claim Message
        buttons.put(16, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                player.closeInventory();
                state.setState(EditorState.State.ENTERING_CLAIM_MESSAGE);
                state.setEditingRedeemId(redeem.getId());
                player.sendMessage(CC.t("&aType the claim message. Use &e%redeemname% &aand &e%player% &aas placeholders."));
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.PAPER)
                        .setName("&e&lClaim Message")
                        .setLore(
                                "&7",
                                "&6Current: &f" + CC.t(redeem.getClaimMessage()),
                                "&7",
                                "&eClick to edit",
                                "&7Placeholders: %redeemname%, %player%"
                        )
                        .toItemStack();
            }
        });

        // Slot 16 - Already Claimed Message
        buttons.put(17, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                player.closeInventory();
                state.setState(EditorState.State.ENTERING_ALREADY_CLAIMED_MESSAGE);
                state.setEditingRedeemId(redeem.getId());
                player.sendMessage(CC.t("&aType the already-claimed message. Use &e%redeemname% &aand &e%player% &aas placeholders."));
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.BOOK)
                        .setName("&e&lAlready Claimed Message")
                        .setLore(
                                "&7",
                                "&6Current: &f" + CC.t(redeem.getAlreadyClaimedMessage()),
                                "&7",
                                "&eClick to edit",
                                "&7Placeholders: %redeemname%, %player%"
                        )
                        .toItemStack();
            }
        });

        // Slot 22 - Back
        buttons.put(23, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                player.closeInventory();
                new RedeemEditorMenu(getManager(), player, redeemManager).open();
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.BED)
                        .setName("&c&lBack")
                        .toItemStack();
            }
        });

        return buttons;
    }
}

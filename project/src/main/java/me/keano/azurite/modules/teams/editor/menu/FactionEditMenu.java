package me.keano.azurite.modules.teams.editor.menu;

import me.keano.azurite.modules.events.koth.Koth;
import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import me.keano.azurite.modules.teams.Team;
import me.keano.azurite.modules.teams.claims.Claim;
import me.keano.azurite.modules.teams.editor.FactionEditorManager;
import me.keano.azurite.modules.teams.enums.TeamType;
import me.keano.azurite.utils.CC;
import me.keano.azurite.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Per-faction edit menu - color editor, claims, metadata selector, and back button.
 * For koth (EVENT) teams, also shows capture time, capture zone, and koth loot.
 */
public class FactionEditMenu extends Menu {

    private final Team team;

    // 1-indexed button slots (Menu framework subtracts 1)
    private static final int COLOR_SLOT = 11;       // 0-indexed 10
    private static final int CLAIMS_SLOT = 12;      // 0-indexed 11
    private static final int METADATA_SLOT = 13;    // 0-indexed 12
    private static final int CAPTURE_TIME_SLOT = 14; // 0-indexed 13 (koth only)
    private static final int CAPTURE_ZONE_SLOT = 15; // 0-indexed 14 (koth only)
    private static final int KOTH_LOOT_SLOT = 16;    // 0-indexed 15 (koth only)
    private static final int BACK_SLOT = 23;        // 0-indexed 22

    // Base functional slots (always present)
    private static final Set<Integer> BASE_FUNCTIONAL = new HashSet<>(Arrays.asList(10, 11, 12, 22));

    // Koth functional slots (in addition to base)
    private static final Set<Integer> KOTH_FUNCTIONAL = new HashSet<>(Arrays.asList(13, 14, 15));

    public FactionEditMenu(MenuManager manager, Player player, Team team) {
        super(manager, player, CC.t("&8Editor - " + team.getName()), 27, false);
        this.team = team;
    }

    private FactionEditorManager getEditorManager() {
        return getInstance().getFactionEditorManager();
    }

    private boolean isKoth() {
        return team.getType() == TeamType.EVENT
                || team.getType() == TeamType.CONQUEST
                || team.getType() == TeamType.CITADEL;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        Set<Integer> functionalSlots = new HashSet<>(BASE_FUNCTIONAL);
        if (isKoth()) {
            functionalSlots.addAll(KOTH_FUNCTIONAL);
        }

        ItemStack pane = new ItemBuilder(Material.STAINED_GLASS_PANE)
                .data(getManager(), 15)
                .setName(" ")
                .toItemStack();

        for (int i = 0; i < 27; i++) {
            if (functionalSlots.contains(i)) continue;

            final int slot = i;
            int buttonSlot = slot + 1;
            buttons.put(buttonSlot, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    e.setCancelled(true);
                }

                @Override
                public ItemStack getItemStack() {
                    return pane;
                }
            });
        }

        buttons.put(COLOR_SLOT, createColorButton(player));
        buttons.put(CLAIMS_SLOT, createClaimsButton(player));
        buttons.put(METADATA_SLOT, createMetadataButton(player));

        if (isKoth()) {
            buttons.put(CAPTURE_TIME_SLOT, createCaptureTimeButton(player));
            buttons.put(CAPTURE_ZONE_SLOT, createCaptureZoneButton(player));
            buttons.put(KOTH_LOOT_SLOT, createKothLootButton(player));
        }

        buttons.put(BACK_SLOT, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                Player p = (Player) e.getWhoClicked();
                p.closeInventory();
                getEditorManager().openMainMenu(p);
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.BED)
                        .setName("&cGo Back")
                        .toItemStack();
            }
        });

        return buttons;
    }

    private Button createColorButton(Player player) {
        return new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                Player p = (Player) e.getWhoClicked();
                p.closeInventory();

                p.sendMessage(CC.t("&aEnter a display name for " + team.getName()));

                getEditorManager().awaitChatInput(p, (message) -> {
                    String colorCode = message;
                    if (!colorCode.startsWith("&") && !colorCode.startsWith("\u00a7")) {
                        if (colorCode.length() >= 1) {
                            colorCode = "&" + colorCode.charAt(0);
                        }
                    }

                    if (colorCode.length() >= 2) {
                        char c = Character.toLowerCase(colorCode.charAt(1));
                        if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f') {
                            getEditorManager().setFactionColor(team, colorCode);

                            // Also update koth color if this is a koth
                            Koth koth = getInstance().getKothManager().getKoth(team.getName());
                            if (koth != null) {
                                koth.setColor(CC.t(colorCode));
                                koth.save();
                            }

                            p.sendMessage(CC.t("&aColor set to " + colorCode + team.getName()));
                        } else {
                            p.sendMessage(CC.t("&cInvalid color code! Use format like &c, &d, &3, etc."));
                        }
                    } else {
                        p.sendMessage(CC.t("&cInvalid color code! Use format like &c, &d, &3, etc."));
                    }

                    getEditorManager().reopenEditMenu(p, team);
                });
            }

            @Override
            public ItemStack getItemStack() {
                String currentColor = team.getCustomColor();
                String currentDisplay = team.getDisplayName(player);

                List<String> lore = new ArrayList<>();
                lore.add("&7");
                if (currentColor != null && !currentColor.isEmpty()) {
                    lore.add("&eCurrent: " + currentDisplay);
                } else {
                    lore.add("&eCurrent: &cNone");
                }

                return new ItemBuilder(Material.ITEM_FRAME)
                        .setName("&6&lDisplay Name")
                        .setLore(lore)
                        .toItemStack();
            }
        };
    }

    private Button createClaimsButton(Player player) {
        return new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                Player p = (Player) e.getWhoClicked();

                if (e.getClick() == ClickType.LEFT) {
                    p.closeInventory();
                    getEditorManager().giveClaimWand(p, team);
                    p.sendMessage(CC.t("&eYou have been given a claim wand. Right-click blocks to select corners, shift-left-click to claim, right-click air to cancel."));
                    return;
                }

                if (e.getClick() == ClickType.RIGHT) {
                    boolean success = getEditorManager().unclaimAtPlayer(p, team);
                    if (success) {
                        p.sendMessage(CC.t("&aUnclaimed successfully from &f" + team.getName() + "&a!"));
                    } else {
                        p.sendMessage(CC.t("&cYou are not standing in a claim belonging to " + team.getName() + "!"));
                    }

                    p.closeInventory();
                    getEditorManager().reopenEditMenu(p, team);
                    return;
                }
            }

            @Override
            public ItemStack getItemStack() {
                List<Claim> claims = team.getClaims();
                List<String> lore = new ArrayList<>();

                if (claims.isEmpty()) {
                    lore.add("&7This koth currently has no");
                    lore.add("&7claims, left click to add a claim.");
                } else {
                    lore.add("&7This koth has &a" + claims.size() + " &7claim(s).");
                }
                lore.add("&7");
                lore.add("&aLeft click to add a claim");
                lore.add("&cRight click to unclaim");

                return new ItemBuilder(Material.DIAMOND_HOE)
                        .setName("&6&lClaims")
                        .setLore(lore)
                        .toItemStack();
            }
        };
    }

    private Button createMetadataButton(Player player) {
        return new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                Player p = (Player) e.getWhoClicked();

                TeamType applied = getEditorManager().getAppliedType(p);
                boolean typeLocked = (applied != null);

                if (e.getClick() == ClickType.SHIFT_LEFT) {
                    getEditorManager().removeAppliedType(p);
                    team.setType(TeamType.SAFEZONE);
                    team.setDeathban(false);
                    team.save();
                    p.sendMessage(CC.t("&eClaim type has been reset to default."));
                    update();
                    return;
                }

                // If the team already has a non-default type, treat it as locked
                if (team.getType() != TeamType.SAFEZONE && applied == null) {
                    applied = team.getType();
                    getEditorManager().setAppliedType(p, applied);
                    typeLocked = true;
                }

                if (typeLocked && e.getClick() != ClickType.SHIFT_LEFT) {
                    p.sendMessage(CC.t("&cA type is already set. Shift + Left Click to remove it first."));
                    return;
                }

                if (e.getClick() == ClickType.LEFT) {
                    int current = getEditorManager().getScrollIndex(p);
                    int newIndex = current - 1;
                    if (newIndex < 0) {
                        newIndex = getEditorManager().getClaimTypes().size() - 1;
                    }
                    getEditorManager().setScrollIndex(p, newIndex);
                    update();
                    return;
                }

                if (e.getClick() == ClickType.RIGHT) {
                    int current = getEditorManager().getScrollIndex(p);
                    int newIndex = current + 1;
                    if (newIndex >= getEditorManager().getClaimTypes().size()) {
                        newIndex = 0;
                    }
                    getEditorManager().setScrollIndex(p, newIndex);
                    update();
                    return;
                }

                if (e.getClick() == ClickType.MIDDLE) {
                    int current = getEditorManager().getScrollIndex(p);
                    TeamType selectedType = getEditorManager().getClaimTypes().get(current);
                    getEditorManager().setAppliedType(p, selectedType);

                    team.setType(selectedType);
                    team.setDeathban(selectedType != TeamType.SAFEZONE && selectedType != TeamType.NO_PEARLING);
                    team.save();

                    p.sendMessage(CC.t("&aClaim type set to &f" + selectedType.name() + "&a!"));
                    update();
                    return;
                }
            }

            @Override
            public ItemStack getItemStack() {
                List<TeamType> types = getEditorManager().getClaimTypes();
                int current = getEditorManager().getScrollIndex(player);
                TeamType applied = getEditorManager().getAppliedType(player);

                // Sync applied with team type
                if (applied == null && team.getType() != TeamType.SAFEZONE) {
                    applied = team.getType();
                }

                List<String> lore = new ArrayList<>();
                lore.add("&7");

                for (int i = 0; i < types.size(); i++) {
                    String typeName = formatTypeName(types.get(i));

                    if (applied != null && applied == types.get(i)) {
                        lore.add("&a" + i + ". &a" + typeName);
                    } else if (i == current) {
                        lore.add("&a" + i + ". &f" + typeName);
                    } else {
                        lore.add("&7" + i + ". &f" + typeName);
                    }
                }

                lore.add("&7");
                lore.add("&eLeft and right click to scroll");
                lore.add("&eShift + Left Click to remove");
                lore.add("&bMiddle click to set the Claim Type");

                return new ItemBuilder(Material.COMMAND)
                        .setName("&6&lMetadata")
                        .setLore(lore)
                        .toItemStack();
            }
        };
    }

    private Button createCaptureTimeButton(Player player) {
        return new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                Player p = (Player) e.getWhoClicked();
                p.closeInventory();

                p.sendMessage(CC.t("&aPlease input a duration. &f(1s/1m/1h/1m30s/1h30m30s)"));

                getEditorManager().awaitChatInput(p, (input) -> {
                    long millis = parseDuration(input);
                    if (millis <= 0) {
                        p.sendMessage(CC.t("&cInvalid duration format! Example: 1s, 1m, 1h, 5m30s, 1h30m25s"));
                        getEditorManager().reopenEditMenu(p, team);
                        return;
                    }

                    Koth koth = getInstance().getKothManager().getKoth(team.getName());
                    if (koth != null) {
                        koth.setMinutes(millis);
                        if (!koth.isActive()) {
                            koth.setRemaining(millis);
                        }
                        koth.save();
                        p.sendMessage(CC.t("&aCapture time set to &f" + formatTime(millis) + "&a!"));
                    }

                    getEditorManager().reopenEditMenu(p, team);
                });
            }

            @Override
            public ItemStack getItemStack() {
                Koth koth = getInstance().getKothManager().getKoth(team.getName());
                String timeStr = "00:00";
                if (koth != null) {
                    timeStr = formatTime(koth.isActive() ? koth.getRemaining() : koth.getMinutes());
                }

                List<String> lore = new ArrayList<>();
                lore.add("&7");
                lore.add("&eCurrent: &c" + timeStr);

                return new ItemBuilder(Material.WATCH)
                        .setName("&6&lCapture Time")
                        .setLore(lore)
                        .toItemStack();
            }
        };
    }

    private Button createCaptureZoneButton(Player player) {
        return new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                Player p = (Player) e.getWhoClicked();

                if (e.getClick() == ClickType.LEFT) {
                    p.closeInventory();
                    getEditorManager().giveCaptureZoneWand(p, team);
                    p.sendMessage(CC.t("&eYou have been given a capture zone wand. Select two corners, shift-left-click to accept, shift-right-click to cancel."));
                    return;
                }

                if (e.getClick() == ClickType.RIGHT) {
                    Koth koth = getInstance().getKothManager().getKoth(team.getName());
                    if (koth != null && koth.getCaptureZone() != null) {
                        koth.checkZone(true);
                        koth.setCaptureZone(null);
                        koth.save();
                        p.sendMessage(CC.t("&aCapture zone removed for &f" + team.getName() + "&a!"));
                    } else {
                        p.sendMessage(CC.t("&cThis koth has no capture zone set!"));
                    }
                    update();
                    return;
                }
            }

            @Override
            public ItemStack getItemStack() {
                Koth koth = getInstance().getKothManager().getKoth(team.getName());
                List<String> lore = new ArrayList<>();
                lore.add("&7");

                if (koth != null && koth.getCaptureZone() != null) {
                    int min1 = koth.getCaptureZone().getMinimumX();
                    int min2 = koth.getCaptureZone().getMinimumZ();
                    int max1 = koth.getCaptureZone().getMaximumX();
                    int max2 = koth.getCaptureZone().getMaximumZ();
                    lore.add("&eCurrent: &c[" + min1 + "," + min2 + "] - [" + max1 + "," + max2 + "]");
                } else {
                    lore.add("&eCurrent: &cNone");
                }

                lore.add("&7");
                lore.add("&aLeft click to set capture zone");
                lore.add("&cRight click to remove");

                return new ItemBuilder(Material.GOLD_HOE)
                        .setName("&e&lCapture Zone")
                        .setLore(lore)
                        .toItemStack();
            }
        };
    }

    private Button createKothLootButton(Player player) {
        return new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                Player p = (Player) e.getWhoClicked();
                Koth koth = getInstance().getKothManager().getKoth(team.getName());
                if (koth == null) return;

                if (e.getClick() == ClickType.SHIFT_RIGHT) {
                    // Add command reward
                    p.closeInventory();
                    p.sendMessage(CC.t("&aPlease input a value."));

                    getEditorManager().awaitChatInput(p, (cmd) -> {
                        koth.getCommandRewards().add(cmd);
                        koth.save();
                        p.sendMessage(CC.t("&aCommand reward added: &f" + cmd));
                        getEditorManager().reopenEditMenu(p, team);
                    });
                    return;
                }

                if (e.getClick() == ClickType.SHIFT_LEFT) {
                    // Remove selected command reward
                    int selected = getEditorManager().getSelectedCommandIndex(p);
                    if (koth.getCommandRewards().isEmpty()) {
                        p.sendMessage(CC.t("&cThere are no command rewards to remove!"));
                        return;
                    }
                    if (selected >= koth.getCommandRewards().size()) {
                        selected = 0;
                    }
                    String removed = koth.getCommandRewards().remove(selected);
                    koth.save();
                    p.sendMessage(CC.t("&cRemoved command reward: &f" + removed));
                    update();
                    return;
                }

                if (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.RIGHT) {
                    // Move selection
                    if (koth.getCommandRewards().isEmpty()) return;
                    int current = getEditorManager().getSelectedCommandIndex(p);
                    if (e.getClick() == ClickType.LEFT) {
                        current++;
                        if (current >= koth.getCommandRewards().size()) current = 0;
                    } else {
                        current--;
                        if (current < 0) current = koth.getCommandRewards().size() - 1;
                    }
                    getEditorManager().setSelectedCommandIndex(p, current);
                    update();
                    return;
                }

                if (e.getClick() == ClickType.DROP || e.getClick() == ClickType.CONTROL_DROP) {
                    // Press Q - open loot item menu
                    p.closeInventory();
                    new KothLootMenu(getManager(), p, team).open();
                    return;
                }
            }

            @Override
            public ItemStack getItemStack() {
                Koth koth = getInstance().getKothManager().getKoth(team.getName());
                int selected = getEditorManager().getSelectedCommandIndex(player);

                List<String> lore = new ArrayList<>();
                lore.add("&7&lPlaceholders");
                lore.add("&6%player% &fPlayer Name");
                lore.add("&7");

                if (koth != null && !koth.getCommandRewards().isEmpty()) {
                    for (int i = 0; i < koth.getCommandRewards().size(); i++) {
                        String prefix = (i == selected) ? "&a" : "&7";
                        lore.add(prefix + i + ". &f" + koth.getCommandRewards().get(i));
                    }
                    lore.add("&7");
                }

                lore.add("&aShift Right Click to add a command-reward");
                lore.add("&cShift Left Click to remove a command-reward");
                lore.add("&eRight and Left Click to move");
                lore.add("&bPress Q to add a Item Reward");

                return new ItemBuilder(Material.CHEST)
                        .setName("&6&lKoth Loot")
                        .setLore(lore)
                        .toItemStack();
            }
        };
    }

    private String formatTypeName(TeamType type) {
        switch (type) {
            case ROAD:
                return "Road";
            case SAFEZONE:
                return "Safe-Zone";
            case EVENT:
                return "Event";
            case CONQUEST:
                return "Conquest";
            case CITADEL:
                return "Citadel";
            case NO_PEARLING:
                return "No Pearling";
            default:
                return type.name();
        }
    }

    private String formatTime(long millis) {
        if (millis <= 0) return "00:00";
        long totalSeconds = millis / 1000L;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private long parseDuration(String input) {
        if (input == null || input.isEmpty()) return -1;
        input = input.trim().toLowerCase();
        long total = 0;
        StringBuilder num = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                if (num.length() == 0) return -1;
                long val = Long.parseLong(num.toString());
                switch (c) {
                    case 'h':
                        total += val * 3600 * 1000;
                        break;
                    case 'm':
                        total += val * 60 * 1000;
                        break;
                    case 's':
                        total += val * 1000;
                        break;
                    default:
                        return -1;
                }
                num = new StringBuilder();
            }
        }
        if (num.length() > 0) return -1;
        return total;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
    }

    @Override
    public void onClickOwn(InventoryClickEvent e) {
        e.setCancelled(true);
    }
}

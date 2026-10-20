package me.keano.azurite.modules.teams.editor.menu;

import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import me.keano.azurite.modules.events.koth.Koth;
import me.keano.azurite.modules.teams.Team;
import me.keano.azurite.modules.teams.editor.FactionEditorManager;
import me.keano.azurite.modules.teams.enums.TeamType;
import me.keano.azurite.utils.CC;
import me.keano.azurite.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Main /f editor menu - shows all system factions and a create button.
 */
public class FactionEditorMenu extends Menu {

    // 1-indexed button slots (Menu framework subtracts 1 internally)
    // Decorative slots (0-indexed): 0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,37,38,39,40,41,42,43,44
    // Create button: slot 4 (1-indexed: 5)
    // Faction slots: 10-34 excluding decorative (1-indexed: 11-35 excluding decorative)

    private static final Set<Integer> DECORATIVE_SLOTS = new HashSet<>(Arrays.asList(
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    ));

    // 1-indexed slots for buttons (Menu framework does slot-1)
    private static final int CREATE_BUTTON_SLOT = 5; // 0-indexed slot 4

    // Available faction slots (0-indexed): 10-34, excluding decorative ones
    // 1-indexed for the buttons map
    private static final List<Integer> FACTION_SLOTS = new ArrayList<>();

    static {
        for (int i = 10; i <= 34; i++) {
            if (!DECORATIVE_SLOTS.contains(i)) {
                FACTION_SLOTS.add(i + 1); // Convert to 1-indexed for Menu framework
            }
        }
    }

    public FactionEditorMenu(MenuManager manager, Player player) {
        super(manager, player, CC.t("&8Systeam Factions"), 45, false);
    }

    private FactionEditorManager getEditorManager() {
        return getInstance().getFactionEditorManager();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        // Decorative black stained glass panes
        ItemStack pane = new ItemBuilder(Material.STAINED_GLASS_PANE)
                .data(getManager(), 15)
                .setName(" ")
                .toItemStack();

        for (int slot : DECORATIVE_SLOTS) {
            int buttonSlot = slot + 1; // 1-indexed
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

        // Create Faction button (slot 4 -> 1-indexed 5)
        buttons.put(CREATE_BUTTON_SLOT, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                Player p = (Player) e.getWhoClicked();

                p.closeInventory();

                p.sendMessage(CC.t("&ePlease type a name for this faction, or type &cCancel &eto cancel."));

                getEditorManager().awaitChatInput(p, (message) -> {
                    if (message.equalsIgnoreCase("cancel")) {
                        p.sendMessage(CC.t("&cFaction creation cancelled."));
                        getEditorManager().openMainMenu(p);
                        return;
                    }

                    Team team = getEditorManager().createFaction(message);
                    if (team == null) {
                        p.sendMessage(CC.t("&cA faction with that name already exists!"));
                        getEditorManager().openMainMenu(p);
                        return;
                    }

                    p.sendMessage(CC.t("&aFaction &f" + message + " &acreated successfully!"));
                    getEditorManager().openMainMenu(p);
                });
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.NETHER_STAR)
                        .setName("&a&lCreate Faction")
                        .toItemStack();
            }
        });

        // Populate faction items from all system teams
        Collection<Team> systemTeams = getInstance().getTeamManager().getSystemTeams().values();
        int slotIndex = 0;

        for (Team team : systemTeams) {
            if (slotIndex >= FACTION_SLOTS.size()) break;

            int buttonSlot = FACTION_SLOTS.get(slotIndex);
            int woolData = FactionEditorManager.colorToWoolData(team.getCustomColor());
            String displayName = team.getDisplayName(player);

            buttons.put(buttonSlot, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    e.setCancelled(true);
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();

                    // Open the faction edit menu for this team
                    new FactionEditMenu(getManager(), p, team).open();
                }

                @Override
                public ItemStack getItemStack() {
                    List<String> lore = new ArrayList<>();
                    lore.add("&8" + team.getName());
                    lore.add("&7");

                    // Add metadata line based on team type
                    String metadataLine = getMetadataLoreLine(team);
                    if (metadataLine != null) {
                        lore.add(metadataLine);
                        lore.add("&7");
                    }

                    // Add koth-specific info if this team has a koth
                    Koth koth = getInstance().getKothManager().getKoth(team.getName());
                    if (koth != null) {
                        long timeToShow = koth.isActive() ? koth.getRemaining() : koth.getMinutes();
                        lore.add("&7Time: &c" + formatTime(timeToShow));
                        lore.add("&7Capture Zone: " + (koth.getCaptureZone() != null ? "&aYes" : "&cNo"));
                        lore.add("&7");
                    }

                    lore.add("&eClick to edit faction.");
                    return new ItemBuilder(Material.WOOL)
                            .data(getManager(), woolData)
                            .setName(displayName)
                            .setLore(lore)
                            .toItemStack();
                }
            });

            slotIndex++;
        }

        return buttons;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
    }

    @Override
    public void onClickOwn(InventoryClickEvent e) {
        e.setCancelled(true);
    }

    private String getMetadataLoreLine(Team team) {
        TeamType type = team.getType();
        switch (type) {
            case SAFEZONE:
                return "&aSafe-Zone";
            case EVENT:
                return "&aEvent";
            case CONQUEST:
                return "&aConquest";
            case CITADEL:
                return "&aCitadel";
            case ROAD:
                return "&aRoad";
            case NO_PEARLING:
                return "&5No Pearling";
            default:
                return null;
        }
    }

    private String formatTime(long millis) {
        if (millis <= 0) return "00:00";
        long totalSeconds = millis / 1000L;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hours = minutes / 60;
        if (hours > 0) {
            minutes = minutes % 60;
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }
}

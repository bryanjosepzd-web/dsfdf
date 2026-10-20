package me.keano.azurite.modules.teams.editor.menu;

import me.keano.azurite.modules.events.koth.Koth;
import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
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
 * /game editor menu - same as /f editor but with different title and Create Game button.
 */
public class GameEditorMenu extends Menu {

    private static final Set<Integer> DECORATIVE_SLOTS = new HashSet<>(Arrays.asList(
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    ));

    private static final int CREATE_BUTTON_SLOT = 5;

    private static final List<Integer> FACTION_SLOTS = new ArrayList<>();

    static {
        for (int i = 10; i <= 34; i++) {
            if (!DECORATIVE_SLOTS.contains(i)) {
                FACTION_SLOTS.add(i + 1);
            }
        }
    }

    public GameEditorMenu(MenuManager manager, Player player) {
        super(manager, player, CC.t("&8Game Editor"), 45, false);
    }

    private FactionEditorManager getEditorManager() {
        return getInstance().getFactionEditorManager();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        ItemStack pane = new ItemBuilder(Material.STAINED_GLASS_PANE)
                .data(getManager(), 15)
                .setName(" ")
                .toItemStack();

        for (int slot : DECORATIVE_SLOTS) {
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

        // Create Game button
        buttons.put(CREATE_BUTTON_SLOT, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                Player p = (Player) e.getWhoClicked();
                p.closeInventory();
                p.sendMessage(CC.t("&aPlease type game name"));

                getEditorManager().awaitChatInput(p, (name) -> {
                    if (name.equalsIgnoreCase("cancel")) {
                        p.sendMessage(CC.t("&cCancelled."));
                        getEditorManager().openGameMenu(p);
                        return;
                    }

                    // Store the name and ask for event type
                    p.sendMessage(CC.t("&aPlease put event type: &fKOTH - CONQUEST - CITADEL"));

                    getEditorManager().awaitChatInput(p, (typeStr) -> {
                        String typeUpper = typeStr.toUpperCase().trim();
                        TeamType eventType;

                        // Map "KOTH" to EVENT since the enum uses EVENT for koths
                        if (typeUpper.equals("KOTH")) {
                            eventType = TeamType.EVENT;
                        } else {
                            try {
                                eventType = TeamType.valueOf(typeUpper);
                            } catch (IllegalArgumentException ex) {
                                p.sendMessage(CC.t("&cInvalid event type! Use KOTH, CONQUEST, or CITADEL"));
                                getEditorManager().openGameMenu(p);
                                return;
                            }
                        }

                        if (eventType != TeamType.EVENT && eventType != TeamType.CONQUEST && eventType != TeamType.CITADEL) {
                            p.sendMessage(CC.t("&cInvalid event type! Use KOTH, CONQUEST, or CITADEL"));
                            getEditorManager().openGameMenu(p);
                            return;
                        }

                        Team team = getEditorManager().createFaction(name);
                        if (team == null) {
                            p.sendMessage(CC.t("&cA faction with that name already exists!"));
                            getEditorManager().openGameMenu(p);
                            return;
                        }

                        // Set the team type to the event type
                        team.setType(eventType);
                        team.setDeathban(true);
                        team.save();

                        // Create a Koth object for all event types (KOTH, CONQUEST, CITADEL)
                        getInstance().getKothManager().createKoth(name, CC.t("&f"));

                        p.sendMessage(CC.t("&aGame &f" + name + " &acreated as &f" + typeUpper + "&a!"));
                        getEditorManager().openGameMenu(p);
                    });
                });
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.NETHER_STAR)
                        .setName("&a&lCreate Game")
                        .toItemStack();
            }
        });

        // Populate faction items - same as f editor
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
                    new FactionEditMenu(getManager(), p, team).open();
                }

                @Override
                public ItemStack getItemStack() {
                    List<String> lore = new ArrayList<>();
                    lore.add("&8" + team.getName());
                    lore.add("&7");

                    String metadataLine = getMetadataLoreLine(team);
                    if (metadataLine != null) {
                        lore.add(metadataLine);
                        lore.add("&7");
                    }

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

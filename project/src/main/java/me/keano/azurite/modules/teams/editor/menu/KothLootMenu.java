package me.keano.azurite.modules.teams.editor.menu;

import me.keano.azurite.modules.events.koth.Koth;
import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import me.keano.azurite.modules.teams.Team;
import me.keano.azurite.modules.teams.editor.FactionEditorManager;
import me.keano.azurite.utils.CC;
import me.keano.azurite.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 6-row menu for placing item rewards into a koth. Items are saved on close.
 */
public class KothLootMenu extends Menu {

    private final Team team;
    private final int backSlot = 54; // 1-indexed, last slot (0-indexed 53)

    // Slots that are locked (the back button) - 1-indexed
    private static final Set<Integer> LOCKED_SLOTS = new HashSet<>(Arrays.asList(54));

    public KothLootMenu(MenuManager manager, Player player, Team team) {
        super(manager, player, CC.t("&8Edit Loot - " + team.getName()), 54, false);
        this.team = team;
        this.allowInteract = true;
    }

    private FactionEditorManager getEditorManager() {
        return getInstance().getFactionEditorManager();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        // Back button in last slot
        buttons.put(backSlot, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                Player p = (Player) e.getWhoClicked();
                p.closeInventory();
            }

            @Override
            public ItemStack getItemStack() {
                return new ItemBuilder(Material.BED)
                        .setName("&c&lBack")
                        .toItemStack();
            }
        });

        // Pre-fill with existing loot items in slots 1-53
        Koth koth = getInstance().getKothManager().getKoth(team.getName());
        if (koth != null && !koth.getLoot().isEmpty()) {
            int slot = 1;
            for (ItemStack item : koth.getLoot()) {
                if (item == null || item.getType() == Material.AIR) continue;
                if (slot >= 54) break;
                int finalSlot = slot;
                buttons.put(finalSlot, new Button() {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        // Allow interaction for loot items
                    }

                    @Override
                    public ItemStack getItemStack() {
                        return item;
                    }
                });
                slot++;
            }
        }

        return buttons;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        // Allow interaction except on locked slots
        int rawSlot = e.getRawSlot();
        if (rawSlot < 54) {
            // Check if it's the back button slot (0-indexed 53)
            Button button = getButtons().get(rawSlot + 1);
            if (button != null && rawSlot + 1 == backSlot) {
                e.setCancelled(true);
            }
            // Otherwise allow placing/removing items
        }
    }

    @Override
    public void onClickOwn(InventoryClickEvent e) {
        // Allow shift-clicking items from own inventory into the loot menu
    }

    @Override
    public void onClose() {
        Koth koth = getInstance().getKothManager().getKoth(team.getName());
        if (koth == null) return;

        List<ItemStack> newLoot = new ArrayList<>();

        // Read all items from the inventory except the last slot (back button)
        for (int i = 0; i < 53; i++) {
            ItemStack item = getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                newLoot.add(item.clone());
            }
        }

        koth.setLoot(newLoot);
        koth.save();

        Player p = getPlayer();
        if (p.isOnline()) {
            p.sendMessage(CC.t("&aKoth loot saved for &f" + team.getName() + "&a!"));
            getEditorManager().reopenEditMenu(p, team);
        }
    }
}

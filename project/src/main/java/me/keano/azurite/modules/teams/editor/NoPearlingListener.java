package me.keano.azurite.modules.teams.editor;

import me.keano.azurite.modules.framework.Module;
import me.keano.azurite.modules.teams.Team;
import me.keano.azurite.modules.teams.enums.TeamType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Prevents ender pearl usage in claims with the NO_PEARLING metadata.
 */
public class NoPearlingListener extends Module<FactionEditorManager> implements Listener {

    public NoPearlingListener(FactionEditorManager manager) {
        super(manager);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPearlInteract(PlayerInteractEvent e) {
        if (!e.getAction().name().contains("RIGHT")) return;
        if (e.getItem() == null || e.getItem().getType() != Material.ENDER_PEARL) return;

        Player player = e.getPlayer();
        Team team = getInstance().getTeamManager().getClaimManager().getTeam(player.getLocation());

        if (team != null && team.getType() == TeamType.NO_PEARLING) {
            e.setCancelled(true);
            player.updateInventory();
            player.sendMessage(getLanguageConfig().getString("NO_PEARLING.ZONE_MESSAGE"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPearlTeleport(PlayerTeleportEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;

        Player player = e.getPlayer();
        Team team = getInstance().getTeamManager().getClaimManager().getTeam(e.getTo());

        if (team != null && team.getType() == TeamType.NO_PEARLING) {
            e.setCancelled(true);
            getInstance().getTimerManager().getEnderpearlTimer().removeTimer(player);
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.ENDER_PEARL));
            player.sendMessage(getLanguageConfig().getString("NO_PEARLING.ZONE_MESSAGE"));
        }
    }
}

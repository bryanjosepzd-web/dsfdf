package me.keano.azurite.modules.api;

import me.keano.azurite.HCF;
import me.keano.azurite.modules.teams.Team;
import me.keano.azurite.modules.teams.enums.TeamType;
import me.keano.azurite.modules.teams.type.*;
import me.keano.azurite.modules.users.User;
import me.keano.azurite.utils.extra.Pair;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class AzuriteAPI {

    private final HCF hcf;

    public AzuriteAPI(JavaPlugin plugin) {
        Plugin hcf = plugin.getServer().getPluginManager().getPlugin("Azurite");

        if (hcf == null) {
            throw new IllegalArgumentException("Azurite is not loaded");
        }

        this.hcf = (HCF) hcf;
    }

    public boolean isConquest(Location location) {
        return hcf.getTeamManager().getClaimManager().getTeam(location).getType() == TeamType.CONQUEST;
    }

    public boolean isSpawn(Location location) {
        return hcf.getTeamManager().getClaimManager().getTeam(location).getType() == TeamType.SAFEZONE;
    }

    public boolean isCitadel(Location location) {
        return hcf.getTeamManager().getClaimManager().getTeam(location).getType() == TeamType.CITADEL;
    }

    public boolean isKoth(Location location) {
        return hcf.getTeamManager().getClaimManager().getTeam(location).getType() == TeamType.EVENT;
    }

    public boolean isSotwActive(Player player) {
        return hcf.getSotwManager().isActive() && !hcf.getSotwManager().isEnabled(player);
    }

    public boolean isInSpawn(Player player) {
        return hcf.getTeamManager().getClaimManager().getTeam(player.getLocation()).getType() == TeamType.SAFEZONE;
    }

    public boolean isInOwnClaim(Player player) {
        Team team = hcf.getTeamManager().getClaimManager().getTeam(player.getLocation());
        return team instanceof PlayerTeam && ((PlayerTeam) team).getPlayers().contains(player.getUniqueId());
    }

    public boolean hasGlobalAbilityCooldown(Player player) {
        return hcf.getAbilityManager().getGlobalCooldown().hasTimer(player);
    }

    public boolean hasCombatTag(Player player) {
        return hcf.getTimerManager().getCombatTimer().hasTimer(player);
    }

    public String getPlayerFaction(Player player) {
        PlayerTeam pt = hcf.getTeamManager().getByPlayer(player.getUniqueId());
        return (pt != null ? pt.getName() : null);
    }

    public void giveArcherTag(Player player, long time) {
        hcf.getTimerManager().getArcherTagTimer().applyTimer(player, time);
    }

    public int getBalance(Player player) {
        User user = hcf.getUserManager().getByUUID(player.getUniqueId());
        return (user == null ? 0 : user.getBalance());
    }

    public List<Pair<Location, Location>> getClaims(Player player) {
        PlayerTeam pt = hcf.getTeamManager().getByPlayer(player.getUniqueId());

        if (pt != null) {
            return pt.getClaims().stream().map(c -> new Pair<>(
                    new Location(c.getWorld(), c.getX1(), c.getY1(), c.getZ1()),
                    new Location(c.getWorld(), c.getX2(), c.getY2(), c.getZ2())
            )).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public List<Player> getOnlineTeammates(Player player) {
        PlayerTeam pt = hcf.getTeamManager().getByPlayer(player.getUniqueId());

        if (pt != null) {
            return pt.getOnlinePlayers(false);
        }

        return Collections.emptyList();
    }
}
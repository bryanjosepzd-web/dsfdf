package me.keano.azurite.modules.commands.type;

import me.keano.azurite.modules.commands.CommandManager;
import me.keano.azurite.modules.framework.Config;
import me.keano.azurite.modules.framework.commands.Command;
import me.keano.azurite.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ResetRedeemCommand extends Command {

    public ResetRedeemCommand(CommandManager manager) {
        super(manager, "resetredeem");
        this.setPermissible("azurite.resetredeem");
    }

    @Override
    public List<String> aliases() {
        return Collections.emptyList();
    }

    @Override
    public List<String> usage() {
        return Collections.singletonList(CC.t("&cUsage: /resetredeem <player> [redeem_id]"));
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sendMessage(sender, Config.PLAYER_NOT_FOUND.replace("%player%", args[0]));
            return;
        }

        if (args.length >= 2) {
            String id = args[1].toLowerCase();
            if (getInstance().getRedeemManager().getRedeem(id) == null) {
                sendMessage(sender, "&cThat redeem does not exist!");
                return;
            }
            getInstance().getRedeemManager().resetPlayerRedeem(target.getUniqueId(), id);
            sendMessage(sender, "&aReset redeem &e" + id + " &afor &e" + target.getName());
        } else {
            getInstance().getRedeemManager().resetPlayer(target.getUniqueId());
            sendMessage(sender, "&aReset all redeems for &e" + target.getName());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return null;
        }
        if (args.length == 2) {
            return getInstance().getRedeemManager().getRedeemsList().stream()
                    .map(me.keano.azurite.modules.redeems.Redeem::getId)
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());

        }
        return Collections.emptyList();
    }
}

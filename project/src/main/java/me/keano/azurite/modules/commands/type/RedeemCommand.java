package me.keano.azurite.modules.commands.type;

import me.keano.azurite.modules.commands.CommandManager;
import me.keano.azurite.modules.framework.Config;
import me.keano.azurite.modules.framework.commands.Command;
import me.keano.azurite.modules.redeems.menu.RedeemEditorMenu;
import me.keano.azurite.modules.redeems.menu.RedeemMenu;
import me.keano.azurite.utils.CC;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RedeemCommand extends Command {

    public RedeemCommand(CommandManager manager) {
        super(manager, "redeem");
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("support");
    }

    @Override
    public List<String> usage() {
        return Arrays.asList(
                CC.t("&7&m-------------------------"),
                CC.t("&cRedeem Commands:"),
                CC.t("&e/redeem &7- Open the redeem menu"),
                CC.t("&e/redeem editor &7- Open the redeem editor (admin)"),
                CC.t("&7&m-------------------------")
        );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, Config.PLAYER_ONLY);
            return;
        }

        Player player = (Player) sender;

        if (args.length >= 1 && args[0].equalsIgnoreCase("editor")) {
            if (!player.hasPermission("azurite.redeemadmin")) {
                sendMessage(sender, Config.INSUFFICIENT_PERM);
                return;
            }
            new RedeemEditorMenu(getInstance().getMenuManager(), player, getInstance().getRedeemManager()).open();
            return;
        }

        new RedeemMenu(getInstance().getMenuManager(), player, getInstance().getRedeemManager()).open();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("azurite.redeemadmin")) {
            return Collections.singletonList("editor");
        }
        return Collections.emptyList();
    }
}

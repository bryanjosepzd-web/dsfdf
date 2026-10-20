package me.keano.azurite.modules.teams.commands.game;

import me.keano.azurite.modules.commands.CommandManager;
import me.keano.azurite.modules.framework.commands.Command;
import me.keano.azurite.modules.teams.commands.game.args.GameEditorArg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /game - Parent command for game-related operations.
 */
public class GameCommand extends Command {

    public GameCommand(CommandManager manager) {
        super(manager, "game");
        this.handleArguments(new GameEditorArg(manager));
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList("games");
    }

    @Override
    public List<String> usage() {
        return Collections.singletonList(
                "&e/game editor &7- Open the game editor menu"
        );
    }
}

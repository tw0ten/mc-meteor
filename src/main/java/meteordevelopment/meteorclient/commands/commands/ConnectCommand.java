package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerInfo.ServerType;
import net.minecraft.command.CommandSource;

public class ConnectCommand extends Command {
    public ConnectCommand() {
        super("connect", "Connect to a server");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("address", StringArgumentType.string()).executes(context -> {
            final var address = context.getArgument("address", String.class);
            // TOOD: ??????
            ConnectScreen.connect(mc.currentScreen, mc,
                    ServerAddress.parse(address), new ServerInfo(getName(), address, ServerType.OTHER),
                    true, null);
            return SINGLE_SUCCESS;
        }));
    }
}

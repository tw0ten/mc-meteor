/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.commands.*;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Commands {
    public static final List<Command> COMMANDS = new ArrayList<>();
    public static CommandDispatcher<CommandSource> DISPATCHER = new CommandDispatcher<>();

    @PostInit(dependencies = PathManagers.class)
    public static void init() {
        add(new BindCommand());
        add(new BindsCommand());
        add(new CommandsCommand());
        add(new ConnectCommand());
        add(new DamageCommand());
        add(new DisconnectCommand());
        add(new DismountCommand());
        add(new DropCommand());
        add(new EnchantCommand());
        add(new EnderChestCommand());
        add(new FakePlayerCommand());
        add(new FovCommand());
        add(new FriendsCommand());
        add(new GamemodeCommand());
        add(new GiveCommand());
        add(new HClipCommand());
        add(new InputCommand());
        add(new InventoryCommand());
        add(new LocateCommand());
        add(new MacroCommand());
        add(new ModulesCommand());
        add(new NameHistoryCommand());
        add(new NbtCommand());
        add(new NotebotCommand());
        add(new PeekCommand());
        add(new ProfilesCommand());
        add(new QuickSwapCommand());
        add(new ReloadCommand());
        add(new ResetCommand());
        add(new RotationCommand());
        add(new SaveMapCommand());
        add(new SayCommand());
        add(new ServerCommand());
        add(new SessionCommand());
        add(new SettingCommand());
        add(new SpectateCommand());
        add(new SwarmCommand());
        add(new SysgcCommand());
        add(new ToggleCommand());
        add(new VClipCommand());
        add(new WaspCommand());
        add(new WaypointCommand());

        COMMANDS.sort(Comparator.comparing(Command::getName));

        MeteorClient.EVENT_BUS.subscribe(Commands.class);
    }

    public static void add(Command command) {
        COMMANDS.removeIf(existing -> existing.getName().equals(command.getName()));
        COMMANDS.add(command);
    }

    public static void dispatch(String message) throws CommandSyntaxException {
        DISPATCHER.execute(message, mc.getNetworkHandler().getCommandSource());
    }

    public static Command get(String name) {
        for (Command command : COMMANDS) {
            if (command.getName().equals(name)) {
                return command;
            }
        }

        return null;
    }

    /**
     * Argument types that rely on Minecraft registries access those registries through a {@link CommandRegistryAccess}
     * object. Since dynamic registries are specific to each server, we need to make a new CommandRegistryAccess object
     * every time we join a server.
     * <p>
     * The command tree and by extension the {@link CommandDispatcher} also have to be rebuilt because:
     * <ol>
     * <li>Argument types that require registries use a registry wrapper object that is created and stored in the
     *     argument type objects when the command tree is built.
     * <li>Registry entries and keys are compared using referential equality. Even if the data encoded is the same,
     *     registry wrapper objects' dynamic data becomes stale after joining another server.
     * <li>The CommandDispatcher's node merging only adds missing children, it cannot replace stale argument type
     *     objects.
     * </ol>
     *
     * @author Crosby
     */
    @EventHandler
    private static void onJoin(GameJoinedEvent event) {
        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        Command.REGISTRY_ACCESS = CommandRegistryAccess.of(networkHandler.getRegistryManager(), networkHandler.getEnabledFeatures());

        DISPATCHER = new CommandDispatcher<>();
        for (Command command : COMMANDS) {
            command.registerTo(DISPATCHER);
        }
    }
}

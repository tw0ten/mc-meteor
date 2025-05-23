/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.macros;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Macros extends System<Macros> implements Iterable<Macro> {
    private List<Macro> macros = new ArrayList<>();

    public Macros() {
        super("macros");

        new Thread(() -> {
            final var pipe = MeteorClient.FOLDER.toPath().resolve("in").toFile();
            if (!pipe.exists())
                try {
                    Runtime.getRuntime().exec(new String[] { "mkfifo", pipe.toPath().toString() });
                } catch (final IOException e) {
                    e.printStackTrace();
                    return;
                }
            while (true) {
                try (final var stream = new FileInputStream(pipe)) {
                    final var buffer = new ByteArrayOutputStream();
                    {
                        final var temp = new byte[1024];
                        var bytesRead = -1;
                        while ((bytesRead = stream.read(temp)) != -1)
                            buffer.write(temp, 0, bytesRead);
                    }
                    final var s = buffer.toString().replace("\n", " ");
                    MeteorClient.LOG.info(pipe.toPath() + ": " + s);
                    if (s.length() == 0) {
                        Thread.sleep(500);
                        continue;
                    }
                    ChatUtils.sendPlayerMsg(s);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static Macros get() {
        return Systems.get(Macros.class);
    }

    public void add(Macro macro) {
        macros.add(macro);
        MeteorClient.EVENT_BUS.subscribe(macro);
        save();
    }

    public Macro get(String name) {
        for (Macro macro : macros) {
            if (macro.name.get().equalsIgnoreCase(name)) return macro;
        }

        return null;
    }

    public List<Macro> getAll() {
        return macros;
    }

    public void remove(Macro macro) {
        if (macros.remove(macro)) {
            MeteorClient.EVENT_BUS.unsubscribe(macro);
            save();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.Release) return;

        for (Macro macro : macros) {
            if (macro.onAction(true, event.key, event.modifiers)) return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onButton(MouseButtonEvent event) {
        if (event.action == KeyAction.Release) return;

        for (Macro macro : macros) {
            if (macro.onAction(false, event.button, 0)) return;
        }
    }

    public boolean isEmpty() {
        return macros.isEmpty();
    }

    @Override
    public @NotNull Iterator<Macro> iterator() {
        return macros.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("macros", NbtUtils.listToTag(macros));
        return tag;
    }

    @Override
    public Macros fromTag(NbtCompound tag) {
        for (Macro macro : macros) MeteorClient.EVENT_BUS.unsubscribe(macro);

        macros = NbtUtils.listFromTag(tag.getListOrEmpty("macros"), Macro::new);

        for (Macro macro : macros) MeteorClient.EVENT_BUS.subscribe(macro);
        return this;
    }
}

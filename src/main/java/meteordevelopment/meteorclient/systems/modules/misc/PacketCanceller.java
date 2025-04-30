/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import static meteordevelopment.meteorclient.MeteorClient.LOG;

import java.nio.file.Path;
import java.util.Set;

import org.spongepowered.include.com.google.common.io.Files;

import com.fasterxml.jackson.databind.ObjectMapper;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;

public class PacketCanceller extends Module {
    enum LogMode {
        None,
        Log,
        Save,
        SaveJSON
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Set<Class<? extends Packet<?>>>> s2cPackets = sgGeneral.add(new PacketListSetting.Builder()
            .name("S2C-packets")
            .description("Server-to-client packets to cancel.")
            .filter(aClass -> PacketUtils.getS2CPackets().contains(aClass))
            .build());

    private final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = sgGeneral.add(new PacketListSetting.Builder()
            .name("C2S-packets")
            .description("Client-to-server packets to cancel.")
            .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
            .build());

    private final SettingGroup sgLog = settings.createGroup("Log");

    private final Setting<LogMode> log = sgLog.add(new EnumSetting.Builder<LogMode>()
            .name("log")
            .defaultValue(LogMode.None)
            .build());

    private final Setting<Boolean> logCancelled = sgLog.add(new BoolSetting.Builder()
            .name("cancelled")
            .description("Only log cancelled packets.")
            .defaultValue(true)
            .visible(() -> log.get() != LogMode.None)
            .build());

    private final Setting<Set<Class<? extends Packet<?>>>> logExclude = sgLog.add(new PacketListSetting.Builder()
            .name("exclude")
            .description("Packets to not log.")
            .defaultValue(Set.of(ClientTickEndC2SPacket.class, KeepAliveC2SPacket.class, KeepAliveS2CPacket.class))
            .visible(() -> log.get() != LogMode.None)
            .build());

    private final Path path = Path.of("logs", "packets");

    public PacketCanceller() {
        super(Categories.Misc, "packet-canceller", "Allows you to cancel certain packets.");
        runInMainMenu = true;
        path.toFile().mkdirs();
    }

    @Override
    public String getInfoString() {
        return c2sPackets.get().size() + " " + s2cPackets.get().size();
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onReceivePacket(final PacketEvent.Receive event) {
        final var c = event.packet.getClass();

        if (s2cPackets.get().contains(c))
            event.cancel();

        if (log.get() != LogMode.None && !logExclude.get().contains(c))
            if (!logCancelled.get() || event.isCancelled()) {
                LOG.info((event.isCancelled() ? "Drop " : "") + "Packet.Receive " + event.packet.getPacketType());
                save(event.packet);
            }
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onSendPacket(final PacketEvent.Send event) {
        final var c = event.packet.getClass();

        if (c2sPackets.get().contains(c))
            event.cancel();

        if (log.get() != LogMode.None && !logExclude.get().contains(c))
            if (logCancelled.get() && event.isCancelled()) {
                LOG.info("Cancel Packet.Send " + event.packet.getPacketType());
                save(event.packet);
            }
    }

    @EventHandler
    private void onSentPacket(final PacketEvent.Sent event) {
        if (log.get() != LogMode.None && !logCancelled.get() && !logExclude.get().contains(event.packet.getClass())) {
            LOG.info("Packet.Sent " + event.packet.getPacketType());
            save(event.packet);
        }
    }

    @SuppressWarnings("unchecked")
    private void save(final Packet<?> packet) {
        if (log.get() == LogMode.Log)
            return;
        final var time = System.nanoTime();
        final var mapper = new ObjectMapper();
        try {
            Files.write(
                    log.get() == LogMode.SaveJSON ? mapper.writeValueAsString(packet).getBytes()
                            : mapper.writeValueAsBytes(packet),
                    path.resolve(time + " " + PacketUtils.getName((Class<? extends Packet<?>>) packet.getClass()))
                            .toFile());
        } catch (final Exception e) {
        }
    }
}

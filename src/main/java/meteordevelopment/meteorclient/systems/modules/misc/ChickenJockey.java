package meteordevelopment.meteorclient.systems.modules.misc;

import java.util.HashSet;
import java.util.Set;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Formatting;

public class ChickenJockey extends Module {
    private static final SoundEvent SOUND = SoundEvent.of(MeteorClient.identifier("chicken_jockey"));

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> idChicken = sgGeneral.add(new BoolSetting.Builder()
            .name("id-chicken")
            .description("Name the chicken.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> idJockey = sgGeneral.add(new BoolSetting.Builder()
            .name("id-jockey")
            .description("Name jockeys.")
            .defaultValue(true)
            .build());

    private final SettingGroup sgAudio = settings.createGroup("Audio");

    private final Setting<Double> volume = sgAudio.add(new DoubleSetting.Builder()
            .name("volume")
            .defaultValue(10)
            .min(0)
            .sliderRange(0, 20)
            .build());

    private final Set<EntityPassengersSetS2CPacket> packets = new HashSet<>();

    public ChickenJockey() {
        super(Categories.Misc, "chicken-jockey", "CHICKEN JOCKEY!");
    }

    @Override
    public void onDeactivate() {
        packets.clear();
    }

    @EventHandler
    private void onPacketReceive(final PacketEvent.Receive event) {
        switch (event.packet) {
            case final EntityPassengersSetS2CPacket packet when packet.getPassengerIds().length > 0 ->
                packets.add(packet);
            default -> {
                return;
            }
        }
    }

    @EventHandler
    private void onTick(final TickEvent.Post event) {
        synchronized (packets) {
            for (final var i : packets) {
                final var chicken = mc.world.getEntityById(i.getEntityId());
                if (chicken == null)
                    continue;
                final var jockeys = new Entity[i.getPassengerIds().length];
                for (var j = 0; j < jockeys.length; j++)
                    if ((jockeys[j] = mc.world.getEntityById(i.getPassengerIds()[j])) == null)
                        continue;

                final var sb = new StringBuilder("" + Formatting.RED + Formatting.BOLD);
                sb.append(idChicken.get() ? chicken.getName().getString().toUpperCase() : "CHICKEN").append(" ");

                switch (jockeys.length) {
                    case 0 -> {
                        continue;
                    }
                    case 1 -> {
                        final var jockey = jockeys[0];
                        if (idJockey.get())
                            if (!(jockey.getType() == EntityType.ZOMBIE && ((ZombieEntity) jockey).isBaby()))
                                sb.append(jockey.getName().getString()).append(" ");
                        sb.append("JOCKEY");
                    }
                    default -> {
                        sb.append("JOCKEYS");
                    }
                }

                info(sb.append("!").toString().replace("%", "%%"));
                mc.world.playSoundClient(SOUND, SoundCategory.MASTER,
                        (float) (double) volume.get(),
                        0.5f + (float) Math.random() * 2);
            }
            packets.clear();
        }
    }
}

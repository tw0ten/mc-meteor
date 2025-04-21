package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

public class ChickenJockey extends Module {
    private static interface SOUNDS {
        SoundEvent CHICKEN_JOCKEY = soundIdentifier("chicken_jockey");
        SoundEvent CHICKEN = soundIdentifier("chicken");
        SoundEvent JOCKEY = soundIdentifier("jockey");
        SoundEvent WATER_BUCKET = soundIdentifier("water_bucket");
        SoundEvent RELEASE = soundIdentifier("release");
        SoundEvent FLINT_AND_STEEL = soundIdentifier("flint_and_steel");
        SoundEvent AN_ENDER_PEARL = soundIdentifier("an_ender_pearl");
        SoundEvent THE_OVERWORLD = soundIdentifier("welcome_to_the_overworld");
        SoundEvent COMING_IN_HOT = soundIdentifier("coming_in_hot");
        SoundEvent CRAFTING_TABLE = soundIdentifier("crafting_table");
        SoundEvent ELYTRA_WINGSUIT = soundIdentifier("elytra_wingsuits");
        SoundEvent FIREWORK_ROCKET = soundIdentifier("firework_rockets");
        SoundEvent THE_NETHER = soundIdentifier("the_nether");
    }

    private static SoundEvent soundIdentifier(final String name) {
        return SoundEvent.of(MeteorClient.identifier("chicken_jockey" + "." + name));
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> waterBucketRelease = sgGeneral.add(new BoolSetting.Builder()
            .name("water-bucket-release")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> flintAndSteel = sgGeneral.add(new BoolSetting.Builder()
            .name("flint-and-steel")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> dimensions = sgGeneral.add(new BoolSetting.Builder()
            .name("dimensions")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> enderPearl = sgGeneral.add(new BoolSetting.Builder()
            .name("ender-pearl")
            .defaultValue(true)
            .build());

    private final SettingGroup sgCickenJockey = settings.createGroup("Chicken Jockey");

    private final Setting<Boolean> chickenJockeyEnabled = sgCickenJockey.add(new BoolSetting.Builder()
            .name("enable")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> noJockeys = sgCickenJockey.add(new BoolSetting.Builder()
            .name("no-jockeys")
            .description("Chicken with no jockeys is still a chicken though.")
            .defaultValue(true)
            .visible(chickenJockeyEnabled::get)
            .build());

    private final Setting<Boolean> idChicken = sgCickenJockey.add(new BoolSetting.Builder()
            .name("id-chicken")
            .description("Name the chicken.")
            .defaultValue(true)
            .visible(chickenJockeyEnabled::get)
            .build());

    private final Setting<Boolean> idJockey = sgCickenJockey.add(new BoolSetting.Builder()
            .name("id-jockey")
            .description("Name jockeys.")
            .defaultValue(false)
            .visible(chickenJockeyEnabled::get)
            .build());

    private final SettingGroup sgAudio = settings.createGroup("Audio");

    private final Setting<Double> volume = sgAudio.add(new DoubleSetting.Builder()
            .name("volume")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 2)
            .build());

    public ChickenJockey() {
        super(Categories.Misc, "chicken-jockey", "brainrot");
    }

    @Override
    public void info(final String message, final Object... args) {
        super.info("%s%s" + message + "!", Formatting.RED, Formatting.BOLD, args);
    }

    @EventHandler
    private void onEntityAdded(final EntityAddedEvent event) {
        if (chickenJockeyEnabled.get()) { // TODO: ????
            if (event.entity.hasVehicle())
                chickenJockey(event.entity.getVehicle());
            if (event.entity.hasPassengers())
                chickenJockey(event.entity);
        }
        if (enderPearl.get() && event.entity instanceof final EnderPearlEntity pearl)
            if (pearl.getOwner() != null && pearl.getOwner() instanceof final PlayerEntity p && p.equals(mc.player)) {
                playSound(SOUNDS.AN_ENDER_PEARL);
                info("AN ENDER PEARL");
            }
    }

    @EventHandler
    private void onPacketReceive(final PacketEvent.Receive event) {
        switch (event.packet) {
            case final EntityPassengersSetS2CPacket packet -> {
                if (!chickenJockeyEnabled.get())
                    return;

                final var i = mc.world.getEntityById(packet.getEntityId());
                if (i == null)
                    return;
                final var e = new Entity[packet.getPassengerIds().length];
                for (var j = 0; j < e.length; j++)
                    if ((e[j] = mc.world.getEntityById(packet.getPassengerIds()[j])) == null) {
                        chickenJockey(i);
                        return;
                    }
                chickenJockey(i, e);
            }
            case final PlayerRespawnS2CPacket packet -> { // TODO
                if (!dimensions.get())
                    return;
                if (PlayerUtils.getDimension() == Dimension.Nether) {
                    info("THE NETHER");
                    playSound(SOUNDS.THE_NETHER);
                }
                if (PlayerUtils.getDimension() == Dimension.Overworld) {
                    info("THE OVERWORLD");
                    playSound(SOUNDS.THE_OVERWORLD);
                }
            }
            default -> {
                return;
            }
        }
    }

    @EventHandler
    private void onInteractBlock(final InteractBlockEvent event) {
        final var item = event.hand == Hand.MAIN_HAND ? mc.player.getInventory().getSelectedStack()
                : mc.player.getInventory().getStack(SlotUtils.OFFHAND);
        switch (item.getItem()) {
            case final FlintAndSteelItem i when flintAndSteel.get() -> {
                info("FLINT AND STEEL");
                playSound(SOUNDS.FLINT_AND_STEEL);
            }
            case final BucketItem i when waterBucketRelease.get() -> {
                if (i == Items.WATER_BUCKET) {
                    info("RELEASE");
                    playSound(SOUNDS.RELEASE);
                }
                if (i == Items.BUCKET
                        && mc.world.getBlockState(event.result.getBlockPos()).getFluidState().isOf(Fluids.WATER)) {
                    info("WATER BUCKET"); // TODO
                    playSound(SOUNDS.WATER_BUCKET);
                }
            }
            default -> {
                return;
            }
        }
    }

    @EventHandler
    private void onOpenScreen(final OpenScreenEvent event) {
        if (event.screen instanceof CraftingScreen) {
            playSound(SOUNDS.CRAFTING_TABLE);
            info("THIS... IS A CRAFTING TABLE");
        }
    }

    private void playSound(final SoundEvent i) {
        mc.world.playSoundClient(i, SoundCategory.MASTER,
                (float) (double) volume.get(),
                0.5f + (float) Math.random() * 2);
    }

    private void chickenJockey(final Entity chicken) {
        chickenJockey(chicken, chicken.getPassengerList().toArray(Entity[]::new));
    }

    private void chickenJockey(final Entity chicken, final Entity[] jockeys) {
        final var sb = new StringBuilder(idChicken.get() ? chicken.getName().getString().toUpperCase() : "CHICKEN");
        sb.append(" ");

        switch (jockeys.length) {
            case 0 -> {
                if (noJockeys.get()) {
                    sb.delete(sb.length() - 1, sb.length());
                    playSound(SOUNDS.CHICKEN);
                    break;
                }
                return;
            }
            case 1 -> {
                final var jockey = jockeys[0];
                if (idJockey.get())
                    if (!(jockey.getType() == EntityType.ZOMBIE && ((ZombieEntity) jockey).isBaby()))
                        sb.append(jockey.getName().getString()).append(" ");
                sb.append("JOCKEY");
                playSound(SOUNDS.CHICKEN_JOCKEY);
            }
            default -> {
                playSound(SOUNDS.CHICKEN);
                for (var i = 0; i < jockeys.length; i++) {
                    sb.append(jockeys[i].getName().getString())
                            .append(i == jockeys.length - 1 ? "" : i == jockeys.length - 2 ? " &" : ",")
                            .append(" ");
                    playSound(SOUNDS.JOCKEY);
                }
                sb.append("JOCKEYS");
            }
        }

        info(sb.toString().replace("%", "%%"));
    }
}

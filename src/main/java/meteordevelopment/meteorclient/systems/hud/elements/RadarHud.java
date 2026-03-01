package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.MapColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class RadarHud extends HudElement {
    public static final HudElementInfo<RadarHud> INFO = new HudElementInfo<>(Hud.GROUP, "radar", "Minimap", RadarHud::new);

    private static final int s = 16;

    private static final Identifier MAP_TEXTURE = Identifier.of("textures/map/map_background_checkerboard.png");
    private static final Identifier PLAYER_ICON = Identifier.of("textures/map/decorations/player.png");
    private static final Identifier ENTITY_ICON = Identifier.of("textures/map/decorations/player_off_limits.png");

    // TODO: textures or something or a shader idk
    private final Map<Long, Color[][]> cache = Collections.synchronizedMap(new HashMap<>());

    private int chunks;
    private Color[][][][] data;
    private ChunkPos refPos = new ChunkPos(0, 0);
    private final Set<Chunk> update = Collections.synchronizedSet(new HashSet<>());

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> size = sgGeneral.add(new IntSetting.Builder()
        .name("size")
        .description("The width and height of the radar in blocks.")
        .defaultValue(11 * s - 1)
        .min(1)
        .sliderRange(1, (2 + 2 * mc.options.getViewDistance().getValue()) * s)
        .onChanged((i) -> {
            chunks = (i + s - 1) / s + 2;
            data = new Color[chunks][chunks][s][s];
            calculateSize();
            if (mc.player != null)
                update();
        })
        .build());

    private final SettingGroup sgScale = settings.createGroup("Scale");

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The global scale.")
        .defaultValue(1)
        .onChanged((i) -> calculateSize())
        .build());

    private final Setting<Double> scaleSelf = sgScale.add(new DoubleSetting.Builder()
        .name("self")
        .defaultValue(1.75)
        .build());

    private final Setting<Double> scalePlayers = sgScale.add(new DoubleSetting.Builder()
        .name("players")
        .defaultValue(1.5)
        .build());

    private final Setting<Double> scaleEntities = sgScale.add(new DoubleSetting.Builder()
        .name("entities")
        .defaultValue(1)
        .build());

    private final SettingGroup sgOther = settings.createGroup("Other");

    private final Setting<Boolean> mapTexture = sgOther.add(new BoolSetting.Builder()
        .name("map-texture")
        .defaultValue(true)
        .build());

    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<Integer> opacity = sgColors.add(new IntSetting.Builder()
        .name("opacity")
        .defaultValue(200)
        .range(0, 0xFF)
        .sliderRange(0, 0xFF)
        .build());

    private final Setting<SettingColor> selfColor = sgColors.add(new ColorSetting.Builder()
        .name("self")
        .defaultValue(Color.WHITE.copy().a(200))
        .build());

    private final Setting<SettingColor> playersColor = sgColors.add(new ColorSetting.Builder()
        .name("players")
        .defaultValue(new SettingColor(205, 205, 205, 127))
        .build());

    private final Setting<SettingColor> animalsColor = sgColors.add(new ColorSetting.Builder()
        .name("animals")
        .defaultValue(new SettingColor(145, 255, 145, 127))
        .build());

    private final Setting<SettingColor> waterAnimalsColor = sgColors.add(new ColorSetting.Builder()
        .name("water-animals")
        .defaultValue(new SettingColor(145, 145, 255, 127))
        .build());

    private final Setting<SettingColor> monstersColor = sgColors.add(new ColorSetting.Builder()
        .name("monsters")
        .defaultValue(new SettingColor(255, 145, 145, 127))
        .build());

    private final Setting<SettingColor> ambientColor = sgColors.add(new ColorSetting.Builder()
        .name("ambient")
        .defaultValue(new SettingColor(75, 75, 75, 127))
        .build());

    private final Setting<SettingColor> miscColor = sgColors.add(new ColorSetting.Builder()
        .name("misc")
        .defaultValue(new SettingColor(145, 145, 145, 127))
        .build());

    public RadarHud() {
        super(INFO);

        RainbowColors.register(() -> {
            selfColor.get().update();
            playersColor.get().update();
            animalsColor.get().update();
            waterAnimalsColor.get().update();
            monstersColor.get().update();
            ambientColor.get().update();
            miscColor.get().update();
        });

        size.onChanged();

        toggle();
        toggle();
    }

    @Override
    public void toggle() {
        super.toggle();
        clear();
        if (isActive())
            MeteorClient.EVENT_BUS.subscribe(this);
        else
            MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    @Override
    public void remove() {
        super.remove();
        if (isActive())
            toggle();
    }

    @Override
    public void render(final HudRenderer r) {
        final var scale = this.scale.get();

        if (mapTexture.get()) {
            final var border = getWidth() / (128 - 6 * 2) * 6;
            r.texture(MAP_TEXTURE, getX() - border, getY() - border,
                getWidth() + border * 2, getHeight() + border * 2, Color.WHITE.copy().a(opacity.get()));
        }

        if (mc.world == null)
            return;
        assert mc.player != null;

        {
            final var size = this.size.get();
            final var sx = mc.player.getBlockX() - size / 2 - refPos.getStartPos().getX();
            final var sy = mc.player.getBlockZ() - size / 2 - refPos.getStartPos().getZ();

            for (var x = 0; x < size; x++)
                for (var z = 0; z < size; z++) {
                    final var ax = x + sx;
                    final var az = z + sy;
                    if (ax < 0 || az < 0 || ax >= chunks * s || az >= chunks * s)
                        continue;
                    final var chunk = data[ax / s][az / s];
                    if (chunk == null)
                        continue;
                    final var color = chunk[ax % s][az % s];
                    if (color == null)
                        continue;
                    r.quad(getX() + x * scale, getY() + z * scale, scale, scale, color.a(opacity.get()));
                }
        }

        r.post(() -> {
            mc.world.getEntities().forEach(e -> {
                if (e != mc.player)
                    entity(r, e);
            });
            entity(r, mc.player);
        });
    }

    @Override
    public void tick(final HudRenderer renderer) {
        super.tick(renderer);
        if (update.isEmpty())
            return;
        update.removeIf(i -> {
            cache(i);
            return true;
        });
        update();
    }

    private void entity(final HudRenderer r, final Entity entity) {
        assert mc.player != null;
        var x = entity.getEntityPos().x - mc.player.getEntityPos().x + size.get() / 2;
        var y = entity.getEntityPos().z - mc.player.getEntityPos().z + size.get() / 2;
        if (x < 0 || y < 0 || x > size.get() || y > size.get())
            return;
        x *= scale.get();
        x += getX();
        y *= scale.get();
        y += getY();

        if (entity instanceof final PlayerEntity player) {
            player(player, x, y);
            return;
        }

        final var s = 10 * scaleEntities.get() * scale.get();
        r.texture(ENTITY_ICON, x - s / 2, y - s / 2, s, s,
            switch (entity.getType().getSpawnGroup()) {
                case CREATURE -> animalsColor.get();
                case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> waterAnimalsColor.get();
                case MONSTER -> monstersColor.get();
                case AMBIENT -> ambientColor.get();
                default -> miscColor.get();
            });
    }

    private void player(final PlayerEntity player, final double x, final double y) {
        final var s = 10 * scale.get() * (player == mc.player ? scaleSelf.get() : scalePlayers.get());
        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x - s / 2, y - s / 2, s, s,
            player.headYaw - 180,
            0, 0 - 1 / 8d, 1 + 1 / 8d, 1,
            player == mc.player ? selfColor.get() : PlayerUtils.getPlayerColor(player, playersColor.get()));
        final var texture = mc.getTextureManager().getTexture(PLAYER_ICON);
        Renderer2D.TEXTURE.render(texture.getGlTextureView(), texture.getSampler());
    }

    private void cache(final Chunk c) {
        cache.put(c.getPos().toLong(), map(c, mc.world));
    }

    private static Color[][] map(final Chunk c, final World w) {
        final var out = new Color[s][s];
        final var bp = new BlockPos.Mutable();

        for (var x = 0; x < s; x++) {
            bp.setX(x);
            for (var z = 0; z < s; z++) {
                bp.setZ(z);

                for (var y = w.getDimension().hasCeiling()
                    ? 64
                    : c.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z); y >= c.getBottomY(); y--) {
                    final var block = c.getBlockState(bp.setY(y));
                    if (block.getMapColor(w, bp) == MapColor.CLEAR)
                        continue;

                    break;
                }

                out[x][z] = new Color(c.getBlockState(bp).getMapColor(w, bp).color);
            }
        }

        return out;
    }


    private void calculateSize() {
        final var scale = this.scale.get();
        final var size = this.size.get();
        setSize(scale * size, scale * size);
    }

    @EventHandler
    private void onBlockUpdate(final BlockUpdateEvent event) {
        assert mc.world != null;
        update.add(mc.world.getChunk(event.pos));
    }

    @EventHandler
    private void onPacketReceive(final PacketEvent.Receive event) {
        switch (event.packet) {
            case final UnloadChunkS2CPacket p -> {
                final var pos = p.pos();
                update.removeIf(e -> e.getPos().equals(pos));
                cache.remove(pos.toLong());
            }
            case final PlayerRespawnS2CPacket p -> clear();
            default -> {
                return;
            }
        }
    }

    @EventHandler
    private void onChunkData(final ChunkDataEvent event) {
        cache(event.chunk());
        update();
    }

    @EventHandler
    private void onGameLeft(final GameLeftEvent event) {
        clear();
    }

    private void update() {
        assert mc.player != null;
        final var p = mc.player.getChunkPos();
        refPos = new ChunkPos(p.x - chunks / 2, p.z - chunks / 2);
        for (var x = 0; x < chunks; x++)
            for (var z = 0; z < chunks; z++)
                data[x][z] = cache.get(ChunkPos.toLong(refPos.x + x, refPos.z + z));
    }

    private void clear() {
        update.clear();
        cache.clear();

        if (mc.world != null && isActive()) {
            Utils.chunks().forEach(this::cache);
            update();
        }
    }

}

package me.meow.lapisdiamond;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class LapisDiamond extends JavaPlugin implements Listener {

    private boolean enabled = true;

    private BlockData diamondOre;
    private BlockData deepslateDiamondOre;
    private BlockData air;

    private static final String BREAK_SOUND = "minecraft:lapis_break";

    @Override
    public void onEnable() {

        diamondOre = Bukkit.createBlockData(Material.DIAMOND_ORE);
        deepslateDiamondOre =
                Bukkit.createBlockData(Material.DEEPSLATE_DIAMOND_ORE);
        air = Bukkit.createBlockData(Material.AIR);

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("LapisDiamond enabled!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(
                this,
                () -> refreshPlayer(player),
                20L
        );
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {

        if (!enabled) {
            return;
        }

        Chunk chunk = event.getChunk();

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!player.getWorld().equals(chunk.getWorld())) {
                continue;
            }

            int playerChunkX =
                    player.getLocation().getBlockX() >> 4;

            int playerChunkZ =
                    player.getLocation().getBlockZ() >> 4;

            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();

            int distanceX =
                    Math.abs(playerChunkX - chunkX);

            int distanceZ =
                    Math.abs(playerChunkZ - chunkZ);

            if (distanceX <= Bukkit.getViewDistance()
                    && distanceZ <= Bukkit.getViewDistance()) {

                disguiseChunk(player, chunk);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        if (!enabled) {
            return;
        }

        Block block = event.getBlock();
        Material type = block.getType();

        if (type != Material.LAPIS_ORE
                && type != Material.DEEPSLATE_LAPIS_ORE) {
            return;
        }

        // Play custom break sound to nearby players
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!player.getWorld().equals(block.getWorld())) {
                continue;
            }

            if (player.getLocation()
                    .distanceSquared(block.getLocation()) > 64 * 64) {
                continue;
            }

            player.playSound(
                    block.getLocation(),
                    BREAK_SOUND,
                    SoundCategory.BLOCKS,
                    1.0f,
                    1.0f
            );
        }

        // Let the server break the real Lapis Ore.
        // The client will be updated to AIR afterward.
        Bukkit.getScheduler().runTaskLater(
                this,
                () -> {

                    if (block.getType() == Material.AIR) {

                        for (Player player :
                                Bukkit.getOnlinePlayers()) {

                            if (!player.getWorld()
                                    .equals(block.getWorld())) {
                                continue;
                            }

                            player.sendBlockChange(
                                    block.getLocation(),
                                    air
                            );
                        }
                    }

                },
                1L
        );
    }

    private void disguiseBlock(
            Player player,
            Block block) {

        Material type = block.getType();

        if (type == Material.LAPIS_ORE) {

            player.sendBlockChange(
                    block.getLocation(),
                    diamondOre
            );

        } else if (type == Material.DEEPSLATE_LAPIS_ORE) {

            player.sendBlockChange(
                    block.getLocation(),
                    deepslateDiamondOre
            );
        }
    }

    private void disguiseChunk(
            Player player,
            Chunk chunk) {

        if (!enabled) {
            return;
        }

        int minY =
                player.getWorld().getMinHeight();

        int maxY =
                player.getWorld().getMaxHeight();

        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {

                for (int y = minY; y < maxY; y++) {

                    Block block =
                            chunk.getBlock(x, y, z);

                    disguiseBlock(player, block);
                }
            }
        }
    }

    private void refreshPlayer(Player player) {

        if (!enabled) {
            return;
        }

        int centerX =
                player.getLocation().getBlockX() >> 4;

        int centerZ =
                player.getLocation().getBlockZ() >> 4;

        int viewDistance =
                Bukkit.getViewDistance();

        for (int x = centerX - viewDistance;
             x <= centerX + viewDistance;
             x++) {

            for (int z = centerZ - viewDistance;
                 z <= centerZ + viewDistance;
                 z++) {

                if (!player.getWorld()
                        .isChunkLoaded(x, z)) {
                    continue;
                }

                Chunk chunk =
                        player.getWorld()
                                .getChunkAt(x, z);

                disguiseChunk(player, chunk);
            }
        }
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        if (!command.getName()
                .equalsIgnoreCase("lapisdiamond")) {

            return false;
        }

        if (args.length == 0) {

            sender.sendMessage(
                    "LapisDiamond: "
                            + (enabled ? "ON" : "OFF")
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {

            enabled = true;

            for (Player player :
                    Bukkit.getOnlinePlayers()) {

                refreshPlayer(player);
            }

            sender.sendMessage(
                    "LapisDiamond da BAT!"
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {

            enabled = false;

            sender.sendMessage(
                    "LapisDiamond da TAT!"
            );

            return true;
        }

        sender.sendMessage(
                "Dung: /lapisdiamond <on|off>"
        );

        return true;
    }
}

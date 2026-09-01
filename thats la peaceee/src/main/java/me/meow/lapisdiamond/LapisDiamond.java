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

    private static final String BREAK_SOUND =
            "lapisdiamond.lapis_break";

    @Override
    public void onEnable() {

        diamondOre =
                Bukkit.createBlockData(Material.DIAMOND_ORE);

        deepslateDiamondOre =
                Bukkit.createBlockData(Material.DEEPSLATE_DIAMOND_ORE);

        air =
                Bukkit.createBlockData(Material.AIR);

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("LapisDiamond enabled!");
    }

    // =========================================================
    // PLAYER JOIN
    // =========================================================

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

    // =========================================================
    // CHUNK LOAD
    // =========================================================

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

    // =========================================================
    // BLOCK BREAK
    // =========================================================

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

        // Phát sound custom cho người chơi gần block
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

        /*
         * Không cancel event.
         *
         * Server vẫn phá LAPIS_ORE thật.
         * Vì vậy drop vẫn là Lapis Lazuli.
         */

        Bukkit.getScheduler().runTaskLater(
                this,
                () -> {

                    // Nếu đã phá thành AIR,
                    // gửi AIR cho client.
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

    // =========================================================
    // DISGUISE BLOCK
    // =========================================================

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

    // =========================================================
    // DISGUISE CHUNK
    // =========================================================

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

    // =========================================================
    // REFRESH PLAYER
    // =========================================================

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

    // =========================================================
    // COMMAND
    // =========================================================

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

        // /lapisdiamond
        if (args.length == 0) {

            sender.sendMessage(
                    "§eLapisDiamond: "
                    + (enabled ? "§aON" : "§cOFF")
            );

            return true;
        }

        // /lapisdiamond on
        if (args[0].equalsIgnoreCase("on")) {

            enabled = true;

            for (Player player :
                    Bukkit.getOnlinePlayers()) {

                refreshPlayer(player);
            }

            sender.sendMessage(
                    "§aLapisDiamond đã BẬT!"
            );

            return true;
        }

        // /lapisdiamond off
        if (args[0].equalsIgnoreCase("off")) {

            enabled = false;

            sender.sendMessage(
                    "§cLapisDiamond đã TẮT!"
            );

            return true;
        }

        sender.sendMessage(
                "§cDùng: /lapisdiamond <on|off>"
        );

        return true;
    }
}

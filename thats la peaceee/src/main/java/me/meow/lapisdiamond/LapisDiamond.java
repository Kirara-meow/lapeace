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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class LapisDiamond extends JavaPlugin implements Listener {

    private boolean enabled = true;

    private BlockData diamondOre;
    private BlockData deepslateDiamondOre;

    private static final String BREAK_SOUND =
            "lapisdiamond:lapis_break";

    @Override
    public void onEnable() {

        diamondOre = Bukkit.createBlockData(Material.DIAMOND_ORE);

        deepslateDiamondOre =
                Bukkit.createBlockData(Material.DEEPSLATE_DIAMOND_ORE);

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("=================================");
        getLogger().info("LapisDiamond enabled!");
        getLogger().info("Lapis Ore -> Diamond Ore");
        getLogger().info("Deepslate Lapis Ore -> Deepslate Diamond Ore");
        getLogger().info("=================================");

        /*
         * Refresh người chơi sau khi plugin bật.
         */
        Bukkit.getScheduler().runTaskLater(
                this,
                () -> {
                    if (!enabled) {
                        return;
                    }

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        refreshPlayer(player);
                    }
                },
                40L
        );
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

        /*
         * Đợi client nhận chunk xong rồi mới gửi fake blocks.
         */
        Bukkit.getScheduler().runTaskLater(
                this,
                () -> {

                    if (!player.isOnline()) {
                        return;
                    }

                    refreshPlayer(player);

                },
                40L
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

        /*
         * Không gửi ngay lập tức.
         *
         * Đợi client nhận chunk trước.
         */
        Bukkit.getScheduler().runTaskLater(
                this,
                () -> {

                    if (!enabled) {
                        return;
                    }

                    for (Player player : Bukkit.getOnlinePlayers()) {

                        if (!player.getWorld().equals(chunk.getWorld())) {
                            continue;
                        }

                        int playerChunkX =
                                player.getLocation().getBlockX() >> 4;

                        int playerChunkZ =
                                player.getLocation().getBlockZ() >> 4;

                        int distanceX =
                                Math.abs(playerChunkX - chunk.getX());

                        int distanceZ =
                                Math.abs(playerChunkZ - chunk.getZ());

                        int viewDistance =
                                Bukkit.getViewDistance();

                        if (distanceX <= viewDistance
                                && distanceZ <= viewDistance) {

                            disguiseChunk(player, chunk);
                        }
                    }

                },
                5L
        );
    }

    // =========================================================
    // PLAYER MOVE
    // =========================================================

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        if (!enabled) {
            return;
        }

        if (event.getFrom().getBlockX() >> 4
                == event.getTo().getBlockX() >> 4
                && event.getFrom().getBlockZ() >> 4
                == event.getTo().getBlockZ() >> 4) {

            return;
        }

        Player player = event.getPlayer();

        /*
         * Người chơi vừa sang chunk khác.
         * Đợi client nhận chunk rồi refresh.
         */
        Bukkit.getScheduler().runTaskLater(
                this,
                () -> {

                    if (player.isOnline() && enabled) {
                        refreshPlayer(player);
                    }

                },
                5L
        );
    }

    // =========================================================
    // TELEPORT
    // =========================================================

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {

        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(
                this,
                () -> {

                    if (player.isOnline() && enabled) {
                        refreshPlayer(player);
                    }

                },
                20L
        );
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

        /*
         * Chỉ xử lý Lapis Ore.
         */
        if (type != Material.LAPIS_ORE
                && type != Material.DEEPSLATE_LAPIS_ORE) {

            return;
        }

        /*
         * Phát custom sound.
         */
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
         * KHÔNG cancel event.
         *
         * Server vẫn phá Lapis thật.
         * Người chơi vẫn nhận Lapis Lazuli.
         *
         * Sau khi server phá xong, gửi AIR cho client
         * để fake Diamond Ore biến mất.
         */
        Bukkit.getScheduler().runTask(
                this,
                () -> {

                    for (Player player : Bukkit.getOnlinePlayers()) {

                        if (!player.getWorld()
                                .equals(block.getWorld())) {
                            continue;
                        }

                        player.sendBlockChange(
                                block.getLocation(),
                                Material.AIR.createBlockData()
                        );
                    }

                }
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

        if (!player.isOnline()) {
            return;
        }

        /*
         * Chỉ tìm Lapis Ore.
         *
         * Không cần làm gì với các block khác.
         */
        int minY =
                chunk.getWorld().getMinHeight();

        int maxY =
                chunk.getWorld().getMaxHeight();

        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {

                for (int y = minY; y < maxY; y++) {

                    Block block =
                            chunk.getBlock(x, y, z);

                    Material type =
                            block.getType();

                    if (type == Material.LAPIS_ORE) {

                        player.sendBlockChange(
                                block.getLocation(),
                                diamondOre
                        );

                    } else if (
                            type == Material.DEEPSLATE_LAPIS_ORE) {

                        player.sendBlockChange(
                                block.getLocation(),
                                deepslateDiamondOre
                        );
                    }
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

        if (!player.isOnline()) {
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

                disguiseChunk(
                        player,
                        chunk
                );
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
                    + (enabled
                    ? "§aON"
                    : "§cOFF")
            );

            return true;
        }

        // /lapisdiamond on
        if (args[0].equalsIgnoreCase("on")) {

            enabled = true;

            sender.sendMessage(
                    "§aLapisDiamond đã BẬT!"
            );

            for (Player player :
                    Bukkit.getOnlinePlayers()) {

                Bukkit.getScheduler().runTaskLater(
                        this,
                        () -> refreshPlayer(player),
                        5L
                );
            }

            return true;
        }

        // /lapisdiamond off
        if (args[0].equalsIgnoreCase("off")) {

            enabled = false;

            sender.sendMessage(
                    "§cLapisDiamond đã TẮT!"
            );

            /*
             * Reload chunk cho player để trả texture
             * về block thật.
             */
            for (Player player :
                    Bukkit.getOnlinePlayers()) {

                player.sendMessage(
                        "§7LapisDiamond OFF."
                );
            }

            return true;
        }

        sender.sendMessage(
                "§cDùng: /lapisdiamond <on|off>"
        );

        return true;
    }
}

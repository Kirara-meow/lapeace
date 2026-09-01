
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

    // BlockData mà người chơi sẽ nhìn thấy
    private BlockData diamondData;

    // Tên sound custom trong Resource Pack
    private static final String BREAK_SOUND =
            "lapisdiamond.lapis_break";

    @Override
    public void onEnable() {

        // Diamond Ore dùng để giả lập hiển thị
        diamondData = Bukkit.createBlockData(Material.DIAMOND_ORE);

        // Đăng ký event
        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("=================================");
        getLogger().info("LapisDiamond đã được bật!");
        getLogger().info("Lapis Ore -> Diamond Ore");
        getLogger().info("Drop vẫn là Lapis Lazuli");
        getLogger().info("=================================");
    }

    @Override
    public void onDisable() {

        getLogger().info("LapisDiamond đã tắt!");
    }

    /*
     * ==========================================
     * PLAYER JOIN
     * ==========================================
     */

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();

        // Chờ client load xong
        Bukkit.getScheduler().runTaskLater(
                this,
                () -> refreshLoadedChunks(player),
                20L
        );
    }

    /*
     * ==========================================
     * CHUNK LOAD
     * ==========================================
     */

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {

        if (!enabled) {
            return;
        }

        Chunk chunk = event.getChunk();

        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!player.getWorld().equals(chunk.getWorld())) {
                continue;
            }

            int playerChunkX =
                    player.getLocation().getBlockX() >> 4;

            int playerChunkZ =
                    player.getLocation().getBlockZ() >> 4;

            /*
             * Chỉ disguise chunk mà player đang đứng.
             */
            if (playerChunkX == chunkX &&
                playerChunkZ == chunkZ) {

                disguiseChunk(player, chunk);
            }
        }
    }

    /*
     * ==========================================
     * BLOCK BREAK
     * ==========================================
     */

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
        if (type != Material.LAPIS_ORE &&
            type != Material.DEEPSLATE_LAPIS_ORE) {

            return;
        }

        /*
         * ======================================
         * PHÁT ÂM THANH
         * ======================================
         *
         * Sound này nằm trong Resource Pack.
         */
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!player.getWorld().equals(block.getWorld())) {
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
         * Bukkit vẫn phá block thật.
         *
         * Vì block thật vẫn là Lapis Ore
         * nên drop vẫn là Lapis Lazuli.
         */

        Bukkit.getScheduler().runTaskLater(
                this,
                () -> updateBrokenBlock(block),
                1L
        );
    }

    /*
     * ==========================================
     * UPDATE SAU KHI PHÁ
     * ==========================================
     */

    private void updateBrokenBlock(Block block) {

        /*
         * Nếu block đã bị phá thì client phải
         * nhìn thấy AIR.
         */
        if (block.getType() == Material.AIR) {

            for (Player player : Bukkit.getOnlinePlayers()) {

                if (!player.getWorld().equals(block.getWorld())) {
                    continue;
                }

                player.sendBlockChange(
                        block.getLocation(),
                        Bukkit.createBlockData(Material.AIR)
                );
            }

            return;
        }

        /*
         * Trường hợp block vẫn còn là Lapis
         */
        updateBlockForPlayers(block);
    }

    /*
     * ==========================================
     * UPDATE BLOCK HIỂN THỊ
     * ==========================================
     */

    private void updateBlockForPlayers(Block block) {

        if (!enabled) {
            return;
        }

        Material type = block.getType();

        if (type != Material.LAPIS_ORE &&
            type != Material.DEEPSLATE_LAPIS_ORE) {

            return;
        }

        /*
         * Gửi Diamond Ore giả cho tất cả player
         */
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!player.getWorld().equals(block.getWorld())) {
                continue;
            }

            player.sendBlockChange(
                    block.getLocation(),
                    diamondData
            );
        }
    }

    /*
     * ==========================================
     * DISGUISE CHUNK
     * ==========================================
     */

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

        /*
         * Duyệt toàn bộ block trong chunk.
         */
        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {

                for (int y = minY; y < maxY; y++) {

                    Block block =
                            chunk.getBlock(x, y, z);

                    Material type =
                            block.getType();

                    /*
                     * Lapis Ore
                     */
                    if (type == Material.LAPIS_ORE ||
                        type == Material.DEEPSLATE_LAPIS_ORE) {

                        player.sendBlockChange(
                                block.getLocation(),
                                diamondData
                        );
                    }
                }
            }
        }
    }

    /*
     * ==========================================
     * REFRESH CHUNK
     * ==========================================
     */

    private void refreshLoadedChunks(Player player) {

        if (!enabled) {
            return;
        }

        int centerX =
                player.getLocation().getBlockX() >> 4;

        int centerZ =
                player.getLocation().getBlockZ() >> 4;

        /*
         * Lấy view distance server.
         */
        int viewDistance =
                Bukkit.getViewDistance();

        /*
         * Duyệt các chunk xung quanh player.
         */
        for (int x = centerX - viewDistance;
             x <= centerX + viewDistance;
             x++) {

            for (int z = centerZ - viewDistance;
                 z <= centerZ + viewDistance;
                 z++) {

                if (!player.getWorld().isChunkLoaded(x, z)) {
                    continue;
                }

                Chunk chunk =
                        player.getWorld().getChunkAt(x, z);

                disguiseChunk(player, chunk);
            }
        }
    }

    /*
     * ==========================================
     * COMMAND
     * ==========================================
     *
     * /lapisdiamond
     * /lapisdiamond on
     * /lapisdiamond off
     */

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

        /*
         * Không có argument
         */
        if (args.length == 0) {

            sender.sendMessage(
                    "§eLapisDiamond: §f"
                    + (enabled ? "ON" : "OFF")
            );

            sender.sendMessage(
                    "§7Dùng: §f/lapisdiamond <on|off>"
            );

            return true;
        }

        /*
         * ======================================
         * ON
         * ======================================
         */

        if (args[0].equalsIgnoreCase("on")) {

            enabled = true;

            /*
             * Disguise lại cho tất cả player
             */
            for (Player player :
                    Bukkit.getOnlinePlayers()) {

                refreshLoadedChunks(player);
            }

            sender.sendMessage(
                    "§a✔ LapisDiamond đã BẬT!"
            );

            sender.sendMessage(
                    "§7Lapis Ore hiện thành Diamond Ore."
            );

            return true;
        }

        /*
         * ======================================
         * OFF
         * ======================================
         */

        if (args[0].equalsIgnoreCase("off")) {

            enabled = false;

            /*
             * Reload chunk để trả block về
             * trạng thái thật.
             */
            for (Player player :
                    Bukkit.getOnlinePlayers()) {

                player.sendMessage(
                        "§cLapisDiamond đã TẮT!"
                );

                /*
                 * Gửi lại chunk thật cho client.
                 */
                player.getWorld().refreshChunk(
                        player.getLocation().getBlockX() >> 4,
                        player.getLocation().getBlockZ() >> 4
                );
            }

            sender.sendMessage(
                    "§c✔ LapisDiamond đã TẮT!"
            );

            return true;
        }

        /*
         * Sai command
         */
        sender.sendMessage(
                "§cDùng: /lapisdiamond <on|off>"
        );

        return true;
    }
}

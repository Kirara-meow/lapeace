Today 10:18 AM
bắt đầu lại từ đầu khúc này
Pasted code.java
File

Đúng, tạm thời bỏ qua texture và âm thanh. Mình xử lý trước lỗi đập Lapis nhưng block không đổi sang Diamond.

Vấn đề là code cũ dùng sendBlockChange() nhưng chỉ gửi một lần. Client có thể nhận lại trạng thái thật từ server nên Diamond biến mất.

Cách test đơn giản nhất: không cần Resource Pack.

Sửa LapisDiamond.java

Trong onBlockBreak(), thay toàn bộ phần xử lý bằng đoạn này:

@EventHandler
public void onBlockBreak(BlockBreakEvent event) {

    if (!enabled) return;

    Block block = event.getBlock();

    if (block.getType() != Material.LAPIS_ORE
            && block.getType() != Material.DEEPSLATE_LAPIS_ORE) {
        return;
    }

    // Sau khi block thật bị phá, gửi AIR cho client
    Bukkit.getScheduler().runTaskLater(this, () -> {

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!player.getWorld().equals(block.getWorld())) {
                continue;
            }

            player.sendBlockChange(
                    block.getLocation(),
                    Bukkit.createBlockData(Material.AIR)
            );
        }

    }, 1L);
}

Nhưng đoạn này chỉ xử lý block sau khi đập. Nếu ý bạn là:

Đang nhìn thấy Lapis → đập nó → trong lúc phá muốn nó hiện Diamond

thì cần xử lý animation/break progress, không chỉ BlockBreakEvent.

Quan trọng hơn: test /lapisdiamond on

Trước tiên vào server:

/lapisdiamond on

Sau đó đi tới một Lapis Ore mới, không phải block đã nhìn thấy từ trước.

Nếu vẫn nhìn thấy Lapis thì lỗi nằm ở phần disguiseChunk().

👉 Để mình sửa chính xác, bạn gửi toàn bộ nội dung LapisDiamond.java hiện tại (bản bạn đang build), mình sẽ viết lại nguyên file, tập trung trước vào:

Lapis thật → nhìn Diamond → đập → drop vẫn là Lapis.

Chưa đụng Resource Pack hay âm thanh cho tới khi phần này chạy ổn.

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

        if (**!**enabled) {

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

        if (**!**enabled) {

            return;

        }

        Chunk chunk = event.getChunk();

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (**!**player.getWorld().equals(chunk.getWorld())) {

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

        if (**!**enabled) {

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

            if (**!**player.getWorld().equals(block.getWorld())) {

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

*         * Không cancel event.*

*         **

*         * Server vẫn phá LAPIS_ORE thật.*

*         * Vì vậy drop vẫn là Lapis Lazuli.*

*         */*

        Bukkit.getScheduler().runTaskLater(

                this,

                () -> {

                    // Nếu đã phá thành AIR,

                    // gửi AIR cho client.

                    if (block.getType() == Material.AIR) {

                        for (Player player :

                                Bukkit.getOnlinePlayers()) {

                            if (**!**player.getWorld()

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

        if (**!**enabled) {

            return;

        }

        int minY =

                player.getWorld().getMinHeight();

        int maxY =

                player.getWorld().getMaxHeight();

        for (int x = 0; x < 16; x**++**) {

            for (int z = 0; z < 16; z**++**) {

                for (int y = minY; y < maxY; y**++**) {

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

        if (**!**enabled) {

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

             x**++**) {

            for (int z = centerZ - viewDistance;

                 z <= centerZ + viewDistance;

                 z**++**) {

                if (**!**player.getWorld()

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

        if (**!**command.getName()

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

Mình thấy code rồi. Có một lỗi quan trọng: code hiện tại không hề có đoạn biến Lapis thành Diamond khi người chơi nhìn thấy block nếu chunk không được load lại đúng thời điểm. sendBlockChange() cũng dễ bị server ghi đè.

Nếu mục tiêu trước mắt là test chắc chắn Lapis → Diamond, mình đề nghị đổi cách làm: khi người chơi đặt chân vào khu vực/chunk, plugin liên tục gửi lại các Lapis trong chunk. Nhưng quét toàn bộ 16 × 16 × chiều cao liên tục sẽ rất nặng.

Cách tốt hơn là dùng ProtocolLib/packet interception để thay block ngay khi server gửi chunk — nhưng đó là một dependency/plugin khác.

Nếu muốn giữ plugin hiện tại

Trước hết, hãy thử một bản test cực đơn giản để xác nhận sendBlockChange() hoạt động trên server của bạn.

Thêm vào onEnable():

getLogger().info("LapisDiamond TEST ENABLED");

và thêm command test:

if (args.length > 0 && args[0].equalsIgnoreCase("test")) {

    Player player = (Player) sender;

    Block block = player.getTargetBlockExact(10);

    if (block == null) {
        sender.sendMessage("§cKhông tìm thấy block.");
        return true;
    }

    sender.sendMessage(
            "§eBlock thật: §f" + block.getType()
    );

    player.sendBlockChange(
            block.getLocation(),
            diamondOre
    );

    sender.sendMessage(
            "§aĐã gửi Diamond Ore giả!"
    );

    return true;
}

Sau đó dùng:

/lapisdiamond test

Nhìn thẳng vào một Lapis Ore trong khoảng 10 block rồi chạy lệnh.

Nếu Lapis đổi thành Diamond → sendBlockChange() hoạt động, lỗi nằm ở phần tìm/quét Lapis.
Nếu vẫn là Lapis → vấn đề nằm ở API/client/server hoặc plugin đang chạy không phải JAR mới.
Mình nghi nhất trường hợp thứ hai

Bạn hãy vào console server và chạy:

/plugins

rồi xem LapisDiamond có màu xanh không.

Sau đó chạy:

/version

và:

/lapisdiamond

Nếu cả ba đều đúng, gửi mình ảnh console lúc server khởi động có dòng LapisDiamond enabled!.

Mình sẽ dựa đúng vào log đó để xác định tại sao JAR mới chạy lệnh nhưng sendBlockChange() không có hiệu lực, thay vì bắt bạn sửa code vòng vòng.

viết đủ file

Được. Đây là toàn bộ file LapisDiamond.java để bạn thay hết code cũ. Bản này tập trung trước vào việc Lapis Ore hiển thị thành Diamond Ore, còn block thật và drop vẫn là Lapis.

package me.meow.lapisdiamond;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
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

    @Override
    public void onEnable() {

        diamondOre = Bukkit.createBlockData(Material.DIAMOND_ORE);
        deepslateDiamondOre =
                Bukkit.createBlockData(Material.DEEPSLATE_DIAMOND_ORE);
        air = Bukkit.createBlockData(Material.AIR);

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("================================");
        getLogger().info("LapisDiamond enabled!");
        getLogger().info("Lapis Ore -> Diamond Ore");
        getLogger().info("================================");
    }

    // =========================================================
    // PLAYER JOIN
    // =========================================================

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        if (!enabled) {
            return;
        }

        Player player = event.getPlayer();

        // Đợi client load xong world
        Bukkit.getScheduler().runTaskLater(
                this,
                () -> refreshPlayer(player),
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

        // Chờ 1 tick để chunk gửi tới client
        Bukkit.getScheduler().runTaskLater(
                this,
                () -> {

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

                        if (distanceX <= Bukkit.getViewDistance()
                                && distanceZ <= Bukkit.getViewDistance()) {

                            disguiseChunk(player, chunk);
                        }
                    }

                },
                1L
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

        if (type != Material.LAPIS_ORE
                && type != Material.DEEPSLATE_LAPIS_ORE) {
            return;
        }

        /*
         * Không cancel event.
         *
         * Block thật vẫn là LAPIS_ORE.
         * Minecraft vẫn xử lý drop Lapis Lazuli bình thường.
         */

        // Sau khi block bị phá, báo client rằng vị trí này là AIR.
        Bukkit.getScheduler().runTaskLater(
                this,
                () -> {

                    for (Player player : Bukkit.getOnlinePlayers()) {

                        if (!player.getWorld().equals(block.getWorld())) {
                            continue;
                        }

                        player.sendBlockChange(
                                block.getLocation(),
                                air
                        );
                    }

                },
                1L
        );
    }

    // =========================================================
    // DISGUISE ONE BLOCK
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

        int minY = player.getWorld().getMinHeight();
        int maxY = player.getWorld().getMaxHeight();

        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {

                for (int y = minY; y < maxY; y++) {

                    Block block =
                            chunk.getBlock(x, y, z);

                    Material type = block.getType();

                    if (type == Material.LAPIS_ORE) {

                        player.sendBlockChange(
                                block.getLocation(),
                                diamondOre
                        );

                    } else if (type ==
                            Material.DEEPSLATE_LAPIS_ORE) {

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
                            + (enabled
                            ? "§aON"
                            : "§cOFF")
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
Sau khi thay code

1. Commit/push lên GitHub.

2. Chờ Actions build xanh ✅.

3. Tải .jar mới.

4. Xóa JAR cũ trong:

plugins/

rồi bỏ JAR mới vào.

5. Restart server, không dùng /reload.

6. Vào server và chạy:

/lapisdiamond on

Sau đó đi tới một khu vực có Lapis mới.

⚠️ Một điểm rất quan trọng: nếu với file hoàn chỉnh này mà Lapis vẫn không đổi, thì mình sẽ không bắt bạn sửa Java tiếp nữa. Khi đó gần như chắc chắn sendBlockChange() đang bị client/server ghi đè, và ta sẽ chuyển sang cách packet/Resource Pack, ổn định hơn.

Nếu vẫn không đổi, gửi mình ảnh console lúc server khởi động + ảnh /lapisdiamond, mình xử lý bước đó.

package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ObsidianCommand implements CommandExecutor {

    private final Obsidianwars plugin;
    
    // Arenada olan oyunçuları izləmək üçün
    public static final Map<UUID, String> playersInArena = new HashMap<>();

    public ObsidianCommand(Obsidianwars plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String key) {
        return MessagesConfigManager.getMessage(key);
    }

    private String getMessage(String key, String... replacements) {
        return MessagesConfigManager.getMessage(key, replacements);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player_only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        // 1. PLAY
        if (args[0].equalsIgnoreCase("play")) {
            List<String> arenaNames = ArenaConfigManager.getArenaNames();

            if (arenaNames.isEmpty()) {
                player.sendMessage(getMessage("arena_no_arenas"));
                return true;
            }

            // Yalnız READY statuslu arenaları axtarırıq
            String readyArena = null;
            for (String arenaName : arenaNames) {
                String status = ArenaConfigManager.getArenaStatus(arenaName);
                if ("READY".equals(status)) {
                    readyArena = arenaName;
                    break;
                }
            }

            if (readyArena == null) {
                player.sendMessage(getMessage("arena_not_ready"));
                return true;
            }

            String arenaName = readyArena;
            Location lobbyLocation = ArenaConfigManager.getLobbySpawn(arenaName);

            if (lobbyLocation == null) {
                player.sendMessage(getMessage("missing_lobby"));
                return true;
            }

            World world = lobbyLocation.getWorld();
            if (world == null) {
                player.sendMessage(getMessage("world_not_found", "worldName", "Unknown"));
                return true;
            }

            // Oyunçu limitini yoxlayırıq
            int currentPlayers = getArenaPlayerCount(arenaName);
            int maxPlayers = ArenaConfigManager.getMaxPlayers(arenaName);
            if (currentPlayers >= maxPlayers) {
                player.sendMessage(getMessage("arena_full", "current", String.valueOf(currentPlayers), "max", String.valueOf(maxPlayers)));
                return true;
            }

            player.teleport(lobbyLocation);

            // Reset player state
            resetPlayerState(player);

            // Oyunçunu arenada qeyd edirik
            playersInArena.put(player.getUniqueId(), arenaName);

            // IMMEDIATE AUTO-ASSIGNMENT: Assign to balanced team right away
            String assignedTeam = TeamManager.autoAssignTeam(player, arenaName);
            String teamName = assignedTeam.equals("red") ? "Qırmızı" : "Mavi";
            String teamColor = assignedTeam.equals("red") ? "§c" : "§9";
            player.sendMessage(MessagesConfigManager.getMessage("auto_team", "teamColor", teamColor, "teamName", teamName));

            // Lobby items veririk
            giveLobbyItems(player);

            // Scoreboard qururuq
            ScoreboardManager.updateScoreboard(player);

            // Broadcast mesajı
            broadcastToArena(arenaName, getMessage("player_joined", "player", player.getName(), "current", String.valueOf(getArenaPlayerCount(arenaName)), "max", String.valueOf(getMaxPlayers(arenaName))));

            player.sendMessage(getMessage("join_lobby", "arenaName", arenaName));

            // Play join sound
            String sound = MessagesConfigManager.getSound("join_lobby");
            Sound joinSound = Obsidianwars.parseSound(sound);
            if (joinSound != null) {
                player.playSound(player.getLocation(), joinSound, 1.0f, 1.0f);
            }

            // Send welcome message
            sendWelcomeMessage(player);

            // Schedule rules announcement after 5 seconds
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                sendRulesAnnouncement(player);
            }, 100L); // 5 seconds (100 ticks)

            // Oyun başlama şəraitini yoxlayırıq
            GameManager.checkGameStart(arenaName);
            
            return true;
        }

        // 2. WAND
        if (args[0].equalsIgnoreCase("wand")) {
            ItemStack wand = new ItemStack(Material.WOODEN_AXE);
            ItemMeta meta = wand.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6Obsidian Wars Wand");
                meta.setLore(Collections.singletonList("§eKüncləri seçmək üçün bloklara sol və sağ klikləyin."));
                wand.setItemMeta(meta);
            }
            player.getInventory().addItem(wand);
            player.sendMessage(getMessage("wand_received"));
            return true;
        }

        // 3. TEAM
        if (args[0].equalsIgnoreCase("team")) {
            // Yoxlayırıq ki, oyunçu arenada olub-olmadığını
            if (!playersInArena.containsKey(player.getUniqueId())) {
                player.sendMessage(getMessage("join_first"));
                return true;
            }

            // Check if countdown is 5 seconds or fewer - prevent team changes
            String arenaName = playersInArena.get(player.getUniqueId());
            int countdown = GameManager.getCountdown(arenaName);

            if (countdown > 0 && countdown <= 5) {
                player.sendMessage("§cKomanda dəyişdirilməsi son 5 saniyədə qadağandır!");
                return true;
            }

            openTeamSelectionGUI(player);
            return true;
        }

        // 4. LEAVE
        if (args[0].equalsIgnoreCase("leave")) {
            if (!playersInArena.containsKey(player.getUniqueId())) {
                player.sendMessage(getMessage("not_in_arena"));
                return true;
            }

            String arenaName = playersInArena.get(player.getUniqueId());

            // Check if in countdown state
            GameManager.ArenaGame game = GameManager.getGame(arenaName);
            boolean wasInCountdown = (game != null && game.getGameState() == GameManager.GameState.COUNTDOWN);
            boolean wasInGame = (game != null && (game.getGameState() == GameManager.GameState.PLAYING || game.getGameState() == GameManager.GameState.PREPARATION));

            // Clear disconnect record for this player (manual leave, not disconnect)
            GameManager.clearDisconnectRecord(player.getUniqueId());

            // Oyunçunu arenadan çıxarırıq
            playersInArena.remove(player.getUniqueId());
            TeamManager.removePlayerFromTeam(player);
            ParticleManager.removeSpawnProtection(player);

            // Reset player state and teleport to spawn
            Location mainSpawn = player.getWorld().getSpawnLocation();
            PlayerUtils.resetPlayerFull(player, mainSpawn);

            // Broadcast mesajı
            broadcastToArena(arenaName, getMessage("player_left", "player", player.getName()));

            player.sendMessage(getMessage("game_ended"));

            // CANCELLATION LOGIC: If player left during countdown, check if we need to cancel
            if (wasInCountdown) {
                if (TeamManager.isAnyTeamEmpty(arenaName) ||
                    TeamManager.getTotalPlayerCount(arenaName) < ArenaConfigManager.getMinPlayers(arenaName)) {
                    GameManager.cancelCountdown(arenaName);
                }
            }

            // WIN CONDITION: If player left during active game, check for team elimination
            if (wasInGame) {
                GameManager.checkTeamEliminationOnLeave(arenaName);
            }

            // Oyun statusunu yoxlayırıq
            GameManager.checkGameStart(arenaName);

            return true;
        }

        // 4. ARENA
        if (args[0].equalsIgnoreCase("arena")) {
            if (args.length < 2) {
                player.sendMessage("§cİstifadəsi: /obsidian arena <create|setlobby|setplayers>");
                return true;
            }

            String action = args[1];

            if (action.equalsIgnoreCase("create")) {
                if (args.length < 3) {
                    player.sendMessage("§cArena adı daxil edin: /obsidian arena create <ad>");
                    return true;
                }

                String arenaName = args[2];
                Location pos1 = WandListener.pos1Map.get(player.getUniqueId());
                Location pos2 = WandListener.pos2Map.get(player.getUniqueId());

                if (pos1 == null || pos2 == null) {
                    player.sendMessage(getMessage("wand_need_positions"));
                    return true;
                }

                if (ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_already_exists"));
                    return true;
                }

                ArenaConfigManager.createArenaConfig(arenaName);
                ArenaConfigManager.setArenaPosition(arenaName, "main", pos1, pos2);

                player.sendMessage(getMessage("arena_created", "arenaName", arenaName));
                return true;
            }

            if (action.equalsIgnoreCase("setlobby")) {
                if (args.length < 3) {
                    player.sendMessage("§cİstifadəsi: /obsidian arena setlobby <arenaAdı>");
                    return true;
                }
                String arenaName = args[2];

                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return true;
                }

                Location loc = player.getLocation();
                ArenaConfigManager.setLobbySpawn(arenaName, loc);

                player.sendMessage(getMessage("lobby_spawn_set", "arenaName", arenaName));
                return true;
            }

            if (action.equalsIgnoreCase("setplayers")) {
                if (args.length < 5) {
                    player.sendMessage("§cİstifadəsi: /obsidian arena setplayers <arenaAdı> <min> <max>");
                    return true;
                }
                String arenaName = args[2];

                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return true;
                }

                int min, max;
                try {
                    min = Integer.parseInt(args[3]);
                    max = Integer.parseInt(args[4]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number format! Usage: /obsidian arena setplayers <arena> <min> <max>");
                    return true;
                }

                if (min < 2) {
                    player.sendMessage("§cMinimum players must be at least 2!");
                    return true;
                }

                if (max < min) {
                    player.sendMessage("§cMaximum players must be greater than or equal to minimum players!");
                    return true;
                }

                ArenaConfigManager.setPlayerLimits(arenaName, min, max);

                player.sendMessage(getMessage("player_limits_set", "arenaName", arenaName, "min", String.valueOf(min), "max", String.valueOf(max)));
                return true;
            }

            if (action.equalsIgnoreCase("delete")) {
                if (args.length < 3) {
                    player.sendMessage("§cİstifadəsi: /obsidian arena delete <arenaAdı>");
                    return true;
                }
                String arenaName = args[2];

                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return true;
                }

                // Arenanı silirik
                ArenaConfigManager.deleteArenaConfig(arenaName);
                
                // GameManager-dən oyunu silirik
                GameManager.removeGame(arenaName);

                // Arenadakı oyunçuları təmizləyirik
                for (UUID uuid : new ArrayList<>(playersInArena.keySet())) {
                    if (playersInArena.get(uuid).equals(arenaName)) {
                        Player arenaPlayer = Bukkit.getPlayer(uuid);
                        if (arenaPlayer != null) {
                            // Oyunçunu arenadan çıxarırıq
                            playersInArena.remove(uuid);
                            TeamManager.removePlayerFromTeam(arenaPlayer);

                            // Reset player state and teleport to spawn
                            Location mainSpawn = arenaPlayer.getWorld().getSpawnLocation();
                            PlayerUtils.resetPlayerFull(arenaPlayer, mainSpawn);

                            arenaPlayer.sendMessage("§cArena silindi, siz arenadan çıxarıldınız!");
                        }
                    }
                }

                player.sendMessage(getMessage("arena_deleted", "arenaName", arenaName));
                return true;
            }

            if (action.equalsIgnoreCase("setspawn")) {
                if (args.length < 4) {
                    player.sendMessage("§cİstifadəsi: /obsidian arena setspawn <arenaAdı> <red|blue>");
                    return true;
                }
                String arenaName = args[2];
                String teamColor = args[3].toLowerCase();

                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return true;
                }

                if (!teamColor.equals("red") && !teamColor.equals("blue")) {
                    player.sendMessage(getMessage("invalid_team_color"));
                    return true;
                }

                Location loc = player.getLocation();
                ArenaConfigManager.setTeamSpawn(arenaName, teamColor, loc);

                String teamName = teamColor.equals("red") ? "Qırmızı" : "Mavi";
                String teamColorCode = teamColor.equals("red") ? "§c" : "§9";
                player.sendMessage(getMessage("team_spawn_set", "arenaName", arenaName, "teamColor", teamColorCode, "teamName", teamName));
                return true;
            }

            if (action.equalsIgnoreCase("setobsidian")) {
                if (args.length < 4) {
                    player.sendMessage("§cİstifadəsi: /obsidian arena setobsidian <arenaAdı> <red|blue>");
                    return true;
                }
                String arenaName = args[2];
                String teamColor = args[3].toLowerCase();

                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return true;
                }

                if (!teamColor.equals("red") && !teamColor.equals("blue")) {
                    player.sendMessage(getMessage("invalid_team_color"));
                    return true;
                }

                // Oyunçunun baxdığı bloku alırıq
                org.bukkit.block.Block targetBlock = player.rayTraceBlocks(5) != null ? player.rayTraceBlocks(5).getHitBlock() : null;
                if (targetBlock == null || targetBlock.getType() != Material.OBSIDIAN) {
                    player.sendMessage(getMessage("invalid_obsidian_target"));
                    return true;
                }

                Location obsidianLoc = targetBlock.getLocation();
                ArenaConfigManager.setObsidianLocation(arenaName, teamColor, obsidianLoc);

                String teamName = teamColor.equals("red") ? "Qırmızı" : "Mavi";
                String teamColorCode = teamColor.equals("red") ? "§c" : "§9";
                player.sendMessage(getMessage("obsidian_set", "arenaName", arenaName, "teamColor", teamColorCode, "teamName", teamName));
                return true;
            }

            if (action.equalsIgnoreCase("finish")) {
                if (args.length < 3) {
                    player.sendMessage("§cİstifadəsi: /obsidian arena finish <arenaAdı>");
                    return true;
                }
                String arenaName = args[2];

                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return true;
                }

                // Yoxlama siyahısı
                List<String> missingRequirements = new ArrayList<>();

                // 1. Min/Max oyunçu limiti
                if (ArenaConfigManager.getMinPlayers(arenaName) == 0 || ArenaConfigManager.getMaxPlayers(arenaName) == 0) {
                    missingRequirements.add("Oyunçu limitləri (setplayers)");
                }

                // 2. Lobby spawn nöqtəsi
                if (ArenaConfigManager.getLobbySpawn(arenaName) == null) {
                    missingRequirements.add("Lobi spawn nöqtəsi (setlobby)");
                }

                // 3. Red Team spawn
                if (ArenaConfigManager.getTeamSpawn(arenaName, "red") == null) {
                    missingRequirements.add("Qırmızı Komanda spawn (setspawn red)");
                }

                // 4. Blue Team spawn
                if (ArenaConfigManager.getTeamSpawn(arenaName, "blue") == null) {
                    missingRequirements.add("Mavi Komanda spawn (setspawn blue)");
                }

                // 5. Red Team obsidian
                if (ArenaConfigManager.getObsidianLocation(arenaName, "red") == null) {
                    missingRequirements.add("Qırmızı Komanda obsidian (setobsidian red)");
                }

                // 6. Blue Team obsidian
                if (ArenaConfigManager.getObsidianLocation(arenaName, "blue") == null) {
                    missingRequirements.add("Mavi Komanda obsidian (setobsidian blue)");
                }

                // 7. Resource blokları
                if (!ArenaConfigManager.hasResourceBlocks(arenaName)) {
                    missingRequirements.add("Resource blokları (setblocks)");
                }

                // Əgər məcburiyyətlər varsa, xəta göstəririk
                if (!missingRequirements.isEmpty()) {
                    player.sendMessage(getMessage("arena_incomplete"));
                    for (String req : missingRequirements) {
                        player.sendMessage("§c- " + req);
                    }
                    return true;
                }

                // Check for optional features
                List<String> optionalFeatures = new ArrayList<>();
                if (ArenaConfigManager.getMobAreaLocation(arenaName) == null) {
                    optionalFeatures.add("Mob spawn area (setmobarea) - İstəyə bağlı");
                }

                // Bütün yoxlamalar keçdi, statusu READY edirik
                ArenaConfigManager.setArenaStatus(arenaName, "READY");

                player.sendMessage(getMessage("arena_finished", "arenaName", arenaName));
                player.sendMessage("§aArena artıq oyunçular tərəfindən istifadə edilə bilər.");
                player.sendMessage("§eResource blok sayı: " + ArenaConfigManager.getResourceBlockCount(arenaName));
                
                if (!optionalFeatures.isEmpty()) {
                    player.sendMessage("§eİstəyə bağlı xüsusiyyətlər:");
                    for (String feature : optionalFeatures) {
                        player.sendMessage("§7- " + feature);
                    }
                }
                return true;
            }

            if (action.equalsIgnoreCase("forcestart")) {
                if (args.length < 3) {
                    player.sendMessage("§cİstifadəsi: /obsidian arena forcestart <arenaAdı>");
                    return true;
                }
                String arenaName = args[2];

                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return true;
                }

                // Oyun artıq başlayıbsa
                if (GameManager.getGame(arenaName) != null) {
                    player.sendMessage("§cBu arena artıq aktivdir!");
                    return true;
                }

                // Force start
                GameManager.forceStartGame(arenaName);
                player.sendMessage(getMessage("force_started", "arenaName", arenaName));
                return true;
            }

            if (action.equalsIgnoreCase("settimelimit")) {
                if (args.length < 4) {
                    player.sendMessage("§cİstifadəsi: /obsidian arena settimelimit <arenaAdı> <dəqiqə>");
                    return true;
                }
                String arenaName = args[2];
                int minutes;

                try {
                    minutes = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cZaman limiti bir rəqəm olmalıdır!");
                    return true;
                }

                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return true;
                }

                if (minutes < 5) {
                    player.sendMessage("§cZaman limiti minimum 5 dəqiqə olmalıdır!");
                    return true;
                }

                ArenaConfigManager.setTimeLimit(arenaName, minutes);
                player.sendMessage("§aArena zaman limiti " + minutes + " dəqiqə olaraq təyin edildi!");
                return true;
            }

            if (action.equalsIgnoreCase("end")) {
                if (args.length < 3) {
                    player.sendMessage("§cİstifadəsi: /obsidian arena end <arenaAdı>");
                    return true;
                }
                String arenaName = args[2];

                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return true;
                }

                // Oyunu bitiririk
                GameManager.endGame(arenaName, null); // null = admin ended, no winner
                player.sendMessage(getMessage("game_admin_ended"));
                return true;
            }

            if (action.equalsIgnoreCase("setblocks")) {
                if (args.length < 3) {
                    player.sendMessage("§cİstifadəsi: /obsidian arena setblocks <arenaAdı>");
                    return true;
                }
                String arenaName = args[2];

                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return true;
                }

                // Əgər oyunçu artıq setup mode-dadırsa, çıxarırıq
                if (ResourceBlockManager.isInSetupMode(player)) {
                    ResourceBlockManager.exitSetupMode(player);
                    return true;
                }

                // Setup mode-a daxil edirik
                ResourceBlockManager.enterSetupMode(player, arenaName);
                return true;
            }

            if (action.equalsIgnoreCase("setwall")) {
                if (args.length < 4) {
                    player.sendMessage("§cİstifadəsi: /obsidian arena setwall <arenaAdı> <red|blue>");
                    return true;
                }
                String arenaName = args[2];
                String teamColor = args[3].toLowerCase();

                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return true;
                }

                if (!teamColor.equals("red") && !teamColor.equals("blue")) {
                    player.sendMessage(getMessage("invalid_team_color"));
                    return true;
                }

                Location pos1 = WandListener.pos1Map.get(player.getUniqueId());
                Location pos2 = WandListener.pos2Map.get(player.getUniqueId());

                if (pos1 == null || pos2 == null) {
                    player.sendMessage(getMessage("wand_need_positions"));
                    return true;
                }

                WallManager.setWall(arenaName, teamColor, pos1, pos2);
                
                String teamName = teamColor.equals("red") ? "Qırmızı" : "Mavi";
                String teamColorCode = teamColor.equals("red") ? "§c" : "§9";
                player.sendMessage(getMessage("wall_set", "teamColor", teamColorCode, "teamName", teamName));
                return true;
            }

            if (action.equalsIgnoreCase("settimer")) {
                if (args.length < 4) {
                    player.sendMessage("§cİstifadəsi: /obsidian arena settimer <arenaAdı> <preparation_time> [sudden_death_time]");
                    return true;
                }
                String arenaName = args[2];

                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return true;
                }

                int prepMinutes;
                try {
                    prepMinutes = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cXəta: Hazırlıq müddəti rəqəm olmalıdır!");
                    return true;
                }

                if (prepMinutes < 1) {
                    player.sendMessage("§cPreparation time must be at least 1 minute!");
                    return true;
                }

                WallManager.setTimer(arenaName, prepMinutes);
                player.sendMessage(getMessage("timer_set", "arenaName", arenaName, "minutes", String.valueOf(prepMinutes)));

                // Optional sudden death timer
                if (args.length >= 5) {
                    int suddenDeathMinutes;
                    try {
                        suddenDeathMinutes = Integer.parseInt(args[4]);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cXəta: Sudden death müddəti rəqəm olmalıdır!");
                        return true;
                    }

                    if (suddenDeathMinutes < 1) {
                        player.sendMessage("§cSudden death time must be at least 1 minute!");
                        return true;
                    }

                    ArenaConfigManager.setSuddenDeathTimer(arenaName, suddenDeathMinutes);
                    player.sendMessage("§aSudden death müddəti " + suddenDeathMinutes + " dəqiqə olaraq təyin edildi!");
                }

                return true;
            }

            if (action.equalsIgnoreCase("setmobarea")) {
                if (args.length < 3) {
                    player.sendMessage("§cİstifadəsi: /obsidian arena setmobarea <arenaAdı>");
                    return true;
                }
                String arenaName = args[2];

                if (!ArenaConfigManager.arenaExists(arenaName)) {
                    player.sendMessage(getMessage("arena_not_found", "arenaName", arenaName));
                    return true;
                }

                Location loc = player.getLocation();
                MobSpawnerManager.setMobArea(arenaName, loc);

                player.sendMessage("§aMob spawn area set for arena " + arenaName + " at your current location!");
                player.sendMessage("§eWither Skeletons will spawn every 2 minutes during combat phase.");
                return true;
            }
        }

        // Unusulyn sub-komanda ýazylsa kömek menýusyny görkezýär
        sendHelp(player);
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(getMessage("help_header"));
        player.sendMessage(getMessage("help_play"));
        player.sendMessage(getMessage("help_team"));
        player.sendMessage(getMessage("help_leave"));
        player.sendMessage(getMessage("help_wand"));
        player.sendMessage(getMessage("help_arena_create"));
        player.sendMessage(getMessage("help_arena_setlobby"));
        player.sendMessage(getMessage("help_arena_setplayers"));
        player.sendMessage(getMessage("help_arena_setspawn"));
        player.sendMessage(getMessage("help_arena_setobsidian"));
        player.sendMessage(getMessage("help_arena_setblocks"));
        player.sendMessage(getMessage("help_arena_setwall"));
        player.sendMessage("§e/obsidian arena settimer <arenaAdı> <prep_time> [sudden_death] §7- Hazırlıq və sudden death müddəti təyin et");
        player.sendMessage("§e/obsidian arena setmobarea <arenaAdı> §7- Mob spawn bölgəsini təyin edir");
        player.sendMessage(getMessage("help_arena_finish"));
        player.sendMessage(getMessage("help_arena_forcestart"));
        player.sendMessage(getMessage("help_arena_end"));
        player.sendMessage(getMessage("help_arena_delete"));
    }

    public void openTeamSelectionGUI(Player player) {
        // 9-slotalı inventory yaradırıq
        Inventory teamGUI = plugin.getServer().createInventory(null, 9, "§6Komanda Seçimi");

        // Qırmızı Komanda (Red Team) - Red Wool
        ItemStack redTeam = new ItemStack(org.bukkit.Material.RED_WOOL);
        ItemMeta redMeta = redTeam.getItemMeta();
        if (redMeta != null) {
            redMeta.setDisplayName("§cQırmızı Komanda");
            
            // Qırmızı komandadakı oyunçuların siyahısı
            List<String> redLore = new ArrayList<>();
            redLore.add("§7Üzvlər:");
            
            List<String> redPlayers = getPlayersInTeam("red");
            if (redPlayers.isEmpty()) {
                redLore.add("§cHeç kim yoxdur (0 oyunçu)");
            } else {
                for (String p : redPlayers) {
                    redLore.add("§7- " + p);
                }
                redLore.add("§7Cəmi: " + redPlayers.size() + " oyunçu");
            }
            
            redMeta.setLore(redLore);
            redTeam.setItemMeta(redMeta);
        }
        teamGUI.setItem(3, redTeam); // Sol tərəf

        // Mavi Komanda (Blue Team) - Blue Wool
        ItemStack blueTeam = new ItemStack(org.bukkit.Material.BLUE_WOOL);
        ItemMeta blueMeta = blueTeam.getItemMeta();
        if (blueMeta != null) {
            blueMeta.setDisplayName("§9Mavi Komanda");
            
            // Mavi komandadakı oyunçuların siyahısı
            List<String> blueLore = new ArrayList<>();
            blueLore.add("§7Üzvlər:");
            
            List<String> bluePlayers = getPlayersInTeam("blue");
            if (bluePlayers.isEmpty()) {
                blueLore.add("§9Heç kim yoxdur (0 oyunçu)");
            } else {
                for (String p : bluePlayers) {
                    blueLore.add("§7- " + p);
                }
                blueLore.add("§7Cəmi: " + bluePlayers.size() + " oyunçu");
            }
            
            blueMeta.setLore(blueLore);
            blueTeam.setItemMeta(blueMeta);
        }
        teamGUI.setItem(5, blueTeam); // Sağ tərəf

        player.openInventory(teamGUI);
    }

    private List<String> getPlayersInTeam(String team) {
        List<String> players = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : TeamListener.playerTeams.entrySet()) {
            if (entry.getValue().equals(team)) {
                // Oyunçunun adını əldə edirik
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    players.add(player.getName());
                }
            }
        }
        return players;
    }

    private void giveLobbyItems(Player player) {
        // Komanda Seçimi Kompası (Slot 0)
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        if (compassMeta != null) {
            compassMeta.setDisplayName("§eKomanda Seçimi");
            compass.setItemMeta(compassMeta);
        }
        player.getInventory().setItem(0, compass);

        // Oyundan Çıxış Qapısı (Slot 8)
        ItemStack door = new ItemStack(Material.OAK_DOOR);
        ItemMeta doorMeta = door.getItemMeta();
        if (doorMeta != null) {
            doorMeta.setDisplayName("§cOyundan Çıx");
            door.setItemMeta(doorMeta);
        }
        player.getInventory().setItem(8, door);
    }

    public static void broadcastToArena(String arenaName, String message) {
        for (UUID uuid : playersInArena.keySet()) {
            if (playersInArena.get(uuid).equals(arenaName)) {
                Player arenaPlayer = Bukkit.getPlayer(uuid);
                if (arenaPlayer != null) {
                    arenaPlayer.sendMessage(message);
                }
            }
        }
    }

    private int getArenaPlayerCount(String arenaName) {
        int count = 0;
        for (String arena : playersInArena.values()) {
            if (arena.equals(arenaName)) {
                count++;
            }
        }
        return count;
    }

    private int getMaxPlayers(String arenaName) {
        return ArenaConfigManager.getMaxPlayers(arenaName);
    }

    private void resetPlayerState(Player player) {
        PlayerUtils.resetPlayerState(player);
    }

    private void sendWelcomeMessage(Player player) {
        List<String> welcomeLines = MessagesConfigManager.getMessagesConfig().getStringList("messages.welcome_message");
        for (String line : welcomeLines) {
            player.sendMessage(line);
        }
    }

    private void sendRulesAnnouncement(Player player) {
        List<String> rulesLines = MessagesConfigManager.getMessagesConfig().getStringList("messages.rules_announcement");
        for (String line : rulesLines) {
            player.sendMessage(line);
        }
    }

    public static void cleanup() {
        playersInArena.clear();
    }
}
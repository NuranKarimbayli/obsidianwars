package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GameListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if victim is a player
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();

        // Check if victim has spawn protection (global check, not just in arena)
        if (ParticleManager.hasSpawnProtection(victim)) {
            event.setCancelled(true);
            victim.sendMessage(MessagesConfigManager.getMessage("spawn_protection_warning"));

            // Optional: Send a message to the attacker if it's a player
            if (event.getDamager() instanceof Player) {
                Player attacker = (Player) event.getDamager();
                attacker.sendMessage("§c" + victim.getName() + " has spawn protection!");
            }
            return;
        }

        // Only process arena-specific damage if damager is a player
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player damager = (Player) event.getDamager();

        // Yalnız arenadakı oyunçular üçün
        if (!ObsidianCommand.playersInArena.containsKey(victim.getUniqueId()) ||
            !ObsidianCommand.playersInArena.containsKey(damager.getUniqueId())) {
            return;
        }

        // Spectators can't damage or be damaged
        if (victim.getGameMode() == org.bukkit.GameMode.SPECTATOR || damager.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }

        // Check game state - PvP is disabled during PREPARATION phase
        String arenaName = ObsidianCommand.playersInArena.get(victim.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        if (game != null) {
            if (game.getGameState() == GameManager.GameState.PREPARATION) {
                event.setCancelled(true);
                damager.sendMessage("§cPvP is disabled during preparation phase!");
                return;
            }
            // Log debug info for PLAYING state
            if (game.getGameState() == GameManager.GameState.PLAYING) {
                Obsidianwars.getInstance().getLogger().info("PvP check: Arena " + arenaName + " is in PLAYING state, allowing PvP");
            }
        } else {
            Obsidianwars.getInstance().getLogger().warning("PvP check: Game is null for arena " + arenaName);
        }

        String victimTeam = TeamListener.playerTeams.get(victim.getUniqueId());
        String damagerTeam = TeamListener.playerTeams.get(damager.getUniqueId());

        Obsidianwars.getInstance().getLogger().info("PvP check: Victim team=" + victimTeam + ", Damager team=" + damagerTeam);

        // Eyni komanda üzvləri bir-birinə zərər verə bilməz
        if (victimTeam != null && victimTeam.equals(damagerTeam)) {
            event.setCancelled(true);
            damager.sendMessage("§cEyni komanda üzvlərinə zərər verə bilməzsiniz!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Yalnız arenadakı oyunçular üçün
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        // Əgər oyun aktiv deyilsə və ya preparation deyilsə, blok qırmağı qadağan edirik
        if (game == null || (game.getGameState() != GameManager.GameState.PLAYING && game.getGameState() != GameManager.GameState.PREPARATION)) {
            event.setCancelled(true);
            return;
        }

        // Obsidian blokunu yoxlayırıq
        if (block.getType() == Material.OBSIDIAN) {
            event.setCancelled(true); // Blok qırmağı ləğv edirik, biz logic aparacağıq
            
            // Obsidian can only be destroyed during PLAYING state (not PREPARATION)
            if (game.getGameState() != GameManager.GameState.PLAYING) {
                player.sendMessage("§cObsidian can only be destroyed after preparation phase!");
                return;
            }
            
            // Bu blokun arena obsidianı olub-olmadığını yoxlayırıq
            String team = isArenaObsidian(arenaName, block.getLocation());
            if (team != null) {
                // Əgər obsidian artıq məhv edilibsə
                if (game.isObsidianDestroyed(team)) {
                    player.sendMessage(MessagesConfigManager.getMessage("already_destroyed"));
                    return;
                }

                // Oyunçunun öz komandasının obsidianını qıra bilməz
                String playerTeam = TeamListener.playerTeams.get(player.getUniqueId());
                if (playerTeam != null && playerTeam.equals(team)) {
                    player.sendMessage(MessagesConfigManager.getMessage("cant_destroy_own"));
                    return;
                }

                // Obsidianı məhv edirik
                destroyObsidian(arenaName, team, player, block);
            }
        } else {
            // Resource bloklar üçün yoxlayırıq
            boolean isResourceBlock = false;
            try {
                isResourceBlock = ArenaFileManager.isResourceBlock(arenaName, block.getLocation());
            } catch (Exception e) {
                // ArenaFileManager problemi varsa, sadə qadağa
                event.setCancelled(true);
                player.sendMessage(Obsidianwars.getInstance().getConfig().getString("messages.cant_break_block"));
                return;
            }
            
            if (isResourceBlock) {
                // Resource blok - spawn color-matched particle effect
                ParticleManager.spawnResourceBreakParticle(block.getLocation(), block.getType());
            }
            // REMOVED: No longer restrict block breaking - players can break any blocks anywhere
        }
    }

    private String isArenaObsidian(String arenaName, Location blockLoc) {
        // Red Team obsidian
        Location redObsidian = getObsidianLocation(arenaName, "red");
        if (redObsidian != null && redObsidian.equals(blockLoc)) {
            return "red";
        }

        // Blue Team obsidian
        Location blueObsidian = getObsidianLocation(arenaName, "blue");
        if (blueObsidian != null && blueObsidian.equals(blockLoc)) {
            return "blue";
        }

        return null;
    }

    private Location getObsidianLocation(String arenaName, String team) {
        return ArenaConfigManager.getObsidianLocation(arenaName, team);
    }

    private void destroyObsidian(String arenaName, String team, Player destroyer, Block block) {
        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        if (game == null) return;

        // Obsidian statusunu məhv edildi kimi qeyd edirik
        game.setObsidianDestroyed(team, true);

        // Stop particles for the destroyed obsidian
        ParticleManager.stopObsidianParticles(arenaName, team);

        // Dramatik səs effekti
        String sound = MessagesConfigManager.getSound("obsidian_destroyed");
        Sound destroySound = Obsidianwars.parseSound(sound);
        if (destroySound != null) {
            for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                    Player arenaPlayer = org.bukkit.Bukkit.getPlayer(uuid);
                    if (arenaPlayer != null) {
                        arenaPlayer.playSound(arenaPlayer.getLocation(), destroySound, 1.0f, 1.0f);
                    }
                }
            }
        }
        
        // Dynamic team titles - GREEN for attackers, RED for victims
        String teamName = team.equals("red") ? "Qırmızı" : "Mavi";
        String teamColor = team.equals("red") ? "§c" : "§9";
        String attackerTeam = team.equals("red") ? "blue" : "red";
        String attackerColor = team.equals("red") ? "§9" : "§c";
        
        for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                Player arenaPlayer = org.bukkit.Bukkit.getPlayer(uuid);
                if (arenaPlayer != null) {
                    String playerTeam = TeamListener.playerTeams.get(uuid);
                    
                    if (playerTeam != null && playerTeam.equals(attackerTeam)) {
                        // Attacker team gets GREEN title
                        arenaPlayer.sendTitle("§a§lOBSIDIAN DESTROYED!", "§e" + teamName + " obsidian məhv edildi!", 10, 70, 20);
                    } else {
                        // Victim team gets RED title
                        arenaPlayer.sendTitle("§c§lOBSIDIAN DESTROYED!", "§4" + teamName + " obsidian məhv edildi!", 10, 70, 20);
                    }
                }
            }
        }

        // Obsidian blokunu vizual olaraq qırırıq
        block.setType(Material.AIR);

        // Broadcast mesajı
        String broadcastMessage = MessagesConfigManager.getMessage("obsidian_team_destroyed",
            "teamColor", teamColor, "teamName", teamName, "player", destroyer.getName());
        ObsidianCommand.broadcastToArena(arenaName, broadcastMessage);

        // Win condition yoxlaması
        checkWinCondition(arenaName);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Yalnız arenadakı oyunçular üçün
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        if (game == null || (game.getGameState() != GameManager.GameState.PLAYING && game.getGameState() != GameManager.GameState.PREPARATION)) {
            return;
        }

        String playerTeam = TeamListener.playerTeams.get(player.getUniqueId());
        if (playerTeam == null) return;

        // Death message gizlədirik
        event.setDeathMessage(null);

        // During PREPARATION, just respawn at team spawn without spectating
        if (game.getGameState() == GameManager.GameState.PREPARATION) {
            Location teamSpawn = getTeamSpawn(arenaName, playerTeam);
            if (teamSpawn != null) {
                Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
                    if (player.isOnline()) {
                        player.setHealth(20);
                        player.setFoodLevel(20);
                        player.teleport(teamSpawn);
                        player.sendMessage("§eYou died during preparation - respawned at team spawn!");
                    }
                }, 1L);
            }
            return;
        }

        // Get obsidian location for spectating
        Location obsidianLoc = getObsidianLocation(arenaName, playerTeam);
        if (obsidianLoc == null) {
            Obsidianwars.getInstance().getLogger().severe("Obsidian location is null for " + playerTeam + " in arena " + arenaName);
            return;
        }

        // Get team spawn location for respawn
        Location teamSpawn = getTeamSpawn(arenaName, playerTeam);
        if (teamSpawn == null) {
            Obsidianwars.getInstance().getLogger().severe("Team spawn is null for " + playerTeam + " in arena " + arenaName);
            return;
        }

        // Əgər obsidianı məhv edilibsə, spectator mode near obsidian
        if (game.isObsidianDestroyed(playerTeam)) {
            // Delay gamemode change and teleport by 1 tick to avoid Paper API conflicts
            Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
                if (player.isOnline()) {
                    player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    player.teleport(obsidianLoc);
                    
                    // Give spectator items
                    giveSpectatorItems(player, arenaName);
                    
                    String diedTitle = MessagesConfigManager.getMessage("you_died");
                    String finalElim = MessagesConfigManager.getMessage("final_elimination");
                    player.sendTitle(diedTitle, finalElim, 10, 40, 20);
                    player.sendMessage("§cSiz final olaraq elimine edildiniz!");
                    
                    // Scoreboard yenilə
                    ScoreboardManager.updateScoreboard(player);
                }
            }, 1L);
        } else {
            // Obsidianı sağdırsa, respawn with spectator countdown near obsidian (delayed by 1 tick)
            Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
                if (player.isOnline()) {
                    startRespawnCountdown(player, arenaName, playerTeam, obsidianLoc, teamSpawn);
                }
            }, 1L);
        }

        // Win condition yoxlaması
        checkWinCondition(arenaName);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Yalnız arenadakı oyunçular üçün
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        if (game == null || (game.getGameState() != GameManager.GameState.PLAYING && game.getGameState() != GameManager.GameState.PREPARATION)) {
            return;
        }

        String playerTeam = TeamListener.playerTeams.get(player.getUniqueId());
        if (playerTeam == null) return;

        // Force respawn at team spawn location instead of world spawn
        Location teamSpawn = getTeamSpawn(arenaName, playerTeam);
        if (teamSpawn != null) {
            event.setRespawnLocation(teamSpawn);
            Obsidianwars.getInstance().getLogger().info("Setting respawn location for " + player.getName() + " to team spawn at " + teamSpawn);
            
            // Force teleport to team spawn immediately after respawn to guarantee location
            Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
                if (player.isOnline()) {
                    player.teleport(teamSpawn);
                    Obsidianwars.getInstance().getLogger().info("Force-teleported " + player.getName() + " to team spawn after respawn");
                }
            }, 1L);
        } else {
            Obsidianwars.getInstance().getLogger().severe("Team spawn is null for " + playerTeam + " in arena " + arenaName + " - cannot set respawn location");
        }
    }

    private void startRespawnCountdown(Player player, String arenaName, String team, Location obsidianLoc, Location teamSpawn) {
        // Teleport to obsidian location for spectating and set spectator mode
        try {
            player.teleport(obsidianLoc);
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            player.setAllowFlight(true);
            player.setFlying(true);
            Obsidianwars.getInstance().getLogger().info("Teleported " + player.getName() + " to obsidian for respawn countdown at " + obsidianLoc);
        } catch (Exception e) {
            Obsidianwars.getInstance().getLogger().severe("Failed to teleport player to obsidian: " + e.getMessage());
            return;
        }

        // DO NOT give spectator items here - player will respawn, not become a permanent spectator
        // Spectator items are only given to eliminated players (obsidian destroyed) or joining spectators

        // Get respawn wait time from config (default to 5 if not set)
        int respawnWaitTime = Obsidianwars.getInstance().getConfig().getInt("respawn-wait-time", 5);

        // Send initial chat message with the actual wait time
        String respawnMessage = MessagesConfigManager.getMessage("respawning", "seconds", String.valueOf(respawnWaitTime));
        if (respawnMessage == null) {
            respawnMessage = "§eRespawning in " + respawnWaitTime + " seconds...";
        }
        player.sendMessage(respawnMessage);

        // Start countdown with the configured wait time
        respawnCountdown(player, arenaName, team, teamSpawn, respawnWaitTime);
    }

    private void respawnCountdown(Player player, String arenaName, String team, Location teamSpawn, int count) {
        if (count <= 0) {
            // Respawn complete
            respawnPlayer(player, arenaName, team, teamSpawn);
            return;
        }

        // Show countdown title
        player.sendTitle("§c" + count, "", 0, 20, 0);
        
        // Schedule next countdown
        org.bukkit.Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
            if (player.isOnline() && ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
                respawnCountdown(player, arenaName, team, teamSpawn, count - 1);
            }
        }, 20L); // 1 second (20 ticks)
    }

    private void respawnPlayer(Player player, String arenaName, String team, Location teamSpawn) {
        // Set survival mode
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Teleport to team spawn (force teleport to guarantee location)
        player.teleport(teamSpawn);
        Obsidianwars.getInstance().getLogger().info("Respawned " + player.getName() + " at team spawn " + teamSpawn);
        
        // Restore full health and hunger
        player.setHealth(20);
        player.setFoodLevel(20);
        
        // Remove potion effects
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        
        // Apply team color and equipment
        TeamListener.applyTeamColor(player, team);
        
        // Give spawn protection
        ParticleManager.giveSpawnProtection(player, 3);
        
        // Send respawn message
        String respawnedMessage = MessagesConfigManager.getMessage("respawned");
        player.sendMessage(respawnedMessage);
        
        // Update scoreboard
        ScoreboardManager.updateScoreboard(player);
    }

    private Location getTeamSpawn(String arenaName, String team) {
        return ArenaConfigManager.getTeamSpawn(arenaName, team);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // ALWAYS clean up wand positions regardless of arena state
        WandListener.pos1Map.remove(uuid);
        WandListener.pos2Map.remove(uuid);

        // Clean up disconnect timer if exists
        GameManager.clearDisconnectRecord(uuid);

        // Yalnız arenadakı oyunçular üçün
        if (!ObsidianCommand.playersInArena.containsKey(uuid)) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(uuid);
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        String playerTeam = TeamListener.playerTeams.get(uuid);

        // Handle disconnect for rejoin grace period
        GameManager.handleDisconnect(player);

        if (game == null || (game.getGameState() != GameManager.GameState.PLAYING && game.getGameState() != GameManager.GameState.PREPARATION)) {
            // Əgər oyun aktiv deyilsə, sadəcə çıxarırıq
            ObsidianCommand.playersInArena.remove(uuid);
            TeamListener.playerTeams.remove(uuid);
            TeamManager.removePlayerFromTeams(player);
            ScoreboardManager.removeScoreboard(player);
            ParticleManager.removeSpawnProtection(player);
            return;
        }

        // Oyunçunu sistemdən çıxarırıq
        ObsidianCommand.playersInArena.remove(uuid);
        TeamListener.playerTeams.remove(uuid);
        TeamManager.removePlayerFromTeams(player);
        ParticleManager.removeSpawnProtection(player);

        // Broadcast mesajı
        String quitMessage = MessagesConfigManager.getMessage("player_quit", "player", player.getName());
        ObsidianCommand.broadcastToArena(arenaName, quitMessage);

        // Win condition yoxlaması
        checkWinCondition(arenaName);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if player can rejoin
        if (GameManager.canRejoin(player)) {
            // Find the arena they were in
            String arenaName = null;
            for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                if (uuid.equals(player.getUniqueId())) {
                    arenaName = ObsidianCommand.playersInArena.get(uuid);
                    break;
                }
            }
            
            if (arenaName != null) {
                // Allow rejoin within grace period
                GameManager.ArenaGame game = GameManager.getGame(arenaName);
                if (game != null && game.getGameState() == GameManager.GameState.PLAYING) {
                    String rejoinMessage = MessagesConfigManager.getMessage("rejoin_allowed");
                    player.sendMessage(rejoinMessage);
                    
                    // Clear disconnect record
                    GameManager.clearDisconnectRecord(player.getUniqueId());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Yalnız arenadakı oyunçular üçün
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        if (game == null || (game.getGameState() != GameManager.GameState.PLAYING && game.getGameState() != GameManager.GameState.PREPARATION)) {
            return;
        }

        // Check if player fell into void (Y <= 0)
        if (player.getLocation().getY() <= 0) {
            // Handle void death
            handleVoidDeath(player, arenaName);
        }
    }

    private void handleVoidDeath(Player player, String arenaName) {
        String playerTeam = TeamListener.playerTeams.get(player.getUniqueId());
        if (playerTeam == null) return;

        GameManager.ArenaGame game = GameManager.getGame(arenaName);
        if (game == null) return;
        
        // During PREPARATION, just respawn at team spawn without spectating
        if (game.getGameState() == GameManager.GameState.PREPARATION) {
            Location teamSpawn = getTeamSpawn(arenaName, playerTeam);
            if (teamSpawn != null) {
                player.teleport(teamSpawn);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.sendMessage("§eYou fell during preparation - respawned at team spawn!");
            }
            return;
        }

        // Get obsidian location for spectating
        Location obsidianLoc = getObsidianLocation(arenaName, playerTeam);
        if (obsidianLoc == null) {
            Obsidianwars.getInstance().getLogger().severe("Obsidian location is null for " + playerTeam + " in arena " + arenaName);
            return;
        }

        // Get team spawn location for respawn
        Location teamSpawn = getTeamSpawn(arenaName, playerTeam);
        if (teamSpawn == null) {
            Obsidianwars.getInstance().getLogger().severe("Team spawn is null for " + playerTeam + " in arena " + arenaName);
            return;
        }

        // Əgər obsidianı məhv edilibsə, final elimination
        if (game.isObsidianDestroyed(playerTeam)) {
            // Final elimination - spectator mode near obsidian with 1-tick delay
            Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
                if (player.isOnline()) {
                    player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    player.teleport(obsidianLoc);
                    
                    // Give spectator items
                    giveSpectatorItems(player, arenaName);
                    
                    String diedTitle = MessagesConfigManager.getMessage("you_died");
                    String finalElim = MessagesConfigManager.getMessage("final_elimination");
                    player.sendTitle(diedTitle, finalElim, 10, 40, 20);
                    player.sendMessage("§cSiz boşluğa düşdünüz və final olaraq elimine edildiniz!");
                    
                    // Scoreboard yenilə
                    ScoreboardManager.updateScoreboard(player);
                }
            }, 1L);
        } else {
            // Obsidianı sağdırsa, respawn with countdown near obsidian (delayed by 1 tick)
            Bukkit.getScheduler().runTaskLater(Obsidianwars.getInstance(), () -> {
                if (player.isOnline()) {
                    startRespawnCountdown(player, arenaName, playerTeam, obsidianLoc, teamSpawn);
                }
            }, 1L);
        }

        // Win condition yoxlaması
        checkWinCondition(arenaName);
    }

    private void checkWinCondition(String arenaName) {
        // Use the static method from GameManager
        GameManager.checkArenaWinCondition(arenaName);
    }

    private void giveSpectatorItems(Player player, String arenaName) {
        // Clear inventory
        player.getInventory().clear();
        
        // Give teleport compass (teleport to teammates GUI)
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        if (compassMeta != null) {
            compassMeta.setDisplayName("§eKomanda Yoldaşlarına Teleport");
            compassMeta.setLore(java.util.Arrays.asList("§7Canlı komanda yoldaşlarına teleport"));
            compass.setItemMeta(compassMeta);
        }
        player.getInventory().setItem(0, compass);
        
        // Give obsidian return item
        ItemStack obsidianEye = new ItemStack(Material.ENDER_EYE);
        ItemMeta eyeMeta = obsidianEye.getItemMeta();
        if (eyeMeta != null) {
            eyeMeta.setDisplayName("§cObsidianya Qayıt");
            eyeMeta.setLore(java.util.Arrays.asList("§7Öz komandanızın obsidianına teleport"));
            obsidianEye.setItemMeta(eyeMeta);
        }
        player.getInventory().setItem(4, obsidianEye);
        
        // Give enemy spectate item
        ItemStack enemyCompass = new ItemStack(Material.REDSTONE);
        ItemMeta enemyMeta = enemyCompass.getItemMeta();
        if (enemyMeta != null) {
            enemyMeta.setDisplayName("§cDüşmən Komandasına Bax");
            enemyMeta.setLore(java.util.Arrays.asList("§7Düşmən komandasına teleport"));
            enemyCompass.setItemMeta(enemyMeta);
        }
        player.getInventory().setItem(8, enemyCompass);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        String itemName = item.getItemMeta().getDisplayName();
        
        // Spectator compass interaction - teleport to teammates
        if (itemName.equals("§eKomanda Yoldaşlarına Teleport")) {
            if (player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                return;
            }

            if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
                return;
            }

            String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
            String playerTeam = TeamListener.playerTeams.get(player.getUniqueId());

            if (playerTeam == null) {
                player.sendMessage("§cSiz heç bir komandada deyilsiniz!");
                return;
            }

            // Find alive teammates
            java.util.List<Player> teammates = new java.util.ArrayList<>();
            for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                    String team = TeamListener.playerTeams.get(uuid);
                    if (team != null && team.equals(playerTeam)) {
                        Player teammate = Bukkit.getPlayer(uuid);
                        if (teammate != null && teammate.isOnline() && teammate.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                            teammates.add(teammate);
                        }
                    }
                }
            }

            if (teammates.isEmpty()) {
                player.sendMessage("§cCanlı komanda yoldaşı yoxdur!");
                return;
            }

            // Teleport to random teammate
            Player target = teammates.get(new java.util.Random().nextInt(teammates.size()));
            player.teleport(target.getLocation());
            player.sendMessage("§a" + target.getName() + "-a teleport oldunuz!");
            event.setCancelled(true);
        }
        
        // Obsidian return interaction
        if (itemName.equals("§cObsidianya Qayıt")) {
            if (player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                return;
            }

            if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
                return;
            }

            String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
            String playerTeam = TeamListener.playerTeams.get(player.getUniqueId());

            if (playerTeam == null) {
                player.sendMessage("§cSiz heç bir komandada deyilsiniz!");
                return;
            }

            Location obsidianLoc = getObsidianLocation(arenaName, playerTeam);
            if (obsidianLoc != null) {
                player.teleport(obsidianLoc);
                player.sendMessage("§aÖz komandanızın obsidianına teleport oldunuz!");
            }
            event.setCancelled(true);
        }
        
        // Enemy spectate interaction
        if (itemName.equals("§cDüşmən Komandasına Bax")) {
            if (player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                return;
            }

            if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
                return;
            }

            String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
            String playerTeam = TeamListener.playerTeams.get(player.getUniqueId());

            if (playerTeam == null) {
                player.sendMessage("§cSiz heç bir komandada deyilsiniz!");
                return;
            }

            String enemyTeam = playerTeam.equals("red") ? "blue" : "red";
            
            // Find alive enemy players
            java.util.List<Player> enemies = new java.util.ArrayList<>();
            for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                if (ObsidianCommand.playersInArena.get(uuid).equals(arenaName)) {
                    String team = TeamListener.playerTeams.get(uuid);
                    if (team != null && team.equals(enemyTeam)) {
                        Player enemy = Bukkit.getPlayer(uuid);
                        if (enemy != null && enemy.isOnline() && enemy.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                            enemies.add(enemy);
                        }
                    }
                }
            }

            if (enemies.isEmpty()) {
                // If no alive enemies, teleport to enemy obsidian
                Location enemyObsidian = getObsidianLocation(arenaName, enemyTeam);
                if (enemyObsidian != null) {
                    player.teleport(enemyObsidian);
                    player.sendMessage("§aDüşmən komandasının obsidianına teleport oldunuz!");
                } else {
                    player.sendMessage("§cDüşmən komandasında canlı oyunçu yoxdur!");
                }
            } else {
                // Teleport to random enemy
                Player target = enemies.get(new java.util.Random().nextInt(enemies.size()));
                player.teleport(target.getLocation());
                player.sendMessage("§a" + target.getName() + "-a (düşmən) teleport oldunuz!");
            }
            event.setCancelled(true);
        }
    }
}
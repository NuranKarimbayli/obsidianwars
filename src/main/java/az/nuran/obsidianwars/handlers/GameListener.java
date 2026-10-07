package az.nuran.obsidianwars.handlers;

import az.nuran.obsidianwars.Obsidianwars;
import az.nuran.obsidianwars.commands.ObsidianCommand;
import az.nuran.obsidianwars.managers.ArenaConfigManager;
import az.nuran.obsidianwars.managers.ArenaFileManager;
import az.nuran.obsidianwars.managers.ArenaStateManager;
import az.nuran.obsidianwars.managers.GameManager;
import az.nuran.obsidianwars.managers.LobbyScoreboardManager;
import az.nuran.obsidianwars.managers.MessagesConfigManager;
import az.nuran.obsidianwars.managers.ParticleManager;
import az.nuran.obsidianwars.managers.ScoreboardManager;
import az.nuran.obsidianwars.managers.SpectatorManager;
import az.nuran.obsidianwars.managers.StatsManager;
import az.nuran.obsidianwars.managers.TaskManager;
import az.nuran.obsidianwars.managers.TeamManager;
import az.nuran.obsidianwars.models.PlayerStats;
import az.nuran.obsidianwars.services.DeathMessagesConfigManager;
import az.nuran.obsidianwars.services.DebugManager;
import az.nuran.obsidianwars.services.PlayerUtils;
import az.nuran.obsidianwars.services.TeamConfig;

import java.util.UUID;

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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
                attacker.sendMessage(MessagesConfigManager.getMessage("spawn_protection_attacker", "player", victim.getName()));
            }
            return;
        }

        // Only process arena-specific damage if damager is a player
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player damager = (Player) event.getDamager();

        // Only for players in arena
        if (!ObsidianCommand.playersInArena.containsKey(victim.getUniqueId()) ||
            !ObsidianCommand.playersInArena.containsKey(damager.getUniqueId())) {
            return;
        }

        // Check game state - PvP is enabled during PREPARATION and PLAYING states
        String arenaName = ObsidianCommand.playersInArena.get(victim.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        // If game is null or not in PREPARATION/PLAYING state, cancel PvP
        if (game == null || (game.getGameState() != ArenaStateManager.ArenaState.PREPARATION &&
                           game.getGameState() != ArenaStateManager.ArenaState.PLAYING)) {
            event.setCancelled(true);
            return;
        }

        String victimTeam = TeamListener.playerTeams.get(victim.getUniqueId());
        String damagerTeam = TeamListener.playerTeams.get(damager.getUniqueId());

        // Same team members cannot damage each other
        if (victimTeam != null && damagerTeam != null && victimTeam.equals(damagerTeam)) {
            event.setCancelled(true);
            damager.sendMessage(MessagesConfigManager.getMessage("team_damage_prevented"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        // Check if victim is a player
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Only process arena players
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        // Lobby invincibility: Cancel all damage during WAITING and STARTING (COUNTDOWN) states
        if (game != null && (game.getGameState() == ArenaStateManager.ArenaState.WAITING ||
                           game.getGameState() == ArenaStateManager.ArenaState.STARTING)) {
            event.setCancelled(true);
            return;
        }

        // Cancel damage during ENDED state (game over, no more damage)
        if (game != null && game.getGameState() == ArenaStateManager.ArenaState.ENDED) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Only for players in arena
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        // If game is not active or not in preparation, we prohibit block breaking
        if (game == null || (game.getGameState() != ArenaStateManager.ArenaState.PLAYING && game.getGameState() != ArenaStateManager.ArenaState.PREPARATION)) {
            event.setCancelled(true);
            return;
        }

        // We check obsidian block
        if (block.getType() == Material.OBSIDIAN) {
            event.setCancelled(true); // We cancel block breaking, we will handle logic

            // Obsidian can only be destroyed during PLAYING state (not PREPARATION)
            if (game.getGameState() != ArenaStateManager.ArenaState.PLAYING) {
                player.sendMessage(MessagesConfigManager.getMessage("obsidian_only_after_prep"));
                return;
            }

            // We check if this block is arena obsidian
            String team = isArenaObsidian(arenaName, block.getLocation());
            if (team != null) {
                // If obsidian is already destroyed
                if (game.isObsidianDestroyed(team)) {
                    player.sendMessage(MessagesConfigManager.getMessage("already_destroyed"));
                    return;
                }

                // Player cannot break their own team's obsidian
                String playerTeam = TeamListener.playerTeams.get(player.getUniqueId());
                if (playerTeam != null && playerTeam.equals(team)) {
                    player.sendMessage(MessagesConfigManager.getMessage("cant_destroy_own"));
                    return;
                }

                // Destroy the obsidian
                destroyObsidian(arenaName, team, player, block);
            }
        } else {
            // Check for resource blocks
            boolean isResourceBlock = false;
            try {
                isResourceBlock = ArenaFileManager.isResourceBlock(arenaName, block.getLocation());
            } catch (Exception e) {
                // If ArenaFileManager has issues, simply block it
                event.setCancelled(true);
                player.sendMessage(Obsidianwars.getInstance().getConfig().getString("messages.cant_break_block"));
                return;
            }

            if (isResourceBlock) {
                // Resource blok - spawn color-matched particle effect
                ParticleManager.spawnResourceBreakParticle(block.getLocation(), block.getType());
            } else {
                // Track block break for snapshot restoration (store ORIGINAL block data before break)
                // NOTE: The ArenaSnapshotManager now handles this in its own event listener
                // We don't need to track here anymore to avoid duplication
            }
            // REMOVED: No longer restrict block breaking - players can break any blocks anywhere
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Spectators are handled by SpectatorListener
        if (SpectatorManager.isSpectator(player)) {
            return;
        }

        // Only for players in arena
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        // If game is not active or not in preparation, we prohibit block placement
        if (game == null || (game.getGameState() != ArenaStateManager.ArenaState.PLAYING && game.getGameState() != ArenaStateManager.ArenaState.PREPARATION)) {
            event.setCancelled(true);
            return;
        }

        // NOTE: Block tracking is now handled by ArenaSnapshotManager's own event listeners
        // We don't need to track here anymore to avoid duplication
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // Spectators are handled by SpectatorListener
        if (SpectatorManager.isSpectator(player)) {
            return;
        }

        // Only for players in arena
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        // If game is not active or not in preparation, we prohibit item dropping
        if (game == null || (game.getGameState() != ArenaStateManager.ArenaState.PLAYING && game.getGameState() != ArenaStateManager.ArenaState.PREPARATION)) {
            event.setCancelled(true);
            return;
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

        // Mark obsidian status as destroyed
        game.setObsidianDestroyed(team, true);

        // Track stats - obsidian destroyed
        PlayerStats stats = StatsManager.getPlayerStats(destroyer);
        stats.addObsidianBroken();
        StatsManager.savePlayerStats(destroyer.getUniqueId(), destroyer.getName());

        // Award XP for breaking obsidian
        XPAwardListener.awardObsidianXP(destroyer);

        // Stop particles for the destroyed obsidian
        ParticleManager.stopObsidianParticles(arenaName, team);

        // Dramatic sound effect
        String sound = MessagesConfigManager.getSound("obsidian_destroyed");
        Sound destroySound = Obsidianwars.parseSound(sound);
        if (destroySound != null) {
            for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
                String playerArena = ObsidianCommand.playersInArena.get(uuid);
                if (playerArena != null && playerArena.equals(arenaName)) {
                    Player arenaPlayer = org.bukkit.Bukkit.getPlayer(uuid);
                    if (arenaPlayer != null) {
                        arenaPlayer.playSound(arenaPlayer.getLocation(), destroySound, 1.0f, 1.0f);
                    }
                }
            }
        }
        
        // Dynamic team titles - GREEN for attackers, RED for victims
        String teamName = TeamConfig.getTeamName(team);
        String teamColor = TeamConfig.getTeamColor(team);
        String attackerTeam = team.equals("red") ? "blue" : "red";
        String attackerColor = TeamConfig.getTeamColor(attackerTeam);
        
        for (java.util.UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                Player arenaPlayer = org.bukkit.Bukkit.getPlayer(uuid);
                if (arenaPlayer != null) {
                    String playerTeam = TeamListener.playerTeams.get(uuid);
                    
                    if (playerTeam != null && playerTeam.equals(attackerTeam)) {
                        // Attacker team gets GREEN title
                        int fadeIn = Obsidianwars.getInstance().getConfig().getInt("titles.obsidian_destroyed.fade-in", 10);
                        int stay = Obsidianwars.getInstance().getConfig().getInt("titles.obsidian_destroyed.stay", 70);
                        int fadeOut = Obsidianwars.getInstance().getConfig().getInt("titles.obsidian_destroyed.fade-out", 20);
                        arenaPlayer.sendTitle("§a§lOBSIDIAN DESTROYED!", "§e" + teamName + " obsidian destroyed!", fadeIn, stay, fadeOut);
                    } else {
                        // Victim team gets RED title
                        int fadeIn = Obsidianwars.getInstance().getConfig().getInt("titles.obsidian_destroyed.fade-in", 10);
                        int stay = Obsidianwars.getInstance().getConfig().getInt("titles.obsidian_destroyed.stay", 70);
                        int fadeOut = Obsidianwars.getInstance().getConfig().getInt("titles.obsidian_destroyed.fade-out", 20);
                        arenaPlayer.sendTitle("§c§lOBSIDIAN DESTROYED!", "§4" + teamName + " obsidian destroyed!", fadeIn, stay, fadeOut);
                    }
                }
            }
        }

        // Visually break the obsidian block
        block.setType(Material.AIR);

        // Broadcast message
        String broadcastMessage = MessagesConfigManager.getMessage("obsidian_team_destroyed",
            "teamColor", teamColor, "teamName", teamName, "player", destroyer.getName());
        ObsidianCommand.broadcastToArena(arenaName, broadcastMessage);

        DebugManager.logDebug("Obsidian destroyed: " + teamColor + " team obsidian broken by " + destroyer.getName(), arenaName);

        // Track stats for the victim team (obsidian lost)
        // The 'team' parameter represents the team whose obsidian was broken (the victim)
        for (UUID uuid : ObsidianCommand.playersInArena.keySet()) {
            String playerArena = ObsidianCommand.playersInArena.get(uuid);
            if (playerArena != null && playerArena.equals(arenaName)) {
                String playerTeam = TeamListener.playerTeams.get(uuid);
                if (playerTeam != null && playerTeam.equals(team)) {
                    Player victimPlayer = Bukkit.getPlayer(uuid);
                    if (victimPlayer != null) {
                        PlayerStats victimStats = StatsManager.getPlayerStats(victimPlayer);
                        victimStats.addObsidianLost();
                        StatsManager.savePlayerStats(uuid, victimPlayer.getName());
                    }
                }
            }
        }

        // Win condition check - check immediately after obsidian destruction
        // Delay by 1 tick to ensure any pending spectator mode changes are processed
        TaskManager.getInstance().runLater("win-check-obsidian-" + arenaName, () -> {
            checkWinCondition(arenaName);
        }, 1L, arenaName);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Only for players in arena
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        if (game == null || (game.getGameState() != ArenaStateManager.ArenaState.PLAYING && game.getGameState() != ArenaStateManager.ArenaState.PREPARATION)) {
            return;
        }

        String playerTeam = TeamListener.playerTeams.get(player.getUniqueId());
        if (playerTeam == null) return;

        DebugManager.logDebug("Player death: " + player.getName() + " (" + playerTeam + " team)", arenaName);

        // Invalidate team health cache for this arena
        ScoreboardManager.invalidateTeamHealthCache(arenaName);

        // Track death stat
        PlayerStats stats = StatsManager.getPlayerStats(player);
        stats.addDeath();

        // Check if this is a final kill (obsidian destroyed)
        if (game.isObsidianDestroyed(playerTeam)) {
            stats.addFinalDeath();
            DebugManager.logDebug("Final death: " + player.getName() + " (obsidian destroyed)", arenaName);
        }

        // Reset victim's kill streak
        game.resetKillStreak(player.getUniqueId());

        // Track kill for the killer if it was a player
        if (player.getKiller() instanceof Player) {
            Player killer = (Player) player.getKiller();
            if (ObsidianCommand.playersInArena.containsKey(killer.getUniqueId())) {
                PlayerStats killerStats = StatsManager.getPlayerStats(killer);
                killerStats.addKill();

                // Check if this is a final kill (victim's obsidian destroyed)
                if (game.isObsidianDestroyed(playerTeam)) {
                    killerStats.addFinalKill();
                    DebugManager.logDebug("Final kill: " + killer.getName() + " killed " + player.getName() + " (obsidian destroyed)", arenaName);
                    // Award XP for final kill
                    XPAwardListener.awardFinalKillXP(killer);
                } else {
                    // Award XP for regular kill
                    XPAwardListener.awardKillXP(killer);
                }

                // Add to kill streak
                game.addKill(killer.getUniqueId());

                // Update longest streak in stats
                int currentStreak = game.getKillStreak(killer.getUniqueId());
                if (currentStreak > killerStats.getLongestKillStreak()) {
                    killerStats.setLongestKillStreak(currentStreak);
                }

                DebugManager.logDebug("Kill: " + killer.getName() + " killed " + player.getName() + " (streak: " + currentStreak + ")", arenaName);

                // Send custom death message
                sendDeathMessage(arenaName, killer, player, event);
            }
        } else {
            // Death by other means (void, fall, etc.)
            DebugManager.logDebug("Player death by non-player cause: " + player.getName(), arenaName);
            sendDeathMessage(arenaName, null, player, event);
        }

        // Hide death message (we send our own)
        event.setDeathMessage(null);

        // During PREPARATION, just respawn at team spawn without spectating
        if (game.getGameState() == ArenaStateManager.ArenaState.PREPARATION) {
            Location teamSpawn = getTeamSpawn(arenaName, playerTeam);
            if (teamSpawn != null) {
                TaskManager.getInstance().runLater("prep-respawn-" + player.getUniqueId(), () -> {
                    if (player.isOnline()) {
                        player.setHealth(20);
                        player.setFoodLevel(20);
                        player.teleport(teamSpawn);
                        player.sendMessage(MessagesConfigManager.getMessage("preparation_respawn"));
                    }
                }, 1L, arenaName);
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

        // If obsidian is destroyed, spectator mode at spectator spawn
        if (game.isObsidianDestroyed(playerTeam)) {
            // Delay gamemode change and teleport by 1 tick to avoid Paper API conflicts
            TaskManager.getInstance().runLater("spectator-mode-" + player.getUniqueId(), () -> {
                if (player.isOnline()) {
                    SpectatorManager.setSpectatorMode(player, arenaName);

                    String diedTitle = MessagesConfigManager.getMessage("you_died");
                    String finalElim = MessagesConfigManager.getMessage("final_elimination");
                    int fadeIn = Obsidianwars.getInstance().getConfig().getInt("titles.death.fade-in", 10);
                    int stay = Obsidianwars.getInstance().getConfig().getInt("titles.death.stay", 40);
                    int fadeOut = Obsidianwars.getInstance().getConfig().getInt("titles.death.fade-out", 20);
                    player.sendTitle(diedTitle, finalElim, fadeIn, stay, fadeOut);
                    player.sendMessage("§cYou were finally eliminated!");

                    // Update scoreboard
                    ScoreboardManager.updateScoreboard(player);
                }
                // CRITICAL: Check win condition AFTER spectator mode is set
                checkWinCondition(arenaName);
            }, 1L, arenaName);
        } else {
            // If obsidian is intact, respawn with spectator countdown near obsidian (delayed by 1 tick)
            TaskManager.getInstance().runLater("respawn-countdown-" + player.getUniqueId(), () -> {
                if (player.isOnline()) {
                    startRespawnCountdown(player, arenaName, playerTeam, obsidianLoc, teamSpawn);
                }
            }, 1L, arenaName);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Only for players in arena
        if (!ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        if (game == null || (game.getGameState() != ArenaStateManager.ArenaState.PLAYING && game.getGameState() != ArenaStateManager.ArenaState.PREPARATION)) {
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
            TaskManager.getInstance().runLater("respawn-teleport-" + player.getUniqueId(), () -> {
                if (player.isOnline()) {
                    player.teleport(teamSpawn);
                    Obsidianwars.getInstance().getLogger().info("Force-teleported " + player.getName() + " to team spawn after respawn");
                }
            }, 1L, arenaName);
        } else {
            Obsidianwars.getInstance().getLogger().severe("Team spawn is null for " + playerTeam + " in arena " + arenaName + " - cannot set respawn location");
        }
    }

    private void startRespawnCountdown(Player player, String arenaName, String team, Location obsidianLoc, Location teamSpawn) {
        // Mark player as waiting to respawn
        SpectatorManager.setRespawning(player.getUniqueId());

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
        TaskManager.getInstance().runLater("respawn-tick-" + player.getUniqueId() + "-" + count, () -> {
            if (player.isOnline() && ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
                respawnCountdown(player, arenaName, team, teamSpawn, count - 1);
            }
        }, 20L, arenaName); // 1 second (20 ticks)
    }

    private void respawnPlayer(Player player, String arenaName, String team, Location teamSpawn) {
        // Clear respawning flag - player is now alive again
        SpectatorManager.clearRespawning(player.getUniqueId());

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

        // Apply team color without leather armor (false parameter)
        TeamListener.applyTeamColor(player, team, false);

        // NO spawn protection - players can take damage immediately
    }

    private void sendDeathMessage(String arenaName, Player killer, Player victim, PlayerDeathEvent event) {
        String weapon = getWeaponName(event);
        String distance = "";
        String messageType = "sword"; // default

        if (killer != null) {
            distance = calculateDistance(killer, victim);
            messageType = determineMessageType(killer, victim, event);
        } else {
            // Death by non-player (void, fall, etc.)
            messageType = determineNonPlayerDeathType(event);
        }

        String message = getDeathMessageFromConfig(messageType, killer, victim, weapon, distance);
        if (message != null && !message.isEmpty()) {
            // Check if it's a final kill
            GameManager.ArenaGame game = GameManager.getGame(arenaName);
            if (game != null) {
                String victimTeam = TeamListener.playerTeams.get(victim.getUniqueId());
                if (victimTeam != null && game.isObsidianDestroyed(victimTeam)) {
                    String finalKillMsg = DeathMessagesConfigManager.getDeathMessagesConfig().getString("death-messages.final-kill",
                        "§4§lFINAL KILL! §c%killer% §7eliminated §c%victim%!");
                    if (killer != null) {
                        finalKillMsg = finalKillMsg.replace("%killer%", killer.getName());
                    } else {
                        finalKillMsg = finalKillMsg.replace("%killer%", "Void");
                    }
                    finalKillMsg = finalKillMsg.replace("%victim%", victim.getName());
                    ObsidianCommand.broadcastToArena(arenaName, finalKillMsg);
                    return;
                }
            }

            ObsidianCommand.broadcastToArena(arenaName, message);
        }
    }

    private String getWeaponName(PlayerDeathEvent event) {
        ItemStack item = event.getEntity().getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            return "fists";
        }
        return item.getType().name().toLowerCase().replace("_", " ");
    }

    private String calculateDistance(Player killer, Player victim) {
        double distance = killer.getLocation().distance(victim.getLocation());
        return String.format("%.1f", distance);
    }

    private String determineMessageType(Player killer, Player victim, PlayerDeathEvent event) {
        ItemStack item = killer.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            return "fists";
        }

        Material type = item.getType();
        if (type.name().contains("SWORD")) {
            return "sword";
        } else if (type.name().contains("AXE")) {
            return "axe";
        } else if (type == Material.BOW || type == Material.CROSSBOW) {
            return "bow";
        } else if (type == Material.TRIDENT) {
            return "trident";
        } else {
            return "sword"; // default fallback
        }
    }

    private String determineNonPlayerDeathType(PlayerDeathEvent event) {
        EntityDamageEvent.DamageCause cause = event.getEntity().getLastDamageCause() != null ?
            event.getEntity().getLastDamageCause().getCause() : null;

        if (cause == null) return "generic";

        switch (cause) {
            case VOID:
                return "void";
            case FALL:
                return "fall";
            case LAVA:
            case FIRE:
            case FIRE_TICK:
                return "fire";
            case DROWNING:
                return "drown";
            case STARVATION:
                return "starve";
            case SUFFOCATION:
                return "suffocate";
            default:
                return "generic";
        }
    }

    private String getDeathMessageFromConfig(String type, Player killer, Player victim, String weapon, String distance) {
        String path = "death-messages." + type;
        String message = DeathMessagesConfigManager.getDeathMessagesConfig().getString(path);

        if (message == null || message.isEmpty()) {
            // Fallback default message
            if (killer != null) {
                return "§c" + killer.getName() + " §7killed §c" + victim.getName();
            } else {
                return "§c" + victim.getName() + " §7died";
            }
        }

        // Replace placeholders
        message = message.replace("%killer%", killer != null ? killer.getName() : "Void");
        message = message.replace("%victim%", victim.getName());
        message = message.replace("%weapon%", weapon);
        message = message.replace("%distance%", distance);

        return message;
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

        // Only for players in arena
        if (!ObsidianCommand.playersInArena.containsKey(uuid)) {
            return;
        }

        String arenaName = ObsidianCommand.playersInArena.get(uuid);
        GameManager.ArenaGame game = GameManager.getGame(arenaName);

        String playerTeam = TeamListener.playerTeams.get(uuid);

        // Handle disconnect for rejoin grace period
        GameManager.handleDisconnect(player);

        // CANCELLATION LOGIC: If player leaves during COUNTDOWN, check if we need to cancel
        if (game != null && game.getGameState() == ArenaStateManager.ArenaState.STARTING) {
            // Remove player from arena first
            ObsidianCommand.playersInArena.remove(uuid);
            TeamManager.removePlayerFromTeam(player);
            PlayerUtils.resetPlayerArenaLeave(player);
            ParticleManager.removeSpawnProtection(player);

            // Update lobby scoreboard after leaving
            LobbyScoreboardManager.updateLobbyScoreboard(player);

            // Check if countdown conditions are still met
            // Cancel if: any team becomes empty OR total players drop below minPlayers
            if (TeamManager.isAnyTeamEmpty(arenaName) ||
                TeamManager.getTotalPlayerCount(arenaName) < ArenaConfigManager.getMinPlayers(arenaName)) {
                // Cancel countdown immediately
                GameManager.cancelCountdown(arenaName);
            }

            // Broadcast quit message
            String quitMessage = MessagesConfigManager.getMessage("player_quit", "player", player.getName());
            ObsidianCommand.broadcastToArena(arenaName, quitMessage);

            return;
        }

        if (game == null || (game.getGameState() != ArenaStateManager.ArenaState.PLAYING && game.getGameState() != ArenaStateManager.ArenaState.PREPARATION)) {
            // If game is not active, simply remove them
            ObsidianCommand.playersInArena.remove(uuid);
            TeamManager.removePlayerFromTeam(player);
            PlayerUtils.resetPlayerArenaLeave(player);
            ParticleManager.removeSpawnProtection(player);

            // Update lobby scoreboard after leaving
            LobbyScoreboardManager.updateLobbyScoreboard(player);

            return;
        }

        // RECONNECT LOGIC: During PLAYING/PREPARATION, do NOT remove player from tracking
        // They stay in playersInArena and playerTeams maps for potential rejoin
        // State is saved in GameManager.handleDisconnect()

        // Remove spawn protection
        ParticleManager.removeSpawnProtection(player);

        // Broadcast disconnect message with grace period info
        String teamName = playerTeam.equals("red") ? "Red" : "Blue";
        String teamColor = playerTeam.equals("red") ? "§c" : "§9";
        String disconnectMessage = teamColor + player.getName() + " §edisconnected! They have 30 seconds to rejoin.";
        ObsidianCommand.broadcastToArena(arenaName, disconnectMessage);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if player is a disconnected player that can rejoin
        if (GameManager.canRejoin(player)) {
            // Attempt to restore player state
            boolean restored = GameManager.restoreDisconnectedPlayer(player);

            if (restored) {
                // Successfully restored
                String arenaName = GameManager.getDisconnectedPlayerArena(player.getUniqueId());
                String rejoinMessage = "§aYou have reconnected to the arena!";
                player.sendMessage(rejoinMessage);

                // Broadcast to arena
                String broadcastMessage = "§a" + player.getName() + " has reconnected!";
                ObsidianCommand.broadcastToArena(arenaName, broadcastMessage);

                // Play rejoin sound
                String sound = MessagesConfigManager.getSound("join_lobby");
                Sound rejoinSound = Obsidianwars.parseSound(sound);
                if (rejoinSound != null) {
                    player.playSound(player.getLocation(), rejoinSound, 1.0f, 1.0f);
                }

                // Hide existing spectators from the rejoining player
                hideExistingSpectatorsFromPlayer(player, arenaName);

                return;
            }
        }

        // POST-GAME CLEANUP: If player rejoins after game ended, ensure they are clean
        // Check if player was in an arena but game is no longer active
        if (ObsidianCommand.playersInArena.containsKey(player.getUniqueId())) {
            String arenaName = ObsidianCommand.playersInArena.get(player.getUniqueId());
            GameManager.ArenaGame game = GameManager.getGame(arenaName);

            if (game == null || game.getGameState() == ArenaStateManager.ArenaState.ENDED) {
                // Game ended, clean up player
                ObsidianCommand.playersInArena.remove(player.getUniqueId());
                TeamManager.removePlayerFromTeam(player);

                // Reset player state and teleport to spawn
                Location mainSpawn = player.getWorld().getSpawnLocation();
                PlayerUtils.resetPlayerFull(player, mainSpawn);

                player.sendMessage("§eThe game has ended. You have been returned to spawn.");

                // Update lobby scoreboard
                LobbyScoreboardManager.updateLobbyScoreboard(player);
            }
        } else {
            // Player is not in any arena, show lobby scoreboard
            LobbyScoreboardManager.updateLobbyScoreboard(player);
        }
    }

    /**
     * Hides all existing spectators from a player who just joined.
     *
     * @param player The player who joined
     * @param arenaName The arena name
     */
    private void hideExistingSpectatorsFromPlayer(Player player, String arenaName) {
        for (Player spectator : SpectatorManager.getSpectatorsInArena(arenaName)) {
            player.hidePlayer(Obsidianwars.getInstance(), spectator);
        }
    }

    private void giveSpectatorItems(Player player, String arenaName) {
        // Use SpectatorManager to give items
        player.getInventory().clear();
        player.getInventory().setItem(0, SpectatorManager.createTeleporterCompass());
        player.getInventory().setItem(3, SpectatorManager.createFlySpeedFeather(player));
        player.getInventory().setItem(4, SpectatorManager.createNightVisionToggle(player));
        player.getInventory().setItem(8, SpectatorManager.createLeaveItem());
    }

    private void checkWinCondition(String arenaName) {
        // Use the static method from GameManager
        GameManager.checkArenaWinCondition(arenaName);
    }
}
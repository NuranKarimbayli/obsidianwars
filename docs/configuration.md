# Configuration

Complete reference for all ObsidianWars configuration files.

## Configuration Files

ObsidianWars uses several YAML configuration files located in `plugins/ObsidianWars/`:

- `config.yml` - Main plugin configuration
- `messages.yml` - Customizable messages
- `scoreboards.yml` - Scoreboard display settings
- `death-messages.yml` - Death message templates
- `kill-streaks.yml` - Kill streak rewards
- `resource-blocks.yml` - Allowed resource blocks
- `languages/` - Language-specific message files

## config.yml

The main configuration file controls all core plugin settings.

### Permission Configuration

```yaml
no-permission-message: "&cYou don't have permission to use this command."
```

The message shown when a player lacks permission for a command.

### UI Features Toggle

```yaml
features:
  scoreboard: true
  tablist: true
```

Enable or disable built-in UI features. Set to `false` if you prefer custom plugins for these features.

### Team Configuration

```yaml
teams:
  red:
    color: "&c"
    name: "Red"
    wool_type: "RED_WOOL"
  blue:
    color: "&9"
    name: "Blue"
    wool_type: "BLUE_WOOL"
```

Customize team colors, display names, and wool types. You can add more teams by extending this section.

### Database Configuration

```yaml
database:
  storage: "sqlite"
  sqlite:
    file-name: "stats.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "obsidianwars"
    username: "root"
    password: "password"
    pool-size: 10
```

**Options**:
- `storage`: Either `"sqlite"` or `"mysql"`
- SQLite requires no additional setup
- MySQL requires a running MySQL server and database creation

### Language Configuration

```yaml
language: "en"
player-language:
  enabled: true
```

- `language`: Default language for the server (en, es, az)
- `player-language.enabled`: Allow players to set individual language preferences with `/lang`

### Respawn Settings

```yaml
respawn-wait-time: 5
```

Time in seconds players wait before respawning (if their bed is intact).

### Health Display Configuration

```yaml
health-display:
  tablist: true
  scoreboard: true
  above-head: false
  format: "hearts"
  use-average: true
  obsidian-integrity: true
```

- `tablist`: Show health in tablist
- `scoreboard`: Show health in scoreboard
- `above-head`: Show health above player heads
- `format`: `"hearts"` or `"number"` - display format
- `use-average`: For scoreboard, show average per player (true) or total team health (false)
- `obsidian-integrity`: Display obsidian bed integrity percentage

### Timer Settings

```yaml
timers:
  default-countdown: 20
  default-preparation: "10m"
  sudden-death: "30m"
  game-time: "45m"
```

Default timers used when creating arenas (can be overridden per arena):
- `default-countdown`: Seconds before game starts
- `default-preparation`: Preparation phase duration (supports `s`, `m`, `h` suffixes)
- `sudden-death`: Time before sudden death activates
- `game-time`: Maximum game duration

### Reconnect Settings

```yaml
reconnect-timeout-seconds: 300
reconnect-grace-period-seconds: 30
```

- `reconnect-timeout-seconds`: Time players have to rejoin after disconnect (default: 5 minutes)
- `reconnect-grace-period-seconds`: Grace period before team elimination check

### Game Cleanup Settings

```yaml
arena-cleanup-delay-seconds: 10
rules-announcement-delay-seconds: 5
```

- `arena-cleanup-delay-seconds`: Delay before arena cleanup after game ends
- `rules-announcement-delay-seconds`: Delay before rules are announced after joining lobby

### Sound Effects Configuration

```yaml
sounds:
  join_lobby: "UI_TOAST_IN"
  match_start: "BLOCK_NOTE_BLOCK_PLING"
  obsidian_destroyed: "ENTITY_WITHER_DEATH"
  wall_fall: "ENTITY_ENDER_DRAGON_GROWL"
  protection_warn: "ENTITY_VILLAGER_NO"
  win_victory: "UI_TOAST_CHALLENGE_COMPLETE"
  countdown_tick: "BLOCK_NOTE_BLOCK_HAT"
  countdown_final: "BLOCK_NOTE_BLOCK_PLING"
  countdown_cancelled: "ENTITY_VILLAGER_NO"
  success: "ENTITY_PLAYER_LEVELUP"
  error: "ENTITY_VILLAGER_NO"
  sudden_death: "ENTITY_WITHER_SPAWN"
```

Sound effects for various game events. Use Spigot sound names from the [Sound API](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html).

### Title Configuration

```yaml
titles:
  victory:
    fade-in: 10
    stay: 60
    fade-out: 20
  obsidian_destroyed:
    fade-in: 10
    stay: 70
    fade-out: 20
  wall_fallen:
    fade-in: 10
    stay: 60
    fade-out: 20
  death:
    fade-in: 10
    stay: 40
    fade-out: 20
```

Controls title animation timing (in ticks, 20 ticks = 1 second):
- `fade-in`: Fade in duration
- `stay`: Display duration
- `fade-out`: Fade out duration

### Particle Effects Configuration

```yaml
particles:
  obsidian_target:
    type: "dust_color_transition"
    from_color: [0.7, 0.0, 1.0]
    to_color: [1.0, 0.0, 0.2]
    scale: 2.0
  resource_break:
    type: "dust_color_transition"
    from_color: [0.9, 0.8, 0.7]
    to_color: [1.0, 1.0, 1.0]
    scale: 1.0
```

Particle effects for game events. Colors are RGB values (0.0-1.0).

### Mob Area Settings

```yaml
mob-area:
  enabled: true
  mob-type: "ZOMBIE"
  spawn-interval-minutes: 1
  spawn-amount: 3
  max-mobs-per-area: 10
```

- `enabled`: Enable mob spawning in configured mob areas
- `mob-type`: Spigot EntityType name
- `spawn-interval-minutes`: How often mobs spawn
- `spawn-amount`: Number of mobs per spawn
- `max-mobs-per-area`: Maximum mobs in area at once

### XP Rewards Configuration

```yaml
xp-rewards:
  kill: 10
  final-kill: 20
  obsidian-broken: 50
  win: 100
  per-minute-played: 5
```

XP awarded for various actions:
- `kill`: Regular kill
- `final-kill`: Kill when bed is destroyed (no respawn)
- `obsidian-broken`: Destroying enemy obsidian
- `win`: Winning the game
- `per-minute-played`: XP per minute of gameplay

### Level-Up Configuration

```yaml
level-up:
  broadcast: false
  title: "&a&lLEVEL UP!"
  subtitle: "&7You are now Level &e{level}"
  sound: "ENTITY_PLAYER_LEVELUP"
  message: "&aCongratulations! You are now Level &e{level}!"
  broadcast-message: "&e{player} &ahas reached Level &e{level}!"
```

Level-up celebration settings:
- `broadcast`: Announce to entire server (true) or just player (false)
- `title`: Title shown to player
- `subtitle`: Subtitle shown to player (supports placeholders)
- `sound`: Sound effect played
- `message`: Chat message to player
- `broadcast-message`: Server broadcast if enabled

### Economy Configuration

```yaml
economy:
  type: "VAULT"
  rewards:
    obsidian-broken: 10.0
    kill: 5.0
    killstreak: 15.0
    win: 50.0
    play: 25.0
```

- `type`: `"VAULT"` (requires Vault plugin) or `"BUILT_IN"` (internal database)
- `rewards`: Currency awarded for various actions

If Vault is selected but not available, automatically falls back to BUILT_IN.

### Sudden Death Configuration

```yaml
sudden-death:
  disable-respawns: true
  border-contraction: false
  border-contraction-speed: 10
  wither-effect: true
  wither-duration: 10
  strength-effect: true
  strength-amplifier: 1
  glowing-effect: true
  glowing-duration: 999999
```

Sudden death mode effects:
- `disable-respawns`: No respawns during sudden death
- `border-contraction`: Contract world border (requires setup)
- `border-contraction-speed`: Border contraction speed
- `wither-effect`: Apply wither effect to players
- `wither-duration`: Wither effect duration in seconds
- `strength-effect`: Apply strength effect to players
- `strength-amplifier`: Strength effect amplifier (1 = Strength I)
- `glowing-effect`: Apply glowing effect to players
- `glowing-duration`: Glowing effect duration in seconds

### Queue System Configuration

```yaml
queue:
  enabled: true
  matchmaking-interval: 20
  max-queue-time: 300
```

- `enabled`: Enable queue matchmaking system
- `matchmaking-interval`: Auto-matchmaking interval in ticks (20 ticks = 1 second)
- `max-queue-time`: Maximum queue time before auto-assign in seconds (0 = disabled)

### Auto-Death Configuration

```yaml
auto-death:
  enabled: true
  y-level: -64
  death-message: "&cYou fell into the void!"
```

- `enabled`: Enable automatic void death
- `y-level`: Y-level at which players die (default: -64, void level)
- `death-message`: Message sent to player

### Per-World Features Configuration

```yaml
per-world:
  chat: false
  tablist: true
```

- `chat`: Separate chat channels per world (players in different worlds can't see each other's chat)
- `tablist`: Separate tablist per world (players only see others in their world)

## messages.yml

Customize all player-facing messages. This file contains message keys for all plugin events.

### Structure

```yaml
# Example messages.yml structure
prefix: "&7[&6ObsidianWars&7] "
join_arena: "&aYou joined arena {arena}!"
leave_arena: "&cYou left the arena."
...
```

Messages support color codes (`&`) and placeholders like `{arena}`, `{player}`, `{team}`.

### Common Placeholders

- `{player}` - Player name
- `{arena}` - Arena name
- `{team}` - Team name
- `{level}` - Player level
- `{kills}` - Kill count
- `{deaths}` - Death count

## scoreboards.yml

Configure scoreboard displays for lobbies and in-game.

### Structure

```yaml
lobby:
  title: "&6&lObsidianWars"
  lines:
    - "&7Arena: &f{arena}"
    - "&7Players: &f{players}/{max_players}"
    - ""
    - "&7Waiting for game start..."
game:
  title: "&c&l{team} Team"
  lines:
    - "&7Kills: &f{kills}"
    - "&7Bed: &f{bed_status}"
    - ""
    - "&7Time: &f{time}"
```

Supports dynamic placeholders and color codes.

## death-messages.yml

Configure death message templates.

### Structure

```yaml
default:
  - "{player} was slain by {killer}"
  - "{player} fell to their death"
  - "{player} was eliminated by {killer}"
```

Messages are randomly selected from the list when a player dies.

## kill-streaks.yml

Configure kill streak rewards and announcements.

### Structure

```yaml
streaks:
  3:
    message: "&e{player} is on a kill streak of 3!"
    sound: "ENTITY_PLAYER_LEVELUP"
  5:
    message: "&6{player} is dominating with 5 kills!"
    sound: "ENTITY_ENDER_DRAGON_GROWL"
  10:
    message: "&c&l{player} is UNSTOPPABLE with 10 kills!"
    sound: "ENTITY_WITHER_SPAWN"
```

Keys are kill streak counts. Each streak can have a message and sound effect.

## resource-blocks.yml

Configure which blocks can be mined as resources.

### Structure

```yaml
allowed-blocks:
  - "IRON_ORE"
  - "GOLD_ORE"
  - "DIAMOND_ORE"
  - "COAL_ORE"
  - "REDSTONE_ORE"
  - "LAPIS_ORE"
  - "EMERALD_ORE"
```

Use Spigot Material names. Only these blocks will be counted as resources when mined.

## Language Files

Language files are in `plugins/ObsidianWars/languages/`:
- `en.yml` - English
- `es.yml` - Spanish
- `az.yml` - Azerbaijani

### Adding a New Language

1. Copy `en.yml` to a new file (e.g., `fr.yml`)
2. Translate all message values
3. Add the language code to `config.yml`:
   ```yaml
   language: "fr"
   ```
4. Players can use `/lang fr` to select it

### Language File Structure

```yaml
# fr.yml example
commands:
  join: "&aVous avez rejoint l'arène {arena}!"
  leave: "&cVous avez quitté l'arène."
game:
  countdown: "&eLe jeu commence dans {seconds} secondes!"
  victory: "&6Victoire pour l'équipe {team}!"
```

## Configuration Best Practices

### Testing Changes

1. Always backup configuration files before editing
2. Test changes on a test server first
3. Use `/o reload` to reload configurations without restart
4. Check console for YAML syntax errors

### Performance Tuning

- Disable unused features (scoreboard, tablist) if using custom plugins
- Reduce particle effects for lower-end servers
- Increase database pool size for MySQL with many players
- Limit mob spawning in small arenas

### Balancing

- Adjust XP rewards to match your server's economy
- Tune game timers for desired match length
- Modify sudden death effects for intensity
- Balance kill streak rewards for motivation

## Troubleshooting

### Configuration Not Loading

- Check YAML syntax (no tabs, proper indentation)
- Verify file encoding is UTF-8
- Check console for specific error messages
- Ensure values match expected types (strings, numbers, booleans)

### Messages Not Showing

- Verify color codes use `&` not `§`
- Check that message keys match code references
- Ensure language file is loaded correctly
- Test with default English messages first

### Scoreboard Issues

- Disable scoreboard if using custom scoreboard plugins
- Check that title and lines are properly formatted
- Verify placeholders are supported
- Test with simple static text first

## Next Steps

- Review [Commands and Permissions](commands-and-permissions.md) for management
- Set up [Health Monitoring](health-and-metrics.md) for production
- Test configuration changes on a staging server

# Commands and Permissions

Complete reference for all ObsidianWars commands and permission nodes.

## Command Overview

ObsidianWars provides commands for both players and administrators. Most commands have aliases for convenience.

## Player Commands

These commands are available to all players by default.

### `/join [arena]` or `/play [arena]`

Join an arena to play.

**Aliases**: `play` (via `/obsidian play`)

**Permission**: `obsidianwars.command.join` (default: true)

**Usage**:
```
/join arena1
/play arena2
```

**Description**: Joins the specified arena. If no arena is specified, opens the arena selector GUI.

### `/leave`

Leave the current arena.

**Permission**: `obsidianwars.command.leave` (default: true)

**Usage**:
```
/leave
```

**Description**: Leaves the current arena and returns to the previous location.

### `/stats [player]`

View player statistics.

**Permission**: `obsidianwars.command.stats` (default: true)

**Usage**:
```
/stats
/stats PlayerName
```

**Description**: Displays statistics for yourself or another player, including:
- Games played
- Wins and losses
- Kills and deaths
- Level and XP
- Coins (if economy enabled)

### `/rejoin`

Rejoin an arena after disconnecting.

**Permission**: `obsidianwars.command.rejoin` (default: true)

**Usage**:
```
/rejoin
```

**Description**: Rejoins the last arena you were in, if within the reconnect timeout period (default: 5 minutes).

### `/spectate <arena|player>`

Spectate an arena or specific player.

**Permission**: `obsidianwars.command.spectate` (default: true)

**Usage**:
```
/spectate arena1
/spectate PlayerName
```

**Description**: Enter spectator mode to watch a game. Spectators can fly and are invisible to players.

### `/queue [arena]`

Join the queue for an arena.

**Permission**: `obsidianwars.command.queue` (default: true)

**Usage**:
```
/queue
/queue arena1
```

**Description**: Joins the matchmaking queue. If no arena specified, joins the queue for the best available arena.

### `/lang <language_code>`

Set your language preference.

**Aliases**: `language`

**Permission**: `obsidianwars.command.lang` (default: true)

**Usage**:
```
/lang en
/lang es
/lang az
```

**Description**: Sets your personal language preference. Requires `player-language.enabled: true` in config.yml.

## Main Command: `/obsidian` or `/o`

The main command provides access to all plugin features.

**Aliases**: `o`, `obsidianwars`

**Permission**: `obsidianwars.command.*` (varies by subcommand)

### `/o play <arena>`

Join an arena (same as `/join`).

**Permission**: `obsidianwars.command.join`

### `/o leave`

Leave current arena (same as `/leave`).

**Permission**: `obsidianwars.command.leave`

### `/o gui`

Open the arena selector GUI.

**Permission**: `obsidianwars.command.gui` (default: true)

**Usage**:
```
/o gui
```

**Description**: Opens a graphical interface showing all available arenas with their status.

### `/o team`

Open the team selection GUI.

**Permission**: `obsidianwars.command.team` (default: true)

**Usage**:
```
/o team
```

**Description**: Opens a GUI to select your team when joining an arena.

### `/o cmds`

Display all available commands.

**Permission**: `obsidianwars.command.cmds` (default: true)

**Usage**:
```
/o cmds
```

**Description**: Shows a list of all commands you have permission to use.

### `/o arenalist`

List all configured arenas.

**Permission**: `obsidianwars.command.arenalist` (default: true)

**Usage**:
```
/o arenalist
```

**Description**: Displays all arenas with their current status (READY, PLAYING, DISABLED, etc.).

## Admin Commands

These commands require OP or specific admin permissions.

### `/o create <arena_name>`

Create a new arena.

**Permission**: `obsidianwars.command.create` (default: op)

**Usage**:
```
/o create myarena
```

**Description**: Creates a new arena with the specified name. Follow with arena setup commands.

### `/o delete <arena_name>`

Delete an arena.

**Permission**: `obsidianwars.command.delete` (default: op)

**Usage**:
```
/o delete myarena
```

**Description**: Permanently deletes an arena and its configuration. This cannot be undone.

### `/o arena <arena_name> [subcommand]`

Arena setup and management commands.

**Permission**: `obsidianwars.command.arena` (default: op)

**Subcommands**:

#### `/o arena <arena_name> setlobby`
Set the waiting lobby location.

#### `/o arena <arena_name> setspawn <team>`
Set a team's spawn point. Team names: `red`, `blue`

#### `/o arena <arena_name> setbed <team>`
Set a team's bed location. Team names: `red`, `blue`

#### `/o arena <arena_name> setregion`
Set the arena region (requires wand selection).

#### `/o arena <arena_name> setmobarea`
Set the mob spawning area (requires wand selection).

#### `/o arena <arena_name> setcountdown <seconds>`
Set the countdown timer before game starts.

#### `/o arena <arena_name> setpreparation <time>`
Set the preparation phase duration (e.g., `10m`).

#### `/o arena <arena_name> setsuddendeath <time>`
Set the sudden death trigger time (e.g., `30m`).

#### `/o arena <arena_name> setgametime <time>`
Set the maximum game duration (e.g., `45m`).

#### `/o arena <arena_name> info`
Display arena configuration and status.

#### `/o arena <arena_name> restoresnapshot`
Restore arena from snapshot.

### `/o wand`

Get the arena setup wand.

**Permission**: `obsidianwars.command.wand` (default: op)

**Usage**:
```
/o wand
```

**Description**: Gives you a wooden axe for selecting regions. Left-click to set position 1, right-click to set position 2.

### `/o forcestart <arena_name>`

Force start a game.

**Permission**: `obsidianwars.command.force` (default: op)

**Usage**:
```
/o forcestart arena1
```

**Description**: Starts the game immediately, bypassing the countdown. Useful for testing.

### `/o forceend <arena_name>`

Force end a game.

**Permission**: `obsidianwars.command.forceend` (default: op)

**Usage**:
```
/o forceend arena1
```

**Description**: Ends the current game immediately.

### `/o forceprep <arena_name>`

Skip preparation phase.

**Permission**: `obsidianwars.command.forceprep` (default: op)

**Usage**:
```
/o forceprep arena1
```

**Description**: Skips the preparation phase and drops bedrock walls immediately.

### `/o disablearena <arena_name>`

Disable an arena.

**Permission**: `obsidianwars.command.disablearena` (default: op)

**Usage**:
```
/o disablearena arena1
```

**Description**: Disables an arena so players cannot join it.

### `/o enablearena <arena_name>`

Enable an arena.

**Permission**: `obsidianwars.command.enablearena` (default: op)

**Usage**:
```
/o enablearena arena1
```

**Description**: Enables a disabled arena for players to join.

### `/o admin <subcommand>`

Admin management commands.

**Permission**: `obsidianwars.command.admin` (default: op)

**Subcommands**:

#### `/o admin level <player> <amount>`
Set a player's level.

#### `/o admin coin <player> <amount>`
Set a player's coins.

#### `/o admin exp <player> <amount>`
Set a player's XP.

#### `/o admin stats <player> <stat> <value>`
Set a specific player statistic.

### `/o reload`

Reload plugin configurations.

**Permission**: `obsidianwars.command.reload` (default: op)

**Usage**:
```
/o reload
```

**Description**: Reloads all configuration files (config.yml, messages.yml, etc.) without restarting the server.

### `/o debug`

Toggle debug logging.

**Permission**: `obsidianwars.command.debug` (default: op)

**Usage**:
```
/o debug
```

**Description**: Enables or disables detailed debug logging in the console for troubleshooting.

## Permission Nodes

### Wildcard Permissions

#### `obsidianwars.*`
Gives access to all ObsidianWars permissions.

**Default**: op

**Children**:
- `obsidianwars.command.*`

#### `obsidianwars.command.*`
Gives access to all ObsidianWars commands.

**Default**: op

**Children**:
- All individual command permissions

### Player Command Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `obsidianwars.command.join` | true | Join arenas |
| `obsidianwars.command.leave` | true | Leave arenas |
| `obsidianwars.command.stats` | true | View statistics |
| `obsidianwars.command.rejoin` | true | Rejoin after disconnect |
| `obsidianwars.command.spectate` | true | Spectate games |
| `obsidianwars.command.queue` | true | Join matchmaking queue |
| `obsidianwars.command.gui` | true | Open arena selector GUI |
| `obsidianwars.command.team` | true | Open team selection GUI |
| `obsidianwars.command.cmds` | true | View command list |
| `obsidianwars.command.arenalist` | true | List all arenas |
| `obsidianwars.command.lang` | true | Set language preference |

### Admin Command Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `obsidianwars.command.create` | op | Create arenas |
| `obsidianwars.command.delete` | op | Delete arenas |
| `obsidianwars.command.arena` | op | Arena setup commands |
| `obsidianwars.command.wand` | op | Get setup wand |
| `obsidianwars.command.force` | op | Force start/end games |
| `obsidianwars.command.forceend` | op | Force end games (legacy) |
| `obsidianwars.command.forcestart` | op | Force start games (legacy) |
| `obsidianwars.command.forceprep` | op | Skip preparation phase |
| `obsidianwars.command.disablearena` | op | Disable arenas |
| `obsidianwars.command.enablearena` | op | Enable arenas |
| `obsidianwars.command.admin` | op | Admin management commands |
| `obsidianwars.command.reload` | op | Reload configurations |
| `obsidianwars.command.debug` | op | Toggle debug mode |

## Permission Examples

### Regular Player
Give players basic access:
```yaml
permissions:
  obsidianwars.command.join: true
  obsidianwars.command.leave: true
  obsidianwars.command.stats: true
  obsidianwars.command.rejoin: true
  obsidianwars.command.spectate: true
  obsidianwars.command.queue: true
  obsidianwars.command.gui: true
  obsidianwars.command.team: true
  obsidianwars.command.cmds: true
  obsidianwars.command.arenalist: true
  obsidianwars.command.lang: true
```

### Moderator
Give moderators additional management access:
```yaml
permissions:
  obsidianwars.command.*: true
  obsidianwars.command.create: false
  obsidianwars.command.delete: false
  obsidianwars.command.admin: false
```

### Admin
Give admins full access:
```yaml
permissions:
  obsidianwars.*: true
```

## Command Aliases

For convenience, many commands have aliases:

| Command | Aliases |
|---------|---------|
| `/obsidian` | `/o`, `/obsidianwars` |
| `/join` | `/play` (via `/o play`) |
| `/lang` | `/language` |

## Tab Completion

Most commands support tab completion for:
- Arena names
- Player names
- Subcommands
- Language codes

## Next Steps

- Configure [Game Settings](configuration.md) for gameplay balance
- Set up [Health Monitoring](health-and-metrics.md) for production servers
- Review [Arena Setup](arena-setup.md) for creating custom arenas

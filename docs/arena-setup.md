# Arena Setup Guide

This guide walks you through creating and configuring arenas for ObsidianWars.

## Overview

An arena in ObsidianWars consists of:
- **Waiting Lobby**: Where players wait before the game starts
- **Team Spawns**: Where players respawn during the game
- **Beds**: The obsidian beds that teams must defend
- **Regions**: Defined areas for gameplay boundaries
- **Resource Areas**: Locations where resources spawn

## Prerequisites

Before creating an arena:
1. Have a world ready for the arena (can be the main world or a separate world)
2. Have OP permission or the `obsidianwars.command.*` permission node
3. Understand basic server administration and world editing

## Creating an Arena

### Step 1: Get the Setup Wand

The setup wand is a wooden axe used to select regions and points.

```
/o wand
```

You will receive a golden axe named "Obsidian Wars Wand" in your inventory.

### Step 2: Create the Arena

Create a new arena with a unique name:

```
/o create <arena_name>
```

Example:
```
/o create arena1
```

### Step 3: Arena Setup Wizard

Use the arena command to enter setup mode:

```
/o arena <arena_name>
```

This will start the interactive setup wizard. Follow the prompts to configure each component.

## Setup Components

### 1. Set Waiting Lobby

The waiting lobby is where players gather before the game starts.

```
/o arena <arena_name> setlobby
```

Stand at the desired lobby location and run this command. The lobby should:
- Be in a safe area
- Have enough space for all players
- Be away from the gameplay area

### 2. Set Team Spawns

Each team needs a spawn point where players appear at game start and respawn (if their bed is intact).

For Red team:
```
/o arena <arena_name> setspawn red
```

For Blue team:
```
/o arena <arena_name> setspawn blue
```

Stand at each team's spawn location when running the command.

### 3. Set Team Beds

The obsidian bed is the core objective - protect yours, destroy the enemy's.

For Red team's bed:
```
/o arena <arena_name> setbed red
```

For Blue team's bed:
```
/o arena <arena_name> setbed blue
```

Stand on or near the obsidian block when running this command.

### 4. Set Arena Region

Define the playable area using the wand:

1. Left-click a block to set Position 1 (first corner)
2. Right-click a block to set Position 2 (opposite corner)
3. Run:
   ```
   /o arena <arena_name> setregion
   ```

The region should:
- Include all team spawns and beds
- Include resource areas
- Exclude the waiting lobby
- Be large enough for gameplay but not excessively large

### 5. Set Mob Area (Optional)

Configure a mob spawning area for added challenge:

```
/o arena <arena_name> setmobarea
```

Use the wand to select the mob spawning region first. Mobs will spawn at intervals configured in `config.yml`.

### 6. Configure Game Timers

Set custom timers for this arena (optional - uses defaults if not set):

```
/o arena <arena_name> setcountdown <seconds>
/o arena <arena_name> setpreparation <time>
/o arena <arena_name> setsuddendeath <time>
/o arena <arena_name> setgametime <time>
```

Time format examples:
- `20` - 20 seconds
- `10m` - 10 minutes
- `1h` - 1 hour

### 7. Verify Arena Configuration

Check your arena setup:

```
/o arena <arena_name> info
```

This displays all configured settings and identifies any missing components.

## Arena Management Commands

### Enable/Disable Arena

Enable an arena for players to join:
```
/o enablearena <arena_name>
```

Disable an arena (players cannot join):
```
/o disablearena <arena_name>
```

### Delete Arena

Remove an arena completely:
```
/o delete <arena_name>
```

**Warning**: This cannot be undone and will delete all arena configuration.

### Force Game States

For testing purposes, you can force game states:

```
/o forcestart <arena_name>  # Start game immediately
/o forceend <arena_name>    # End game immediately
/o forceprep <arena_name>   # Skip preparation phase
```

## Arena Configuration File

Arenas are stored in `plugins/ObsidianWars/arenas/<arena_name>.yml`. You can manually edit these files, but using commands is recommended.

Example arena file structure:
```yaml
arena_name: "arena1"
world: "world"
lobby:
  x: 100
  y: 64
  z: 100
spawns:
  red:
    x: 50
    y: 64
    z: 50
  blue:
    x: 150
    y: 64
    z: 150
beds:
  red:
    x: 50
    y: 64
    z: 55
  blue:
    x: 150
    y: 64
    z: 155
region:
  corner1:
    x: 0
    y: 0
    z: 0
  corner2:
    x: 200
    y: 100
    z: 200
```

## World Snapshots

ObsidianWars automatically creates world snapshots before games and restores them after. This ensures arenas reset to their original state after each game.

### Manual Snapshot Management

Restore an arena to its snapshot state:
```
/o arena <arena_name> restoresnapshot
```

Snapshots are stored in `plugins/ObsidianWars/snapshots/<arena_name>/`.

## Best Practices

### Arena Design

1. **Balance**: Ensure both teams have equal advantages
2. **Size**: Keep arenas appropriately sized for your expected player count
3. **Resources**: Place resource generators strategically
4. **Safety**: Ensure spawn points are safe from immediate attack
5. **Visibility**: Make beds visible but defensible

### Performance

1. **Region size**: Keep regions as small as practical
2. **Entity count**: Limit mob spawning in small arenas
3. **Redstone**: Avoid excessive redstone contraptions
4. **Lighting**: Pre-light arenas to reduce mob spawning

### Testing

1. Test with 1-2 players first
2. Verify all spawn points work correctly
3. Test bed destruction mechanics
4. Check resource spawning
5. Verify game phases (lobby → preparation → playing → sudden death → end)

## Troubleshooting

### Players Can't Join

- Check if arena is enabled: `/o arenalist`
- Verify arena status is READY
- Ensure all required points are set (lobby, spawns, beds, region)

### Spawns Not Working

- Verify spawn coordinates are correct
- Check that the world exists
- Ensure spawn points are within the arena region

### Beds Not Destroyable

- Verify bed coordinates are correct
- Check that beds are made of obsidian
- Ensure the arena is in PLAYING state
- Verify bed destruction is enabled in config

### Region Issues

- Re-select the region with the wand
- Verify both corners are set
- Ensure region is large enough to include all gameplay elements

### World Restoration Fails

- Check disk space
- Verify snapshot directory permissions
- Ensure world is not being used by another plugin
- Check console logs for specific errors

## Example: Complete Arena Setup

Here's a complete example of setting up an arena named "skywars":

```bash
# 1. Get wand
/o wand

# 2. Create arena
/o create skywars

# 3. Set lobby (stand at lobby location)
/o arena skywars setlobby

# 4. Set red spawn (stand at red team spawn)
/o arena skywars setspawn red

# 5. Set blue spawn (stand at blue team spawn)
/o arena skywars setspawn blue

# 6. Set red bed (stand on red obsidian)
/o arena skywars setbed red

# 7. Set blue bed (stand on blue obsidian)
/o arena skywars setbed blue

# 8. Select region with wand (left-click corner 1, right-click corner 2)
# Then:
/o arena skywars setregion

# 9. Set custom timers (optional)
/o arena skywars setcountdown 30
/o arena skywars setpreparation 5m

# 10. Verify setup
/o arena skywars info

# 11. Enable arena
/o enablearena skywars

# 12. Test
/o play skywars
```

## Next Steps

- Customize [Configuration](configuration.md) for gameplay balance
- Set up [Commands and Permissions](commands-and-permissions.md) for staff
- Configure [Health Monitoring](health-and-metrics.md) for production

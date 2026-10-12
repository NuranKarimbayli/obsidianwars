# Getting Started

This guide will help you install and configure ObsidianWars on your Minecraft server.

## Prerequisites

Before installing ObsidianWars, ensure you have:

- A Minecraft server running **Paper 1.21+** or compatible Spigot fork
- **Java 21** or higher installed
- Administrative access to the server (OP or console access)
- Optional: [Vault](https://github.com/MilkBowl/VaultAPI) plugin for economy integration
- Optional: [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) for placeholder support

## Installation

### Step 1: Download the Plugin

1. Download the latest `obsidianwars-x.x.x.jar` from the [GitHub Releases](https://github.com/NuranKarimbayli/obsidianwars/releases)
2. Or build from source using Maven:
   ```bash
   git clone https://github.com/NuranKarimbayli/obsidianwars.git
   cd obsidianwars
   mvn clean package
   ```
   The JAR will be in the `target/` directory.

### Step 2: Install the Plugin

1. Copy the JAR file to your server's `plugins/` directory
2. Restart your server or load the plugin using a plugin manager

### Step 3: Initial Configuration

After the first load, ObsidianWars will create the following files in `plugins/ObsidianWars/`:

- `config.yml` - Main configuration file
- `messages.yml` - Customizable messages
- `plugin.yml` - Plugin metadata (read-only)
- `scoreboards.yml` - Scoreboard configuration
- `death-messages.yml` - Death message templates
- `kill-streaks.yml` - Kill streak rewards
- `resource-blocks.yml` - Allowed resource blocks
- `languages/` - Language files (en.yml, es.yml, az.yml)

### Step 4: Database Setup

#### SQLite (Default)

SQLite is the default database option - no additional setup required. The plugin will create `stats.db` in the plugin folder.

#### MySQL (Optional)

For better performance with large servers, configure MySQL in `config.yml`:

```yaml
database:
  storage: "mysql"
  mysql:
    host: "localhost"
    port: 3306
    database: "obsidianwars"
    username: "your_username"
    password: "your_password"
    pool-size: 10
```

Create the MySQL database before starting the server:
```sql
CREATE DATABASE obsidianwars;
```

### Step 5: Language Configuration

Set the default language in `config.yml`:

```yaml
language: "en"  # Options: en, es, az
```

Enable per-player language preferences:
```yaml
player-language:
  enabled: true
```

Players can then use `/lang <code>` to set their preferred language.

## First Time Setup

### Step 1: Create Your First Arena

See [Arena Setup](arena-setup.md) for detailed arena creation instructions.

Quick start:
1. Get the setup wand: `/o wand`
2. Create a new arena: `/o create <arena_name>`
3. Set up the arena using the arena command: `/o arena <arena_name> setlobby`
4. Follow the setup wizard prompts

### Step 2: Test the Arena

1. Join the arena: `/join <arena_name>` or `/o play <arena_name>`
2. Verify spawn points, regions, and game mechanics
3. Use `/o forcestart` to start the game immediately for testing
4. Use `/o forceend` to end the game

### Step 3: Configure Game Settings

Edit `config.yml` to customize:
- Team colors and names
- Game timers (countdown, preparation, sudden death)
- XP and economy rewards
- Sound effects and particles
- Health display options

## Optional Integrations

### Vault Economy

If you want to use Vault for economy rewards:

1. Install [Vault](https://github.com/MilkBowl/VaultAPI)
2. Install a Vault-compatible economy plugin (e.g., EssentialsX)
3. Configure in `config.yml`:
   ```yaml
   economy:
     type: "VAULT"
   ```

If Vault is not available, the plugin will automatically fall back to the built-in economy system.

### PlaceholderAPI

For placeholder support in other plugins:

1. Install [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI)
2. Reload the server
3. ObsidianWars will automatically register its placeholders

Available placeholders include player stats, level, coins, and more.

## Verification

After installation, verify everything is working:

1. Check the console for successful load messages
2. Run `/o cmds` to see available commands
3. Run `/o arenalist` to view configured arenas
4. Check `/stats` to verify database connectivity
5. Test with a few players to ensure gameplay works correctly

## Troubleshooting

### Plugin Won't Load

- Ensure you're using Paper/Spigot 1.21+
- Check that Java 21+ is installed
- Review console logs for error messages
- Verify no conflicting plugins

### Database Errors

- For MySQL: verify database credentials and connectivity
- Check that the database user has proper permissions
- Ensure the database exists before first run
- Try SQLite if MySQL setup fails

### Arena Issues

- Verify all arena points are set (lobby, spawns, beds, regions)
- Check that the arena world exists
- Use `/o arena <name> info` to view arena configuration
- Review console logs for specific error messages

### Performance Issues

- Reduce particle effects in config.yml
- Disable unused features (scoreboard, tablist)
- Increase database pool size for MySQL
- Check server TPS and resource usage

## Next Steps

- Read [Arena Setup](arena-setup.md) to create custom arenas
- Review [Commands and Permissions](commands-and-permissions.md) for full command reference
- Customize [Configuration](configuration.md) to match your server's needs
- Set up [Health Monitoring](health-and-metrics.md) for production use

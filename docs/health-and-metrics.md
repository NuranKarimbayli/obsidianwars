# Health and Metrics

System health monitoring, metrics collection, and troubleshooting for ObsidianWars.

## Overview

ObsidianWars includes built-in health monitoring and metrics collection to help you maintain optimal performance and troubleshoot issues.

## Health Check Service

The plugin includes a health check service that monitors system health and logs warnings for potential issues.

### Health Checks Performed

1. **Database Connectivity**: Verifies database connection is healthy
2. **Arena State Cleanup**: Detects and clears stuck arena states
3. **Task Management**: Monitors for leaked or stuck tasks
4. **Memory Usage**: Tracks memory consumption trends
5. **Performance Metrics**: Logs TPS (ticks per second) and lag indicators

### Enabling Health Checks

Health checks are enabled by default. They run periodically in the background.

Check health status in console logs:
```
[ObsidianWars] Health check passed - all systems operational
```

If issues are detected:
```
[ObsidianWars] Health check warning: 2 arenas in stuck state
```

## bStats Metrics

ObsidianWars uses [bStats](https://bstats.org/) for anonymous usage statistics.

### What bStats Collects

- Server version (Paper/Spigot)
- Minecraft version
- Plugin version
- Number of configured arenas
- Player count (anonymous)
- Database type (SQLite/MySQL)
- Enabled features (scoreboard, tablist, etc.)

### Privacy

- All data is anonymous
- No personal information is collected
- No server IP or identifiers are sent
- Data is used solely for plugin improvement

### Disabling Metrics

To disable bStats metrics, remove the bStats integration from the plugin code or block the bStats endpoint at the network level.

## Performance Monitoring

The plugin includes a performance monitor that tracks:

- Task execution times
- Event handler performance
- Database query performance
- Memory usage patterns

### Viewing Performance Data

Performance warnings are logged to console:
```
[ObsidianWars] Performance warning: Database query took 150ms (threshold: 100ms)
```

### Performance Thresholds

Default thresholds (configurable in code):
- Database queries: 100ms
- Event handlers: 50ms
- Task execution: 100ms

## Database Health

### SQLite

SQLite is suitable for small to medium servers (up to ~50 concurrent players).

**Monitoring**:
- Check `stats.db` file size (should not grow excessively)
- Monitor query times in console logs
- Look for database lock warnings

**Maintenance**:
- Periodically backup `stats.db`
- Consider migrating to MySQL for larger servers

### MySQL

MySQL is recommended for larger servers (50+ concurrent players).

**Monitoring**:
- Check MySQL connection pool usage
- Monitor query execution times
- Watch for connection timeouts

**Configuration**:
```yaml
database:
  storage: "mysql"
  mysql:
    pool-size: 10  # Increase for larger servers
```

**Maintenance**:
- Optimize MySQL server settings
- Regular database backups
- Monitor connection limits

## Task Management

The plugin uses a centralized task manager to prevent task leaks.

### Task Types

- Countdown timers
- Game phase timers
- Resource spawning
- Mob spawning
- Cleanup tasks
- Health check tasks

### Task Cleanup

Tasks are automatically cleaned up when:
- Arena game ends
- Plugin disables
- Tasks complete naturally

### Stuck Task Detection

The health check service detects stuck tasks and logs warnings:
```
[ObsidianWars] Warning: Arena 'arena1' has 3 stuck tasks
```

Manual cleanup:
```
/o reload  # Reloads and cleans up tasks
```

## Arena State Management

### State Types

Arenas can be in these states:
- `READY` - Available for players to join
- `WAITING` - Players in lobby, waiting for game start
- `STARTING` - Countdown in progress
- `PREPARATION` - Preparation phase (bedrock walls up)
- `PLAYING` - Active game
- `ENDING` - Game ending, showing results
- `DISABLED` - Manually disabled

### Stuck State Detection

On plugin startup, the plugin checks for arenas stuck in active states from server crashes:
```
[ObsidianWars] Checking for stuck arena statuses from previous shutdown...
[ObsidianWars] Arena 'arena1' is stuck in PLAYING status. Resetting to READY...
```

Automatic cleanup:
- Resets stuck arenas to READY
- Cancels lingering tasks
- Restores world snapshots
- Restores world rules

## Troubleshooting

### High CPU Usage

**Symptoms**: Server TPS drops, lag during games

**Solutions**:
1. Reduce particle effects in `config.yml`
2. Disable unused features (scoreboard, tablist)
3. Reduce mob spawning frequency
4. Increase game timers to reduce frequent state changes
5. Check for conflicting plugins

### High Memory Usage

**Symptoms**: Memory warnings, out of memory errors

**Solutions**:
1. Increase server RAM allocation
2. Reduce number of concurrent arenas
3. Clear expired player data regularly
4. Check for memory leaks in other plugins
5. Restart server periodically

### Database Performance Issues

**Symptoms**: Slow stat loading, lag on player join

**Solutions**:
1. For SQLite: Migrate to MySQL
2. For MySQL: Increase pool size
3. Optimize database indexes
4. Check database server performance
5. Reduce frequency of stat updates

### Arena Not Resetting

**Symptoms**: Arena world not clean after game

**Solutions**:
1. Check snapshot directory permissions
2. Verify disk space availability
3. Manual snapshot restore: `/o arena <name> restoresnapshot`
4. Check console for snapshot errors
5. Disable world snapshots if unreliable

### Players Can't Rejoin

**Symptoms**: Rejoin timeout errors

**Solutions**:
1. Increase `reconnect-timeout-seconds` in config
2. Check database connectivity
3. Verify player data is being saved
4. Check arena state is not stuck
5. Review disconnect grace period settings

### Stuck Tasks

**Symptoms**: Tasks not completing, console warnings

**Solutions**:
1. Run `/o reload` to force cleanup
2. Restart server if reload doesn't help
3. Check for plugin conflicts
4. Review console logs for task-related errors
5. Disable problematic features temporarily

## Debug Mode

Enable debug mode for detailed troubleshooting:

```
/o debug
```

Debug mode logs:
- Detailed event flow
- Database query details
- Task execution tracking
- Arena state transitions
- Player data operations

**Warning**: Debug mode produces extensive logs. Disable after troubleshooting.

## Logging

### Log Locations

- Console: Real-time logging
- `logs/latest.log`: Server log file
- Plugin-specific logs: Sent to server log

### Log Levels

- `INFO`: Normal operations
- `WARNING`: Non-critical issues
- `SEVERE`: Critical errors requiring attention

### Important Log Messages

**Startup**:
```
[ObsidianWars] ObsidianWars plugin activated!
[ObsidianWars] Database initialized: SQLite
[ObsidianWars] Loaded 3 arenas
```

**Errors**:
```
[ObsidianWars] SEVERE: Failed to load arena config
[ObsidianWars] WARNING: Database connection lost, reconnecting...
```

**Performance**:
```
[ObsidianWars] Performance warning: Event handler took 80ms
```

## Maintenance Tasks

### Regular Maintenance

1. **Weekly**:
   - Backup configuration files
   - Backup database (SQLite file or MySQL dump)
   - Review console logs for warnings
   - Check disk space

2. **Monthly**:
   - Review player statistics for anomalies
   - Check arena configurations
   - Update plugin to latest version
   - Test backup restoration

3. **Quarterly**:
   - Full server backup
   - Database optimization
   - Performance audit
   - Review and update configurations

### Backup Procedures

**Configuration Files**:
```bash
# Backup plugin configuration
cp -r plugins/ObsidianWars/ backups/obsidianwars-config-$(date +%Y%m%d)/
```

**SQLite Database**:
```bash
# Backup SQLite database
cp plugins/ObsidianWars/stats.db backups/stats-$(date +%Y%m%d).db
```

**MySQL Database**:
```bash
# Backup MySQL database
mysqldump -u root -p obsidianwars > backups/obsidianwars-$(date +%Y%m%d).sql
```

**Arena Snapshots**:
```bash
# Backup arena world snapshots
cp -r plugins/ObsidianWars/snapshots/ backups/snapshots-$(date +%Y%m%d)/
```

## Monitoring Tools

### External Monitoring

Consider using external monitoring tools:
- **Prometheus + Grafana**: For metrics visualization
- **Spark**: For server performance monitoring
- **Plan**: For player analytics
- **Timings**: For plugin performance profiling

### Plugin-Specific Monitoring

Monitor ObsidianWars-specific metrics:
- Active arena count
- Players in arenas
- Database query times
- Task execution times
- Memory usage trends

## Performance Optimization Tips

### General

1. Use Paper server (better performance than Spigot)
2. Allocate sufficient RAM (4GB+ for medium servers)
3. Keep plugin count reasonable
4. Use optimized world settings
5. Regular server restarts (daily or weekly)

### ObsidianWars-Specific

1. Disable unused UI features if using custom plugins
2. Reduce particle effects for lower-end servers
3. Use MySQL instead of SQLite for large servers
4. Limit concurrent arenas based on player count
5. Adjust game timers to reduce frequent state changes

### Configuration Tuning

```yaml
# Reduce resource usage
features:
  scoreboard: false  # If using custom scoreboard plugin
  tablist: false     # If using custom tablist plugin

# Reduce particle effects
particles:
  obsidian_target:
    scale: 1.0  # Reduced from 2.0

# Increase database pool for MySQL
database:
  mysql:
    pool-size: 20  # Increased from 10

# Reduce mob spawning
mob-area:
  spawn-interval-minutes: 2  # Increased from 1
  max-mobs-per-area: 5       # Reduced from 10
```

## Support and Resources

### Getting Help

If you encounter issues not covered here:

1. Check the [GitHub Issues](https://github.com/NuranKarimbayli/obsidianwars/issues)
2. Search existing issues for similar problems
3. Create a new issue with:
   - Server version and software
   - Plugin version
   - Relevant configuration
   - Console logs
   - Steps to reproduce

### Useful Resources

- [PaperMC Documentation](https://docs.papermc.io/)
- [Spigot API Javadocs](https://hub.spigotmc.org/javadocs/spigot/)
- [bStats](https://bstats.org/)
- [Vault API](https://github.com/MilkBowl/VaultAPI)
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI)

## Next Steps

- Review [Configuration](configuration.md) for optimization options
- Check [Commands and Permissions](commands-and-permissions.md) for admin tools
- Set up regular backup procedures
- Configure external monitoring for production servers

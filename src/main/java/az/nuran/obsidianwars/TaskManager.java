package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Centralized task management for ObsidianWars.
 * Manages all BukkitRunnable and BukkitTask instances across the plugin.
 * Provides clean cancellation mechanisms to prevent task leaks.
 */
public class TaskManager {

    private static TaskManager instance;
    private final Plugin plugin;
    private final Logger logger;

    // Map of task IDs to their associated BukkitTask objects
    private final Map<String, BukkitTask> tasks;

    // Map of arena names to their task groups for bulk cancellation
    private final Map<String, java.util.Set<String>> arenaTaskGroups;

    public TaskManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.tasks = new ConcurrentHashMap<>();
        this.arenaTaskGroups = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the TaskManager singleton.
     *
     * @param plugin The plugin instance
     */
    public static void initialize(Plugin plugin) {
        if (instance == null) {
            instance = new TaskManager(plugin);
        }
    }

    /**
     * Gets the TaskManager instance.
     *
     * @return The TaskManager instance
     */
    public static TaskManager getInstance() {
        return instance;
    }

    /**
     * Registers a BukkitTask with a unique ID.
     *
     * @param taskId The unique task identifier
     * @param task The BukkitTask to register
     */
    public void registerTask(String taskId, BukkitTask task) {
        tasks.put(taskId, task);
        logger.fine("Registered task: " + taskId);
    }

    /**
     * Registers a BukkitTask with a unique ID and associates it with an arena.
     *
     * @param taskId The unique task identifier
     * @param task The BukkitTask to register
     * @param arenaName The arena name (for group cancellation)
     */
    public void registerTask(String taskId, BukkitTask task, String arenaName) {
        tasks.put(taskId, task);

        if (arenaName != null) {
            arenaTaskGroups.computeIfAbsent(arenaName, k -> ConcurrentHashMap.newKeySet()).add(taskId);
        }

        logger.fine("Registered task: " + taskId + (arenaName != null ? " for arena: " + arenaName : ""));
    }

    /**
     * Cancels a task by its ID.
     *
     * @param taskId The task ID to cancel
     * @return true if the task was found and cancelled, false otherwise
     */
    public boolean cancelTask(String taskId) {
        BukkitTask task = tasks.remove(taskId);
        if (task != null) {
            task.cancel();
            logger.fine("Cancelled task: " + taskId);
            return true;
        }
        return false;
    }

    /**
     * Cancels all tasks associated with a specific arena.
     *
     * @param arenaName The arena name
     * @return The number of tasks cancelled
     */
    public int cancelArenaTasks(String arenaName) {
        java.util.Set<String> taskIds = arenaTaskGroups.remove(arenaName);
        if (taskIds == null) {
            return 0;
        }

        int cancelled = 0;
        for (String taskId : taskIds) {
            if (cancelTask(taskId)) {
                cancelled++;
            }
        }

        logger.info("Cancelled " + cancelled + " tasks for arena: " + arenaName);
        return cancelled;
    }

    /**
     * Cancels all registered tasks.
     *
     * @return The number of tasks cancelled
     */
    public int cancelAll() {
        int cancelled = 0;
        for (BukkitTask task : tasks.values()) {
            task.cancel();
            cancelled++;
        }
        tasks.clear();
        arenaTaskGroups.clear();
        logger.info("Cancelled all " + cancelled + " tasks");
        return cancelled;
    }

    /**
     * Checks if a task is currently running.
     *
     * @param taskId The task ID to check
     * @return true if the task exists and is running, false otherwise
     */
    public boolean isTaskRunning(String taskId) {
        BukkitTask task = tasks.get(taskId);
        return task != null && !task.isCancelled();
    }

    /**
     * Gets the number of active tasks.
     *
     * @return The number of active tasks
     */
    public int getActiveTaskCount() {
        int count = 0;
        for (BukkitTask task : tasks.values()) {
            if (!task.isCancelled()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the number of active tasks for a specific arena.
     *
     * @param arenaName The arena name
     * @return The number of active tasks for the arena
     */
    public int getArenaTaskCount(String arenaName) {
        java.util.Set<String> taskIds = arenaTaskGroups.get(arenaName);
        if (taskIds == null) {
            return 0;
        }

        int count = 0;
        for (String taskId : taskIds) {
            if (isTaskRunning(taskId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Runs a task asynchronously after a delay.
     *
     * @param taskId The unique task identifier
     * @param runnable The task to run
     * @param delayTicks The delay in ticks
     * @return The BukkitTask
     */
    public BukkitTask runAsyncLater(String taskId, Runnable runnable, long delayTicks) {
        BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
        registerTask(taskId, task);
        return task;
    }

    /**
     * Runs a task synchronously after a delay.
     *
     * @param taskId The unique task identifier
     * @param runnable The task to run
     * @param delayTicks The delay in ticks
     * @return The BukkitTask
     */
    public BukkitTask runLater(String taskId, Runnable runnable, long delayTicks) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        registerTask(taskId, task);
        return task;
    }

    /**
     * Runs a task asynchronously at a fixed interval.
     *
     * @param taskId The unique task identifier
     * @param runnable The task to run
     * @param delayTicks The initial delay in ticks
     * @param periodTicks The period in ticks
     * @return The BukkitTask
     */
    public BukkitTask runAsyncTimer(String taskId, Runnable runnable, long delayTicks, long periodTicks) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
        registerTask(taskId, task);
        return task;
    }

    /**
     * Runs a task synchronously at a fixed interval.
     *
     * @param taskId The unique task identifier
     * @param runnable The task to run
     * @param delayTicks The initial delay in ticks
     * @param periodTicks The period in ticks
     * @return The BukkitTask
     */
    public BukkitTask runTimer(String taskId, Runnable runnable, long delayTicks, long periodTicks) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        registerTask(taskId, task);
        return task;
    }

    /**
     * Runs a task asynchronously at a fixed interval, associated with an arena.
     *
     * @param taskId The unique task identifier
     * @param runnable The task to run
     * @param delayTicks The initial delay in ticks
     * @param periodTicks The period in ticks
     * @param arenaName The arena name
     * @return The BukkitTask
     */
    public BukkitTask runAsyncTimer(String taskId, Runnable runnable, long delayTicks, long periodTicks, String arenaName) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
        registerTask(taskId, task, arenaName);
        return task;
    }

    /**
     * Runs a task synchronously at a fixed interval, associated with an arena.
     *
     * @param taskId The unique task identifier
     * @param runnable The task to run
     * @param delayTicks The initial delay in ticks
     * @param periodTicks The period in ticks
     * @param arenaName The arena name
     * @return The BukkitTask
     */
    public BukkitTask runTimer(String taskId, Runnable runnable, long delayTicks, long periodTicks, String arenaName) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        registerTask(taskId, task, arenaName);
        return task;
    }

    /**
     * Runs a BukkitRunnable at a fixed interval.
     *
     * @param taskId The unique task identifier
     * @param runnable The BukkitRunnable to run
     * @param delayTicks The initial delay in ticks
     * @param periodTicks The period in ticks
     * @return The BukkitTask
     */
    public BukkitTask runTimer(String taskId, BukkitRunnable runnable, long delayTicks, long periodTicks) {
        BukkitTask task = runnable.runTaskTimer(plugin, delayTicks, periodTicks);
        registerTask(taskId, task);
        return task;
    }

    /**
     * Runs a BukkitRunnable at a fixed interval, associated with an arena.
     *
     * @param taskId The unique task identifier
     * @param runnable The BukkitRunnable to run
     * @param delayTicks The initial delay in ticks
     * @param periodTicks The period in ticks
     * @param arenaName The arena name
     * @return The BukkitTask
     */
    public BukkitTask runTimer(String taskId, BukkitRunnable runnable, long delayTicks, long periodTicks, String arenaName) {
        BukkitTask task = runnable.runTaskTimer(plugin, delayTicks, periodTicks);
        registerTask(taskId, task, arenaName);
        return task;
    }

    /**
     * Cleans up the TaskManager (called on plugin disable).
     */
    public void cleanup() {
        cancelAll();
        instance = null;
    }
}

package az.nuran.obsidianwars;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Arena State Machine for ObsidianWars.
 * Implements a strict lifecycle with guard methods to prevent illegal state transitions.
 */
public class ArenaStateManager {

    private static ArenaStateManager instance;
    private final Logger logger;

    // Map of arena names to their current state
    private final Map<String, ArenaState> arenaStates;

    public ArenaStateManager(Logger logger) {
        this.logger = logger;
        this.arenaStates = new HashMap<>();
    }

    /**
     * Initializes the ArenaStateManager singleton.
     *
     * @param logger The plugin logger
     */
    public static void initialize(Logger logger) {
        if (instance == null) {
            instance = new ArenaStateManager(logger);
        }
    }

    /**
     * Gets the ArenaStateManager instance.
     *
     * @return The ArenaStateManager instance
     */
    public static ArenaStateManager getInstance() {
        return instance;
    }

    /**
     * Arena State Enum representing the complete lifecycle of an arena.
     */
    public enum ArenaState {
        CREATED,      // Arena created but not configured
        READY,        // Arena configured and ready for players
        WAITING,      // Players waiting in lobby
        STARTING,     // Countdown in progress
        PREPARATION,  // Preparation phase (walls active)
        PLAYING,      // Active gameplay
        SUDDEN_DEATH, // Sudden death mode
        ENDED         // Game ended, cleanup in progress
    }

    /**
     * Gets the current state of an arena.
     *
     * @param arenaName The arena name
     * @return The current state, or null if arena not found
     */
    public ArenaState getState(String arenaName) {
        return arenaStates.get(arenaName);
    }

    /**
     * Sets the state of an arena with transition validation.
     *
     * @param arenaName The arena name
     * @param newState The new state
     * @return true if transition was successful, false if illegal
     */
    public boolean setState(String arenaName, ArenaState newState) {
        ArenaState currentState = arenaStates.get(arenaName);

        if (currentState == null) {
            // Arena not tracked yet, only allow CREATED state
            if (newState == ArenaState.CREATED) {
                arenaStates.put(arenaName, newState);
                logger.fine("Arena " + arenaName + " initialized in CREATED state");
                return true;
            }
            logger.warning("Cannot set state for untracked arena " + arenaName + " to " + newState);
            return false;
        }

        // Validate transition
        if (!isValidTransition(currentState, newState)) {
            logger.warning("Illegal state transition for arena " + arenaName + ": " + currentState + " -> " + newState);
            return false;
        }

        arenaStates.put(arenaName, newState);
        logger.fine("Arena " + arenaName + " state transition: " + currentState + " -> " + newState);
        return true;
    }

    /**
     * Forces a state transition without validation (use only for recovery).
     *
     * @param arenaName The arena name
     * @param newState The new state
     */
    public void forceSetState(String arenaName, ArenaState newState) {
        ArenaState oldState = arenaStates.get(arenaName);
        arenaStates.put(arenaName, newState);
        logger.warning("Arena " + arenaName + " state forced: " + oldState + " -> " + newState + " (validation bypassed)");
    }

    /**
     * Validates if a state transition is legal.
     *
     * @param from The current state
     * @param to The target state
     * @return true if transition is legal, false otherwise
     */
    private boolean isValidTransition(ArenaState from, ArenaState to) {
        // Same state is always valid (idempotent)
        if (from == to) {
            return true;
        }

        switch (from) {
            case CREATED:
                return to == ArenaState.READY;

            case READY:
                return to == ArenaState.WAITING || to == ArenaState.ENDED;

            case WAITING:
                return to == ArenaState.STARTING || to == ArenaState.READY || to == ArenaState.ENDED;

            case STARTING:
                return to == ArenaState.PREPARATION || to == ArenaState.PLAYING || to == ArenaState.READY || to == ArenaState.ENDED;

            case PREPARATION:
                return to == ArenaState.PLAYING || to == ArenaState.ENDED;

            case PLAYING:
                return to == ArenaState.SUDDEN_DEATH || to == ArenaState.ENDED;

            case SUDDEN_DEATH:
                return to == ArenaState.ENDED;

            case ENDED:
                return to == ArenaState.READY;

            default:
                return false;
        }
    }

    /**
     * Checks if an arena is in a specific state.
     *
     * @param arenaName The arena name
     * @param state The state to check
     * @return true if arena is in the specified state
     */
    public boolean isState(String arenaName, ArenaState state) {
        return arenaStates.get(arenaName) == state;
    }

    /**
     * Checks if an arena is in any of the specified states.
     *
     * @param arenaName The arena name
     * @param states The states to check
     * @return true if arena is in any of the specified states
     */
    public boolean isStateIn(String arenaName, Set<ArenaState> states) {
        ArenaState currentState = arenaStates.get(arenaName);
        return currentState != null && states.contains(currentState);
    }

    /**
     * Checks if an arena is in an active game state.
     *
     * @param arenaName The arena name
     * @return true if arena is in an active game state
     */
    public boolean isActiveGame(String arenaName) {
        return isStateIn(arenaName, EnumSet.of(
            ArenaState.STARTING,
            ArenaState.PREPARATION,
            ArenaState.PLAYING,
            ArenaState.SUDDEN_DEATH
        ));
    }

    /**
     * Checks if an arena is in a lobby state (can accept players).
     *
     * @param arenaName The arena name
     * @return true if arena is in a lobby state
     */
    public boolean isInLobby(String arenaName) {
        return isStateIn(arenaName, EnumSet.of(
            ArenaState.READY,
            ArenaState.WAITING
        ));
    }

    /**
     * Checks if an arena can accept new players.
     *
     * @param arenaName The arena name
     * @return true if arena can accept players
     */
    public boolean canAcceptPlayers(String arenaName) {
        return isInLobby(arenaName) || isState(arenaName, ArenaState.STARTING);
    }

    /**
     * Gets all arena names in a specific state.
     *
     * @param state The state to filter by
     * @return Set of arena names in the specified state
     */
    public java.util.Set<String> getArenasInState(ArenaState state) {
        java.util.Set<String> arenas = new java.util.HashSet<>();
        for (Map.Entry<String, ArenaState> entry : arenaStates.entrySet()) {
            if (entry.getValue() == state) {
                arenas.add(entry.getKey());
            }
        }
        return arenas;
    }

    /**
     * Gets the best available arena for auto-assignment.
     * Prioritizes WAITING arenas with the highest player count.
     *
     * @return The best arena name, or null if none available
     */
    public String getBestAvailableArena() {
        // First, look for WAITING arenas
        java.util.Set<String> waitingArenas = getArenasInState(ArenaState.WAITING);
        if (!waitingArenas.isEmpty()) {
            // Return the arena with the most players
            String bestArena = null;
            int maxPlayers = -1;

            for (String arena : waitingArenas) {
                int playerCount = TeamManager.getTotalPlayerCount(arena);
                if (playerCount > maxPlayers) {
                    maxPlayers = playerCount;
                    bestArena = arena;
                }
            }
            return bestArena;
        }

        // Fallback to READY arenas
        java.util.Set<String> readyArenas = getArenasInState(ArenaState.READY);
        if (!readyArenas.isEmpty()) {
            return readyArenas.iterator().next();
        }

        return null;
    }

    /**
     * Registers a new arena in CREATED state.
     *
     * @param arenaName The arena name
     * @return true if registration was successful
     */
    public boolean registerArena(String arenaName) {
        if (arenaStates.containsKey(arenaName)) {
            logger.warning("Arena " + arenaName + " is already registered");
            return false;
        }
        arenaStates.put(arenaName, ArenaState.CREATED);
        logger.info("Arena " + arenaName + " registered in CREATED state");
        return true;
    }

    /**
     * Unregisters an arena (removes from state tracking).
     *
     * @param arenaName The arena name
     */
    public void unregisterArena(String arenaName) {
        arenaStates.remove(arenaName);
        logger.info("Arena " + arenaName + " unregistered from state tracking");
    }

    /**
     * Gets the count of arenas in a specific state.
     *
     * @param state The state to count
     * @return The number of arenas in the specified state
     */
    public int getCountInState(ArenaState state) {
        int count = 0;
        for (ArenaState s : arenaStates.values()) {
            if (s == state) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the total number of tracked arenas.
     *
     * @return The total arena count
     */
    public int getTotalArenaCount() {
        return arenaStates.size();
    }

    /**
     * Converts string state name to ArenaState enum.
     *
     * @param stateName The state name string
     * @return The ArenaState, or null if invalid
     */
    public static ArenaState fromString(String stateName) {
        try {
            return ArenaState.valueOf(stateName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Converts ArenaState enum to string.
     *
     * @param state The ArenaState
     * @return The string representation
     */
    public static String toString(ArenaState state) {
        return state.name();
    }

    /**
     * Cleans up the ArenaStateManager (called on plugin disable).
     */
    public void cleanup() {
        arenaStates.clear();
        instance = null;
    }
}

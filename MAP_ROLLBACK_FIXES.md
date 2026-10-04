# Map Rollback Fixes

## Problem
After a match ends, the arena map was not resetting back to its original state.

## Root Causes Found

### 1. Logic Bug in `restoreTrackedChanges`
The method was returning `true` even when there were no tracked changes to restore. This prevented the fallback to full snapshot restoration from happening, meaning blocks were never restored.

**Fix:** Modified the method to return `false` when tracked changes map is empty, triggering full snapshot restoration.

### 2. Missing Main Arena Region
The `captureArenaRegion` method was silently skipping arena capture if the main region wasn't defined. This meant player-placed blocks were never captured and thus never restored.

**Fix:**
- Added `/o arena setregion <arena>` command to set the main arena region
- Made region mandatory in arena completion checks (`/o arena finish` and `/o enableArena`)
- Enhanced warning system to notify admins when region is missing

### 3. Insufficient Logging
There was no detailed logging to diagnose what was happening during snapshot capture and restoration.

**Fix:** Added comprehensive logging at each step of the process:
- Snapshot capture logs each tier (obsidian, walls, resources, arena blocks)
- Restoration logs each tier and block counts
- Game start/cleanup logs to track the entire lifecycle

## Changes Made

### ArenaSnapshotManager.java
1. Fixed `restoreTrackedChanges()` to return false when no tracked changes exist
2. Enhanced `takeSnapshot()` with detailed logging for each capture tier
3. Enhanced `restoreSnapshot()` with detailed logging for each restoration tier
4. Enhanced `captureArenaRegion()` to notify admins when region is missing

### GameManager.java
1. Added snapshot success check at game start with warning on failure
2. Enhanced `cleanupArena()` with detailed logging for restoration attempts

### ObsidianCommand.java
1. Added `/o arena setregion <arena>` command
2. Added region check to mandatory arena completion requirements
3. Updated arena setup guide to include region setup step
4. Updated `suggestNextSetupStep()` to include region in workflow

## How to Use

### For Existing Arenas (like "testik")
```
/o wand
[Select corner 1 with left-click]
[Select corner 2 with right-click]
/o arena setregion testik
```

### For New Arenas
The region is automatically set during arena creation using the wand positions.

### Verification
- Try to enable: `/o enableArena testik` - will fail if region not set
- Try to finish: `/o arena finish testik` - will fail if region not set

## What the Rollback System Does

The rollback system has two tiers:

### Tier 1 (Always Reset)
- Obsidian blocks
- Wall regions
- Resource blocks

### Tier 2 (Optional but Recommended)
- Main arena region (captures all player-placed blocks)

## Restoration Process

1. **Game Start:** Takes snapshot of all arena elements
2. **During Game:** Tracks block changes (place/break)
3. **Game End (10 seconds later):**
   - First attempts to restore only tracked changes (efficient)
   - If no tracked changes or tracked restoration fails, falls back to full snapshot restoration
   - Full restoration restores all captured blocks to their original state

## Monitoring

Check the server logs for these messages:

**Snapshot Capture:**
```
Starting snapshot capture for arena <name>
  - Captured X obsidian blocks
  - Captured Y wall blocks
  - Captured Z resource blocks
  - Captured W arena blocks
Snapshot capture complete for arena <name> - Total: N blocks
```

**Warning if Region Missing:**
```
[SEVERE] No main arena region defined for <arena> - skipping arena capture. Player-placed blocks will NOT be restored after game ends!
```

**Restoration:**
```
Starting arena cleanup for <arena>
Attempting tracked changes restoration for <arena>
Tracked changes restored for <arena> - N blocks restored
```

Or if full restoration is needed:
```
Tracked changes restoration failed or empty, attempting full snapshot restoration for <arena>
Starting full snapshot restoration for arena <arena>
  - Restored X obsidian blocks
  - Restored Y wall blocks
  - Restored Z resource blocks
  - Restored W arena blocks
Snapshot restoration complete for arena <arena>
```

## Important Notes

1. **The main arena region is now mandatory** for arena completion. This ensures player-placed blocks are captured and restored.
2. **10-second delay:** There's a 10-second delay between game end and cleanup to allow victory effects to play.
3. **Async saves:** Config saves are asynchronous, which is why we have the `completedStep` parameter in `suggestNextSetupStep` to avoid race conditions.
4. **Logging is your friend:** The new detailed logging will help diagnose any future issues with map rollback.

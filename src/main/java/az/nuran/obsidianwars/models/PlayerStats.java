package az.nuran.obsidianwars.models;

import java.util.UUID;

/**
 * Data class for player statistics.
 */
public class PlayerStats {
    private final UUID uuid;
    private int kills;
    private int deaths;
    private int finalKills;
    private int finalDeaths;
    private int wins;
    private int losses;
    private int gamesPlayed;
    private int obsidianBroken;
    private int obsidianLost;
    private int winstreak;
    private int longestKillStreak;

    // Time-framed stats
    private int dailyWins;
    private int dailyObsidianBroken;
    private int dailyFinalKills;
    private int weeklyWins;
    private int weeklyObsidianBroken;
    private int weeklyFinalKills;
    private int monthlyWins;
    private int monthlyObsidianBroken;
    private int monthlyFinalKills;

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
        this.kills = 0;
        this.deaths = 0;
        this.finalKills = 0;
        this.finalDeaths = 0;
        this.wins = 0;
        this.losses = 0;
        this.gamesPlayed = 0;
        this.obsidianBroken = 0;
        this.obsidianLost = 0;
        this.winstreak = 0;
        this.longestKillStreak = 0;

        this.dailyWins = 0;
        this.dailyObsidianBroken = 0;
        this.dailyFinalKills = 0;
        this.weeklyWins = 0;
        this.weeklyObsidianBroken = 0;
        this.weeklyFinalKills = 0;
        this.monthlyWins = 0;
        this.monthlyObsidianBroken = 0;
        this.monthlyFinalKills = 0;
    }

    // Getters
    public UUID getUuid() { return uuid; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getFinalKills() { return finalKills; }
    public int getFinalDeaths() { return finalDeaths; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getGamesPlayed() { return gamesPlayed; }
    public int getObsidianBroken() { return obsidianBroken; }
    public int getObsidianLost() { return obsidianLost; }
    public int getWinstreak() { return winstreak; }
    public int getLongestKillStreak() { return longestKillStreak; }

    public int getDailyWins() { return dailyWins; }
    public int getDailyObsidianBroken() { return dailyObsidianBroken; }
    public int getDailyFinalKills() { return dailyFinalKills; }
    public int getWeeklyWins() { return weeklyWins; }
    public int getWeeklyObsidianBroken() { return weeklyObsidianBroken; }
    public int getWeeklyFinalKills() { return weeklyFinalKills; }
    public int getMonthlyWins() { return monthlyWins; }
    public int getMonthlyObsidianBroken() { return monthlyObsidianBroken; }
    public int getMonthlyFinalKills() { return monthlyFinalKills; }

    // Level and XP getters (delegates to LevelManager)
    public int getLevel() {
        return az.nuran.obsidianwars.managers.LevelManager.getPlayerLevel(uuid).getLevel();
    }

    public int getXp() {
        return az.nuran.obsidianwars.managers.LevelManager.getPlayerLevel(uuid).getCurrentXp();
    }

    // Calculated fields
    public double getKDRatio() {
        return deaths == 0 ? (double) kills : (double) kills / deaths;
    }

    public double getFinalKDRatio() {
        return finalDeaths == 0 ? (double) finalKills : (double) finalKills / finalDeaths;
    }

    public double getWinRate() {
        return gamesPlayed == 0 ? 0.0 : (double) wins / gamesPlayed * 100;
    }

    // Setters for database loading
    public void setGamesPlayed(int value) { this.gamesPlayed = value; }
    public void setWins(int value) { this.wins = value; }
    public void setLosses(int value) { this.losses = value; }
    public void setKills(int value) { this.kills = value; }
    public void setDeaths(int value) { this.deaths = value; }
    public void setFinalKills(int value) { this.finalKills = value; }
    public void setFinalDeaths(int value) { this.finalDeaths = value; }
    public void setObsidianBroken(int value) { this.obsidianBroken = value; }
    public void setObsidianLost(int value) { this.obsidianLost = value; }
    public void setWinstreak(int value) { this.winstreak = value; }

    public void setDailyWins(int value) { this.dailyWins = value; }
    public void setDailyObsidianBroken(int value) { this.dailyObsidianBroken = value; }
    public void setDailyFinalKills(int value) { this.dailyFinalKills = value; }
    public void setWeeklyWins(int value) { this.weeklyWins = value; }
    public void setWeeklyObsidianBroken(int value) { this.weeklyObsidianBroken = value; }
    public void setWeeklyFinalKills(int value) { this.weeklyFinalKills = value; }
    public void setMonthlyWins(int value) { this.monthlyWins = value; }
    public void setMonthlyObsidianBroken(int value) { this.monthlyObsidianBroken = value; }
    public void setMonthlyFinalKills(int value) { this.monthlyFinalKills = value; }
    public void setLongestKillStreak(int value) { this.longestKillStreak = value; }

    // Increment methods
    public void addKill() { kills++; }
    public void addDeath() { deaths++; }
    public void addFinalKill() { finalKills++; }
    public void addFinalDeath() { finalDeaths++; }
    public void addWin() {
        wins++;
        gamesPlayed++;
        winstreak++;
        dailyWins++;
        weeklyWins++;
        monthlyWins++;
    }
    public void addLoss() {
        losses++;
        gamesPlayed++;
        winstreak = 0;
    }
    public void addObsidianBroken() {
        obsidianBroken++;
        dailyObsidianBroken++;
        weeklyObsidianBroken++;
        monthlyObsidianBroken++;
    }
    public void addObsidianLost() { obsidianLost++; }

    // Reset method for admin command
    public void reset() {
        this.kills = 0;
        this.deaths = 0;
        this.finalKills = 0;
        this.finalDeaths = 0;
        this.wins = 0;
        this.losses = 0;
        this.gamesPlayed = 0;
        this.obsidianBroken = 0;
        this.obsidianLost = 0;
        this.winstreak = 0;
        this.longestKillStreak = 0;

        this.dailyWins = 0;
        this.dailyObsidianBroken = 0;
        this.dailyFinalKills = 0;
        this.weeklyWins = 0;
        this.weeklyObsidianBroken = 0;
        this.weeklyFinalKills = 0;
        this.monthlyWins = 0;
        this.monthlyObsidianBroken = 0;
        this.monthlyFinalKills = 0;
    }
}

package az.nuran.obsidianwars;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

public class TeamManager {

    private static final String RED_TEAM_NAME = "obsidian_red";
    private static final String BLUE_TEAM_NAME = "obsidian_blue";

    public static void setupTeam(Player player, String team) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        Team redTeam = scoreboard.getTeam(RED_TEAM_NAME);
        if (redTeam == null) {
            redTeam = scoreboard.registerNewTeam(RED_TEAM_NAME);
            redTeam.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
            redTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
        }

        Team blueTeam = scoreboard.getTeam(BLUE_TEAM_NAME);
        if (blueTeam == null) {
            blueTeam = scoreboard.registerNewTeam(BLUE_TEAM_NAME);
            blueTeam.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
            blueTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
        }

        // Oyunçunu əvvəlki komandadan çıxarırıq
        redTeam.removeEntry(player.getName());
        blueTeam.removeEntry(player.getName());

        // Yeni komandaya əlavə edirik
        if (team.equals("red")) {
            redTeam.addEntry(player.getName());
        } else if (team.equals("blue")) {
            blueTeam.addEntry(player.getName());
        }

        player.setScoreboard(scoreboard);
    }

    public static void removePlayerFromTeams(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        Team redTeam = scoreboard.getTeam(RED_TEAM_NAME);
        if (redTeam != null) {
            redTeam.removeEntry(player.getName());
        }

        Team blueTeam = scoreboard.getTeam(BLUE_TEAM_NAME);
        if (blueTeam != null) {
            blueTeam.removeEntry(player.getName());
        }
    }
}
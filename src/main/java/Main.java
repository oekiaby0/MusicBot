import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSearchProvider;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

public class Main {
    private static final String token = "INSERT BOT TOKEN HERE";
    static final int SEARCH_TIMEOUT = 120000;
    static JDA jda = null;
    static String id;
    static Map<String, TrackScheduler> schedulers = new HashMap<>();
    static Map<String, AudioPlayer> players = new HashMap<>();
    static Map<String, List<String>> historyMap = new HashMap<>();
    static AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    static String ERR_SAME_CHANNEL = "You must be in the same voice channel as me to control me :flushed:";
    static String NOT_IN_VC = "I am not in a voice channel";
    static String USER_NOT_IN_VC = "You are not in a voice channel";

    static AudioPlayer getPlayer(Guild guild) {
        AudioPlayer player = players.get(guild.getId());
        if (player == null) {
            player = playerManager.createPlayer();
            player.addListener(getScheduler(guild));
            players.put(guild.getId(), player);
        }
        return player;
    }

    static TrackScheduler getScheduler(Guild guild) {
        TrackScheduler scheduler = schedulers.get(guild.getId());
        if (scheduler == null) {
            scheduler = new TrackScheduler(guild.getId());
            schedulers.put(guild.getId(), scheduler);
        }
        return scheduler;
    }

    static List<String> getHistoryList(Guild guild) {
        List<String> historyList = historyMap.get(guild.getId());
        if (historyList == null) {
            historyList = new ArrayList<>();
            historyMap.put(guild.getId(), historyList);
        }
        return historyList;
    }

    public static void main(String[] args) {
        try {
            jda = JDABuilder.createDefault((args.length > 0) ? args[0] : token).build();
        } catch (LoginException e) {
            e.printStackTrace();
            return;
        }
        AudioSourceManagers.registerRemoteSources(playerManager);
        jda.addEventListener(new ListenerAdapter() {
            public void onReady(ReadyEvent e) {
                Main.id = Main.jda.getSelfUser().getId();
            }

            public void onGuildLeave(GuildLeaveEvent event) {
                Main.schedulers.remove(event.getGuild().getId());
                Main.historyMap.remove(event.getGuild().getId());
                Main.players.remove(event.getGuild().getId());
            }

            public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
                if (event.getMember().getId().equals(Main.id)) {
                    Main.schedulers.remove(event.getGuild().getId());
                    Main.players.remove(event.getGuild().getId());
                } else {
                    AudioManager audioManager = event.getGuild().getAudioManager();
                    if (audioManager
                            .isConnected() && event
                            .getChannelLeft().getId().equals(audioManager.getConnectedChannel().getId()) && event
                            .getChannelLeft().getMembers().stream().noneMatch(member -> !member.getUser().isBot()))
                        (new Thread(() -> {
                            try {
                                Thread.sleep(60000);
                                if (event.getChannelLeft().getMembers().stream().noneMatch(member -> !member.getUser().isBot()) && audioManager.isConnected())
                                    audioManager.closeAudioConnection();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                if (event.getChannelLeft().getMembers().stream().noneMatch(member -> !member.getUser().isBot()) && audioManager.isConnected())
                                    audioManager.closeAudioConnection();
                            }
                        })).start();
                }
            }

            public void onMessageReceived(MessageReceivedEvent e) {
                if (e.getAuthor().isBot())
                    return;
                String content = e.getMessage().getContentRaw().toLowerCase(Locale.ENGLISH);
                if (content.startsWith("<")) {
                    content = content.substring(1);
                    if (content.equals("help"))
                        Main.help(e);
                }
            }

            public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
                if (e.getAuthor().isBot())
                    return;
                String content = e.getMessage().getContentRaw().toLowerCase(Locale.ENGLISH).trim();
                if (content.startsWith("<")) {
                    content = content.substring(1);
                    if (content.startsWith("play ") || content.startsWith("p ")) {
                        Main.play(e);
                    } else if (content.equals("play") || content.equals("p") || content.equals("pause")) {
                        Main.pause(e);
                    } else if (content.equals("disconnect") || content.equals("dc")) {
                        Main.disconnect(e);
                    } else if (content.equals("loop") || content.equals("repeat")) {
                        Main.repeat(e);
                    } else if (content.equals("q") || content.equals("queue")) {
                        Main.queue(e);
                    } else if (content.equals("s") || content.equals("skip") || content.startsWith("s ") || content.startsWith("skip ")) {
                        Main.skip(e);
                    } else if (content.equals("i") || content.equals("info") || content.equals("information")) {
                        Main.information(e);
                    } else if (content.startsWith("vol") || content.startsWith("volume") || content.startsWith("v")) {
                        Main.volume(e);
                    } else if (content.equals("history") || content.equals("h")) {
                        Main.history(e);
                    } else if (content.startsWith("search ")) {
                        Main.search(e);
                    } else if (content.startsWith("seek ")) {
                        Main.seek(e);
                    }
                }
            }
        });
    }

    static MessageEmbed error(String content) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Error");
        builder.setColor(Color.RED.brighter());
        builder.setDescription(content);
        return builder.build();
    }

    static MessageEmbed message(String content) {
        return message("Notice", content);
    }

    static MessageEmbed message(String title, String content) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(title);
        builder.setColor(Color.CYAN.darker());
        builder.setDescription(content);
        return builder.build();
    }

    static String getArgs(GuildMessageReceivedEvent e) {
        return getArgs(e.getMessage().getContentRaw());
    }

    static String getArgs(String command) {
        String[] s = command.split(" ", 2);
        return (s.length > 1) ? s[1] : null;
    }

    static void help(MessageReceivedEvent e) {
        e.getChannel().sendMessageEmbeds(message("Help Manual", "The following commands are to be invoked using the '<' character as a prefix.\n• play[alias p] — used to add video/audio links to the queue. If no argument is provided, it will function like the pause command\n• pause — toggles the audio player's pause state\n• search[alias s] — search youtube\n• seek — skip to specific time\n• skip[alias s] — skip the first item in the queue.\n• volume[alias vol, v] — set the audio player's volume. If no argument is provided, it will instead show the current volume level\n• disconnect[alias dc] — disconnect the bot from voice channel\n• repeat[alias loop] — toggle repeat mode\n• queue[alias q] — list queue\n• information[alias info] — show information about currently playing track\n• history[alias h] — show queue history"))
                .queue();
    }

    static void history(GuildMessageReceivedEvent e) {
        StringBuilder content;
        List<String> history = getHistoryList(e.getGuild());
        if (history.size() > 0) {
            content = new StringBuilder("• " + history.get(0));
            if (history.size() > 1)
                for (int i = history.size() - 1; i > 1; i--)
                    content.append("\n• ").append(history.get(i));
        } else {
            content = new StringBuilder("Empty");
        }
        e.getChannel().sendMessageEmbeds(message("History", content.toString())).queue();
    }

    static void volume(GuildMessageReceivedEvent e) {
        String args = getArgs(e);
        if (args == null) {
            e.getChannel().sendMessageEmbeds(message("Volume", "Level: " + getPlayer(e.getGuild()).getVolume())).queue();
        } else if (memberSameVC(e)) {
            try {
                int vol = Integer.parseInt(args);
                getPlayer(e.getGuild()).setVolume(vol);
            } catch (Exception ex) {
                e.getChannel().sendMessageEmbeds(error("Invalid volume argument")).queue();
            }
        }
    }

    static void pause(GuildMessageReceivedEvent e) {
        if (!memberInVC(e) || !botInVC(e) || !memberSameVC(e))
            return;
        AudioPlayer player = getPlayer(e.getGuild());
        player.setPaused(!player.isPaused());
        e.getChannel().sendMessageEmbeds(message("Player is " + (player.isPaused() ? "now paused." : "no longer paused."))).queue();
    }

    static void seek(GuildMessageReceivedEvent e) {
        if (!botInVC(e))
            return;
        if (!memberInVC(e))
            return;
        AudioPlayer player = getPlayer(e.getGuild());
        AudioTrack track = player.getPlayingTrack();
        String args = getArgs(e);
        if (args != null)
            try {
                if (args.startsWith("+")) {
                    track.setPosition(track.getPosition() + Long.parseLong(getArgs(e).substring(1)) * 1000);
                } else if (args.startsWith("-")) {
                    track.setPosition(track.getPosition() - Long.parseLong(getArgs(e).substring(1)) * 1000);
                } else {
                    player.getPlayingTrack().setPosition(Long.parseLong(getArgs(e)) * 1000);
                }
            } catch (NumberFormatException ignore) {
                e.getChannel().sendMessageEmbeds(error("Invalid time position (should be in seconds optionally prefixed by + or -)")).queue();
            }
    }

    static void loadItem(Guild guild, String url, boolean silent) {
        loadItem(null, guild, url, silent);
    }

    static void loadItem(final GuildMessageReceivedEvent e, final Guild guild, final String url, final boolean silent) {
        final TrackScheduler trackScheduler = getScheduler(guild);
        playerManager.loadItem(url, new AudioLoadResultHandler() {
            public void trackLoaded(AudioTrack track) {
                trackScheduler.queue(track);
                if (trackScheduler.queue.size() == 1) {
                    Main.getPlayer(guild).playTrack(track);
                } else if (!silent && e != null) {
                    e.getChannel().sendMessageEmbeds(Main.message("Added to queue", "Position " + trackScheduler.queue.size())).queue();
                }
                List<String> history = Main.getHistoryList(guild);
                history.add(url);
                while (history.size() > 15)
                    history.remove(0);
            }

            public void playlistLoaded(AudioPlaylist playlist) {
                for (AudioTrack track : playlist.getTracks())
                    trackScheduler.queue(track);
            }

            public void noMatches() {
                if (e != null)
                    e.getChannel().sendMessageEmbeds(error("No matches found")).queue();
            }

            public void loadFailed(FriendlyException throwable) {
                if (e != null)
                    e.getChannel().sendMessageEmbeds(error(throwable.getMessage())).queue();
                throwable.printStackTrace();
            }
        });
    }

    static String[] emojis = new String[]{"1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟"};

    static void search(final GuildMessageReceivedEvent e) {
        if (!memberInVC(e))
            return;
        AudioManager audioManager = e.getGuild().getAudioManager();
        if (!audioManager.isConnected()) {
            final TrackScheduler trackScheduler = getScheduler(e.getGuild());
            audioManager.setSendingHandler(new AudioPlayerSendHandler(getPlayer(e.getGuild())));
            trackScheduler.resetQueue();
            getPlayer(e.getGuild()).setVolume(100);
            audioManager.openAudioConnection(e.getMember().getVoiceState().getChannel());
            trackScheduler.repeat = false;
        }
        String args = getArgs(e);
        if (args == null)
            return;
        YoutubeAudioSourceManager sourceManager = new YoutubeAudioSourceManager(true);
        YoutubeSearchProvider searchProvider = new YoutubeSearchProvider();
        BasicAudioPlaylist playlist = (BasicAudioPlaylist) searchProvider.loadSearchResult(args, audioTrackInfo -> sourceManager.decodeTrack(audioTrackInfo, null));
        final List<AudioTrack> searchResults = playlist.getTracks();
        if (searchResults.size() == 0) {
            e.getChannel().sendMessageEmbeds(error("No search results found")).queue();
        } else {
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < Math.min(searchResults.size(), 10); i++) {
                if (i != 0)
                    content.append("\n");
                AudioTrack track = searchResults.get(i);
                content.append(emojis[i]).append(" • ").append(getTitle(track)).append(" — ").append(time(track));
            }
            e.getChannel().sendMessageEmbeds(message("Results", content.toString())).queue(message -> {
                for (String emoji : emojis)
                    message.addReaction(emoji).queue();
                final TrackScheduler trackScheduler = getScheduler(e.getGuild());
                final List<String> history = getHistoryList(e.getGuild());
                final boolean[] added = new boolean[10];
                ListenerAdapter adapter = new ListenerAdapter() {
                    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
                        if (!event.getMember().getId().equals(Main.id)) {
                            String emoji = event.getReactionEmote().getAsReactionCode();
                            int index = -1;
                            for (int i = 0; i < 10; i++) {
                                if (emoji.equals(Main.emojis[i])) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1 && !added[index]) {
                                AudioTrack track = searchResults.get(index);
                                trackScheduler.queue(track);
                                if (trackScheduler.queue.size() == 1) {
                                    Main.getPlayer(e.getGuild()).playTrack(track);
                                } else {
                                    e.getChannel().sendMessageEmbeds(Main.message("Added to queue", (track.getInfo()).title + "\nPosition " + trackScheduler.queue.size())).queue();
                                }
                                history.add((track.getInfo()).uri);
                                while (history.size() > 15)
                                    history.remove(0);
                                added[index] = true;
                            }
                        }
                    }
                };
                jda.addEventListener(adapter);
                try {
                    Thread.sleep(SEARCH_TIMEOUT);
                    jda.removeEventListener(adapter);
                } catch (InterruptedException error) {
                    error.printStackTrace();
                }
            });
        }
    }

    static void play(GuildMessageReceivedEvent e) {
        if (!memberInVC(e))
            return;
        VoiceChannel authorChannel = e.getMember().getVoiceState().getChannel();
        AudioManager audioManager = e.getGuild().getAudioManager();
        TrackScheduler trackScheduler = getScheduler(e.getGuild());
        audioManager.setSendingHandler(new AudioPlayerSendHandler(getPlayer(e.getGuild())));
        if (audioManager.getConnectedChannel() != authorChannel) {
            trackScheduler.resetQueue();
            getPlayer(e.getGuild()).setVolume(100);
            audioManager.openAudioConnection(authorChannel);
            trackScheduler.repeat = false;
        }
        String args = getArgs(e);
        loadItem(e, e.getGuild(), args, false);
    }

    static void skip(GuildMessageReceivedEvent e) {
        String args = getArgs(e);
        if (args != null && botInVC(e))
            try {
                int amount = Integer.parseInt(args);
                List<AudioTrack> queue = (getScheduler(e.getGuild())).queue;
                for (int i = 0; i < amount && queue.size() > 0; i++) {
                    queue.remove(0);
				}
            } catch (NumberFormatException ignore) {
                search(e);
            } catch (Exception ignore) {
                e.getChannel().sendMessageEmbeds(error("An error occurred whilst attempting to skip a track")).queue();
            }
        getPlayer(e.getGuild()).stopTrack();
    }

    static void disconnect(GuildMessageReceivedEvent e) {
        if (botInVC(e) &&
                memberSameVC(e))
            e.getGuild().getAudioManager().closeAudioConnection();
    }

    static void repeat(GuildMessageReceivedEvent e) {
        TrackScheduler scheduler = getScheduler(e.getGuild());
        scheduler.repeat = !scheduler.repeat;
        e.getChannel().sendMessageEmbeds(message("Repeat is now " + (scheduler.repeat ? "enabled" : "disabled"))).queue();
    }

    static void information(GuildMessageReceivedEvent e) {
        AudioPlayer player = getPlayer(e.getGuild());
        AudioTrack track = player.getPlayingTrack();
        e.getChannel().sendMessageEmbeds(
                message(
                        "Information",
                        getTitle(track) + "\nLength: " + time(track) + (
                        (track.getInfo()).title.equals("Unknown title") ? "" :
                                ("\nAuthor: " + (track.getInfo()).author + "\nURL: " + (track.getInfo()).uri)))
                )
                .queue();
    }

    static void queue(GuildMessageReceivedEvent e) {
        TrackScheduler scheduler = getScheduler(e.getGuild());
        List<AudioTrack> queue = scheduler.queue;
        if (!e.getGuild().getAudioManager().isConnected())
            scheduler.resetQueue();
        if (queue.size() == 0) {
            e.getChannel().sendMessageEmbeds(message("Queue", "Empty")).queue();
        } else {
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < Math.min(queue.size(), 10); i++) {
                if (i != 0)
                    content.append("\n");
                AudioTrack track = queue.get(i);
                content.append("• ").append(getTitle(track)).append(" — ").append(time(track));
            }
            e.getChannel().sendMessageEmbeds(message("Queue — " + queue.size() + ((queue.size() == 1) ? " track" : " tracks"), content.toString())).queue();
        }
    }

    static String getTitle(AudioTrack track) {
        return (track.getInfo()).title.equals("Unknown title") ? (track.getInfo()).uri : (track.getInfo()).title;
    }

    static String time(AudioTrack track) {
        return time((track.getInfo()).length);
    }

    static String time(long totalMilli) {
        long totalSeconds = totalMilli / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds - 60 * minutes;
        return minutes + ((seconds < 10) ? ":0" : ":") + seconds;
    }

    static boolean memberInVC(GuildMessageReceivedEvent e) {
        if (!e.getMember().getVoiceState().inVoiceChannel()) {
            e.getChannel().sendMessageEmbeds(error(USER_NOT_IN_VC)).queue();
            return false;
        }
        return true;
    }

    static boolean memberSameVC(GuildMessageReceivedEvent e) {
        if (e.getGuild().getAudioManager().getConnectedChannel() != e.getMember().getVoiceState().getChannel()) {
            e.getChannel().sendMessageEmbeds(error(ERR_SAME_CHANNEL)).queue();
            return false;
        }
        return true;
    }

    static boolean botInVC(GuildMessageReceivedEvent e) {
        if (e.getGuild().getAudioManager().isConnected())
            return true;
        e.getChannel().sendMessageEmbeds(error(NOT_IN_VC)).queue();
        return false;
    }
}



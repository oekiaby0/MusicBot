import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.ArrayList;
import java.util.List;

public class TrackScheduler extends AudioEventAdapter {
    boolean repeat = false;
    public List<AudioTrack> queue = new ArrayList<>();
    public final String guildId;

    TrackScheduler(String guildId) {
        this.guildId = guildId;
    }

    void resetQueue() {
        if (this.queue.size() != 0)
            this.queue = new ArrayList<>();
    }

    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            if (this.repeat) {
                this.queue.set(0, ((AudioTrack) this.queue.get(0)).makeClone());
            } else if (this.queue.size() > 0) {
                this.queue.remove(0);
            }
            if (this.queue.size() > 0)
                player.playTrack(this.queue.get(0));
        } else if (endReason == AudioTrackEndReason.STOPPED) {
            this.queue.remove(0);
            if (this.queue.size() > 0)
                player.playTrack(this.queue.get(0));
        }
    }

    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if (this.repeat)
            this.queue.remove(0);
        exception.printStackTrace();
    }

    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        this.queue.remove(0);
        if (this.queue.size() > 0)
            player.playTrack(this.queue.get(0));
    }

    public void queue(AudioTrack track) {
        this.queue.add(track);
    }
}



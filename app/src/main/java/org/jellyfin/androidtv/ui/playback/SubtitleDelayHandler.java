package org.jellyfin.androidtv.ui.playback;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.SubtitleView;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import timber.log.Timber;

/**
 * Handles subtitle delay by intercepting cues via Player.Listener and delaying before displaying.
 */
@UnstableApi
public class SubtitleDelayHandler implements Player.Listener {
    private final SubtitleView subtitleView;
    private final Handler handler;
    private final Queue<DelayedCue> delayedCues = new LinkedList<>();
    private long offsetMs = 0;
    private Runnable checkRunnable;
    
    private static class DelayedCue {
        final List<Cue> cues;
        final long showTimeMs;
        
        DelayedCue(List<Cue> cues, long showTimeMs) {
            this.cues = cues;
            this.showTimeMs = showTimeMs;
        }
    }
    
    public SubtitleDelayHandler(@NonNull SubtitleView subtitleView) {
        this.subtitleView = subtitleView;
        this.handler = new Handler(Looper.getMainLooper());
    }

    /**
     * Set the subtitle time offset.
     * Positive values delay subtitles (show later), negative values advance them (show earlier).
     *
     * @param offsetMs Time offset in milliseconds
     */
    public void setOffsetMs(long offsetMs) {
        Timber.d("SubtitleDelayHandler: Setting offset to %d ms", offsetMs);
        this.offsetMs = offsetMs;
        
        delayedCues.clear();
        if (checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
            checkRunnable = null;
        }
        
        subtitleView.setCues(Collections.emptyList());
    }

    /**
     * Get the current subtitle time offset in milliseconds.
     */
    public long getOffsetMs() {
        return offsetMs;
    }

    @Override
    public void onCues(@NonNull CueGroup cueGroup) {
        Timber.d("SubtitleDelayHandler.onCues() called with %d cues, offsetMs=%d", cueGroup.cues.size(), offsetMs);
        
        if (offsetMs == 0) {
            Timber.d("SubtitleDelayHandler: No offset, showing %d cues immediately", cueGroup.cues.size());
            subtitleView.setCues(cueGroup.cues);
        } else if (offsetMs > 0) {
            long showTime = System.currentTimeMillis() + offsetMs;
            delayedCues.offer(new DelayedCue(cueGroup.cues, showTime));
            
            Timber.d("SubtitleDelayHandler: Delaying %d cues by %d ms", cueGroup.cues.size(), offsetMs);
            subtitleView.setCues(Collections.emptyList());
            
            scheduleCheck();
        } else {
            Timber.d("SubtitleDelayHandler: Negative offset, showing %d cues immediately", cueGroup.cues.size());
            subtitleView.setCues(cueGroup.cues);
        }
    }

    private void scheduleCheck() {
        if (checkRunnable != null) return;
        
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkRunnable = null;
                long now = System.currentTimeMillis();
                
                while (!delayedCues.isEmpty()) {
                    DelayedCue delayed = delayedCues.peek();
                    if (delayed.showTimeMs <= now) {
                        delayedCues.poll();
                        subtitleView.setCues(delayed.cues);
                    } else {
                        long delay = delayed.showTimeMs - now;
                        handler.postDelayed(this, delay);
                        checkRunnable = this;
                        break;
                    }
                }
            }
        };
        handler.post(checkRunnable);
    }

    /**
     * Clean up resources
     */
    public void release() {
        if (checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
            checkRunnable = null;
        }
        delayedCues.clear();
        subtitleView.setCues(Collections.emptyList());
    }
}

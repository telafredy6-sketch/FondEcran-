import { AudioClip } from '../types';

class AdvancedSoundEngine {
  private audioCtx: AudioContext | null = null;
  private currentSource: AudioBufferSourceNode | null = null;
  private currentElement: HTMLAudioElement | null = null;
  private synthInterval: number | null = null;
  private isPlaying = false;
  private playheadSec = 0;
  private onPositionUpdate?: (sec: number) => void;

  private getAudioContext(): AudioContext {
    if (!this.audioCtx) {
      const AudioCtxClass =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      this.audioCtx = new AudioCtxClass();
    }
    if (this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }
    return this.audioCtx;
  }

  public setPositionCallback(cb?: (sec: number) => void) {
    this.onPositionUpdate = cb;
  }

  public playClip(clip: AudioClip, loop = true) {
    this.stop();
    this.isPlaying = true;

    if (clip.isSynth || !clip.url) {
      this.playSynthPreset(clip.synthType || 'lofi', clip.volume ?? 0.8);
      return;
    }

    try {
      const audio = new Audio(clip.url);
      this.currentElement = audio;
      audio.volume = Math.max(0, Math.min(1, clip.volume ?? 0.8));
      audio.currentTime = clip.startTimeSec;

      const checkTime = () => {
        if (!this.isPlaying) return;
        this.playheadSec = audio.currentTime;
        this.onPositionUpdate?.(audio.currentTime);

        if (clip.endTimeSec > 0 && audio.currentTime >= clip.endTimeSec) {
          if (loop) {
            audio.currentTime = clip.startTimeSec;
            audio.play().catch(() => {});
          } else {
            this.stop();
          }
        }
      };

      audio.ontimeupdate = checkTime;
      audio.onended = () => {
        if (loop) {
          audio.currentTime = clip.startTimeSec;
          audio.play().catch(() => {});
        } else {
          this.stop();
        }
      };

      audio.play().catch((e) => console.warn('Audio play error:', e));
    } catch (err) {
      console.warn('Error starting clip:', err);
    }
  }

  public stop() {
    this.isPlaying = false;
    if (this.currentElement) {
      this.currentElement.pause();
      this.currentElement.src = '';
      this.currentElement = null;
    }
    if (this.currentSource) {
      try {
        this.currentSource.stop();
        this.currentSource.disconnect();
      } catch (e) {}
      this.currentSource = null;
    }
    if (this.synthInterval) {
      window.clearInterval(this.synthInterval);
      this.synthInterval = null;
    }
  }

  public getIsPlaying(): boolean {
    return this.isPlaying;
  }

  // Play procedural system sounds on demand (Ringtones, notifications, alarms)
  public playSystemSound(preset: 'call1' | 'chime' | 'alarmWake' | 'ping' | 'marimba') {
    this.stop();
    const ctx = this.getAudioContext();
    const now = ctx.currentTime;

    if (preset === 'ping') {
      // Clean modern glass notification ping
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(880, now); // A5
      osc.frequency.exponentialRampToValueAtTime(1760, now + 0.08);

      gain.gain.setValueAtTime(0.3, now);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.5);

      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(now);
      osc.stop(now + 0.5);
    } else if (preset === 'chime') {
      // Two-tone soothing notification
      const freqs = [659.25, 987.77]; // E5, B5
      freqs.forEach((freq, idx) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(freq, now + idx * 0.12);

        gain.gain.setValueAtTime(0.0001, now + idx * 0.12);
        gain.gain.exponentialRampToValueAtTime(0.25, now + idx * 0.12 + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + idx * 0.12 + 0.6);

        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(now + idx * 0.12);
        osc.stop(now + idx * 0.12 + 0.6);
      });
    } else if (preset === 'call1') {
      // Melodic ringtone sequence
      const melody = [523.25, 659.25, 783.99, 1046.5]; // C5, E5, G5, C6
      melody.forEach((note, i) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(note, now + i * 0.15);

        gain.gain.setValueAtTime(0.2, now + i * 0.15);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + i * 0.15 + 0.35);

        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(now + i * 0.15);
        osc.stop(now + i * 0.15 + 0.35);
      });
    } else if (preset === 'alarmWake') {
      // Gentle waking pulse
      const notes = [440, 554.37, 659.25];
      notes.forEach((freq, i) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, now + i * 0.2);

        gain.gain.setValueAtTime(0.18, now + i * 0.2);
        gain.gain.exponentialRampToValueAtTime(0.001, now + i * 0.2 + 0.4);

        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(now + i * 0.2);
        osc.stop(now + i * 0.2 + 0.4);
      });
    } else {
      // Marimba gentle ripple
      const marimbaNotes = [523.25, 587.33, 659.25, 783.99, 880, 1046.5];
      marimbaNotes.forEach((f, idx) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(f, now + idx * 0.08);

        gain.gain.setValueAtTime(0.25, now + idx * 0.08);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + idx * 0.08 + 0.3);

        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(now + idx * 0.08);
        osc.stop(now + idx * 0.08 + 0.3);
      });
    }
  }

  private playSynthPreset(synthType: string, volume: number) {
    const ctx = this.getAudioContext();
    const notes = [261.63, 293.66, 329.63, 392.0, 440.0, 523.25, 659.25];
    let step = 0;

    const playTone = () => {
      if (!this.isPlaying) return;
      try {
        const now = ctx.currentTime;
        const note = notes[(step * 2) % notes.length];
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = synthType === 'lofi' ? 'triangle' : 'sine';
        osc.frequency.setValueAtTime(note, now);

        gain.gain.setValueAtTime(0.0001, now);
        gain.gain.exponentialRampToValueAtTime(0.12 * volume, now + 0.1);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + 1.8);

        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(now);
        osc.stop(now + 2.0);
        step++;
      } catch (e) {}
    };

    playTone();
    this.synthInterval = window.setInterval(playTone, 1500);
  }
}

export const soundEngine = new AdvancedSoundEngine();

export const SYSTEM_SOUNDS_CATALOG = [
  {
    id: 'notif-1',
    title: 'Goutte d’eau pure',
    category: 'notification' as const,
    synthPreset: 'ping' as const,
    durationLabel: '0:01',
  },
  {
    id: 'notif-2',
    title: 'Carillon zen',
    category: 'notification' as const,
    synthPreset: 'chime' as const,
    durationLabel: '0:02',
  },
  {
    id: 'ring-1',
    title: 'Mélodie Bleue',
    category: 'ringtone' as const,
    synthPreset: 'call1' as const,
    durationLabel: '0:04',
  },
  {
    id: 'ring-2',
    title: 'Marimba Doux',
    category: 'ringtone' as const,
    synthPreset: 'marimba' as const,
    durationLabel: '0:03',
  },
  {
    id: 'alarm-1',
    title: 'Réveil Lumineux',
    category: 'alarm' as const,
    synthPreset: 'alarmWake' as const,
    durationLabel: '0:03',
  },
];

export const PRESET_AUDIO_TRACKS = [
  { name: 'Lofi Chill Chords', synthType: 'lofi' as const },
  { name: 'Ocean Ambient Calm', synthType: 'ambient' as const },
  { name: 'Nature Breeze', synthType: 'nature' as const },
  { name: 'Sunset Melody', synthType: 'chill' as const },
];

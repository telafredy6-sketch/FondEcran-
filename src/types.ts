export type WallpaperTarget = 0 | 1 | 2; // 0 = Écran d'accueil, 1 = Écran de verrouillage, 2 = Les deux

export interface WallpaperItem {
  id: string;
  name: string;
  dataUrl: string;
  music?: {
    name: string;
    url: string;
    type?: 'custom' | 'synth' | 'preset';
    synthType?: 'lofi' | 'ambient' | 'nature' | 'chill';
  };
  createdAt: number;
}

export type PageId =
  | 'home'
  | 'project-edit'
  | 'image-editor'
  | 'audio-editor'
  | 'system-sounds'
  | 'pinterest-feed'
  | 'settings';

export interface ImageAdjustments {
  zoom: number; // 1.0 to 3.0
  panX: number; // -100 to 100
  panY: number; // -100 to 100
  aspectRatio: '9:16' | '1:1' | '16:9' | 'free';
  brightness: number; // 50 to 150 (100 is normal)
  contrast: number; // 50 to 150 (100 is normal)
  saturation: number; // 0 to 200 (100 is normal)
  hueRotate: number; // 0 to 360
  blur: number; // 0 to 20 px
  filterPreset: 'normal' | 'vivid' | 'bw' | 'vintage' | 'warm' | 'cyberpunk' | 'cinematic';
  bgColor: string;
}

export interface AudioClip {
  id: string;
  name: string;
  url: string;
  durationSec: number;
  startTimeSec: number;
  endTimeSec: number;
  volume: number; // 0 to 1
  fadeIn: boolean;
  fadeOut: boolean;
  isSynth?: boolean;
  synthType?: 'lofi' | 'ambient' | 'nature' | 'chill';
}

export interface SlideItem {
  id: string;
  name: string;
  originalUrl: string;
  processedUrl?: string;
  adjustments: ImageAdjustments;
  durationSec: number; // individual slide duration!
  audio?: AudioClip;
  createdAt: number;
}

export interface SlideshowProject {
  id: string;
  title: string;
  description: string;
  mode: 'slideshow' | 'single';
  slides: SlideItem[];
  globalTarget: WallpaperTarget;
  defaultDurationSec: number;
  globalAudio?: AudioClip;
  createdAt: number;
  updatedAt: number;
}

export interface SystemSound {
  id: string;
  title: string;
  category: 'ringtone' | 'notification' | 'alarm';
  synthPreset: 'call1' | 'chime' | 'alarmWake' | 'ping' | 'marimba';
  durationLabel: string;
}

export interface AssignedSystemSound {
  name: string;
  url?: string;
  type: 'custom' | 'synth' | 'system';
  synthPreset?: 'call1' | 'chime' | 'alarmWake' | 'ping' | 'marimba';
  fileName?: string;
}

export interface AppConfig {
  appWallpaperUrl: string | null;
  appWallpaperBlur: number; // 0 to 20
  appWallpaperOpacity: number; // 10 to 100
  securityPinEnabled: boolean;
  securityPin: string;
  exportQuality: 'high' | 'standard';
  assignedRingtone: AssignedSystemSound;
  assignedNotification: AssignedSystemSound;
  assignedAlarm: AssignedSystemSound;
}

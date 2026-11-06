import React, { useState, useRef, useEffect } from 'react';
import { AudioClip } from '../types';
import { soundEngine } from '../utils/audioEngine';
import {
  Scissors,
  Play,
  Square,
  Volume2,
  Sparkles,
  Upload,
  Clock,
  Check,
  X,
  Radio,
} from 'lucide-react';

interface AudioEditorModalProps {
  initialClip?: AudioClip;
  targetTitle: string;
  onSave: (clip: AudioClip) => void;
  onClose: () => void;
}

export const AudioEditorModal: React.FC<AudioEditorModalProps> = ({
  initialClip,
  targetTitle,
  onSave,
  onClose,
}) => {
  const [name, setName] = useState(initialClip?.name || 'Musique personnalisée');
  const [url, setUrl] = useState(initialClip?.url || '');
  const [isSynth, setIsSynth] = useState(initialClip?.isSynth ?? true);
  const [synthType, setSynthType] = useState<AudioClip['synthType']>(
    initialClip?.synthType || 'lofi'
  );
  const [durationSec, setDurationSec] = useState(initialClip?.durationSec || 60);
  const [startTimeSec, setStartTimeSec] = useState(initialClip?.startTimeSec || 0);
  const [endTimeSec, setEndTimeSec] = useState(initialClip?.endTimeSec || 30);
  const [volume, setVolume] = useState(initialClip?.volume ?? 0.8);
  const [fadeIn, setFadeIn] = useState(initialClip?.fadeIn ?? true);
  const [fadeOut, setFadeOut] = useState(initialClip?.fadeOut ?? true);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentPosition, setCurrentPosition] = useState(0);

  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    soundEngine.setPositionCallback((sec) => {
      setCurrentPosition(sec);
    });
    return () => {
      soundEngine.stop();
      soundEngine.setPositionCallback(undefined);
    };
  }, []);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const audioUrl = URL.createObjectURL(file);
    const audioTest = new Audio(audioUrl);
    audioTest.onloadedmetadata = () => {
      const dur = Math.round(audioTest.duration) || 60;
      setDurationSec(dur);
      setEndTimeSec(Math.min(30, dur));
      setStartTimeSec(0);
      setName(file.name.replace(/\.[^/.]+$/, ''));
      setUrl(audioUrl);
      setIsSynth(false);
    };
  };

  const handlePlayPreview = () => {
    if (isPlaying) {
      soundEngine.stop();
      setIsPlaying(false);
    } else {
      const clipToTest: AudioClip = {
        id: 'preview',
        name,
        url,
        durationSec,
        startTimeSec,
        endTimeSec,
        volume,
        fadeIn,
        fadeOut,
        isSynth,
        synthType,
      };
      soundEngine.playClip(clipToTest, false);
      setIsPlaying(true);
    }
  };

  const handleSave = () => {
    const clip: AudioClip = {
      id: initialClip?.id || `clip-${Date.now()}`,
      name,
      url,
      durationSec,
      startTimeSec: Math.min(startTimeSec, endTimeSec - 1),
      endTimeSec: Math.max(startTimeSec + 1, endTimeSec),
      volume,
      fadeIn,
      fadeOut,
      isSynth,
      synthType,
    };
    onSave(clip);
    onClose();
  };

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const clipDuration = Math.max(1, Math.round(endTimeSec - startTimeSec));

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-3 sm:p-4">
      <div className="bg-white rounded-3xl w-full max-w-lg shadow-2xl overflow-hidden border border-blue-100 flex flex-col max-h-[92vh]">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between bg-blue-50/50">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-[#1E88E5] text-white flex items-center justify-center shadow-xs">
              <Scissors className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold text-[#0D47A1]">Éditeur de Son & Découpe</h2>
              <p className="text-xs text-blue-600 truncate max-w-xs">Pour : {targetTitle}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full hover:bg-gray-100 text-gray-500 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1">
          {/* Audio Source Selection */}
          <div>
            <label className="text-xs font-bold text-[#0D47A1] uppercase tracking-wider block mb-2">
              Source Audio
            </label>
            <div className="flex gap-2">
              <button
                onClick={() => setIsSynth(true)}
                className={`flex-1 p-3 rounded-2xl border text-xs font-semibold flex items-center justify-center gap-2 transition ${
                  isSynth
                    ? 'border-[#1E88E5] bg-blue-50 text-[#1E88E5] ring-2 ring-blue-100'
                    : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                }`}
              >
                <Sparkles className="w-4 h-4 text-amber-500" />
                <span>Ambiance Sans Droits</span>
              </button>

              <button
                onClick={() => {
                  fileInputRef.current?.click();
                }}
                className={`flex-1 p-3 rounded-2xl border text-xs font-semibold flex items-center justify-center gap-2 transition ${
                  !isSynth
                    ? 'border-[#1E88E5] bg-blue-50 text-[#1E88E5] ring-2 ring-blue-100'
                    : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                }`}
              >
                <Upload className="w-4 h-4 text-[#1E88E5]" />
                <span>Fichier Personnel</span>
              </button>
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="audio/*"
              className="hidden"
              onChange={handleFileUpload}
            />
          </div>

          {isSynth ? (
            <div>
              <label className="text-xs font-bold text-[#0D47A1] uppercase tracking-wider block mb-2">
                Choisir une atmosphère
              </label>
              <div className="grid grid-cols-2 gap-2">
                {[
                  { id: 'lofi', label: 'Lofi Chill Chords' },
                  { id: 'ambient', label: 'Océan Méditation' },
                  { id: 'nature', label: 'Brise Naturelle' },
                  { id: 'chill', label: 'Mélodie Crépuscule' },
                ].map((s) => (
                  <button
                    key={s.id}
                    onClick={() => {
                      setSynthType(s.id as AudioClip['synthType']);
                      setName(s.label);
                    }}
                    className={`p-2.5 rounded-xl border text-xs text-left font-medium transition ${
                      synthType === s.id
                        ? 'border-[#1E88E5] bg-blue-50/80 text-[#0D47A1] font-bold'
                        : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    {s.label}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div className="bg-blue-50 p-3 rounded-xl border border-blue-100 flex items-center justify-between">
              <span className="text-xs font-medium text-[#0D47A1] truncate max-w-[220px]">
                {name}
              </span>
              <span className="text-[11px] text-blue-600 font-mono">
                Durée : {formatTime(durationSec)}
              </span>
            </div>
          )}

          {/* Interactive Waveform / Timeline Trimmer */}
          <div className="bg-zinc-900 p-5 rounded-2xl text-white space-y-4 shadow-inner">
            <div className="flex items-center justify-between text-xs text-zinc-400">
              <span className="flex items-center gap-1.5 font-semibold text-white">
                <Scissors className="w-3.5 h-3.5 text-[#1E88E5]" />
                <span>Découpe de la boucle</span>
              </span>
              <span className="bg-[#1E88E5]/20 text-[#1E88E5] px-2 py-0.5 rounded-md font-mono text-[11px] font-bold">
                {clipDuration} sec sélectionnées
              </span>
            </div>

            {/* Visual Simulated Waveform Bar */}
            <div className="relative h-14 bg-zinc-800 rounded-xl overflow-hidden flex items-center px-1">
              {/* Audio wave bars */}
              <div className="flex items-center justify-between w-full h-10 px-2 pointer-events-none">
                {Array.from({ length: 32 }).map((_, i) => {
                  const percent = (i / 32) * durationSec;
                  const isSelected = percent >= startTimeSec && percent <= endTimeSec;
                  const height = 20 + Math.sin(i * 0.8) * 15 + ((i * 7) % 25);
                  return (
                    <div
                      key={i}
                      className={`w-1.5 rounded-full transition-all ${
                        isSelected ? 'bg-[#1E88E5]' : 'bg-zinc-600'
                      }`}
                      style={{ height: `${height}%` }}
                    />
                  );
                })}
              </div>

              {/* Selection Range Overlay */}
              <div
                className="absolute inset-y-0 bg-[#1E88E5]/20 border-x-2 border-[#1E88E5] pointer-events-none"
                style={{
                  left: `${(startTimeSec / durationSec) * 100}%`,
                  right: `${100 - (endTimeSec / durationSec) * 100}%`,
                }}
              />
            </div>

            {/* Start and End Sliders */}
            <div className="space-y-3">
              <div>
                <div className="flex justify-between text-[11px] text-zinc-300 mb-1">
                  <span>Point de départ</span>
                  <span className="font-mono">{formatTime(startTimeSec)}</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max={Math.max(1, durationSec - 1)}
                  value={startTimeSec}
                  onChange={(e) => {
                    const val = parseInt(e.target.value);
                    if (val < endTimeSec) setStartTimeSec(val);
                  }}
                  className="w-full accent-[#1E88E5]"
                />
              </div>

              <div>
                <div className="flex justify-between text-[11px] text-zinc-300 mb-1">
                  <span>Point de fin</span>
                  <span className="font-mono">{formatTime(endTimeSec)}</span>
                </div>
                <input
                  type="range"
                  min={startTimeSec + 1}
                  max={durationSec}
                  value={endTimeSec}
                  onChange={(e) => setEndTimeSec(parseInt(e.target.value))}
                  className="w-full accent-[#1E88E5]"
                />
              </div>
            </div>

            {/* Play Preview Button */}
            <button
              onClick={handlePlayPreview}
              className="w-full py-2.5 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-white font-semibold text-xs flex items-center justify-center gap-2 transition"
            >
              {isPlaying ? (
                <>
                  <Square className="w-3.5 h-3.5 fill-current text-red-400" />
                  <span>Arrêter la pré-écoute</span>
                </>
              ) : (
                <>
                  <Play className="w-3.5 h-3.5 fill-current text-[#1E88E5]" />
                  <span>Écouter l'extrait ({formatTime(startTimeSec)} → {formatTime(endTimeSec)})</span>
                </>
              )}
            </button>
          </div>

          {/* Volume and Fades */}
          <div className="space-y-3">
            <div>
              <div className="flex justify-between text-xs font-semibold text-gray-700 mb-1">
                <span className="flex items-center gap-1.5">
                  <Volume2 className="w-3.5 h-3.5 text-blue-600" />
                  <span>Volume sonore</span>
                </span>
                <span>{Math.round(volume * 100)}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="1"
                step="0.05"
                value={volume}
                onChange={(e) => setVolume(parseFloat(e.target.value))}
                className="w-full accent-[#1E88E5]"
              />
            </div>

            <div className="flex items-center justify-between pt-2">
              <label className="flex items-center gap-2 text-xs font-medium text-gray-700 cursor-pointer">
                <input
                  type="checkbox"
                  checked={fadeIn}
                  onChange={(e) => setFadeIn(e.target.checked)}
                  className="w-4 h-4 text-[#1E88E5] rounded-sm"
                />
                <span>Fondu d'entrée (Fade In)</span>
              </label>

              <label className="flex items-center gap-2 text-xs font-medium text-gray-700 cursor-pointer">
                <input
                  type="checkbox"
                  checked={fadeOut}
                  onChange={(e) => setFadeOut(e.target.checked)}
                  className="w-4 h-4 text-[#1E88E5] rounded-sm"
                />
                <span>Fondu de sortie (Fade Out)</span>
              </label>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-gray-100 bg-gray-50/60 flex items-center justify-end gap-2">
          <button
            onClick={onClose}
            className="px-4 py-2.5 rounded-xl text-xs font-semibold text-gray-600 hover:bg-gray-200 transition"
          >
            Annuler
          </button>
          <button
            onClick={handleSave}
            className="px-5 py-2.5 rounded-xl text-xs font-bold bg-[#1E88E5] hover:bg-[#1976D2] text-white flex items-center gap-1.5 transition shadow-sm"
          >
            <Check className="w-4 h-4" />
            <span>Associer le son</span>
          </button>
        </div>
      </div>
    </div>
  );
};

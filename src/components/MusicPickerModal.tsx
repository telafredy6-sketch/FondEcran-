import React, { useRef } from 'react';
import { X, Music, Upload, Sparkles, Volume2 } from 'lucide-react';
import { PRESET_AUDIO_TRACKS } from '../utils/audioEngine';

interface MusicPickerModalProps {
  imageIndex: number;
  onClose: () => void;
  onSelectPreset: (preset: { name: string; synthType: 'lofi' | 'ambient' | 'nature' | 'chill' }) => void;
  onUploadCustom: (file: File) => void;
}

export const MusicPickerModal: React.FC<MusicPickerModalProps> = ({
  imageIndex,
  onClose,
  onSelectPreset,
  onUploadCustom,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onUploadCustom(file);
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
      <div className="bg-white rounded-2xl w-full max-w-md p-6 shadow-2xl border border-blue-100">
        <div className="flex items-center justify-between pb-3 border-b border-blue-50">
          <div className="flex items-center gap-2 text-[#0D47A1]">
            <Music className="w-5 h-5 text-[#1E88E5]" />
            <h3 className="font-semibold text-base">Musique pour l'Image {imageIndex + 1}</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-full hover:bg-blue-50 text-blue-700 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="mt-4 space-y-4">
          <div>
            <label className="text-xs font-semibold uppercase text-blue-900 tracking-wider mb-2 block">
              1. Importer un fichier audio (MP3, WAV, OGG)
            </label>
            <input
              ref={fileInputRef}
              type="file"
              accept="audio/*"
              className="hidden"
              onChange={handleFileChange}
            />
            <button
              onClick={() => fileInputRef.current?.click()}
              className="w-full py-3 px-4 rounded-xl border-2 border-dashed border-[#1E88E5] text-[#1E88E5] bg-blue-50/50 hover:bg-blue-100/50 flex items-center justify-center gap-2 font-medium text-sm transition"
            >
              <Upload className="w-4 h-4" />
              <span>Choisir un fichier audio sur cet appareil</span>
            </button>
          </div>

          <div>
            <label className="text-xs font-semibold uppercase text-blue-900 tracking-wider mb-2 block flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-amber-500" />
              <span>Ou choisir une ambiance sonore relaxante</span>
            </label>
            <div className="grid grid-cols-1 gap-2">
              {PRESET_AUDIO_TRACKS.map((track: { name: string; synthType: 'lofi' | 'ambient' | 'nature' | 'chill' }) => (
                <button
                  key={track.name}
                  onClick={() => {
                    onSelectPreset(track);
                    onClose();
                  }}
                  className="flex items-center justify-between p-3 rounded-xl bg-blue-50/70 hover:bg-blue-100 border border-blue-100 transition text-left group"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-blue-200/70 text-[#0D47A1] flex items-center justify-center group-hover:bg-[#1E88E5] group-hover:text-white transition">
                      <Volume2 className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="text-sm font-medium text-[#0D47A1]">{track.name}</div>
                      <div className="text-[11px] text-blue-600">Ambiance sonore sans copyright</div>
                    </div>
                  </div>
                  <span className="text-xs text-[#1E88E5] font-semibold">Choisir</span>
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="mt-6 pt-3 border-t border-blue-50 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm text-blue-700 hover:bg-blue-50 rounded-xl"
          >
            Annuler
          </button>
        </div>
      </div>
    </div>
  );
};

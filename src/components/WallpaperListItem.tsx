import React from 'react';
import { WallpaperItem } from '../types';
import { Music, X, Trash2, Eye } from 'lucide-react';

interface WallpaperListItemProps {
  item: WallpaperItem;
  index: number;
  isActive: boolean;
  onPickMusic: () => void;
  onRemoveMusic: () => void;
  onDeleteWallpaper: () => void;
  onPreview: () => void;
}

export const WallpaperListItem: React.FC<WallpaperListItemProps> = ({
  item,
  index,
  isActive,
  onPickMusic,
  onRemoveMusic,
  onDeleteWallpaper,
  onPreview,
}) => {
  return (
    <div
      className={`flex items-center p-2 rounded-xl bg-white shadow-xs mb-2 transition border ${
        isActive ? 'border-[#1E88E5] ring-2 ring-blue-200' : 'border-transparent'
      }`}
    >
      {/* Thumbnail 64x64 matching dp(64) */}
      <div
        onClick={onPreview}
        className="w-16 h-16 rounded-lg overflow-hidden shrink-0 cursor-pointer relative group bg-gray-100"
      >
        <img
          src={item.dataUrl}
          alt={`Image ${index + 1}`}
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 flex items-center justify-center transition">
          <Eye className="w-5 h-5 text-white" />
        </div>
        {isActive && (
          <span className="absolute bottom-0 inset-x-0 bg-[#1E88E5] text-white text-[9px] font-bold text-center py-0.5">
            ACTIF
          </span>
        )}
      </div>

      {/* Label and Music status */}
      <div className="flex-1 px-3 min-w-0">
        <div className="flex items-center gap-1.5">
          <span className="font-semibold text-sm text-[#0D47A1]">
            Image {index + 1}
          </span>
          <span className="text-[11px] text-blue-500 truncate max-w-[120px]">
            {item.name}
          </span>
        </div>
        <div className="text-xs truncate text-[#0D47A1]/80 mt-0.5 flex items-center gap-1">
          {item.music ? (
            <span className="text-emerald-700 font-medium flex items-center gap-1">
              <span>♪</span>
              <span className="truncate">{item.music.name}</span>
            </span>
          ) : (
            <span className="text-blue-400">Aucune musique</span>
          )}
        </div>
      </div>

      {/* Action buttons: ♪ and ✕ matching Android row */}
      <div className="flex items-center gap-1">
        <button
          onClick={onPickMusic}
          title="Associer une musique"
          className="w-10 h-10 rounded-lg bg-[#1E88E5] hover:bg-[#1976D2] active:scale-95 text-white flex items-center justify-center font-bold text-base transition shadow-xs"
        >
          ♪
        </button>

        {item.music && (
          <button
            onClick={onRemoveMusic}
            title="Supprimer la musique"
            className="w-10 h-10 rounded-lg bg-blue-100 hover:bg-blue-200 text-[#0D47A1] active:scale-95 flex items-center justify-center font-bold text-base transition"
          >
            ✕
          </button>
        )}

        <button
          onClick={onDeleteWallpaper}
          title="Retirer cette image"
          className="w-8 h-8 rounded-lg hover:bg-red-50 text-red-500 hover:text-red-700 flex items-center justify-center transition"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};

import React, { useState } from 'react';
import { SlideItem, ImageAdjustments } from '../types';
import {
  Crop,
  Sliders,
  Sparkles,
  Maximize2,
  ZoomIn,
  Move,
  RotateCcw,
  Check,
  X,
  Palette,
  SunMedium,
  Contrast,
  Droplet,
  Smartphone,
  Eye,
} from 'lucide-react';

interface ImageEditorModalProps {
  slide: SlideItem;
  onSave: (updatedAdjustments: ImageAdjustments) => void;
  onClose: () => void;
  onSetAsAppWallpaper?: (imageUrl: string) => void;
}

const FILTER_PRESETS = [
  { id: 'normal', label: 'Normal', icon: '✨' },
  { id: 'vivid', label: 'Vif', icon: '🌈' },
  { id: 'bw', label: 'N&B', icon: '🌑' },
  { id: 'vintage', label: 'Rétro', icon: '🎞️' },
  { id: 'warm', label: 'Chaud', icon: '🌅' },
  { id: 'cyberpunk', label: 'Néon', icon: '⚡' },
  { id: 'cinematic', label: 'Cinéma', icon: '🎬' },
] as const;

export const ImageEditorModal: React.FC<ImageEditorModalProps> = ({
  slide,
  onSave,
  onClose,
  onSetAsAppWallpaper,
}) => {
  const [activeTab, setActiveTab] = useState<'crop' | 'adjust' | 'filters' | 'bg'>('crop');
  const [adj, setAdj] = useState<ImageAdjustments>({
    ...slide.adjustments,
  });

  const handleReset = () => {
    setAdj({
      zoom: 1,
      panX: 0,
      panY: 0,
      aspectRatio: '9:16',
      brightness: 100,
      contrast: 100,
      saturation: 100,
      hueRotate: 0,
      blur: 0,
      filterPreset: 'normal',
      bgColor: '#000000',
    });
  };

  const applyPreset = (preset: typeof FILTER_PRESETS[number]['id']) => {
    let brightness = 100;
    let contrast = 100;
    let saturation = 100;
    let hueRotate = 0;

    if (preset === 'vivid') {
      contrast = 120;
      saturation = 145;
    } else if (preset === 'bw') {
      saturation = 0;
      contrast = 115;
    } else if (preset === 'vintage') {
      brightness = 95;
      contrast = 110;
      saturation = 75;
      hueRotate = 20;
    } else if (preset === 'warm') {
      brightness = 105;
      saturation = 120;
      hueRotate = 10;
    } else if (preset === 'cyberpunk') {
      contrast = 135;
      saturation = 150;
      hueRotate = 300;
    } else if (preset === 'cinematic') {
      contrast = 130;
      saturation = 85;
      brightness = 95;
    }

    setAdj((prev) => ({
      ...prev,
      filterPreset: preset,
      brightness,
      contrast,
      saturation,
      hueRotate,
    }));
  };

  // Compute CSS filter string
  const cssFilter = `
    brightness(${adj.brightness}%)
    contrast(${adj.contrast}%)
    saturate(${adj.saturation}%)
    hue-rotate(${adj.hueRotate}deg)
    blur(${adj.blur}px)
  `;

  const getAspectClass = () => {
    switch (adj.aspectRatio) {
      case '9:16':
        return 'w-64 aspect-[9/16]';
      case '1:1':
        return 'w-72 aspect-square';
      case '16:9':
        return 'w-80 aspect-[16/9]';
      default:
        return 'w-72 h-80';
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-2 sm:p-4">
      <div className="bg-white rounded-3xl w-full max-w-4xl max-h-[92vh] flex flex-col shadow-2xl overflow-hidden border border-blue-100">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between bg-blue-50/50">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-[#1E88E5] text-white flex items-center justify-center shadow-xs">
              <Crop className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold text-[#0D47A1]">Éditeur d'Image</h2>
              <p className="text-xs text-blue-600 truncate max-w-xs">{slide.name}</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleReset}
              className="px-3 py-1.5 rounded-xl text-xs font-semibold text-blue-700 hover:bg-blue-100 flex items-center gap-1 transition"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Réinitialiser</span>
            </button>
            <button
              onClick={onClose}
              className="p-1.5 rounded-full hover:bg-gray-100 text-gray-500 transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Editor Body */}
        <div className="flex-1 overflow-hidden grid grid-cols-1 md:grid-cols-12 min-h-[380px]">
          {/* Left Canvas Preview Area (7 cols) */}
          <div className="md:col-span-7 bg-zinc-950 flex flex-col items-center justify-center p-6 relative overflow-hidden">
            {/* Viewport Frame */}
            <div
              className={`relative rounded-2xl overflow-hidden shadow-2xl transition-all duration-300 ring-2 ring-white/20 ${getAspectClass()}`}
              style={{ backgroundColor: adj.bgColor }}
            >
              <img
                src={slide.originalUrl}
                alt="Aperçu"
                className="w-full h-full object-cover transition-transform select-none pointer-events-none"
                style={{
                  transform: `scale(${adj.zoom}) translate(${adj.panX}px, ${adj.panY}px)`,
                  filter: cssFilter,
                }}
              />

              {/* Grid Overlay for Composition */}
              <div className="absolute inset-0 grid grid-cols-3 grid-rows-3 pointer-events-none opacity-25">
                <div className="border-r border-b border-white" />
                <div className="border-r border-b border-white" />
                <div className="border-b border-white" />
                <div className="border-r border-b border-white" />
                <div className="border-r border-b border-white" />
                <div className="border-b border-white" />
                <div className="border-r border-white" />
                <div className="border-r border-white" />
                <div />
              </div>
            </div>

            <div className="absolute bottom-3 left-4 text-[11px] text-white/60 flex items-center gap-2">
              <Smartphone className="w-3.5 h-3.5 text-blue-400" />
              <span>Ratio : {adj.aspectRatio} • Zoom : {adj.zoom.toFixed(1)}x</span>
            </div>
          </div>

          {/* Right Controls Panel (5 cols) */}
          <div className="md:col-span-5 flex flex-col bg-white border-l border-gray-100 overflow-y-auto">
            {/* Tab navigation */}
            <div className="flex border-b border-gray-100 bg-gray-50/70 p-1.5 gap-1">
              <button
                onClick={() => setActiveTab('crop')}
                className={`flex-1 py-2 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition ${
                  activeTab === 'crop'
                    ? 'bg-white text-[#1E88E5] shadow-xs'
                    : 'text-gray-600 hover:text-black'
                }`}
              >
                <Crop className="w-3.5 h-3.5" />
                <span>Format</span>
              </button>
              <button
                onClick={() => setActiveTab('adjust')}
                className={`flex-1 py-2 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition ${
                  activeTab === 'adjust'
                    ? 'bg-white text-[#1E88E5] shadow-xs'
                    : 'text-gray-600 hover:text-black'
                }`}
              >
                <Sliders className="w-3.5 h-3.5" />
                <span>Ajuster</span>
              </button>
              <button
                onClick={() => setActiveTab('filters')}
                className={`flex-1 py-2 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition ${
                  activeTab === 'filters'
                    ? 'bg-white text-[#1E88E5] shadow-xs'
                    : 'text-gray-600 hover:text-black'
                }`}
              >
                <Sparkles className="w-3.5 h-3.5" />
                <span>Filtres</span>
              </button>
              <button
                onClick={() => setActiveTab('bg')}
                className={`flex-1 py-2 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition ${
                  activeTab === 'bg'
                    ? 'bg-white text-[#1E88E5] shadow-xs'
                    : 'text-gray-600 hover:text-black'
                }`}
              >
                <Palette className="w-3.5 h-3.5" />
                <span>Fond</span>
              </button>
            </div>

            {/* Tab Content */}
            <div className="p-5 flex-1 space-y-5">
              {activeTab === 'crop' && (
                <div className="space-y-4">
                  <div>
                    <label className="text-xs font-bold text-[#0D47A1] uppercase tracking-wider block mb-2">
                      Format d'écran (Ratio)
                    </label>
                    <div className="grid grid-cols-3 gap-2">
                      {[
                        { id: '9:16', label: '9:16 Smartphone' },
                        { id: '1:1', label: '1:1 Carré' },
                        { id: '16:9', label: '16:9 Écran' },
                      ].map((r) => (
                        <button
                          key={r.id}
                          onClick={() =>
                            setAdj((prev) => ({
                              ...prev,
                              aspectRatio: r.id as ImageAdjustments['aspectRatio'],
                            }))
                          }
                          className={`p-2.5 rounded-xl border text-xs font-medium transition text-center ${
                            adj.aspectRatio === r.id
                              ? 'border-[#1E88E5] bg-blue-50 text-[#1E88E5] font-bold ring-1 ring-blue-200'
                              : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                          }`}
                        >
                          {r.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-between text-xs font-semibold text-gray-700 mb-1">
                      <span className="flex items-center gap-1">
                        <ZoomIn className="w-3.5 h-3.5 text-blue-500" />
                        <span>Zoom</span>
                      </span>
                      <span>{adj.zoom.toFixed(2)}x</span>
                    </div>
                    <input
                      type="range"
                      min="1"
                      max="3"
                      step="0.05"
                      value={adj.zoom}
                      onChange={(e) =>
                        setAdj((prev) => ({ ...prev, zoom: parseFloat(e.target.value) }))
                      }
                      className="w-full accent-[#1E88E5]"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between text-xs font-semibold text-gray-700 mb-1">
                      <span className="flex items-center gap-1">
                        <Move className="w-3.5 h-3.5 text-blue-500" />
                        <span>Déplacement Horizontal (X)</span>
                      </span>
                      <span>{adj.panX}px</span>
                    </div>
                    <input
                      type="range"
                      min="-120"
                      max="120"
                      value={adj.panX}
                      onChange={(e) =>
                        setAdj((prev) => ({ ...prev, panX: parseInt(e.target.value) }))
                      }
                      className="w-full accent-[#1E88E5]"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between text-xs font-semibold text-gray-700 mb-1">
                      <span className="flex items-center gap-1">
                        <Move className="w-3.5 h-3.5 text-blue-500" />
                        <span>Déplacement Vertical (Y)</span>
                      </span>
                      <span>{adj.panY}px</span>
                    </div>
                    <input
                      type="range"
                      min="-120"
                      max="120"
                      value={adj.panY}
                      onChange={(e) =>
                        setAdj((prev) => ({ ...prev, panY: parseInt(e.target.value) }))
                      }
                      className="w-full accent-[#1E88E5]"
                    />
                  </div>
                </div>
              )}

              {activeTab === 'adjust' && (
                <div className="space-y-4">
                  {/* Luminosité */}
                  <div>
                    <div className="flex justify-between text-xs font-semibold text-gray-700 mb-1">
                      <span className="flex items-center gap-1.5">
                        <SunMedium className="w-3.5 h-3.5 text-amber-500" />
                        <span>Luminosité</span>
                      </span>
                      <span>{adj.brightness}%</span>
                    </div>
                    <input
                      type="range"
                      min="50"
                      max="150"
                      value={adj.brightness}
                      onChange={(e) =>
                        setAdj((prev) => ({ ...prev, brightness: parseInt(e.target.value) }))
                      }
                      className="w-full accent-[#1E88E5]"
                    />
                  </div>

                  {/* Contraste */}
                  <div>
                    <div className="flex justify-between text-xs font-semibold text-gray-700 mb-1">
                      <span className="flex items-center gap-1.5">
                        <Contrast className="w-3.5 h-3.5 text-indigo-500" />
                        <span>Contraste</span>
                      </span>
                      <span>{adj.contrast}%</span>
                    </div>
                    <input
                      type="range"
                      min="50"
                      max="150"
                      value={adj.contrast}
                      onChange={(e) =>
                        setAdj((prev) => ({ ...prev, contrast: parseInt(e.target.value) }))
                      }
                      className="w-full accent-[#1E88E5]"
                    />
                  </div>

                  {/* Saturation */}
                  <div>
                    <div className="flex justify-between text-xs font-semibold text-gray-700 mb-1">
                      <span className="flex items-center gap-1.5">
                        <Droplet className="w-3.5 h-3.5 text-rose-500" />
                        <span>Saturation</span>
                      </span>
                      <span>{adj.saturation}%</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="200"
                      value={adj.saturation}
                      onChange={(e) =>
                        setAdj((prev) => ({ ...prev, saturation: parseInt(e.target.value) }))
                      }
                      className="w-full accent-[#1E88E5]"
                    />
                  </div>

                  {/* Teinte */}
                  <div>
                    <div className="flex justify-between text-xs font-semibold text-gray-700 mb-1">
                      <span>Rotation de teinte (Hue)</span>
                      <span>{adj.hueRotate}°</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="360"
                      value={adj.hueRotate}
                      onChange={(e) =>
                        setAdj((prev) => ({ ...prev, hueRotate: parseInt(e.target.value) }))
                      }
                      className="w-full accent-[#1E88E5]"
                    />
                  </div>

                  {/* Flou */}
                  <div>
                    <div className="flex justify-between text-xs font-semibold text-gray-700 mb-1">
                      <span>Flou artistique</span>
                      <span>{adj.blur}px</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="15"
                      value={adj.blur}
                      onChange={(e) =>
                        setAdj((prev) => ({ ...prev, blur: parseInt(e.target.value) }))
                      }
                      className="w-full accent-[#1E88E5]"
                    />
                  </div>
                </div>
              )}

              {activeTab === 'filters' && (
                <div>
                  <label className="text-xs font-bold text-[#0D47A1] uppercase tracking-wider block mb-3">
                    Filtres Instantanés
                  </label>
                  <div className="grid grid-cols-2 gap-2.5">
                    {FILTER_PRESETS.map((p) => (
                      <button
                        key={p.id}
                        onClick={() => applyPreset(p.id)}
                        className={`p-3 rounded-2xl border text-left flex items-center gap-2.5 transition ${
                          adj.filterPreset === p.id
                            ? 'border-[#1E88E5] bg-blue-50/80 font-bold text-[#0D47A1] ring-2 ring-blue-200'
                            : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                        }`}
                      >
                        <span className="text-lg">{p.icon}</span>
                        <span className="text-xs">{p.label}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {activeTab === 'bg' && (
                <div className="space-y-3">
                  <label className="text-xs font-bold text-[#0D47A1] uppercase tracking-wider block">
                    Couleur d'arrière-plan du cadrage
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {['#000000', '#111827', '#0D47A1', '#1E88E5', '#4C1D95', '#831843', '#064E3B', '#FFFFFF'].map(
                      (c) => (
                        <button
                          key={c}
                          onClick={() => setAdj((prev) => ({ ...prev, bgColor: c }))}
                          className={`w-9 h-9 rounded-xl border-2 transition ${
                            adj.bgColor === c ? 'scale-110 border-[#1E88E5] shadow-md' : 'border-gray-200'
                          }`}
                          style={{ backgroundColor: c }}
                        />
                      )
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* Bottom Actions */}
            <div className="p-4 border-t border-gray-100 bg-gray-50/60 flex flex-col gap-2">
              {onSetAsAppWallpaper && (
                <button
                  onClick={() => onSetAsAppWallpaper(slide.originalUrl)}
                  className="w-full py-2.5 px-3 rounded-xl bg-blue-50 hover:bg-blue-100 text-[#1E88E5] text-xs font-semibold flex items-center justify-center gap-1.5 transition"
                >
                  <Palette className="w-3.5 h-3.5" />
                  <span>Mettre en fond d'écran dans l'App</span>
                </button>
              )}

              <div className="flex items-center gap-2">
                <button
                  onClick={onClose}
                  className="flex-1 py-2.5 px-4 rounded-xl text-xs font-semibold text-gray-600 hover:bg-gray-200 transition"
                >
                  Annuler
                </button>
                <button
                  onClick={() => {
                    onSave(adj);
                    onClose();
                  }}
                  className="flex-1 py-2.5 px-4 rounded-xl text-xs font-bold bg-[#1E88E5] hover:bg-[#1976D2] text-white flex items-center justify-center gap-1.5 transition shadow-sm"
                >
                  <Check className="w-4 h-4" />
                  <span>Enregistrer</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

import React, { useState } from 'react';
import { PINTEREST_EXPLORE_FEED, PinterestWallpaper } from '../utils/pinterestFeed';
import {
  Search,
  ExternalLink,
  Plus,
  Palette,
  Sparkles,
  Download,
  Eye,
  Check,
} from 'lucide-react';

interface PinterestFeedPageProps {
  onAddToCurrentProject: (wallpaper: PinterestWallpaper) => void;
  onSetAsAppWallpaper: (imageUrl: string) => void;
  onOpenLivePreview: (wallpaper: PinterestWallpaper) => void;
}

export const PinterestFeedPage: React.FC<PinterestFeedPageProps> = ({
  onAddToCurrentProject,
  onSetAsAppWallpaper,
  onOpenLivePreview,
}) => {
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const categories = [
    { id: 'all', label: 'Toutes les tendances' },
    { id: 'nature', label: '🌿 Nature' },
    { id: 'ocean', label: '🌊 Océan' },
    { id: 'anime', label: '✨ Anime' },
    { id: 'cyberpunk', label: '⚡ Cyberpunk' },
    { id: 'minimal', label: '🎨 Minimaliste' },
    { id: 'space', label: '🌌 Espace' },
  ];

  const filtered = PINTEREST_EXPLORE_FEED.filter((item) => {
    const matchesCat = selectedCategory === 'all' || item.category === selectedCategory;
    const matchesSearch =
      searchQuery === '' ||
      item.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.tags.some((t) => t.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesCat && matchesSearch;
  });

  const handleExternalPinterest = (query?: string) => {
    const q = query || searchQuery || 'aesthetic wallpapers';
    window.open(
      `https://www.pinterest.com/search/pins/?q=${encodeURIComponent(q + ' wallpaper')}`,
      '_blank'
    );
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Header and Pinterest Search Banner */}
      <div className="bg-gradient-to-br from-red-600 via-rose-600 to-pink-600 rounded-3xl p-6 text-white shadow-lg space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="w-8 h-8 rounded-full bg-white text-red-600 font-bold flex items-center justify-center text-sm shadow-xs">
                P
              </span>
              <h1 className="text-2xl font-black tracking-tight">Découverte Pinterest</h1>
            </div>
            <p className="text-xs text-red-100 mt-1 max-w-lg">
              Explore et ajoute directement des fonds d'écran tendances ou applique-les comme arrière-plan immersif dans l'application.
            </p>
          </div>

          <button
            onClick={() => handleExternalPinterest()}
            className="self-start sm:self-auto px-4 py-2.5 rounded-xl bg-white text-red-600 hover:bg-red-50 text-xs font-bold flex items-center gap-1.5 transition shadow-sm shrink-0"
          >
            <span>Ouvrir Pinterest</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Search Bar */}
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Chercher : nature, océan, néon, anime..."
              className="w-full bg-white/95 text-gray-900 rounded-xl pl-10 pr-4 py-2.5 text-xs sm:text-sm focus:outline-hidden focus:ring-2 focus:ring-white placeholder:text-gray-400 shadow-inner"
            />
          </div>
          <button
            onClick={() => handleExternalPinterest(searchQuery)}
            className="px-4 py-2.5 bg-black/30 hover:bg-black/40 text-white rounded-xl text-xs font-semibold backdrop-blur-xs transition"
          >
            Rechercher
          </button>
        </div>
      </div>

      {/* Category Pills */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
        {categories.map((cat) => (
          <button
            key={cat.id}
            onClick={() => setSelectedCategory(cat.id)}
            className={`px-3.5 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition ${
              selectedCategory === cat.id
                ? 'bg-[#1E88E5] text-white shadow-xs'
                : 'bg-white/80 hover:bg-white text-[#0D47A1] border border-blue-100'
            }`}
          >
            {cat.label}
          </button>
        ))}
      </div>

      {/* Masonry / Grid Display */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {filtered.map((item) => (
          <div
            key={item.id}
            className="group bg-white rounded-2xl overflow-hidden shadow-sm hover:shadow-xl transition-all duration-300 border border-blue-100 flex flex-col"
          >
            {/* Image Container with 9:16 aspect ratio */}
            <div className="relative aspect-[9/14] overflow-hidden bg-zinc-950">
              <img
                src={item.imageUrl}
                alt={item.title}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                loading="lazy"
              />

              <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-black/30 opacity-80 group-hover:opacity-90 transition-opacity" />

              {/* Top Source Badge */}
              <div className="absolute top-3 left-3 flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-black/40 backdrop-blur-md text-[10px] text-white font-medium border border-white/10">
                <span className="w-1.5 h-1.5 rounded-full bg-red-500" />
                <span>Pinterest</span>
              </div>

              {/* Quick Actions Hover Overlay */}
              <div className="absolute inset-0 flex flex-col justify-end p-4 text-white">
                <h3 className="font-bold text-sm tracking-tight text-white mb-1">
                  {item.title}
                </h3>
                <div className="flex items-center gap-1.5 mb-3 flex-wrap">
                  {item.tags.map((tag) => (
                    <span
                      key={tag}
                      className="text-[9px] px-2 py-0.5 rounded-md bg-white/20 backdrop-blur-md"
                    >
                      #{tag}
                    </span>
                  ))}
                </div>

                <div className="grid grid-cols-2 gap-2 pt-2 border-t border-white/20">
                  <button
                    onClick={() => onAddToCurrentProject(item)}
                    className="py-2 px-2.5 rounded-xl bg-[#1E88E5] hover:bg-[#1976D2] text-white text-[11px] font-bold flex items-center justify-center gap-1 transition shadow-xs"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Ajouter</span>
                  </button>

                  <button
                    onClick={() => {
                      onSetAsAppWallpaper(item.imageUrl);
                      setCopiedId(item.id);
                      setTimeout(() => setCopiedId(null), 2000);
                    }}
                    className="py-2 px-2.5 rounded-xl bg-white/20 hover:bg-white/30 backdrop-blur-md text-white text-[11px] font-semibold flex items-center justify-center gap-1 transition"
                  >
                    {copiedId === item.id ? (
                      <>
                        <Check className="w-3.5 h-3.5 text-emerald-400" />
                        <span>Appliqué !</span>
                      </>
                    ) : (
                      <>
                        <Palette className="w-3.5 h-3.5" />
                        <span>Fond App</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

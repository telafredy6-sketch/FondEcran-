import React, { useState } from 'react';
import { SYSTEM_SOUNDS_CATALOG, soundEngine } from '../utils/audioEngine';
import { Bell, PhoneCall, AlarmClock, Play, Square, Download, Sparkles, Volume2 } from 'lucide-react';

export const SystemSoundsPage: React.FC = () => {
  const [activeCategory, setActiveCategory] = useState<'all' | 'notification' | 'ringtone' | 'alarm'>('all');
  const [playingId, setPlayingId] = useState<string | null>(null);

  const handlePlaySound = (sound: typeof SYSTEM_SOUNDS_CATALOG[number]) => {
    if (playingId === sound.id) {
      soundEngine.stop();
      setPlayingId(null);
    } else {
      soundEngine.playSystemSound(sound.synthPreset);
      setPlayingId(sound.id);
      setTimeout(() => {
        setPlayingId(null);
      }, 3500);
    }
  };

  const filtered = SYSTEM_SOUNDS_CATALOG.filter(
    (s) => activeCategory === 'all' || s.category === activeCategory
  );

  return (
    <div className="space-y-6 pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div>
          <h1 className="text-2xl font-black text-[#0D47A1]">Sons Système & Sonneries</h1>
          <p className="text-sm text-blue-700/80">
            Personnalise tes sonneries d'appels, notifications et alarmes du téléphone.
          </p>
        </div>

        {/* Category Filters */}
        <div className="flex items-center gap-1.5 bg-white/70 backdrop-blur-xs p-1 rounded-2xl border border-blue-100 shadow-xs">
          {[
            { id: 'all', label: 'Tous' },
            { id: 'notification', label: 'Notifications' },
            { id: 'ringtone', label: 'Sonneries' },
            { id: 'alarm', label: 'Alarmes' },
          ].map((cat) => (
            <button
              key={cat.id}
              onClick={() => setActiveCategory(cat.id as any)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
                activeCategory === cat.id
                  ? 'bg-[#1E88E5] text-white shadow-xs'
                  : 'text-gray-600 hover:text-blue-900'
              }`}
            >
              {cat.label}
            </button>
          ))}
        </div>
      </div>

      {/* Sounds Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {filtered.map((sound) => {
          const isPlaying = playingId === sound.id;
          return (
            <div
              key={sound.id}
              className={`p-4 rounded-2xl bg-white border transition shadow-xs flex items-center justify-between group ${
                isPlaying
                  ? 'border-[#1E88E5] ring-2 ring-blue-100'
                  : 'border-blue-100/70 hover:border-blue-200'
              }`}
            >
              <div className="flex items-center gap-3 min-w-0">
                <div
                  className={`w-11 h-11 rounded-2xl flex items-center justify-center shrink-0 transition ${
                    sound.category === 'notification'
                      ? 'bg-amber-100 text-amber-700'
                      : sound.category === 'ringtone'
                      ? 'bg-blue-100 text-[#1E88E5]'
                      : 'bg-rose-100 text-rose-700'
                  }`}
                >
                  {sound.category === 'notification' ? (
                    <Bell className="w-5 h-5" />
                  ) : sound.category === 'ringtone' ? (
                    <PhoneCall className="w-5 h-5" />
                  ) : (
                    <AlarmClock className="w-5 h-5" />
                  )}
                </div>

                <div className="min-w-0">
                  <h3 className="font-bold text-sm text-[#0D47A1] truncate">
                    {sound.title}
                  </h3>
                  <div className="flex items-center gap-2 mt-0.5 text-[11px] text-gray-500">
                    <span className="capitalize font-medium text-blue-600">
                      {sound.category}
                    </span>
                    <span>•</span>
                    <span>{sound.durationLabel}</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <button
                  onClick={() => handlePlaySound(sound)}
                  className={`w-10 h-10 rounded-xl flex items-center justify-center transition active:scale-95 ${
                    isPlaying
                      ? 'bg-red-500 text-white'
                      : 'bg-[#1E88E5] hover:bg-[#1976D2] text-white'
                  }`}
                  title={isPlaying ? 'Arrêter' : 'Écouter'}
                >
                  {isPlaying ? (
                    <Square className="w-4 h-4 fill-current" />
                  ) : (
                    <Play className="w-4 h-4 fill-current ml-0.5" />
                  )}
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Info Card */}
      <div className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white p-5 rounded-3xl shadow-md flex items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2 font-bold text-sm">
            <Sparkles className="w-4 h-4 text-amber-300" />
            <span>Harmonie Sonore Système</span>
          </div>
          <p className="text-xs text-blue-100 max-w-md leading-relaxed">
            Ces sons sont optimisés pour les haut-parleurs mobiles sans saturation et peuvent également être associés à vos diaporamas animés.
          </p>
        </div>
        <Volume2 className="w-12 h-12 text-white/20 shrink-0 hidden sm:block" />
      </div>
    </div>
  );
};

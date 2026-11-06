import React, { useState } from 'react';
import { SlideshowProject } from '../types';
import {
  Plus,
  Play,
  Layers,
  Image as ImageIcon,
  Clock,
  Music,
  Trash2,
  Sparkles,
  Smartphone,
  ChevronRight,
  ExternalLink,
} from 'lucide-react';

interface HomePageProps {
  projects: SlideshowProject[];
  activeProjectId: string;
  onSelectProject: (id: string) => void;
  onCreateProject: (mode: 'slideshow' | 'single') => void;
  onDeleteProject: (id: string) => void;
  onOpenLivePreview: (project: SlideshowProject) => void;
  onNavigatePinterest: () => void;
}

export const HomePage: React.FC<HomePageProps> = ({
  projects,
  activeProjectId,
  onSelectProject,
  onCreateProject,
  onDeleteProject,
  onOpenLivePreview,
  onNavigatePinterest,
}) => {
  const [showNewModal, setShowNewModal] = useState(false);

  return (
    <div className="space-y-6 pb-16">
      {/* Hero Welcome Banner */}
      <div className="bg-gradient-to-r from-[#1E88E5] to-[#0D47A1] rounded-3xl p-6 sm:p-8 text-white shadow-lg relative overflow-hidden flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-2 z-10 max-w-xl">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/15 backdrop-blur-md text-xs font-semibold">
            <Sparkles className="w-3.5 h-3.5 text-amber-300" />
            <span>Créateur Pro de Fonds d'Écran & Diaporamas</span>
          </div>
          <h1 className="text-3xl sm:text-4xl font-black tracking-tight">
            Fond d'écran Studio
          </h1>
          <p className="text-xs sm:text-sm text-blue-100 leading-relaxed">
            Crée des diaporamas animés avec musiques découpées sur-mesure, durées par image personnalisées et prévisualisation smartphone en direct.
          </p>
        </div>

        {/* Big Create Button */}
        <div className="z-10 shrink-0">
          <button
            onClick={() => setShowNewModal(true)}
            className="w-full sm:w-auto px-6 py-3.5 rounded-2xl bg-white text-[#0D47A1] hover:bg-blue-50 active:scale-95 font-bold text-sm flex items-center justify-center gap-2 transition shadow-xl group"
          >
            <Plus className="w-5 h-5 text-[#1E88E5] group-hover:rotate-90 transition-transform" />
            <span>Nouveau Projet</span>
          </button>
        </div>

        {/* Background glow decoration */}
        <div className="absolute -right-10 -bottom-10 w-64 h-64 bg-white/10 rounded-full blur-2xl pointer-events-none" />
      </div>

      {/* Projects List Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-[#0D47A1]">Mes Diaporamas & Fonds</h2>
            <p className="text-xs text-blue-600 font-medium">
              {projects.length} projet(s) sauvegardé(s)
            </p>
          </div>

          <button
            onClick={onNavigatePinterest}
            className="text-xs font-bold text-[#1E88E5] hover:text-[#0D47A1] flex items-center gap-1 transition"
          >
            <span>Explorer Pinterest</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {projects.map((proj) => {
            const isActive = proj.id === activeProjectId;
            const previewThumb = proj.slides[0]?.originalUrl;

            return (
              <div
                key={proj.id}
                className={`bg-white rounded-3xl p-5 border transition-all duration-300 shadow-xs hover:shadow-md flex flex-col justify-between gap-4 group ${
                  isActive
                    ? 'border-[#1E88E5] ring-2 ring-blue-100'
                    : 'border-blue-100/80 hover:border-blue-200'
                }`}
              >
                {/* Project Header */}
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    {/* Thumbnail preview */}
                    <div
                      onClick={() => onSelectProject(proj.id)}
                      className="w-16 h-20 rounded-2xl overflow-hidden shrink-0 bg-zinc-900 shadow-xs cursor-pointer ring-1 ring-black/10 group-hover:scale-105 transition-transform"
                    >
                      {previewThumb ? (
                        <img
                          src={previewThumb}
                          alt={proj.title}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-zinc-500">
                          <ImageIcon className="w-6 h-6" />
                        </div>
                      )}
                    </div>

                    <div>
                      <div className="flex items-center gap-2">
                        <span
                          className={`text-[9px] font-black uppercase tracking-wider px-2 py-0.5 rounded-md ${
                            proj.mode === 'slideshow'
                              ? 'bg-blue-100 text-[#0D47A1]'
                              : 'bg-emerald-100 text-emerald-800'
                          }`}
                        >
                          {proj.mode === 'slideshow' ? 'Diaporama' : 'Image Unique'}
                        </span>
                        {isActive && (
                          <span className="text-[9px] font-bold bg-amber-100 text-amber-800 px-2 py-0.5 rounded-md">
                            Actif
                          </span>
                        )}
                      </div>

                      <h3
                        onClick={() => onSelectProject(proj.id)}
                        className="font-bold text-base text-[#0D47A1] mt-1 hover:text-[#1E88E5] cursor-pointer transition line-clamp-1"
                      >
                        {proj.title}
                      </h3>

                      <div className="flex items-center gap-2 mt-1 text-[11px] text-gray-500">
                        <span className="flex items-center gap-1">
                          <ImageIcon className="w-3 h-3 text-blue-500" />
                          <span>{proj.slides.length} images</span>
                        </span>
                        <span>•</span>
                        <span className="flex items-center gap-1">
                          <Clock className="w-3 h-3 text-amber-500" />
                          <span>{proj.defaultDurationSec}s par défaut</span>
                        </span>
                      </div>
                    </div>
                  </div>

                  {projects.length > 1 && (
                    <button
                      onClick={() => onDeleteProject(proj.id)}
                      className="p-1.5 rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 transition"
                      title="Supprimer ce projet"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>

                {/* Bottom Action Buttons */}
                <div className="pt-3 border-t border-gray-100 flex items-center justify-between gap-2">
                  <button
                    onClick={() => onSelectProject(proj.id)}
                    className="flex-1 py-2 px-3 rounded-xl bg-blue-50 hover:bg-blue-100 text-[#0D47A1] text-xs font-bold flex items-center justify-center gap-1 transition"
                  >
                    <span>Éditer le projet</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>

                  <button
                    onClick={() => onOpenLivePreview(proj)}
                    disabled={proj.slides.length === 0}
                    className="py-2 px-3.5 rounded-xl bg-[#1E88E5] hover:bg-[#1976D2] text-white text-xs font-bold flex items-center gap-1.5 transition shadow-xs disabled:opacity-40"
                    title="Lancer la prévisualisation animée"
                  >
                    <Play className="w-3 h-3 fill-current" />
                    <span>Lancer</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Modal: Choisir le mode (Diaporama ou Image unique) */}
      {showNewModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="bg-white rounded-3xl w-full max-w-md p-6 shadow-2xl border border-blue-100 space-y-5">
            <div>
              <h3 className="text-xl font-black text-[#0D47A1]">Nouveau Projet</h3>
              <p className="text-xs text-gray-500 mt-1">
                Choisis le format de fond d'écran que tu souhaites créer :
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {/* Option 1: Diaporama */}
              <button
                onClick={() => {
                  onCreateProject('slideshow');
                  setShowNewModal(false);
                }}
                className="p-4 rounded-2xl border-2 border-blue-100 hover:border-[#1E88E5] hover:bg-blue-50/50 transition flex flex-col items-center text-center space-y-2 group"
              >
                <div className="w-12 h-12 rounded-2xl bg-blue-100 text-[#1E88E5] flex items-center justify-center group-hover:scale-110 transition-transform">
                  <Layers className="w-6 h-6" />
                </div>
                <div>
                  <h4 className="font-bold text-sm text-[#0D47A1]">Diaporama</h4>
                  <p className="text-[11px] text-gray-500 mt-0.5">
                    Plusieurs images avec durée individuelle & musiques
                  </p>
                </div>
              </button>

              {/* Option 2: Image unique */}
              <button
                onClick={() => {
                  onCreateProject('single');
                  setShowNewModal(false);
                }}
                className="p-4 rounded-2xl border-2 border-blue-100 hover:border-emerald-500 hover:bg-emerald-50/50 transition flex flex-col items-center text-center space-y-2 group"
              >
                <div className="w-12 h-12 rounded-2xl bg-emerald-100 text-emerald-700 flex items-center justify-center group-hover:scale-110 transition-transform">
                  <ImageIcon className="w-6 h-6" />
                </div>
                <div>
                  <h4 className="font-bold text-sm text-[#0D47A1]">Image Unique</h4>
                  <p className="text-[11px] text-gray-500 mt-0.5">
                    Un seul fond ultra haute qualité avec filtres & son optionnel
                  </p>
                </div>
              </button>
            </div>

            <div className="pt-2 flex justify-end">
              <button
                onClick={() => setShowNewModal(false)}
                className="px-4 py-2 text-xs font-semibold text-gray-500 hover:bg-gray-100 rounded-xl"
              >
                Annuler
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

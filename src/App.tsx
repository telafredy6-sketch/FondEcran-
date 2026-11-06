import React, { useState, useEffect, useRef } from 'react';
import {
  PageId,
  SlideshowProject,
  SlideItem,
  AppConfig,
  ImageAdjustments,
  AudioClip,
} from './types';
import { INITIAL_WALLPAPERS } from './utils/sampleWallpapers';
import { PinterestWallpaper } from './utils/pinterestFeed';
import { HomePage } from './components/HomePage';
import { ProjectEditPage } from './components/ProjectEditPage';
import { PinterestFeedPage } from './components/PinterestFeedPage';
import { SystemSoundsPage } from './components/SystemSoundsPage';
import { SettingsPage } from './components/SettingsPage';
import { ImageEditorModal } from './components/ImageEditorModal';
import { AudioEditorModal } from './components/AudioEditorModal';
import { LiveWallpaperModal } from './components/LiveWallpaperModal';
import { SecurityGate } from './components/SecurityGate';
import {
  Home,
  Sliders,
  Sparkles,
  Bell,
  Settings,
  Layers,
  Palette,
  CheckCircle2,
  Lock,
  Smartphone,
  ChevronLeft,
} from 'lucide-react';

const INITIAL_PROJECTS: SlideshowProject[] = [
  {
    id: 'proj-1',
    title: 'Mon Diaporama Nature & Anime',
    description: 'Diaporama animé avec musiques ambiantes',
    mode: 'slideshow',
    globalTarget: 2, // Les deux
    defaultDurationSec: 10,
    createdAt: 1000,
    updatedAt: 1000,
    slides: INITIAL_WALLPAPERS.map((w, idx) => ({
      id: w.id,
      name: w.name,
      originalUrl: w.dataUrl,
      durationSec: idx === 0 ? 5 : idx === 1 ? 10 : 15,
      createdAt: w.createdAt,
      audio: w.music
        ? {
            id: `clip-${w.id}`,
            name: w.music.name,
            url: w.music.url,
            durationSec: 60,
            startTimeSec: 0,
            endTimeSec: 30,
            volume: 0.8,
            fadeIn: true,
            fadeOut: true,
            isSynth: true,
            synthType: w.music.synthType,
          }
        : undefined,
      adjustments: {
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
      },
    })),
  },
  {
    id: 'proj-2',
    title: 'Fond Unique Cyberpunk',
    description: 'Image unique néon haute définition',
    mode: 'single',
    globalTarget: 0, // Accueil
    defaultDurationSec: 30,
    createdAt: 2000,
    updatedAt: 2000,
    slides: [
      {
        id: 'cyber-1',
        name: 'Cyberpunk Skyline',
        originalUrl:
          'https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&w=1080&q=85',
        durationSec: 30,
        createdAt: 2000,
        audio: {
          id: 'clip-cyber',
          name: 'Lofi Chill Chords',
          url: '',
          durationSec: 60,
          startTimeSec: 0,
          endTimeSec: 25,
          volume: 0.8,
          fadeIn: true,
          fadeOut: true,
          isSynth: true,
          synthType: 'lofi',
        },
        adjustments: {
          zoom: 1,
          panX: 0,
          panY: 0,
          aspectRatio: '9:16',
          brightness: 105,
          contrast: 125,
          saturation: 140,
          hueRotate: 0,
          blur: 0,
          filterPreset: 'vivid',
          bgColor: '#000000',
        },
      },
    ],
  },
];

export function App() {
  // Navigation
  const [currentPage, setCurrentPage] = useState<PageId>('home');

  // Multi-project State
  const [projects, setProjects] = useState<SlideshowProject[]>(() => {
    const saved = localStorage.getItem('fond_ecran_projects_v2');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {}
    }
    return INITIAL_PROJECTS;
  });

  const [activeProjectId, setActiveProjectId] = useState<string>(() => {
    return projects[0]?.id || 'proj-1';
  });

  // App Global Config
  const [appConfig, setAppConfig] = useState<AppConfig>(() => {
    const saved = localStorage.getItem('fond_ecran_app_config');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {}
    }
    return {
      appWallpaperUrl: null,
      appWallpaperBlur: 10,
      appWallpaperOpacity: 65,
      securityPinEnabled: false,
      securityPin: '1234',
      exportQuality: 'high',
    };
  });

  // Security gate
  const [isLocked, setIsLocked] = useState<boolean>(() => {
    return appConfig.securityPinEnabled;
  });

  // Modals
  const [editingSlideForImage, setEditingSlideForImage] = useState<SlideItem | null>(null);
  const [editingSlideForAudio, setEditingSlideForAudio] = useState<SlideItem | null>(null);
  const [livePreviewProject, setLivePreviewProject] = useState<SlideshowProject | null>(null);

  // Toast
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const toastTimeoutRef = useRef<number | null>(null);

  // Persistence
  useEffect(() => {
    localStorage.setItem('fond_ecran_projects_v2', JSON.stringify(projects));
  }, [projects]);

  useEffect(() => {
    localStorage.setItem('fond_ecran_app_config', JSON.stringify(appConfig));
  }, [appConfig]);

  const showToast = (msg: string) => {
    if (toastTimeoutRef.current) clearTimeout(toastTimeoutRef.current);
    setToastMessage(msg);
    toastTimeoutRef.current = window.setTimeout(() => {
      setToastMessage(null);
    }, 2800);
  };

  const activeProject = projects.find((p) => p.id === activeProjectId) || projects[0];

  // Project Handlers
  const handleUpdateProject = (updated: SlideshowProject) => {
    setProjects((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
  };

  const handleCreateProject = (mode: 'slideshow' | 'single') => {
    const newProj: SlideshowProject = {
      id: `proj-${Date.now()}`,
      title: mode === 'slideshow' ? 'Nouveau Diaporama' : 'Nouvelle Image Unique',
      description: 'Projet créé récemment',
      mode,
      slides: [],
      globalTarget: 2,
      defaultDurationSec: 15,
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };

    setProjects((prev) => [newProj, ...prev]);
    setActiveProjectId(newProj.id);
    setCurrentPage('project-edit');
    showToast(`Projet « ${newProj.title} » créé`);
  };

  const handleDeleteProject = (id: string) => {
    if (projects.length <= 1) {
      showToast('Impossible de supprimer le dernier projet');
      return;
    }
    setProjects((prev) => prev.filter((p) => p.id !== id));
    if (activeProjectId === id) {
      const remaining = projects.filter((p) => p.id !== id);
      setActiveProjectId(remaining[0].id);
    }
    showToast('Projet supprimé');
  };

  // Image editing callback
  const handleSaveImageAdjustments = (newAdj: ImageAdjustments) => {
    if (!editingSlideForImage || !activeProject) return;

    const updatedSlides = activeProject.slides.map((s) =>
      s.id === editingSlideForImage.id ? { ...s, adjustments: newAdj } : s
    );

    handleUpdateProject({
      ...activeProject,
      slides: updatedSlides,
      updatedAt: Date.now(),
    });
    showToast('Ajustements image enregistrés');
  };

  // Audio editing callback
  const handleSaveAudioClip = (clip: AudioClip) => {
    if (!editingSlideForAudio || !activeProject) return;

    const updatedSlides = activeProject.slides.map((s) =>
      s.id === editingSlideForAudio.id ? { ...s, audio: clip } : s
    );

    handleUpdateProject({
      ...activeProject,
      slides: updatedSlides,
      updatedAt: Date.now(),
    });
    showToast(`Son associé : ${clip.name}`);
  };

  // Pinterest Add to Current Project
  const handleAddPinterestWallpaper = (item: PinterestWallpaper) => {
    if (!activeProject) return;

    const newSlide: SlideItem = {
      id: `pin-${Date.now()}`,
      name: item.title,
      originalUrl: item.imageUrl,
      durationSec: activeProject.defaultDurationSec || 15,
      createdAt: Date.now(),
      adjustments: {
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
      },
    };

    handleUpdateProject({
      ...activeProject,
      slides: [...activeProject.slides, newSlide],
      updatedAt: Date.now(),
    });
    showToast(`« ${item.title} » ajouté à ${activeProject.title}`);
  };

  // Set App Background
  const handleSetAppWallpaper = (url: string) => {
    setAppConfig((prev) => ({
      ...prev,
      appWallpaperUrl: url,
    }));
    showToast("Fond d'écran appliqué dans l'application !");
  };

  return (
    <div className="min-h-screen relative overflow-x-hidden font-sans">
      {/* Dynamic App Wallpaper Background */}
      {appConfig.appWallpaperUrl && (
        <div className="fixed inset-0 z-0 pointer-events-none overflow-hidden">
          <img
            src={appConfig.appWallpaperUrl}
            alt="Fond App"
            className="w-full h-full object-cover transition-all duration-700"
            style={{
              filter: `blur(${appConfig.appWallpaperBlur}px)`,
              opacity: appConfig.appWallpaperOpacity / 100,
            }}
          />
          <div className="absolute inset-0 bg-[#E3F2FD]/80 backdrop-blur-xs" />
        </div>
      )}

      {/* Security Gate Pin Prompt if enabled */}
      {isLocked && (
        <SecurityGate
          correctPin={appConfig.securityPin}
          onUnlock={() => setIsLocked(false)}
        />
      )}

      {/* App Shell Header */}
      <header className="sticky top-0 z-40 bg-[#BBDEFB]/90 backdrop-blur-md border-b border-blue-200/70 shadow-xs px-4 py-3">
        <div className="max-w-5xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-3">
            {currentPage !== 'home' && (
              <button
                onClick={() => setCurrentPage('home')}
                className="p-1.5 rounded-xl hover:bg-blue-200/70 text-[#0D47A1] transition"
                title="Retour à l'accueil"
              >
                <ChevronLeft className="w-5 h-5" />
              </button>
            )}

            <div
              onClick={() => setCurrentPage('home')}
              className="flex items-center gap-2 cursor-pointer"
            >
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-[#0D47A1] to-[#1E88E5] text-white flex items-center justify-center font-black text-sm shadow-sm">
                FE
              </div>
              <div>
                <span className="font-extrabold text-base text-[#0D47A1] tracking-tight">
                  Fond d'écran
                </span>
                <span className="text-[10px] bg-[#1E88E5] text-white font-bold px-1.5 py-0.5 rounded-md ml-2">
                  Studio
                </span>
              </div>
            </div>
          </div>

          {/* Navigation Tabs (Desktop & Tablet) */}
          <nav className="hidden md:flex items-center gap-1 bg-white/70 backdrop-blur-md p-1 rounded-2xl border border-blue-200/60 shadow-xs">
            {[
              { id: 'home', label: 'Accueil', icon: Home },
              { id: 'project-edit', label: 'Édition Projet', icon: Sliders },
              { id: 'pinterest-feed', label: 'Pinterest', icon: Sparkles },
              { id: 'system-sounds', label: 'Sons Système', icon: Bell },
              { id: 'settings', label: 'Paramètres', icon: Settings },
            ].map((tab) => {
              const Icon = tab.icon;
              const isActive = currentPage === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setCurrentPage(tab.id as PageId)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 transition ${
                    isActive
                      ? 'bg-[#1E88E5] text-white shadow-xs'
                      : 'text-[#0D47A1] hover:bg-blue-100/70'
                  }`}
                >
                  <Icon className="w-3.5 h-3.5" />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </nav>

          {/* Header Action: Quick Preview of Active Project */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => setLivePreviewProject(activeProject)}
              disabled={activeProject.slides.length === 0}
              className="px-3 py-1.5 rounded-xl bg-[#1E88E5] hover:bg-[#1976D2] active:scale-95 text-white text-xs font-bold flex items-center gap-1.5 transition shadow-xs disabled:opacity-40"
              title="Prévisualiser dans le smartphone"
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Aperçu Téléphone</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="relative z-10 max-w-5xl mx-auto px-4 pt-6 pb-24 md:pb-12">
        {currentPage === 'home' && (
          <HomePage
            projects={projects}
            activeProjectId={activeProjectId}
            onSelectProject={(id) => {
              setActiveProjectId(id);
              setCurrentPage('project-edit');
            }}
            onCreateProject={handleCreateProject}
            onDeleteProject={handleDeleteProject}
            onOpenLivePreview={(proj) => setLivePreviewProject(proj)}
            onNavigatePinterest={() => setCurrentPage('pinterest-feed')}
          />
        )}

        {currentPage === 'project-edit' && (
          <ProjectEditPage
            project={activeProject}
            onUpdateProject={handleUpdateProject}
            onOpenImageEditor={(slide) => setEditingSlideForImage(slide)}
            onOpenAudioEditor={(slide) => setEditingSlideForAudio(slide)}
            onOpenLivePreview={() => setLivePreviewProject(activeProject)}
            onSetAsAppWallpaper={handleSetAppWallpaper}
            showToast={showToast}
          />
        )}

        {currentPage === 'pinterest-feed' && (
          <PinterestFeedPage
            onAddToCurrentProject={handleAddPinterestWallpaper}
            onSetAsAppWallpaper={handleSetAppWallpaper}
            onOpenLivePreview={(w) => {
              const tempProj: SlideshowProject = {
                id: 'temp-pin',
                title: w.title,
                description: 'Aperçu Pinterest',
                mode: 'single',
                globalTarget: 2,
                defaultDurationSec: 15,
                createdAt: Date.now(),
                updatedAt: Date.now(),
                slides: [
                  {
                    id: w.id,
                    name: w.title,
                    originalUrl: w.imageUrl,
                    durationSec: 15,
                    createdAt: Date.now(),
                    adjustments: {
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
                    },
                  },
                ],
              };
              setLivePreviewProject(tempProj);
            }}
          />
        )}

        {currentPage === 'system-sounds' && <SystemSoundsPage />}

        {currentPage === 'settings' && (
          <SettingsPage
            config={appConfig}
            onUpdateConfig={(partial) => setAppConfig((prev) => ({ ...prev, ...partial }))}
            onResetAppWallpaper={() =>
              setAppConfig((prev) => ({ ...prev, appWallpaperUrl: null }))
            }
            showToast={showToast}
          />
        )}
      </main>

      {/* Mobile Bottom Navigation Bar */}
      <div className="fixed bottom-0 inset-x-0 z-40 bg-white/90 backdrop-blur-lg border-t border-blue-200/70 p-2 flex items-center justify-around md:hidden shadow-lg">
        {[
          { id: 'home', label: 'Accueil', icon: Home },
          { id: 'project-edit', label: 'Édition', icon: Sliders },
          { id: 'pinterest-feed', label: 'Pinterest', icon: Sparkles },
          { id: 'system-sounds', label: 'Sons', icon: Bell },
          { id: 'settings', label: 'Réglages', icon: Settings },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = currentPage === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setCurrentPage(tab.id as PageId)}
              className={`flex flex-col items-center py-1 px-3 rounded-xl transition ${
                isActive ? 'text-[#1E88E5] font-bold' : 'text-gray-500 hover:text-black'
              }`}
            >
              <Icon className="w-5 h-5" />
              <span className="text-[10px] mt-0.5">{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Floating Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-20 md:bottom-6 inset-x-4 max-w-sm mx-auto z-50 flex items-center justify-center pointer-events-none">
          <div className="bg-[#24292e] text-white text-xs sm:text-sm font-semibold px-5 py-2.5 rounded-full shadow-2xl flex items-center gap-2 animate-fade-in border border-zinc-700">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>{toastMessage}</span>
          </div>
        </div>
      )}

      {/* Image Editor Modal */}
      {editingSlideForImage && (
        <ImageEditorModal
          slide={editingSlideForImage}
          onSave={handleSaveImageAdjustments}
          onClose={() => setEditingSlideForImage(null)}
          onSetAsAppWallpaper={handleSetAppWallpaper}
        />
      )}

      {/* Audio Editor Modal */}
      {editingSlideForAudio && (
        <AudioEditorModal
          initialClip={editingSlideForAudio.audio}
          targetTitle={editingSlideForAudio.name}
          onSave={handleSaveAudioClip}
          onClose={() => setEditingSlideForAudio(null)}
        />
      )}

      {/* Full Live Wallpaper Phone Simulation Modal */}
      {livePreviewProject && (
        <LiveWallpaperModal
          project={livePreviewProject}
          onClose={() => setLivePreviewProject(null)}
          onSetAsAppWallpaper={handleSetAppWallpaper}
        />
      )}
    </div>
  );
}

export default App;

import React, { useRef } from 'react';
import { SlideshowProject, SlideItem, WallpaperTarget } from '../types';
import { centerCropImage } from '../utils/imageCropper';
import {
  Clock,
  Music,
  Crop,
  Play,
  Plus,
  Trash2,
  ArrowUp,
  ArrowDown,
  Sparkles,
  Smartphone,
  Eye,
  Sliders,
  Settings,
  Download,
  Palette,
  Volume2,
} from 'lucide-react';

interface ProjectEditPageProps {
  project: SlideshowProject;
  onUpdateProject: (updated: SlideshowProject) => void;
  onOpenImageEditor: (slide: SlideItem) => void;
  onOpenAudioEditor: (slide: SlideItem) => void;
  onOpenLivePreview: () => void;
  onSetAsAppWallpaper: (imageUrl: string) => void;
  showToast: (msg: string) => void;
}

const DURATION_PRESETS = [
  { label: '5 sec', sec: 5 },
  { label: '10 sec', sec: 10 },
  { label: '30 sec', sec: 30 },
  { label: '1 min', sec: 60 },
  { label: '5 min', sec: 300 },
  { label: '15 min', sec: 900 },
  { label: '1 h', sec: 3600 },
];

export const ProjectEditPage: React.FC<ProjectEditPageProps> = ({
  project,
  onUpdateProject,
  onOpenImageEditor,
  onOpenAudioEditor,
  onOpenLivePreview,
  onSetAsAppWallpaper,
  showToast,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleTitleChange = (newTitle: string) => {
    onUpdateProject({
      ...project,
      title: newTitle,
      updatedAt: Date.now(),
    });
  };

  const handleTargetChange = (target: WallpaperTarget) => {
    onUpdateProject({
      ...project,
      globalTarget: target,
      updatedAt: Date.now(),
    });
  };

  const handleAddImages = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    showToast('Importation et recadrage…');
    const newSlides: SlideItem[] = [];

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      try {
        const cropped = await centerCropImage(file);
        newSlides.push({
          id: `slide-${Date.now()}-${i}`,
          name: file.name.replace(/\.[^/.]+$/, ''),
          originalUrl: cropped,
          durationSec: project.defaultDurationSec || 15,
          createdAt: Date.now() + i,
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
        });
      } catch (err) {
        console.error(err);
      }
    }

    if (newSlides.length > 0) {
      onUpdateProject({
        ...project,
        slides: [...project.slides, ...newSlides],
        updatedAt: Date.now(),
      });
      showToast(`${newSlides.length} image(s) ajoutée(s)`);
    }

    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleMoveSlide = (index: number, direction: 'up' | 'down') => {
    const targetIdx = direction === 'up' ? index - 1 : index + 1;
    if (targetIdx < 0 || targetIdx >= project.slides.length) return;

    const copy = [...project.slides];
    const [moved] = copy.splice(index, 1);
    copy.splice(targetIdx, 0, moved);

    onUpdateProject({
      ...project,
      slides: copy,
      updatedAt: Date.now(),
    });
  };

  const handleDeleteSlide = (index: number) => {
    const copy = project.slides.filter((_, i) => i !== index);
    onUpdateProject({
      ...project,
      slides: copy,
      updatedAt: Date.now(),
    });
    showToast('Image retirée du projet');
  };

  const handleUpdateSlideDuration = (index: number, durationSec: number) => {
    const copy = [...project.slides];
    copy[index] = { ...copy[index], durationSec };
    onUpdateProject({
      ...project,
      slides: copy,
      updatedAt: Date.now(),
    });
  };

  return (
    <div className="space-y-6 pb-16">
      {/* Top Banner and Quick Launch */}
      <div className="bg-white/80 backdrop-blur-md p-6 rounded-3xl border border-blue-100 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="space-y-1 flex-1">
          <div className="flex items-center gap-2">
            <span
              className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                project.mode === 'slideshow'
                  ? 'bg-blue-100 text-[#0D47A1]'
                  : 'bg-emerald-100 text-emerald-800'
              }`}
            >
              Mode {project.mode === 'slideshow' ? 'Diaporama Multilmages' : 'Image Unique'}
            </span>
            <span className="text-xs text-gray-400">•</span>
            <span className="text-xs text-gray-500 font-medium">
              {project.slides.length} image(s)
            </span>
          </div>

          <input
            type="text"
            value={project.title}
            onChange={(e) => handleTitleChange(e.target.value)}
            className="text-2xl font-black text-[#0D47A1] bg-transparent border-b border-transparent hover:border-blue-200 focus:border-[#1E88E5] focus:outline-hidden w-full transition"
            placeholder="Titre du projet..."
          />
        </div>

        {/* Global Action Buttons */}
        <div className="flex items-center gap-2.5 shrink-0 flex-wrap">
          <button
            onClick={onOpenLivePreview}
            disabled={project.slides.length === 0}
            className="py-3 px-5 rounded-2xl bg-[#1E88E5] hover:bg-[#1976D2] active:scale-95 text-white font-bold text-sm flex items-center gap-2 transition shadow-md disabled:opacity-50"
          >
            <Play className="w-4 h-4 fill-current" />
            <span>{project.mode === 'slideshow' ? 'Lancer Diaporama' : 'Prévisualiser'}</span>
          </button>
        </div>
      </div>

      {/* Global Project Settings */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Destination Target */}
        <div className="bg-white/70 p-5 rounded-2xl border border-blue-100 shadow-xs space-y-2">
          <label className="text-xs font-bold text-[#0D47A1] uppercase tracking-wider block">
            Appliquer le fond sur :
          </label>
          <div className="grid grid-cols-3 gap-2">
            {[
              { id: 0, label: "Accueil" },
              { id: 1, label: "Verrouillage" },
              { id: 2, label: "Les deux" },
            ].map((t) => (
              <button
                key={t.id}
                onClick={() => handleTargetChange(t.id as WallpaperTarget)}
                className={`py-2 px-1 text-xs font-semibold rounded-xl border transition text-center ${
                  project.globalTarget === t.id
                    ? 'border-[#1E88E5] bg-blue-50 text-[#1E88E5] ring-2 ring-blue-100'
                    : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>
        </div>

        {/* Default Interval for new slides */}
        {project.mode === 'slideshow' && (
          <div className="bg-white/70 p-5 rounded-2xl border border-blue-100 shadow-xs space-y-2">
            <label className="text-xs font-bold text-[#0D47A1] uppercase tracking-wider block">
              Durée par défaut :
            </label>
            <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none">
              {DURATION_PRESETS.map((p) => (
                <button
                  key={p.sec}
                  onClick={() =>
                    onUpdateProject({
                      ...project,
                      defaultDurationSec: p.sec,
                      updatedAt: Date.now(),
                    })
                  }
                  className={`px-3 py-1.5 text-xs font-semibold rounded-xl border whitespace-nowrap transition ${
                    project.defaultDurationSec === p.sec
                      ? 'border-[#1E88E5] bg-blue-50 text-[#1E88E5] font-bold'
                      : 'border-gray-200 text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  {p.label}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Slide Images List */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-[#0D47A1]">
            Images & Réglages Individuels ({project.slides.length})
          </h2>

          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept="image/*"
            className="hidden"
            onChange={handleAddImages}
          />

          <button
            onClick={() => fileInputRef.current?.click()}
            className="py-2 px-3.5 rounded-xl bg-white hover:bg-blue-50 text-[#1E88E5] border border-blue-200 text-xs font-bold flex items-center gap-1.5 transition shadow-xs"
          >
            <Plus className="w-4 h-4" />
            <span>Ajouter des images</span>
          </button>
        </div>

        {project.slides.length === 0 ? (
          <div className="p-12 text-center bg-white/70 rounded-3xl border-2 border-dashed border-blue-200 space-y-3">
            <Smartphone className="w-12 h-12 text-[#1E88E5] mx-auto opacity-70" />
            <h3 className="font-bold text-base text-[#0D47A1]">Aucune image dans ce projet</h3>
            <p className="text-xs text-gray-500 max-w-sm mx-auto">
              Ajoute des images depuis ton appareil ou choisis parmi la sélection Pinterest.
            </p>
            <button
              onClick={() => fileInputRef.current?.click()}
              className="py-2.5 px-5 rounded-xl bg-[#1E88E5] text-white font-bold text-xs inline-flex items-center gap-1.5 shadow-xs"
            >
              <Plus className="w-4 h-4" />
              <span>Choisir des images</span>
            </button>
          </div>
        ) : (
          <div className="space-y-3">
            {project.slides.map((slide, index) => {
              const cssFilter = `
                brightness(${slide.adjustments.brightness}%)
                contrast(${slide.adjustments.contrast}%)
                saturate(${slide.adjustments.saturation}%)
                hue-rotate(${slide.adjustments.hueRotate}deg)
                blur(${slide.adjustments.blur}px)
              `;

              return (
                <div
                  key={slide.id}
                  className="bg-white rounded-2xl p-3.5 sm:p-4 shadow-sm border border-blue-100 flex flex-col md:flex-row md:items-center justify-between gap-4 transition hover:border-blue-300"
                >
                  {/* Left: Thumbnail & Name */}
                  <div className="flex items-center gap-3 min-w-0">
                    {/* Reorder arrows */}
                    {project.mode === 'slideshow' && (
                      <div className="flex flex-col gap-1 text-gray-400 shrink-0">
                        <button
                          onClick={() => handleMoveSlide(index, 'up')}
                          disabled={index === 0}
                          className="hover:text-[#1E88E5] disabled:opacity-20 p-0.5"
                          title="Monter"
                        >
                          <ArrowUp className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => handleMoveSlide(index, 'down')}
                          disabled={index === project.slides.length - 1}
                          className="hover:text-[#1E88E5] disabled:opacity-20 p-0.5"
                          title="Descendre"
                        >
                          <ArrowDown className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    )}

                    {/* Thumbnail with custom filter applied */}
                    <div
                      onClick={() => onOpenImageEditor(slide)}
                      className="w-16 h-20 rounded-xl overflow-hidden shrink-0 cursor-pointer relative group bg-black shadow-xs ring-1 ring-black/10"
                    >
                      <img
                        src={slide.originalUrl}
                        alt={slide.name}
                        className="w-full h-full object-cover select-none"
                        style={{
                          transform: `scale(${slide.adjustments.zoom})`,
                          filter: cssFilter,
                        }}
                      />
                      <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition">
                        <Crop className="w-4 h-4 text-white" />
                      </div>
                      <span className="absolute top-1 left-1 bg-black/60 text-white font-mono text-[9px] px-1 rounded-sm">
                        #{index + 1}
                      </span>
                    </div>

                    <div className="min-w-0 flex-1">
                      <h4 className="font-bold text-sm text-[#0D47A1] truncate">
                        {slide.name}
                      </h4>

                      {/* Music badge */}
                      <div className="flex items-center gap-2 mt-1">
                        {slide.audio ? (
                          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md">
                            <Volume2 className="w-3 h-3" />
                            <span className="truncate max-w-[120px]">{slide.audio.name}</span>
                            <span className="text-[9px] text-emerald-600">
                              ({slide.audio.endTimeSec - slide.audio.startTimeSec}s)
                            </span>
                          </span>
                        ) : (
                          <span className="text-[11px] text-gray-400">Aucun son associé</span>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Right: Individual Controls (Duration, Sound, Edit, Delete) */}
                  <div className="flex items-center gap-2 shrink-0 flex-wrap justify-end pt-2 md:pt-0 border-t md:border-t-0 border-gray-100">
                    {/* Individual Duration Selector */}
                    {project.mode === 'slideshow' && (
                      <div className="flex items-center gap-1.5 bg-blue-50/80 px-2.5 py-1.5 rounded-xl border border-blue-100">
                        <Clock className="w-3.5 h-3.5 text-blue-600 shrink-0" />
                        <select
                          value={slide.durationSec}
                          onChange={(e) =>
                            handleUpdateSlideDuration(index, parseInt(e.target.value))
                          }
                          className="bg-transparent text-xs font-bold text-[#0D47A1] focus:outline-hidden cursor-pointer"
                        >
                          {DURATION_PRESETS.map((p) => (
                            <option key={p.sec} value={p.sec}>
                              {p.label}
                            </option>
                          ))}
                        </select>
                      </div>
                    )}

                    {/* Son individuel button */}
                    <button
                      onClick={() => onOpenAudioEditor(slide)}
                      className={`py-1.5 px-3 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition ${
                        slide.audio
                          ? 'bg-emerald-100 text-emerald-800 hover:bg-emerald-200'
                          : 'bg-blue-50 text-[#1E88E5] hover:bg-blue-100'
                      }`}
                      title="Éditer le son / découper"
                    >
                      <Music className="w-3.5 h-3.5" />
                      <span className="hidden sm:inline">
                        {slide.audio ? 'Modifier Son' : 'Ajouter Son'}
                      </span>
                    </button>

                    {/* Recadrer / Modifier button */}
                    <button
                      onClick={() => onOpenImageEditor(slide)}
                      className="py-1.5 px-3 rounded-xl bg-blue-50 hover:bg-blue-100 text-[#0D47A1] text-xs font-semibold flex items-center gap-1.5 transition"
                      title="Recadrer, filtres et couleurs"
                    >
                      <Crop className="w-3.5 h-3.5 text-[#1E88E5]" />
                      <span>Modifier</span>
                    </button>

                    {/* Set as App wallpaper */}
                    <button
                      onClick={() => {
                        onSetAsAppWallpaper(slide.originalUrl);
                        showToast("Défini comme fond d'écran de l'application !");
                      }}
                      className="p-2 rounded-xl hover:bg-blue-50 text-[#1E88E5] transition"
                      title="Mettre en fond de l'application"
                    >
                      <Palette className="w-4 h-4" />
                    </button>

                    {/* Delete slide */}
                    <button
                      onClick={() => handleDeleteSlide(index)}
                      className="p-2 rounded-xl hover:bg-red-50 text-red-500 hover:text-red-700 transition"
                      title="Supprimer l'image"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

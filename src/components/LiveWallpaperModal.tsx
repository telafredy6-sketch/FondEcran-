import React, { useEffect, useState, useRef } from 'react';
import { SlideshowProject, SlideItem } from '../types';
import { soundEngine } from '../utils/audioEngine';
import {
  X,
  Volume2,
  VolumeX,
  Play,
  Pause,
  Smartphone,
  Download,
  SkipForward,
  SkipBack,
  Clock,
  Sparkles,
  Lock,
} from 'lucide-react';

interface LiveWallpaperModalProps {
  project: SlideshowProject;
  onClose: () => void;
  onSetAsAppWallpaper?: (url: string) => void;
}

export const LiveWallpaperModal: React.FC<LiveWallpaperModalProps> = ({
  project,
  onClose,
  onSetAsAppWallpaper,
}) => {
  const [slideIndex, setSlideIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const [soundMuted, setSoundMuted] = useState(false);
  const [showPhoneFrame, setShowPhoneFrame] = useState(true);
  const [currentTime, setCurrentTime] = useState(new Date());
  const [countdownSec, setCountdownSec] = useState<number>(0);

  const slides = project.slides;
  const currentSlide: SlideItem | undefined = slides[slideIndex] || slides[0];

  // Clock updater
  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  // Individual slide duration countdown & auto slide progression
  useEffect(() => {
    if (!currentSlide || isPaused || slides.length <= 1) return;

    const duration = currentSlide.durationSec || project.defaultDurationSec || 15;
    setCountdownSec(duration);

    const countdownTimer = setInterval(() => {
      setCountdownSec((prev) => {
        if (prev <= 1) {
          setSlideIndex((idx) => (idx + 1) % slides.length);
          return duration;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(countdownTimer);
  }, [slideIndex, isPaused, slides.length, currentSlide, project.defaultDurationSec]);

  // Audio synchronization for current slide
  useEffect(() => {
    if (!currentSlide) return;

    if (!soundMuted && currentSlide.audio) {
      soundEngine.playClip(currentSlide.audio, true);
    } else {
      soundEngine.stop();
    }

    return () => {
      soundEngine.stop();
    };
  }, [slideIndex, soundMuted, currentSlide]);

  const handleNext = () => {
    setSlideIndex((prev) => (prev + 1) % slides.length);
  };

  const handlePrev = () => {
    setSlideIndex((prev) => (prev - 1 + slides.length) % slides.length);
  };

  const handleDownload = () => {
    if (!currentSlide) return;
    const a = document.createElement('a');
    a.href = currentSlide.originalUrl;
    a.download = `${currentSlide.name || 'fond-ecran'}.jpg`;
    a.click();
  };

  const targetLabel =
    project.globalTarget === 0
      ? "Écran d'accueil"
      : project.globalTarget === 1
      ? "Écran de verrouillage"
      : "Accueil & Verrouillage";

  const cssFilter = currentSlide
    ? `
      brightness(${currentSlide.adjustments.brightness}%)
      contrast(${currentSlide.adjustments.contrast}%)
      saturate(${currentSlide.adjustments.saturation}%)
      hue-rotate(${currentSlide.adjustments.hueRotate}deg)
      blur(${currentSlide.adjustments.blur}px)
    `
    : 'none';

  return (
    <div className="fixed inset-0 z-50 bg-black/95 backdrop-blur-md flex flex-col items-center justify-center p-3 select-none">
      {/* Top Floating Controls */}
      <div className="absolute top-4 inset-x-4 max-w-4xl mx-auto flex items-center justify-between z-30 bg-black/60 backdrop-blur-lg py-2.5 px-5 rounded-full text-white border border-white/10 shadow-2xl">
        <div className="flex items-center gap-3">
          <div className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
          <div className="flex items-center gap-2">
            <span className="font-bold text-sm tracking-wide">{project.title}</span>
            <span className="text-xs text-white/60 hidden sm:inline">({targetLabel})</span>
          </div>
        </div>

        <div className="flex items-center gap-1 sm:gap-2">
          {/* Individual Duration countdown pill */}
          {slides.length > 1 && (
            <div className="flex items-center gap-1 bg-white/10 px-2.5 py-1 rounded-full text-[11px] font-mono text-amber-300">
              <Clock className="w-3 h-3" />
              <span>{countdownSec}s</span>
            </div>
          )}

          {/* Sound toggle */}
          <button
            onClick={() => setSoundMuted(!soundMuted)}
            title={soundMuted ? 'Activer le son' : 'Couper le son'}
            className="p-2 rounded-full hover:bg-white/15 text-white transition"
          >
            {soundMuted ? <VolumeX className="w-4 h-4 text-rose-400" /> : <Volume2 className="w-4 h-4 text-emerald-400" />}
          </button>

          {/* Pause / Play */}
          {slides.length > 1 && (
            <button
              onClick={() => setIsPaused(!isPaused)}
              title={isPaused ? 'Reprendre' : 'Pause'}
              className="p-2 rounded-full hover:bg-white/15 text-white transition"
            >
              {isPaused ? <Play className="w-4 h-4" /> : <Pause className="w-4 h-4" />}
            </button>
          )}

          {/* Toggle Phone Frame */}
          <button
            onClick={() => setShowPhoneFrame(!showPhoneFrame)}
            title="Basculer format smartphone / plein écran"
            className="p-2 rounded-full hover:bg-white/15 text-white transition"
          >
            <Smartphone className="w-4 h-4" />
          </button>

          {/* Download */}
          <button
            onClick={handleDownload}
            title="Télécharger l'image courante"
            className="p-2 rounded-full hover:bg-white/15 text-white transition"
          >
            <Download className="w-4 h-4" />
          </button>

          {/* Close */}
          <button
            onClick={onClose}
            className="p-2 rounded-full bg-white/20 hover:bg-white/30 text-white transition ml-1"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Main Wallpaper Display Area */}
      <div className="relative w-full h-full flex items-center justify-center pt-14 pb-16">
        {showPhoneFrame ? (
          // Realistic Smartphone Mockup Frame
          <div className="relative w-full max-w-[340px] sm:max-w-[360px] aspect-[9/19] rounded-[48px] p-3.5 bg-[#161a20] shadow-[0_25px_60px_-15px_rgba(0,0,0,0.9)] ring-1 ring-white/20 flex flex-col overflow-hidden">
            {/* Dynamic Island / Notch */}
            <div className="absolute top-4 inset-x-0 mx-auto w-24 h-4 bg-black rounded-full z-40 flex items-center justify-center">
              <div className="w-2.5 h-2.5 rounded-full bg-zinc-900 ml-auto mr-3.5" />
            </div>

            {/* Screen Viewport */}
            <div
              className="relative w-full h-full rounded-[38px] overflow-hidden flex flex-col"
              style={{ backgroundColor: currentSlide?.adjustments.bgColor || '#000000' }}
            >
              {currentSlide && (
                <img
                  src={currentSlide.originalUrl}
                  alt={currentSlide.name}
                  className="absolute inset-0 w-full h-full object-cover transition-all duration-700 select-none"
                  style={{
                    transform: `scale(${currentSlide.adjustments.zoom}) translate(${currentSlide.adjustments.panX}px, ${currentSlide.adjustments.panY}px)`,
                    filter: cssFilter,
                  }}
                />
              )}

              {/* Gradient Scrim */}
              <div className="absolute inset-0 bg-gradient-to-b from-black/40 via-transparent to-black/70 pointer-events-none" />

              {/* Status bar */}
              <div className="relative z-10 pt-3 px-6 flex justify-between items-center text-white/90 text-xs font-semibold">
                <span>
                  {currentTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </span>
                <div className="flex items-center gap-1.5">
                  <span className="text-[10px]">5G</span>
                  <span>100%</span>
                </div>
              </div>

              {/* Lockscreen Info & Time */}
              <div className="relative z-10 flex-1 flex flex-col items-center justify-between py-10 px-4 text-white text-center">
                <div className="mt-4">
                  <div className="text-5xl font-light tracking-tight text-white drop-shadow-md">
                    {currentTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </div>
                  <div className="text-xs uppercase tracking-widest mt-1.5 text-white/80 font-medium drop-shadow-xs">
                    {currentTime.toLocaleDateString('fr-FR', {
                      weekday: 'long',
                      day: 'numeric',
                      month: 'long',
                    })}
                  </div>
                </div>

                {/* Music pill if slide has audio */}
                {currentSlide?.audio && (
                  <div className="bg-black/50 backdrop-blur-md px-4 py-2.5 rounded-2xl border border-white/15 flex items-center gap-2.5 max-w-[240px] shadow-lg animate-fade-in">
                    <div className="w-7 h-7 rounded-full bg-[#1E88E5] flex items-center justify-center text-white shrink-0">
                      <Volume2 className="w-3.5 h-3.5" />
                    </div>
                    <div className="text-left truncate">
                      <div className="text-[11px] font-bold text-white truncate">
                        {currentSlide.audio.name}
                      </div>
                      <div className="text-[9px] text-white/70">
                        {soundMuted ? 'Son coupé' : 'Extrait découpé synchronisé'}
                      </div>
                    </div>
                  </div>
                )}

                {/* Bottom unlock swipe indicator */}
                <div className="space-y-1">
                  <div className="w-28 h-1 bg-white/70 rounded-full mx-auto" />
                  <div className="text-[10px] text-white/60 tracking-wider">
                    Glisser pour déverrouiller
                  </div>
                </div>
              </div>
            </div>
          </div>
        ) : (
          // Fullscreen Mode
          <div className="relative w-full h-full max-w-5xl rounded-3xl overflow-hidden shadow-2xl flex items-center justify-center bg-black">
            {currentSlide && (
              <img
                src={currentSlide.originalUrl}
                alt={currentSlide.name}
                className="w-full h-full object-contain"
                style={{ filter: cssFilter }}
              />
            )}
          </div>
        )}
      </div>

      {/* Bottom Floating Navigation bar */}
      {slides.length > 1 && (
        <div className="absolute bottom-4 inset-x-4 max-w-xs mx-auto flex items-center justify-between bg-black/60 backdrop-blur-lg py-2 px-5 rounded-full text-white border border-white/10 z-30 shadow-xl">
          <button
            onClick={handlePrev}
            className="p-1 rounded-full hover:bg-white/10 text-white transition"
            title="Précédente"
          >
            <SkipBack className="w-4 h-4" />
          </button>
          <span className="text-xs text-white/80 font-mono font-semibold">
            {slideIndex + 1} / {slides.length}
          </span>
          <button
            onClick={handleNext}
            className="p-1 rounded-full hover:bg-white/10 text-white transition"
            title="Suivante"
          >
            <SkipForward className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  );
};

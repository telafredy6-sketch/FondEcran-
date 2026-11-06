import React, { useState } from 'react';
import { AppConfig } from '../types';
import {
  Settings,
  Shield,
  Palette,
  Sliders,
  RotateCcw,
  Check,
  Lock,
  KeyRound,
  Download,
  Image as ImageIcon,
  Smartphone,
  Eye,
} from 'lucide-react';

interface SettingsPageProps {
  config: AppConfig;
  onUpdateConfig: (newConfig: Partial<AppConfig>) => void;
  onResetAppWallpaper: () => void;
  showToast: (msg: string) => void;
}

export const SettingsPage: React.FC<SettingsPageProps> = ({
  config,
  onUpdateConfig,
  onResetAppWallpaper,
  showToast,
}) => {
  const [newPin, setNewPin] = useState(config.securityPin);
  const [isChangingPin, setIsChangingPin] = useState(false);

  const handleSavePin = () => {
    if (newPin.length !== 4) {
      showToast('Le code PIN doit comporter 4 chiffres');
      return;
    }
    onUpdateConfig({ securityPin: newPin });
    setIsChangingPin(false);
    showToast('Code PIN mis à jour avec succès');
  };

  return (
    <div className="space-y-6 pb-12 max-w-2xl mx-auto">
      <div>
        <h1 className="text-2xl font-black text-[#0D47A1]">Paramètres de l'Application</h1>
        <p className="text-sm text-blue-700/80">
          Personnalise le comportement, l'arrière-plan de l'application et la sécurité.
        </p>
      </div>

      {/* 1. App Wallpaper Customization */}
      <section className="bg-white/90 backdrop-blur-md p-6 rounded-3xl border border-blue-100 shadow-sm space-y-4">
        <div className="flex items-center gap-2.5 pb-2 border-b border-gray-100">
          <div className="w-8 h-8 rounded-xl bg-[#1E88E5] text-white flex items-center justify-center">
            <Palette className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-base font-bold text-[#0D47A1]">
              Fond d'écran de l'Application
            </h2>
            <p className="text-xs text-gray-500">
              Affiche un fond personnalisé à travers toute l'interface de l'application.
            </p>
          </div>
        </div>

        {config.appWallpaperUrl ? (
          <div className="space-y-4">
            <div className="flex items-center justify-between p-3 rounded-2xl bg-blue-50/80 border border-blue-100">
              <div className="flex items-center gap-3">
                <img
                  src={config.appWallpaperUrl}
                  alt="Fond actif"
                  className="w-12 h-16 rounded-xl object-cover shadow-xs border border-white"
                />
                <div>
                  <span className="text-xs font-bold text-[#0D47A1] block">
                    Fond d'écran personnalisé actif
                  </span>
                  <span className="text-[11px] text-blue-600">
                    Appliqué en arrière-plan global
                  </span>
                </div>
              </div>

              <button
                onClick={() => {
                  onResetAppWallpaper();
                  showToast("Fond de l'app réinitialisé");
                }}
                className="px-3 py-1.5 rounded-xl text-xs font-semibold text-red-600 hover:bg-red-50 flex items-center gap-1 transition"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Retirer</span>
              </button>
            </div>

            {/* Blur & Opacity Sliders */}
            <div className="space-y-3 pt-2">
              <div>
                <div className="flex justify-between text-xs font-semibold text-gray-700 mb-1">
                  <span>Flou d'arrière-plan (Backdrop Blur)</span>
                  <span>{config.appWallpaperBlur}px</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="24"
                  value={config.appWallpaperBlur}
                  onChange={(e) =>
                    onUpdateConfig({ appWallpaperBlur: parseInt(e.target.value) })
                  }
                  className="w-full accent-[#1E88E5]"
                />
              </div>

              <div>
                <div className="flex justify-between text-xs font-semibold text-gray-700 mb-1">
                  <span>Opacité du fond</span>
                  <span>{config.appWallpaperOpacity}%</span>
                </div>
                <input
                  type="range"
                  min="15"
                  max="100"
                  value={config.appWallpaperOpacity}
                  onChange={(e) =>
                    onUpdateConfig({ appWallpaperOpacity: parseInt(e.target.value) })
                  }
                  className="w-full accent-[#1E88E5]"
                />
              </div>
            </div>
          </div>
        ) : (
          <div className="p-4 rounded-2xl bg-blue-50/60 border border-dashed border-blue-200 text-center space-y-2">
            <ImageIcon className="w-8 h-8 text-[#1E88E5] mx-auto opacity-70" />
            <p className="text-xs text-[#0D47A1]">
              Tu peux définir n'importe quelle image de tes diaporamas ou du catalogue Pinterest comme arrière-plan de l'application !
            </p>
          </div>
        )}
      </section>

      {/* 2. Security & PIN Authentification */}
      <section className="bg-white/90 backdrop-blur-md p-6 rounded-3xl border border-blue-100 shadow-sm space-y-4">
        <div className="flex items-center gap-2.5 pb-2 border-b border-gray-100">
          <div className="w-8 h-8 rounded-xl bg-[#1E88E5] text-white flex items-center justify-center">
            <Shield className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-base font-bold text-[#0D47A1]">
              Sécurité & Authentification
            </h2>
            <p className="text-xs text-gray-500">
              Verrouillage par code PIN ou empreinte digitale à l'ouverture.
            </p>
          </div>
        </div>

        <div className="flex items-center justify-between py-2">
          <div>
            <span className="font-semibold text-sm text-[#0D47A1] block">
              Activer la protection au démarrage
            </span>
            <span className="text-xs text-gray-500">
              Demande le code PIN ou l'empreinte pour déverrouiller l'application
            </span>
          </div>

          <label className="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              checked={config.securityPinEnabled}
              onChange={(e) => {
                onUpdateConfig({ securityPinEnabled: e.target.checked });
                showToast(
                  e.target.checked
                    ? 'Protection par code activée'
                    : 'Protection désactivée'
                );
              }}
              className="sr-only peer"
            />
            <div className="w-11 h-6 bg-gray-200 peer-focus:outline-hidden rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[#1E88E5]"></div>
          </label>
        </div>

        {/* Change PIN Block */}
        <div className="pt-2 border-t border-gray-100">
          {isChangingPin ? (
            <div className="space-y-3 bg-blue-50/70 p-4 rounded-2xl border border-blue-100">
              <label className="text-xs font-bold text-[#0D47A1] uppercase tracking-wider block">
                Nouveau code PIN (4 chiffres)
              </label>
              <div className="flex gap-2">
                <input
                  type="password"
                  maxLength={4}
                  value={newPin}
                  onChange={(e) => setNewPin(e.target.value.replace(/\D/g, ''))}
                  placeholder="Ex: 1234"
                  className="w-32 bg-white text-center tracking-widest text-lg font-bold py-2 rounded-xl border border-blue-200"
                />
                <button
                  onClick={handleSavePin}
                  className="px-4 py-2 rounded-xl bg-[#1E88E5] text-white text-xs font-bold hover:bg-[#1976D2] transition"
                >
                  Sauvegarder
                </button>
                <button
                  onClick={() => setIsChangingPin(false)}
                  className="px-3 py-2 rounded-xl text-xs text-gray-500 hover:bg-gray-100 transition"
                >
                  Annuler
                </button>
              </div>
            </div>
          ) : (
            <div className="flex items-center justify-between">
              <span className="text-xs text-gray-600 font-medium">
                Code actuel : <strong className="text-[#0D47A1]">••••</strong>
              </span>
              <button
                onClick={() => setIsChangingPin(true)}
                className="text-xs text-[#1E88E5] font-bold hover:underline flex items-center gap-1"
              >
                <KeyRound className="w-3.5 h-3.5" />
                <span>Modifier le code PIN</span>
              </button>
            </div>
          )}
        </div>
      </section>

      {/* 3. Export & Rendering Quality */}
      <section className="bg-white/90 backdrop-blur-md p-6 rounded-3xl border border-blue-100 shadow-sm space-y-4">
        <div className="flex items-center gap-2.5 pb-2 border-b border-gray-100">
          <div className="w-8 h-8 rounded-xl bg-[#1E88E5] text-white flex items-center justify-center">
            <Download className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-base font-bold text-[#0D47A1]">
              Qualité d'exportation
            </h2>
            <p className="text-xs text-gray-500">
              Résolution de recadrage et format des fonds d'écran sauvegardés.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          {[
            { id: 'high', label: 'Haute Résolution (1080 x 1920)', sub: 'Ultra net' },
            { id: 'standard', label: 'Résolution Standard (720 x 1280)', sub: 'Économique' },
          ].map((q) => (
            <button
              key={q.id}
              onClick={() => onUpdateConfig({ exportQuality: q.id as any })}
              className={`p-3.5 rounded-2xl border text-left transition ${
                config.exportQuality === q.id
                  ? 'border-[#1E88E5] bg-blue-50/80 text-[#0D47A1] font-bold ring-2 ring-blue-100'
                  : 'border-gray-200 text-gray-700 hover:bg-gray-50'
              }`}
            >
              <div className="text-xs">{q.label}</div>
              <div className="text-[10px] text-gray-500 mt-0.5">{q.sub}</div>
            </button>
          ))}
        </div>
      </section>
    </div>
  );
};

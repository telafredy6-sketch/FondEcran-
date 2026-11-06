import React, { useState } from 'react';
import { Fingerprint, Lock, ShieldCheck, KeyRound, AlertCircle } from 'lucide-react';

interface SecurityGateProps {
  onUnlock: () => void;
  correctPin: string;
}

export const SecurityGate: React.FC<SecurityGateProps> = ({ onUnlock, correctPin }) => {
  const [pin, setPin] = useState('');
  const [error, setError] = useState(false);
  const [useFingerprint, setUseFingerprint] = useState(false);

  const handleDigit = (digit: string) => {
    if (pin.length >= 4) return;
    const nextPin = pin + digit;
    setPin(nextPin);
    setError(false);

    if (nextPin.length === 4) {
      if (nextPin === correctPin) {
        setTimeout(onUnlock, 150);
      } else {
        setError(true);
        setTimeout(() => setPin(''), 600);
      }
    }
  };

  const handleBackspace = () => {
    setPin((prev) => prev.slice(0, -1));
    setError(false);
  };

  const handleFingerprintSimulation = () => {
    setUseFingerprint(true);
    setTimeout(() => {
      onUnlock();
    }, 700);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#E3F2FD] p-4">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow-xl p-8 flex flex-col items-center border border-blue-100">
        <div className="w-16 h-16 rounded-full bg-blue-100 flex items-center justify-center text-[#1E88E5] mb-4">
          <Lock className="w-8 h-8" />
        </div>

        <h1 className="text-2xl font-bold text-[#0D47A1] mb-1">Fond d'écran</h1>
        <p className="text-sm text-blue-800 text-center mb-6">
          Confirme ton identité pour ouvrir l'app
        </p>

        {/* PIN Dots */}
        <div className={`flex items-center space-x-4 mb-6 ${error ? 'animate-bounce' : ''}`}>
          {[0, 1, 2, 3].map((i) => (
            <div
              key={i}
              className={`w-4 h-4 rounded-full border-2 transition-all duration-200 ${
                pin.length > i
                  ? error
                    ? 'bg-red-500 border-red-500 scale-110'
                    : 'bg-[#1E88E5] border-[#1E88E5] scale-110'
                  : 'border-blue-300 bg-transparent'
              }`}
            />
          ))}
        </div>

        {error && (
          <div className="flex items-center text-xs text-red-600 mb-4 gap-1">
            <AlertCircle className="w-3.5 h-3.5" />
            <span>Code incorrect (Code par défaut: {correctPin})</span>
          </div>
        )}

        {/* Keypad */}
        <div className="grid grid-cols-3 gap-3 w-full max-w-[260px] mb-6">
          {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((digit) => (
            <button
              key={digit}
              onClick={() => handleDigit(digit)}
              className="w-16 h-16 rounded-full bg-blue-50 hover:bg-blue-100 text-[#0D47A1] text-2xl font-semibold flex items-center justify-center transition active:scale-95 mx-auto"
            >
              {digit}
            </button>
          ))}
          <button
            onClick={handleFingerprintSimulation}
            className="w-16 h-16 rounded-full bg-blue-50 hover:bg-blue-100 text-[#1E88E5] flex items-center justify-center transition active:scale-95 mx-auto"
            title="Empreinte digitale"
          >
            <Fingerprint className={`w-7 h-7 ${useFingerprint ? 'text-green-600 animate-pulse' : ''}`} />
          </button>
          <button
            onClick={() => handleDigit('0')}
            className="w-16 h-16 rounded-full bg-blue-50 hover:bg-blue-100 text-[#0D47A1] text-2xl font-semibold flex items-center justify-center transition active:scale-95 mx-auto"
          >
            0
          </button>
          <button
            onClick={handleBackspace}
            className="w-16 h-16 rounded-full bg-blue-50 hover:bg-blue-100 text-[#0D47A1] text-xs font-semibold flex items-center justify-center transition active:scale-95 mx-auto"
          >
            Effacer
          </button>
        </div>

        {/* Fingerprint fast-track / quick unlock */}
        <button
          onClick={handleFingerprintSimulation}
          className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-blue-50 hover:bg-blue-100 text-[#1E88E5] text-sm font-medium transition"
        >
          <Fingerprint className="w-5 h-5" />
          <span>Déverrouiller par empreinte</span>
        </button>

        <p className="mt-4 text-[11px] text-blue-500 text-center">
          Code par défaut : <strong>{correctPin}</strong> (ou touche l'empreinte)
        </p>
      </div>
    </div>
  );
};

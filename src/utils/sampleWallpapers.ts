import { WallpaperItem } from '../types';

// Helper to generate crisp SVG wallpaper data URLs for out-of-the-box delight
function createArtWallpaper(title: string, topColor: string, bottomColor: string, shapeType: 'sun' | 'mountain' | 'waves' | 'stars'): string {
  const width = 1080;
  const height = 1920;

  let shapesSvg = '';
  if (shapeType === 'sun') {
    shapesSvg = `
      <circle cx="540" cy="800" r="280" fill="#FFB74D" opacity="0.9" />
      <circle cx="540" cy="800" r="340" fill="#FFE082" opacity="0.35" />
      <path d="M0,1400 Q270,1300 540,1400 T1080,1400 L1080,1920 L0,1920 Z" fill="#2E7D32" opacity="0.8" />
      <path d="M0,1550 Q270,1480 540,1550 T1080,1550 L1080,1920 L0,1920 Z" fill="#1B5E20" />
    `;
  } else if (shapeType === 'mountain') {
    shapesSvg = `
      <polygon points="540,550 200,1300 880,1300" fill="#3949AB" opacity="0.7" />
      <polygon points="540,550 420,800 660,800" fill="#E8EAF6" opacity="0.9" />
      <polygon points="250,850 0,1450 600,1450" fill="#283593" opacity="0.8" />
      <polygon points="800,900 450,1500 1080,1500" fill="#1A237E" opacity="0.9" />
      <rect x="0" y="1450" width="1080" height="470" fill="#0D47A1" />
    `;
  } else if (shapeType === 'waves') {
    shapesSvg = `
      <circle cx="850" cy="500" r="160" fill="#FFF59D" opacity="0.85" />
      <path d="M0,1100 C300,1000 600,1200 1080,1050 L1080,1920 L0,1920 Z" fill="#0288D1" opacity="0.6" />
      <path d="M0,1280 C400,1200 700,1380 1080,1220 L1080,1920 L0,1920 Z" fill="#0277BD" opacity="0.8" />
      <path d="M0,1480 C350,1420 750,1550 1080,1440 L1080,1920 L0,1920 Z" fill="#01579B" />
    `;
  } else {
    // stars / anime aesthetic
    shapesSvg = `
      <circle cx="540" cy="700" r="220" fill="#BA68C8" opacity="0.6" />
      <circle cx="300" cy="400" r="8" fill="#FFF" opacity="0.8" />
      <circle cx="780" cy="350" r="12" fill="#FFF" opacity="0.9" />
      <circle cx="200" cy="900" r="6" fill="#FFF" opacity="0.7" />
      <circle cx="850" cy="850" r="10" fill="#FFF" opacity="0.8" />
      <circle cx="450" cy="250" r="14" fill="#FFF" opacity="0.9" />
      <polygon points="0,1500 540,1200 1080,1500 1080,1920 0,1920" fill="#4A148C" opacity="0.9" />
      <polygon points="200,1650 740,1400 1080,1600 1080,1920 0,1920" fill="#311B92" />
    `;
  }

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
    <defs>
      <linearGradient id="bgGrad" x1="0%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stop-color="${topColor}" />
        <stop offset="100%" stop-color="${bottomColor}" />
      </linearGradient>
    </defs>
    <rect width="${width}" height="${height}" fill="url(#bgGrad)" />
    ${shapesSvg}
    <text x="540" y="1800" text-anchor="middle" fill="#FFFFFF" opacity="0.7" font-family="sans-serif" font-size="32" font-weight="600" letter-spacing="4">${title.toUpperCase()}</text>
  </svg>`;

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

export const INITIAL_WALLPAPERS: WallpaperItem[] = [
  {
    id: 'wp-1',
    name: 'Nature Horizon',
    dataUrl: createArtWallpaper('Nature', '#81D4FA', '#FFF9C4', 'sun'),
    music: {
      name: 'Nature Breeze',
      url: '',
      type: 'synth',
      synthType: 'nature',
    },
    createdAt: 1000,
  },
  {
    id: 'wp-2',
    name: 'Océan Pacifique',
    dataUrl: createArtWallpaper('Océan', '#4DD0E1', '#01579B', 'waves'),
    music: {
      name: 'Ocean Ambient Calm',
      url: '',
      type: 'synth',
      synthType: 'ambient',
    },
    createdAt: 2000,
  },
  {
    id: 'wp-3',
    name: 'Montagnes Éternelles',
    dataUrl: createArtWallpaper('Montagne', '#7986CB', '#1A237E', 'mountain'),
    music: {
      name: 'Lofi Chill Chords',
      url: '',
      type: 'synth',
      synthType: 'lofi',
    },
    createdAt: 3000,
  },
  {
    id: 'wp-4',
    name: 'Anime Crépuscule',
    dataUrl: createArtWallpaper('Anime', '#E1BEE7', '#311B92', 'stars'),
    music: {
      name: 'Sunset Melody',
      url: '',
      type: 'synth',
      synthType: 'chill',
    },
    createdAt: 4000,
  },
];

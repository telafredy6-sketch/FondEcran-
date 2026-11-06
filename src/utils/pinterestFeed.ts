export interface PinterestWallpaper {
  id: string;
  title: string;
  category: 'nature' | 'ocean' | 'anime' | 'cyberpunk' | 'minimal' | 'space';
  author: string;
  pinUrl: string;
  imageUrl: string;
  tags: string[];
}

export const PINTEREST_EXPLORE_FEED: PinterestWallpaper[] = [
  {
    id: 'pin-1',
    title: 'Cascade Émeraude',
    category: 'nature',
    author: '@natural_wanderlust',
    pinUrl: 'https://www.pinterest.com/search/pins/?q=emerald%20waterfall%20wallpaper',
    imageUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1080&q=85',
    tags: ['Nature', 'Cascade', 'Forêt'],
  },
  {
    id: 'pin-2',
    title: 'Vague Océanique Crépuscule',
    category: 'ocean',
    author: '@ocean_vibes',
    pinUrl: 'https://www.pinterest.com/search/pins/?q=sunset%20ocean%20waves%20wallpaper',
    imageUrl: 'https://images.unsplash.com/photo-1518837695005-2083093ee35b?auto=format&fit=crop&w=1080&q=85',
    tags: ['Océan', 'Vagues', 'Coucher de soleil'],
  },
  {
    id: 'pin-3',
    title: 'Ciel Étoilé Anime Néo-Tokyo',
    category: 'anime',
    author: '@aesthetic_anime_jp',
    pinUrl: 'https://www.pinterest.com/search/pins/?q=anime%20night%20sky%20wallpaper',
    imageUrl: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1080&q=85',
    tags: ['Anime', 'Nuit', 'Étoiles'],
  },
  {
    id: 'pin-4',
    title: 'Cyberpunk Neon Rain',
    category: 'cyberpunk',
    author: '@future_synth',
    pinUrl: 'https://www.pinterest.com/search/pins/?q=cyberpunk%20neon%20rain%20wallpaper',
    imageUrl: 'https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&w=1080&q=85',
    tags: ['Cyberpunk', 'Néon', 'Pluie'],
  },
  {
    id: 'pin-5',
    title: 'Minimalisme Courbes Pastel',
    category: 'minimal',
    author: '@minimal_design',
    pinUrl: 'https://www.pinterest.com/search/pins/?q=minimalist%20pastel%20wallpaper',
    imageUrl: 'https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?auto=format&fit=crop&w=1080&q=85',
    tags: ['Minimaliste', 'Pastel', 'Abstrait'],
  },
  {
    id: 'pin-6',
    title: 'Nébuleuse Cosmique Violette',
    category: 'space',
    author: '@deep_space',
    pinUrl: 'https://www.pinterest.com/search/pins/?q=space%20nebula%20purple%20wallpaper',
    imageUrl: 'https://images.unsplash.com/photo-1462331940025-496dfbfc7564?auto=format&fit=crop&w=1080&q=85',
    tags: ['Espace', 'Galaxie', 'Cosmos'],
  },
];

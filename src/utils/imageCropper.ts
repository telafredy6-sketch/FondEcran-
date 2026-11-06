/**
 * Crops and compresses an image to standard 9:16 mobile wallpaper format,
 * matching Slides.centerCrop in Slides.kt
 */
export async function centerCropImage(file: File, targetWidth = 1080, targetHeight = 1920): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = targetWidth;
        canvas.height = targetHeight;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          resolve(e.target?.result as string);
          return;
        }

        const targetRatio = targetWidth / targetHeight;
        const srcRatio = img.width / img.height;

        let srcX = 0;
        let srcY = 0;
        let srcW = img.width;
        let srcH = img.height;

        if (srcRatio > targetRatio) {
          srcW = img.height * targetRatio;
          srcX = (img.width - srcW) / 2;
        } else {
          srcH = img.width / targetRatio;
          srcY = (img.height - srcH) / 2;
        }

        ctx.drawImage(img, srcX, srcY, srcW, srcH, 0, 0, targetWidth, targetHeight);
        resolve(canvas.toDataURL('image/jpeg', 0.9));
      };
      img.onerror = reject;
      img.src = e.target?.result as string;
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

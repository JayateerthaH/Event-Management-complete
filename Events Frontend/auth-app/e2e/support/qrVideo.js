// Builds a Y4M video showing a QR code, which Chromium can use as a fake webcam
// (--use-file-for-fake-video-capture). Lets the e2e suite test the real camera scanning path.
import QRCode from "qrcode";
import fs from "node:fs";

const WIDTH = 640;
const HEIGHT = 480;
const FRAMES = 20;

export function writeQrVideo(text, filePath) {
  const qr = QRCode.create(text, { errorCorrectionLevel: "M" });
  const size = qr.modules.size;
  const quiet = 4; // light border required by QR readers
  // QRScanner only decodes a 250x250 box in the centre of the frame, so keep the code inside ~200px
  const scale = Math.max(2, Math.floor(200 / (size + quiet * 2)));
  const offsetX = Math.floor((WIDTH - size * scale) / 2);
  const offsetY = Math.floor((HEIGHT - size * scale) / 2);

  // Luma plane: white background, black modules
  const y = Buffer.alloc(WIDTH * HEIGHT, 235);
  for (let row = 0; row < size; row++) {
    for (let col = 0; col < size; col++) {
      if (!qr.modules.get(row, col)) continue;
      for (let dy = 0; dy < scale; dy++) {
        const start = (offsetY + row * scale + dy) * WIDTH + offsetX + col * scale;
        y.fill(16, start, start + scale);
      }
    }
  }
  const chroma = Buffer.alloc((WIDTH / 2) * (HEIGHT / 2), 128); // neutral (grey) colour planes

  const out = fs.openSync(filePath, "w");
  fs.writeSync(out, `YUV4MPEG2 W${WIDTH} H${HEIGHT} F10:1 Ip A1:1 C420jpeg\n`);
  for (let i = 0; i < FRAMES; i++) {
    fs.writeSync(out, "FRAME\n");
    fs.writeSync(out, y);
    fs.writeSync(out, chroma);
    fs.writeSync(out, chroma);
  }
  fs.closeSync(out);
}

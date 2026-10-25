#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/src/main/assets
ffmpeg -hide_banner -loglevel error -y -f lavfi -i "color=c=0x17233b:s=960x540:r=24:d=40" -f lavfi -i "sine=frequency=220:sample_rate=44100:duration=40" -vf "drawbox=x=0:y=380:w=960:h=160:color=0x365c50:t=fill,drawtext=fontfile=/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf:text='●':fontcolor=0xf5c678:fontsize=100:x='mod(t*40,860)':y=210,drawtext=fontfile=/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf:text='Семейный кинотеатр':fontcolor=white:fontsize=42:x=(w-tw)/2:y=70,drawtext=fontfile=/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf:text='Тихий вечер • тестовое видео':fontcolor=white:fontsize=26:x=(w-tw)/2:y=135" -af "volume=0.03,afade=t=in:d=2,afade=t=out:st=38:d=2" -threads 2 -c:v libx264 -pix_fmt yuv420p -profile:v baseline -c:a aac -movflags +faststart -shortest app/src/main/assets/quiet-evening.mp4
sha256sum app/src/main/assets/quiet-evening.mp4

#!/system/bin/sh
# gradlew для Android-терминала: сам скачивает Gradle 8.7 (нужен для AGP 8.5.2) и запускает его.
V=8.7
D="${HOME:-/data/local/tmp}/.gradle-bm"
G="$D/gradle-$V/bin/gradle"
if [ ! -x "$G" ]; then
  mkdir -p "$D"
  U="https://services.gradle.org/distributions/gradle-$V-bin.zip"
  echo "Скачиваю Gradle $V..."
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$U" -o "$D/g.zip" || rm -f "$D/g.zip"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$U" -O "$D/g.zip" || rm -f "$D/g.zip"
  fi
  if [ -f "$D/g.zip" ]; then
    unzip -q -o "$D/g.zip" -d "$D" && rm -f "$D/g.zip"
  fi
fi
if [ -x "$G" ]; then
  exec "$G" "$@"
elif command -v gradle >/dev/null 2>&1; then
  echo "Не удалось скачать Gradle $V, использую системный gradle"
  exec gradle "$@"
else
  echo "Нет gradle: не удалось скачать (нужны интернет, curl/wget и unzip)"
  exit 1
fi

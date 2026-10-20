#!/usr/bin/env bash
set -euo pipefail

APP_NAME="RogueClient"
VERSION="$(grep -m1 "^version" build.gradle | sed -E "s/version = ['\"]([^'\"]+)['\"]/\1/")"
BUILD_DIR="$(pwd)/build"
APPDIR="${BUILD_DIR}/AppDir"
JAVAFX_JMODS="${HOME}/javafx-jmods-21.0.12"
FATJAR_DIR="${BUILD_DIR}/libs"
OUTPUT_DIR="$(pwd)/Appimagetools"

echo "==> Building fat jar with Gradle"
./gradlew clean fatJar

FATJAR="$(find "${FATJAR_DIR}" -name "*-all.jar" | head -n1)"
if [ -z "${FATJAR}" ]; then echo "ERROR: fat jar not found in ${FATJAR_DIR}"; exit 1; fi
echo "Found fat jar: ${FATJAR}"

rm -rf "${APPDIR}"
mkdir -p "${APPDIR}/usr/bin" "${APPDIR}/usr/lib" "${APPDIR}/usr/share/applications" "${APPDIR}/usr/share/icons/hicolor/256x256/apps"

if [ ! -d "${JAVAFX_JMODS}" ]; then echo "ERROR: JavaFX jmods not found at ${JAVAFX_JMODS}"; exit 1; fi

JAVA_MODS="java.base,java.desktop,java.logging,java.naming,java.net.http,java.scripting,java.sql,java.xml,jdk.unsupported,jdk.crypto.ec,java.management,jdk.management"
FX_MODS="javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.swing,javafx.web,javafx.media"

rm -rf "${BUILD_DIR}/runtime"
jlink \
    --module-path "${JAVA_HOME}/jmods:${JAVAFX_JMODS}" \
    --add-modules "${JAVA_MODS},${FX_MODS}" \
    --output "${BUILD_DIR}/runtime" \
    --strip-debug --no-header-files --no-man-pages --compress=2

cp -r "${BUILD_DIR}/runtime" "${APPDIR}/usr/lib/runtime"
cp "${FATJAR}" "${APPDIR}/usr/lib/${APP_NAME}.jar"

ICON_SRC="src/main/resources/icons/rogue-launch.png"
if [ -f "${ICON_SRC}" ]; then
    cp "${ICON_SRC}" "${APPDIR}/usr/share/icons/hicolor/256x256/apps/${APP_NAME}.png"
    cp "${ICON_SRC}" "${APPDIR}/${APP_NAME}.png"
else
    echo "WARNING: icon not found at ${ICON_SRC}"
fi

cat > "${APPDIR}/usr/share/applications/${APP_NAME}.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=Rogue Client
Comment=Rogue Client Minecraft Launcher
Exec=${APP_NAME}
Icon=${APP_NAME}
Categories=Game;
Terminal=false
EOF
cp "${APPDIR}/usr/share/applications/${APP_NAME}.desktop" "${APPDIR}/${APP_NAME}.desktop"

cat > "${APPDIR}/AppRun" <<'EOF'
#!/usr/bin/env bash
HERE="$(dirname "$(readlink -f "${0}")")"
export PATH="${HERE}/usr/lib/runtime/bin:${PATH}"
exec "${HERE}/usr/lib/runtime/bin/java" -jar "${HERE}/usr/lib/RogueClient.jar" "$@"
EOF
chmod +x "${APPDIR}/AppRun"

APPIMAGETOOL="${OUTPUT_DIR}/appimagetool-x86_64.AppImage"
mkdir -p "${OUTPUT_DIR}"
if [ ! -f "${APPIMAGETOOL}" ]; then
    curl -L -o "${APPIMAGETOOL}" "https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage"
    chmod +x "${APPIMAGETOOL}"
fi

cd "${OUTPUT_DIR}"
ARCH=x86_64 "${APPIMAGETOOL}" "${APPDIR}" "${OUTPUT_DIR}/${APP_NAME}-${VERSION}-x86_64.AppImage"

echo "==> Done: ${OUTPUT_DIR}/${APP_NAME}-${VERSION}-x86_64.AppImage"

/**
 * Browser QR scanner bridge for Kinetic reception (html5-qrcode).
 * Events:
 *   - kinetic-qr-ready   (camera started)
 *   - kinetic-qr-scanned (detail: decoded string)
 *   - kinetic-qr-error   (detail: error message)
 */
(function () {
    const SCAN_COOLDOWN_MS = 2500;

    window.KineticQrScanner = {
        instance: null,
        mountId: null,
        lastScan: null,
        lastScanAt: 0,
        starting: false,

        start: function (elementId) {
            if (this.starting) {
                return;
            }
            this.stop();
            this.mountId = elementId;
            this.starting = true;

            if (typeof Html5Qrcode === "undefined") {
                this.starting = false;
                window.dispatchEvent(
                    new CustomEvent("kinetic-qr-error", {
                        detail: "No se cargó la librería del escáner. Recargá la página.",
                    })
                );
                return;
            }

            const mount = document.getElementById(elementId);
            if (!mount) {
                this.starting = false;
                window.dispatchEvent(
                    new CustomEvent("kinetic-qr-error", {
                        detail: "No se encontró el contenedor de la cámara.",
                    })
                );
                return;
            }

            // Limpiar y preparar el contenedor
            mount.innerHTML = "";
            mount.style.display = "flex";
            mount.style.flexDirection = "column";
            mount.style.alignItems = "center";
            mount.style.justifyContent = "center";
            mount.style.backgroundColor = "black";
            mount.style.overflow = "hidden";

            const scanner = new Html5Qrcode(elementId);
            this.instance = scanner;

            const config = {
                fps: 15,
                qrbox: function (viewWidth, viewHeight) {
                    // Asegurar que el recuadro sea cuadrado y ocupe el 70% del lado más corto
                    const size = Math.floor(Math.min(viewWidth, viewHeight) * 0.7);
                    return { width: size, height: size };
                },
                aspectRatio: 1.0, // Sugerencia de aspecto cuadrado
            };

            const onReady = () => {
                this.starting = false;
                // Asegurar que el video ocupe el contenedor y esté centrado
                const video = mount.querySelector('video');
                if (video) {
                    video.style.width = "100%";
                    video.style.height = "100%";
                    video.style.objectFit = "cover";
                    video.style.transform = "scaleX(-1)"; // Espejo opcional para cámara frontal
                }
                window.dispatchEvent(new CustomEvent("kinetic-qr-ready"));
            };

            const onFail = (err) => {
                this.starting = false;
                const message = String(err || "No se pudo abrir la cámara");
                window.dispatchEvent(
                    new CustomEvent("kinetic-qr-error", {
                        detail: formatCameraError(message),
                    })
                );
            };

            scanner
                .start(
                    { facingMode: "environment" },
                    config,
                    (decodedText) => {
                        const now = Date.now();
                        if (
                            decodedText === this.lastScan &&
                            now - this.lastScanAt < SCAN_COOLDOWN_MS
                        ) {
                            return;
                        }
                        this.lastScan = decodedText;
                        this.lastScanAt = now;
                        window.dispatchEvent(
                            new CustomEvent("kinetic-qr-scanned", {
                                detail: decodedText,
                            })
                        );
                    },
                    () => {}
                )
                .then(onReady)
                .catch(onFail);
        },

        stop: function () {
            this.starting = false;
            if (!this.instance) {
                this.mountId = null;
                return;
            }
            const scanner = this.instance;
            const mountId = this.mountId;
            this.instance = null;
            this.mountId = null;
            this.lastScan = null;
            this.lastScanAt = 0;
            scanner
                .stop()
                .then(() => {
                    scanner.clear();
                    if (mountId) {
                        const mount = document.getElementById(mountId);
                        if (mount) mount.innerHTML = "";
                    }
                })
                .catch(() => {});
        },
    };

    function formatCameraError(message) {
        const lower = message.toLowerCase();
        if (lower.includes("notallowed") || lower.includes("permission")) {
            return "Permiso de cámara denegado. Permití el acceso en el navegador y volvé a intentar.";
        }
        if (lower.includes("notfound") || lower.includes("devices")) {
            return "No se detectó ninguna cámara en este dispositivo.";
        }
        if (lower.includes("secure") || lower.includes("https")) {
            return "La cámara requiere HTTPS o localhost. Abrí el panel admin desde localhost.";
        }
        return message;
    }

    window.kineticQrStart = function (elementId) {
        window.KineticQrScanner.start(elementId);
    };

    window.kineticQrStop = function () {
        window.KineticQrScanner.stop();
    };

    window.kineticQrReadDetail = function (event) {
        return String(event && event.detail != null ? event.detail : "");
    };
})();

/**
 * Bridge para el escáner QR con centrado estricto y formato cuadrado.
 */
(function () {
    const SCAN_COOLDOWN_MS = 2500;

    window.KineticQrScanner = {
        instance: null,
        mountId: null,
        lastScan: null,
        lastScanAt: 0,
        starting: false,
        observer: null,

        start: function (elementId) {
            if (this.starting) return;
            this.stop();
            this.mountId = elementId;
            this.starting = true;

            const mount = document.getElementById(elementId);
            if (!mount) {
                this.starting = false;
                return;
            }

            // Estilos base ultra-agresivos para el contenedor
            mount.innerHTML = "";
            mount.style.cssText = `
                background-color: black !important;
                display: flex !important;
                align-items: center !important;
                justify-content: center !important;
                position: relative !important;
                overflow: hidden !important;
                width: 100% !important;
                height: 100% !important;
            `;

            if (typeof Html5Qrcode === "undefined") {
                this.starting = false;
                window.dispatchEvent(new CustomEvent("kinetic-qr-error", { detail: "html5-qrcode library not found" }));
                return;
            }

            const scanner = new Html5Qrcode(elementId);
            this.instance = scanner;

            const config = {
                fps: 25,
                qrbox: (viewWidth, viewHeight) => {
                    // Fuerza un recuadro cuadrado ocupando el 75% del lado menor
                    const min = Math.min(viewWidth, viewHeight);
                    const size = Math.floor(min * 0.75);
                    return { width: size, height: size };
                },
                aspectRatio: 1.0
            };

            const forceStrictStyles = () => {
                const video = mount.querySelector('video');
                const canvas = mount.querySelector('canvas');
                const region = mount.querySelector('#' + elementId + '__scan_region');

                if (video) {
                    video.style.setProperty("width", "100%", "important");
                    video.style.setProperty("height", "100%", "important");
                    video.style.setProperty("object-fit", "cover", "important");
                    video.style.setProperty("position", "absolute", "important");
                    video.style.setProperty("top", "0", "important");
                    video.style.setProperty("left", "0", "important");
                    video.style.setProperty("transform", "none", "important");
                    video.style.setProperty("display", "block", "important");
                }

                if (canvas) {
                    canvas.style.setProperty("width", "100%", "important");
                    canvas.style.setProperty("height", "100%", "important");
                    canvas.style.setProperty("position", "absolute", "important");
                    canvas.style.setProperty("top", "0", "important");
                    canvas.style.setProperty("left", "0", "important");
                    canvas.style.setProperty("object-fit", "cover", "important");
                    canvas.style.setProperty("z-index", "1", "important");
                }

                if (region) {
                    region.style.setProperty("width", "100%", "important");
                    region.style.setProperty("height", "100%", "important");
                    region.style.setProperty("display", "flex", "important");
                    region.style.setProperty("align-items", "center", "important");
                    region.style.setProperty("justify-content", "center", "important");
                    region.style.setProperty("border", "none", "important");
                    region.style.setProperty("position", "absolute", "important");
                    region.style.setProperty("top", "0", "important");
                    region.style.setProperty("left", "0", "important");
                }
            };

            scanner.start(
                { facingMode: "environment" },
                config,
                (decodedText) => {
                    const now = Date.now();
                    if (decodedText === this.lastScan && now - this.lastScanAt < SCAN_COOLDOWN_MS) return;
                    this.lastScan = decodedText;
                    this.lastScanAt = now;
                    window.dispatchEvent(new CustomEvent("kinetic-qr-scanned", { detail: decodedText }));
                },
                () => {}
            ).then(() => {
                this.starting = false;
                forceStrictStyles();
                if (this.observer) this.observer.disconnect();
                this.observer = new MutationObserver(forceStrictStyles);
                this.observer.observe(mount, { childList: true, subtree: true, attributes: true });
                window.dispatchEvent(new CustomEvent("kinetic-qr-ready"));
            }).catch((err) => {
                this.starting = false;
                window.dispatchEvent(new CustomEvent("kinetic-qr-error", { detail: String(err) }));
            });
        },

        stop: function () {
            this.starting = false;
            if (this.observer) {
                this.observer.disconnect();
                this.observer = null;
            }
            if (!this.instance) return;
            const scanner = this.instance;
            const mid = this.mountId;
            this.instance = null;
            this.mountId = null;
            this.lastScan = null;
            this.lastScanAt = 0;
            scanner.stop().then(() => {
                scanner.clear();
                const m = document.getElementById(mid);
                if (m) m.innerHTML = "";
            }).catch(() => {});
        }
    };

    window.kineticQrStart = (id) => window.KineticQrScanner.start(id);
    window.kineticQrStop = () => window.KineticQrScanner.stop();
    window.kineticQrReadDetail = (e) => String(e && e.detail ? e.detail : "");
})();

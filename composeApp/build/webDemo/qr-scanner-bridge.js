/**
 * Bridge para el escáner QR.
 *
 * IMPORTANTE: Este div es posicionado con `position: fixed` por Kotlin (ensureOverlay).
 * NO usar style.cssText en start() porque sobreescribe position/width/height del div.
 * Usar setProperty() solo para agregar estilos visuales, sin tocar el layout.
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

            // ── Limpiar contenido previo ──────────────────────────────────────
            mount.innerHTML = "";

            // ── Estilos visuales del contenedor ──────────────────────────────
            // CRÍTICO: NO usar style.cssText aquí. Sobreescribiría position/left/top/
            // width/height que Kotlin seteó en ensureOverlay(). Usar setProperty().
            // Tampoco setear `position`, `width`, `height` —  son responsabilidad de Kotlin.
            mount.style.setProperty("background-color", "#000000", "important");
            mount.style.setProperty("display", "flex", "important");
            mount.style.setProperty("align-items", "center", "important");
            mount.style.setProperty("justify-content", "center", "important");
            mount.style.setProperty("overflow", "hidden", "important");

            if (typeof Html5Qrcode === "undefined") {
                this.starting = false;
                window.dispatchEvent(new CustomEvent("kinetic-qr-error", { detail: "html5-qrcode library not found" }));
                return;
            }

            // ── Calcular qrbox cuadrado sobre las dimensiones reales del div ──
            // offsetWidth/offsetHeight reflejan el tamaño fijado por ensureOverlay.
            const divW = mount.offsetWidth  || 320;
            const divH = mount.offsetHeight || 320;
            const side = Math.floor(Math.min(divW, divH) * 0.72);

            const config = {
                fps: 25,
                qrbox: { width: side, height: side }
                // Sin aspectRatio — deja que la cámara use su ratio nativo.
                // El video llenará el div con object-fit:cover (ver forceStrictStyles).
            };

            const scanner = new Html5Qrcode(elementId);
            this.instance = scanner;

            // ── Corrección de estilos internos de html5-qrcode ───────────────
            const forceStrictStyles = () => {
                const video  = mount.querySelector("video");
                const canvas = mount.querySelector("canvas");
                const region = mount.querySelector("#" + elementId + "__scan_region");

                if (video) {
                    video.style.setProperty("width",      "100%",     "important");
                    video.style.setProperty("height",     "100%",     "important");
                    video.style.setProperty("object-fit", "cover",    "important");
                    video.style.setProperty("position",   "absolute", "important");
                    video.style.setProperty("top",        "0",        "important");
                    video.style.setProperty("left",       "0",        "important");
                    video.style.setProperty("transform",  "none",     "important");
                    video.style.setProperty("display",    "block",    "important");
                }

                if (canvas) {
                    canvas.style.setProperty("width",      "100%",     "important");
                    canvas.style.setProperty("height",     "100%",     "important");
                    canvas.style.setProperty("position",   "absolute", "important");
                    canvas.style.setProperty("top",        "0",        "important");
                    canvas.style.setProperty("left",       "0",        "important");
                    canvas.style.setProperty("object-fit", "cover",    "important");
                    canvas.style.setProperty("z-index",    "1",        "important");
                }

                if (region) {
                    region.style.setProperty("width",           "100%",     "important");
                    region.style.setProperty("height",          "100%",     "important");
                    region.style.setProperty("display",         "flex",     "important");
                    region.style.setProperty("align-items",     "center",   "important");
                    region.style.setProperty("justify-content", "center",   "important");
                    region.style.setProperty("border",          "none",     "important");
                    region.style.setProperty("position",        "absolute", "important");
                    region.style.setProperty("top",             "0",        "important");
                    region.style.setProperty("left",            "0",        "important");
                }
            };

            scanner.start(
                { facingMode: "environment" },
                config,
                (decodedText) => {
                    const now = Date.now();
                    if (decodedText === this.lastScan && now - this.lastScanAt < SCAN_COOLDOWN_MS) return;
                    this.lastScan    = decodedText;
                    this.lastScanAt  = now;
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
            const scanner    = this.instance;
            const mid        = this.mountId;
            this.instance    = null;
            this.mountId     = null;
            this.lastScan    = null;
            this.lastScanAt  = 0;
            scanner.stop().then(() => {
                scanner.clear();
                const m = document.getElementById(mid);
                if (m) m.innerHTML = "";
            }).catch(() => {});
        }
    };

    // ── API pública ─────────────────────────────────────────────────────────
    window.kineticQrStart      = (id) => window.KineticQrScanner.start(id);
    window.kineticQrStop       = ()   => window.KineticQrScanner.stop();
    window.kineticQrReadDetail = (e)  => String(e && e.detail ? e.detail : "");

    // ── Helpers para que Kotlin pueda calcular la posición CSS del overlay ──
    // Permite detectar si el canvas de Compose no está en (0,0) o tiene escala distinta.
    window.kineticCanvasCssLeft  = () => { const c = document.querySelector("canvas"); return c ? c.getBoundingClientRect().left  : 0; };
    window.kineticCanvasCssTop   = () => { const c = document.querySelector("canvas"); return c ? c.getBoundingClientRect().top   : 0; };
    window.kineticCanvasCssWidth = () => { const c = document.querySelector("canvas"); return c ? c.getBoundingClientRect().width : window.innerWidth;  };
    window.kineticCanvasCssHeight= () => { const c = document.querySelector("canvas"); return c ? c.getBoundingClientRect().height: window.innerHeight; };
    window.kineticCanvasPhysWidth= () => { const c = document.querySelector("canvas"); return c ? c.width  : window.innerWidth;  };
    window.kineticCanvasPhysHeight=() => { const c = document.querySelector("canvas"); return c ? c.height : window.innerHeight; };
})();

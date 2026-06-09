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

            // ── Wrapper Interno ───────────────────────────────────────────────
            // html5-qrcode sobreescribe los estilos del div que se le pasa.
            // Para evitar que rompa el position:fixed de Compose, creamos un hijo.
            const innerId = elementId + "-inner";
            const innerDiv = document.createElement("div");
            innerDiv.id = innerId;
            innerDiv.style.width = "100%";
            innerDiv.style.height = "100%";
            mount.appendChild(innerDiv);

            // ── Estilos visuales del contenedor (padre) ──────────────────────
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

            const config = { fps: 10 };

            const scanner = new Html5Qrcode(innerId);
            this.instance = scanner;

            scanner.start(
                { facingMode: "environment" },
                config,
                (decodedText) => {
                    console.log("[KineticQrScanner] Decoded text:", decodedText);
                    const now = Date.now();
                    if (decodedText === this.lastScan && now - this.lastScanAt < SCAN_COOLDOWN_MS) {
                        console.log("[KineticQrScanner] Ignored due to cooldown");
                        return;
                    }
                    this.lastScan    = decodedText;
                    this.lastScanAt  = now;
                    window.dispatchEvent(new CustomEvent("kinetic-qr-scanned", { detail: decodedText }));
                },
                () => {}
            ).then(() => {
                this.starting = false;
                
                // Asegurarse que el video tome 100% sin romper el canvas oculto
                const video = mount.querySelector("video");
                if (video) {
                    video.style.width = "100%";
                    video.style.height = "100%";
                    video.style.objectFit = "cover";
                }
                
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
    const getComposeCanvas = () => document.querySelector("#compose-target canvas");
    window.kineticCanvasCssLeft  = () => { const c = getComposeCanvas(); return c ? c.getBoundingClientRect().left  : 0; };
    window.kineticCanvasCssTop   = () => { const c = getComposeCanvas(); return c ? c.getBoundingClientRect().top   : 0; };
    window.kineticCanvasCssWidth = () => { const c = getComposeCanvas(); return c ? c.getBoundingClientRect().width : window.innerWidth;  };
    window.kineticCanvasCssHeight= () => { const c = getComposeCanvas(); return c ? c.getBoundingClientRect().height: window.innerHeight; };
    window.kineticCanvasPhysWidth= () => { const c = getComposeCanvas(); return c ? c.width  : window.innerWidth;  };
    window.kineticCanvasPhysHeight=() => { const c = getComposeCanvas(); return c ? c.height : window.innerHeight; };
})();

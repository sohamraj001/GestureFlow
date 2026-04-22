// Debug helper
function logDebug(msg) {
    console.log("[DEBUG]", msg);
    const debugDiv = document.getElementById('debug-log');
    if (debugDiv) {
        debugDiv.innerText += "\n> " + msg;
    }
}

logDebug("Script gesture.js loaded.");

const videoElement = document.getElementById('input-video');
const canvasElement = document.getElementById('gesture-canvas');
const canvasCtx = canvasElement.getContext('2d');
const gestureIndicator = document.getElementById('gesture-indicator');

let isGestureSystemActive = true;
let lastScrollTime = 0;
const SCROLL_COOLDOWN = 1200; 
let MOVEMENT_THRESHOLD = 0.20; 
const SENSITIVITY_MIN = 0.05;
const SENSITIVITY_MAX = 0.35;
let frameCount = 0;
const FRAME_SKIP = 2; // Increased skip for i3 stability
const INIT_TIMEOUT_MS = 15000; // 15s timeout for AI engine
let indexTrail = [];
const TRAIL_MAX_SIZE = 10;
let currentDirectionLabel = "";

// Listen for sensitivity updates
window.addEventListener('sensitivityChange', (e) => {
    MOVEMENT_THRESHOLD = e.detail.value;
});

// Listen for toggle updates
window.addEventListener('gestureToggle', (e) => {
    isGestureSystemActive = e.detail.enabled;
    const previewContainer = document.querySelector('.camera-preview-container');
    if (previewContainer) {
        previewContainer.style.opacity = isGestureSystemActive ? '1' : '0.4';
    }
});

function onResults(results) {
    if (!results.image) return;
    frameCount++;
    if (frameCount % (FRAME_SKIP + 1) !== 0) return;

    canvasCtx.save();
    canvasCtx.clearRect(0, 0, canvasElement.width, canvasElement.height);
    canvasCtx.drawImage(results.image, 0, 0, canvasElement.width, canvasElement.height);

    if (!isGestureSystemActive) {
        canvasCtx.restore();
        return;
    }

    if (results.multiHandLandmarks && results.multiHandLandmarks.length > 0) {
        gestureIndicator.classList.add('detected');
        
        const landmarks = results.multiHandLandmarks[0]; 
        const indexTip = landmarks[8];
        const currentX = indexTip.x * canvasElement.width;
        const currentY = indexTip.y * canvasElement.height;

        indexTrail.push({x: currentX, y: currentY});
        if (indexTrail.length > TRAIL_MAX_SIZE) indexTrail.shift();

        if (indexTrail.length > 1) {
            canvasCtx.beginPath();
            canvasCtx.moveTo(indexTrail[0].x, indexTrail[0].y);
            for (let i = 1; i < indexTrail.length; i++) {
                canvasCtx.lineTo(indexTrail[i].x, indexTrail[i].y);
            }
            canvasCtx.strokeStyle = 'rgba(0, 242, 234, 0.6)';
            canvasCtx.lineWidth = 3;
            canvasCtx.stroke();
        }

        canvasCtx.beginPath();
        canvasCtx.arc(currentX, currentY, 8, 0, 2 * Math.PI);
        canvasCtx.fillStyle = '#00f2ea';
        canvasCtx.fill();
        canvasCtx.strokeStyle = '#fff';
        canvasCtx.lineWidth = 2;
        canvasCtx.stroke();

        const now = Date.now();
        if (now - lastScrollTime > SCROLL_COOLDOWN) {
            const startPoint = indexTrail[0];
            const endPoint = indexTrail[indexTrail.length - 1];
            const deltaY = (endPoint.y - startPoint.y) / canvasElement.height;

            if (Math.abs(deltaY) > MOVEMENT_THRESHOLD) {
                if (deltaY < 0) {
                    currentDirectionLabel = "Moving Up ↑";
                    triggerScroll('down');
                } else {
                    currentDirectionLabel = "Moving Down ↓";
                    triggerScroll('up');
                }
                lastScrollTime = now;
                indexTrail = []; 
            } else {
                currentDirectionLabel = "";
            }
        }
        drawStatusLabel(currentDirectionLabel || "Index Finger Detected");
    } else {
        gestureIndicator.classList.remove('detected');
        indexTrail = [];
        currentDirectionLabel = "";
        drawStatusLabel("No Hand Detected", "#ff0050");
    }
    canvasCtx.restore();
}

function drawStatusLabel(text, color = "#00f2ea") {
    canvasCtx.fillStyle = "rgba(0,0,0,0.6)";
    canvasCtx.fillRect(0, canvasElement.height - 25, canvasElement.width, 25);
    canvasCtx.font = "bold 10px Outfit";
    canvasCtx.fillStyle = color;
    canvasCtx.textAlign = "center";
    canvasCtx.fillText(text.toUpperCase(), canvasElement.width / 2, canvasElement.height - 8);
}

function triggerScroll(direction) {
    if (window.scrollToReel) {
        window.scrollToReel(direction);
    }
}

// Initialize Hands
let hands;
function initHands() {
    try {
        logDebug("Initializing Hands Library...");
        // Check for common MediaPipe global namespaces
        const HandConstructor = window.Hands || 
                           (window.mpHands ? window.mpHands.Hands : null) ||
                           (window.MediaPipeHands ? window.MediaPipeHands.Hands : null);
        
        if (!HandConstructor) {
            logDebug("Error: Hands constructor not found.");
            return false;
        }

        hands = new HandConstructor({
            locateFile: (file) => {
                // Use a more reliable CDN structure for WASM files
                return `https://cdn.jsdelivr.net/npm/@mediapipe/hands/${file}`;
            }
        });

        hands.setOptions({
            maxNumHands: 1,
            modelComplexity: 0, // 0 is essential for i3
            minDetectionConfidence: 0.5,
            minTrackingConfidence: 0.5,
            selfieMode: true
        });

        hands.onResults(onResults);
        logDebug("Hands setup complete (Model 0).");
        return true;
    } catch (e) {
        logDebug("Hands Init Failed: " + e.message);
        return false;
    }
}

async function startSystem() {
    const lText = document.getElementById('loading-text');
    const manualBtn = document.getElementById('manual-start');
    
    // Add a global timeout to prevent infinite loader
    const timeout = setTimeout(() => {
        logDebug("System initialization timed out.");
        if (lText) lText.innerText = "Connection Slow. Try Manual Start?";
        if (manualBtn) manualBtn.style.display = 'block';
    }, INIT_TIMEOUT_MS);

    try {
        logDebug("Starting System...");
        if (lText) lText.innerText = "Requesting Camera...";
        
        if (!hands && !initHands()) {
            throw new Error("Could not initialize AI Engine.");
        }

        // Check if browser allows auto-start
        const stream = await navigator.mediaDevices.getUserMedia({
            video: { 
                width: { ideal: 320 }, // Lower resolution for i3
                height: { ideal: 240 },
                facingMode: "user" 
            }
        }).catch(err => {
            if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
                throw new Error("Camera Access Denied. Click 'Grant Camera' to fix.");
            }
            throw err;
        });
        
        logDebug("Camera access granted.");
        clearTimeout(timeout);
        
        videoElement.srcObject = stream;
        
        if (lText) lText.innerText = "Loading AI Model...";
        
        await new Promise((resolve, reject) => {
            videoElement.onloadedmetadata = () => resolve();
            videoElement.onerror = () => reject(new Error("Video failed to load."));
            // Safety timeout for metadata
            setTimeout(() => resolve(), 3000);
        });

        await videoElement.play().catch(e => {
            logDebug("Play failed, likely user gesture needed.");
            if (manualBtn) manualBtn.style.display = 'block';
            if (lText) lText.innerText = "Click to Start Camera";
        });
        
        logDebug("Detection loop starting.");
        if (lText) lText.innerText = "AI Engine Active";

        async function detectionLoop() {
            if (videoElement.paused || videoElement.ended) {
                requestAnimationFrame(detectionLoop);
                return;
            }
            try {
                await hands.send({ image: videoElement });
            } catch (err) {
                // Silently handle loop errors unless constant
                if (frameCount % 300 === 0) console.error("Loop error:", err.message);
            }
            requestAnimationFrame(detectionLoop);
        }
        
        detectionLoop();
        hideLoader();
    } catch (err) {
        clearTimeout(timeout);
        logDebug("INIT ERROR: " + err.message);
        if (lText) lText.innerText = err.message;
        if (manualBtn) {
            manualBtn.style.display = 'block';
            manualBtn.innerText = "Retry Camera";
        }
    }
}

function hideLoader() {
    const loader = document.getElementById('loading-screen');
    if (loader) {
        loader.classList.add('hidden');
        setTimeout(() => loader.style.display = 'none', 600);
    }
}

// Initial Launch
window.addEventListener('load', () => {
    logDebug("Window Load event fired.");
    setTimeout(() => {
        startSystem();
    }, 500);
});

// Manual Controls
document.getElementById('manual-start').addEventListener('click', startSystem);
document.getElementById('skip-ai').addEventListener('click', hideLoader);

function resizeCanvas() {
    canvasElement.width = canvasElement.clientWidth || 320;
    canvasElement.height = canvasElement.clientHeight || 240;
}

window.addEventListener('resize', resizeCanvas);
resizeCanvas();

document.addEventListener('DOMContentLoaded', () => {
    const reelsFeed = document.getElementById('reels-feed');
    const reels = document.querySelectorAll('.reel');
    const videos = document.querySelectorAll('video:not(.input-video)');
    const toggleBtn = document.getElementById('toggle-gesture');
    const feedbackToast = document.getElementById('gesture-feedback');
    const feedbackIcon = feedbackToast.querySelector('.feedback-icon');
    const feedbackText = feedbackToast.querySelector('.feedback-text');

    let currentReelIndex = 0;
    let isGestureEnabled = true;

    // Initialize Intersection Observer to play/pause videos
    const observerOptions = {
        root: reelsFeed,
        threshold: 0.6 // Video must be 60% visible to play
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            const video = entry.target.querySelector('video');
            if (entry.isIntersecting) {
                // Pre-load and try playing
                video.load();
                const playPromise = video.play();
                if (playPromise !== undefined) {
                    playPromise.catch(error => {
                        console.warn("Auto-play prevented. User interaction required:", error);
                    });
                }
                currentReelIndex = Array.from(reels).indexOf(entry.target);
            } else {
                video.pause();
                video.currentTime = 0;
            }
        });
    }, observerOptions);

    reels.forEach(reel => observer.observe(reel));

    // Scroll Logic
    let isScrolling = false;
    window.scrollToReel = (direction) => {
        if (!isGestureEnabled || isScrolling) return;

        let nextIndex = currentReelIndex;
        if (direction === 'down' && currentReelIndex < reels.length - 1) {
            nextIndex++;
            showFeedback('↓', 'Next Reel');
        } else if (direction === 'up' && currentReelIndex > 0) {
            nextIndex--;
            showFeedback('↑', 'Previous Reel');
        } else {
            return; // At the end or start
        }

        isScrolling = true;
        reels[nextIndex].scrollIntoView({ behavior: 'smooth' });
        
        // Reset scrolling flag after animation
        setTimeout(() => {
            isScrolling = false;
        }, 800);
    };

    // UI Feedback
    function showFeedback(icon, text) {
        feedbackIcon.textContent = icon;
        feedbackText.textContent = text;
        feedbackToast.classList.add('show');
        setTimeout(() => {
            feedbackToast.classList.remove('show');
        }, 1000);
    }

    const toggleGestureBtn = document.getElementById('toggle-gesture');
    const toggleWebcamBtn = document.getElementById('toggle-webcam');
    const sensitivitySlider = document.getElementById('sensitivity-slider');
    const cameraPreview = document.querySelector('.camera-preview-container');

    // Toggle Gesture Control
    toggleGestureBtn.addEventListener('click', () => {
        isGestureEnabled = !isGestureEnabled;
        toggleGestureBtn.classList.toggle('active', isGestureEnabled);
        window.dispatchEvent(new CustomEvent('gestureToggle', { detail: { enabled: isGestureEnabled } }));
    });

    // Toggle Webcam Visibility
    toggleWebcamBtn.addEventListener('click', () => {
        const isHidden = cameraPreview.classList.toggle('hidden');
        toggleWebcamBtn.classList.toggle('active', !isHidden);
    });

    // Sensitivity Control
    sensitivitySlider.addEventListener('input', (e) => {
        // Map 5-30 slider to 0.30-0.05 threshold
        // High slider value (30) -> Low threshold (0.05) -> High Sensitivity
        const sensitivity = 0.35 - (parseFloat(e.target.value) / 100);
        window.dispatchEvent(new CustomEvent('sensitivityChange', { detail: { value: sensitivity } }));
    });

    // Handle initial user interaction to allow video playback and audio
    const unmuteOverlay = document.getElementById('unmute-overlay');
    
    unmuteOverlay.addEventListener('click', () => {
        unmuteOverlay.classList.add('hidden');
        
        // Unmute and play current video
        videos.forEach(v => {
            v.muted = false;
        });

        const activeVideo = reels[currentReelIndex].querySelector('video');
        if (activeVideo) {
            activeVideo.play().catch(e => console.warn("Play failed:", e));
        }
    });
});

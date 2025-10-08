class EdgeDetectionViewer {
    constructor() {
        this.processedFrame = document.getElementById('processedFrame');
        this.statusElement = document.getElementById('status');
        this.resolutionElement = document.getElementById('resolution');
        this.fpsElement = document.getElementById('fps');
        this.lastUpdateElement = document.getElementById('lastUpdate');
        this.wsStatusElement = document.getElementById('wsStatus');
        this.framesReceivedElement = document.getElementById('framesReceived');
        this.frameInfoElement = document.getElementById('frameInfo');

        this.frameCount = 0;
        this.lastFrameTime = 0;
        this.currentFPS = 0;

        this.sampleFrameBase64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAPklEQVQYV2NkQAN79+5lZGBg+M9IA4CpgqGhoYERuxFwLhYXwJXjVIFhAloFuhHYVWAzAqcKdBV4TUBXAQDpEc0Mpa1wggAAAABJRU5ErkJggg==";

        this.initialize();
    }

    initialize() {
        this.setupEventListeners();
        this.updateDisplay();
    }

    setupEventListeners() {
        document.getElementById('loadSampleBtn').addEventListener('click', () => {
            this.loadSampleFrame();
        });

        document.getElementById('simulateLiveBtn').addEventListener('click', () => {
            this.simulateLiveFeed();
        });

        document.getElementById('clearBtn').addEventListener('click', () => {
            this.clearDisplay();
        });

        this.simulateWebSocketConnection();
    }

    loadSampleFrame() {
        this.processedFrame.src = this.sampleFrameBase64;
        this.processedFrame.style.display = 'block';

        this.updateFrameStats(640, 480);
        this.statusElement.textContent = 'Sample frame loaded';
        this.frameInfoElement.textContent = 'Sample edge detection frame (640x480)';
        this.frameInfoElement.className = '';

        console.log('Sample frame loaded');
    }

    simulateLiveFeed() {
        this.statusElement.textContent = 'Simulating live feed...';
        this.wsStatusElement.textContent = 'Connected (simulated)';

        let frameCount = 0;
        const interval = setInterval(() => {
            this.loadSampleFrame();
            this.framesReceivedElement.textContent = (++frameCount).toString();
            this.lastUpdateElement.textContent = new Date().toLocaleTimeString();

            if (frameCount >= 5) {
                clearInterval(interval);
                this.statusElement.textContent = 'Live feed simulation completed';
                this.wsStatusElement.textContent = 'Disconnected';
            }
        }, 2000);
    }

    clearDisplay() {
        this.processedFrame.src = '';
        this.processedFrame.style.display = 'none';

        this.statusElement.textContent = 'Display cleared';
        this.resolutionElement.textContent = '-';
        this.fpsElement.textContent = '-';
        this.frameInfoElement.textContent = 'No frame received yet';
        this.frameInfoElement.className = 'loading';
        this.framesReceivedElement.textContent = '0';
    }

    updateFrameStats(width, height) {
        const now = Date.now();
        this.frameCount++;

        if (this.lastFrameTime === 0) {
            this.lastFrameTime = now;
        }

        const elapsed = now - this.lastFrameTime;
        if (elapsed >= 1000) {
            this.currentFPS = (this.frameCount * 1000) / elapsed;
            this.frameCount = 0;
            this.lastFrameTime = now;
        }

        this.resolutionElement.textContent = width + ' × ' + height;
        this.fpsElement.textContent = this.currentFPS.toFixed(1);
        this.lastUpdateElement.textContent = new Date().toLocaleTimeString();
        this.framesReceivedElement.textContent = this.frameCount.toString();
    }

    simulateWebSocketConnection() {
        console.log('WebSocket simulation started');

        setTimeout(() => {
            this.wsStatusElement.textContent = 'Ready (simulated)';
        }, 1000);
    }

    updateDisplay() {
        this.statusElement.textContent = 'Web viewer ready';
        this.wsStatusElement.textContent = 'Initialized';
    }

    receiveFrame(frameData, width, height) {
        this.processedFrame.src = frameData;
        this.updateFrameStats(width, height);
        this.statusElement.textContent = 'Frame received from Android app';
    }
}

// Initialize when DOM loads
document.addEventListener('DOMContentLoaded', () => {
    new EdgeDetectionViewer();
    console.log('Edge Detection Web Viewer initialized');
});
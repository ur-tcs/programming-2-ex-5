const SLOW_REFRESH_DELAY = 1000 / 3;
const FAST_REFRESH_DELAY = 1000 / 30;
let REFRESH_DELAY = FAST_REFRESH_DELAY;

ABORT_TIMEOUT = () => ({ signal: AbortSignal.timeout(500) })

// from https://stackoverflow.com/a/1909508/1846915
function delay(fn, ms) {
  let timer = 0
  return function(...args) {
    clearTimeout(timer)
    timer = setTimeout(fn.bind(this, ...args), ms || 0)
  }
}

const debug = () => {}

class Canvas {
    constructor(root) {
        this.root = root;
        // width is logical, clientWidth is physical
        this.root.width = this.root.clientWidth;
        this.root.height = this.root.clientHeight;
        this.ctx = this.root.getContext("2d");
        this.ctx.globalCompositeOperation = "source-over";
        this.trace = false;
    }

    clear() {
        this.ctx.clearRect(0, 0, this.root.width, this.root.height);
    }

    renderBoid = function(boid, size) {
        var norm = Math.sqrt(boid.vx * boid.vx + boid.vy * boid.vy);
        this.ctx.beginPath();
        if (norm) {
            var vx = boid.vx / norm * size;
            var vy = boid.vy / norm * size;
            this.ctx.moveTo(boid.x + 4 * vx, boid.y + 4 * vy);
            this.ctx.lineTo(boid.x - vy, boid.y + vx);
            this.ctx.lineTo(boid.x + vy, boid.y - vx);
        } else {
            this.ctx.moveTo(boid.x + 2, boid.y + 2);
            this.ctx.lineTo(boid.x + 2, boid.y - 2);
            this.ctx.lineTo(boid.x - 2, boid.y - 2);
            this.ctx.lineTo(boid.x - 2, boid.y + 2);
        }
        this.ctx.fill();
    }

    renderFlock(flock, style, size) {
        this.ctx.fillStyle = style;
        flock.forEach((boid) => this.renderBoid(boid, size));
    }

    renderReference(flock) {
        this.renderFlock(flock, "#8AE234", 2);
    }

    render(flock) {
        if (!this.trace) {
            this.ctx.clearRect(0, 0, this.root.width, this.root.height);
        }
        this.renderFlock(flock, "#3465A4", 3);
    }
}

class App {
    initConfigBox = document.getElementById("initConfig");
    textboxes = Array.from(document.querySelectorAll(".physics input"));

    runButton = document.getElementById("runButton");
    stepButton = document.getElementById("debugStep");
    resetButton = document.getElementById("resetButton");
    presetsSelect = document.getElementById("presets");
    referenceCheckbox = document.getElementById("reference");
    canvas = document.getElementById("canvas");

    running = false;
    fetching = false;
    boidJson = null;
    lastFetchDate = 0;
    stepIndex = 0;

    textboxesEditable = true;
    viewActive = false;
    initial = undefined;
    reference = undefined;

    serverDown = function(err) {
        console.debug(err);
        this.stop();
        document
            .getElementById("serverDown")
            .style.visibility = "visible";
    }

    uiRunning = function() {
        this.running = true;
        runButton.textContent = "pause";
    }

    uiNotRunning = function() {
        this.running = false;
        runButton.textContent = "run";
    }
    
    activateView = function() {
        this.viewActive = true;
        this.runButton.disabled = false;
        this.stepButton.disabled = false;
        this.canvas.style.background = "white";
    }
    
    deactivateView = function() {
        this.viewActive = false;
        this.runButton.disabled = true;
        this.stepButton.disabled = true;
        this.canvas.style.background = "#b0b0b0";
    }

    activateTextboxes = function() {
        this.initConfigBox.disabled = false;
        this.textboxes.forEach((tb) => tb.disabled = false);
    }

    deactivateTextboxes = function() {
        this.initConfigBox.disabled = true;
        this.textboxes.forEach((tb) => tb.disabled = true);
    }

    activateReference = function(r) {
        this.reference = r;
        this.referenceCheckbox.checked = true;
        this.referenceCheckbox.disabled = false;
    }

    deactivateReference = function() {
        this.reference = undefined;
        this.referenceCheckbox.checked = false;
        this.referenceCheckbox.disabled = true;
    }

    runningState = function() {
        this.uiRunning();
        this.deactivateTextboxes();
        this.activateView();
        if (this.initial == undefined) console.debug("initial undefined while running!");
    }

    steppingState = function() {
        this.uiNotRunning();
        this.deactivateTextboxes();
        this.activateView();
        if (this.initial == undefined) console.debug("initial undefined while stepping!");
    }

    presetReadyState = function() {
        this.uiNotRunning();
        this.activateTextboxes();
        this.activateView();
        if (this.initial == undefined) console.debug("initial undefined while preset ready!");
        if (this.reference == undefined) console.debug("reference undefined while preset ready!");
    }

    nonPresetReadyState = function() {
        this.uiNotRunning();
        this.activateTextboxes();
        this.activateView();
        if (this.initial == undefined) console.debug("initial undefined while non-preset ready!");
        this.deactivateReference();
    }

    nonReadyState = function() {
        this.uiNotRunning();
        this.activateTextboxes();
        this.deactivateView();
        this.initial = undefined;
        this.deactivateReference();
    }


    run = function() {
        if (!this.viewActive) console.debug("should not be able to click run while view not active!");
        this.runningState();
        this.loop(this.lastFetchDate);
    }

    step = function() {
        if (!this.viewActive) console.debug("should not be able to click step while view not active!");
        this.steppingState();
        this.fetchUpdateAndRender(true);
    }

    stop = function() {
        if (!this.viewActive) console.debug("should not be able to click stop while view not active!");
        this.steppingState();
        this.displayBoids();
    }

    reset = function() {
        this.initializeWith(this.initial);
        if (this.reference == undefined) {
            this.nonPresetReadyState();
        } else {
            this.presetReadyState();
        }
    }

    initializeWith = function(config) {
        return fetch('http://localhost:8080/initializeWith', {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(config),
            ...ABORT_TIMEOUT()
        })
            .then((response) => response.json())
            .then((config) => {
                this.initial = config;
                this.stepIndex = 0;
                this.boidJson = config.initialBoids;
                this.display.clear();
                this.displayBoids();
            })
            .catch((err) => this.serverDown(err));
    }

    selectPreset = function(preset) {
        var request;
        if (preset == "random") {
            request = fetch(`http://localhost:8080/initializeRandom?boidsCount=500`, ABORT_TIMEOUT());
        } else {
            request = fetch(`http://localhost:8080/testCase?name=${preset}`, ABORT_TIMEOUT());
        }
        return request
            .then((response) => response.json())
            .then((testCase) => {
                this.activateReference(testCase.reference || []);
                this.initConfigBox.value = JSON.stringify(testCase.initialBoids);
                for (const [param, value] of Object.entries(testCase.physics)) {
                    document.getElementById(param).value = value.toString();
                }
                return testCase;
            })
            .then((testCase) => {
                this.initializeWith(testCase)
                    .then(() => this.presetReadyState());
            })
            .catch((err) => this.serverDown(err));
    }

    tryConfiguration = function() {
        var boids;
        var physics = {};
        try {
            boids = JSON.parse(this.initConfigBox.value);
            for (const tb of this.textboxes) {
                var parsed = parseFloat(tb.value);
                if (Number.isFinite(parsed)) {
                    physics[tb.id] = parsed;
                } else {
                    return false;
                }
            }
            return this.initializeWith({ "initialBoids": boids, "physics": physics });
        } catch (e) {
            return false;
        }
    }

    changeTextboxes = function() {
        this.presetsSelect.value = "custom";
        this.nonReadyState();
        var attempt = this.tryConfiguration();
        if (attempt) {
            attempt.then(() => this.nonPresetReadyState());
        }
    }

    constructor(display) {
        this.display = display;
        this.runButton.addEventListener("click", () => {
            if (this.running) {
                this.stop();
            } else {
                this.run();
            }
        });
        this.stepButton.addEventListener("click", () => this.step());
        this.resetButton.addEventListener("click", () => this.reset());
        this.presetsSelect.addEventListener("change", (event) => {
            if (event.target.value == "custom") {
                this.initializeWith(this.initial);
                this.nonPresetReadyState();
            } else {
                this.selectPreset(event.target.value);
            }
        });
        this.initConfigBox.addEventListener(
            "input",
            delay(() => this.changeTextboxes(), 200));
        for (const tb of this.textboxes) {
            tb.addEventListener("input", delay(() => this.changeTextboxes(), 200));
        }
        fetch('http://localhost:8080/testCases', ABORT_TIMEOUT())
            .then((response) => response.json())
            .then((testCases) => {
                for (const testCase of testCases) {
                    var opt = document.createElement('option');
                    opt.value = testCase;
                    opt.innerHTML = testCase.slice(3);
                    this.presetsSelect.appendChild(opt);
                }
                var random = document.createElement('option');
                random.value = "random";
                random.innerHTML = "random"
                this.presetsSelect.appendChild(random);
                var custom = document.createElement('option');
                custom.value = "custom";
                custom.innerHTML = "write your own&hellip;"
                this.presetsSelect.appendChild(custom);
                this.selectPreset("00_singleBoidNoForces");
            })
            .catch((err) => this.serverDown(err));
    }

    displayBoids = function() {
        this.display.render(this.boidJson);
        if (this.reference !== undefined && this.referenceCheckbox.checked && this.stepIndex < this.reference.length) {
            this.display.renderReference(this.reference[this.stepIndex]);
        }
    }

    fetchUpdateAndRender = function(forceRender) {
        if (this.fetching) {
            debug("Skipped frame");
            this.late = true;
            return;
        }
        this.fetching = true;
        var oldStepIndex = this.stepIndex;
        return fetch(`http://localhost:8080/get`, ABORT_TIMEOUT())
            .then((response) => response.json())
            .then((boidJson) => {
                if (this.stepIndex == oldStepIndex) {
                    this.stepIndex += 1;
                    this.boidJson = boidJson;
                } else {
                    console.debug("outdated response");
                }
            })
            .catch((err) => this.serverDown(err))
            .finally(() => {
                this.fetching = false;
                if (this.late || forceRender || !this.running) {
                    this.displayBoids();
                    this.late = false;
                }
            });
    }

    loop(time_elapsed) {
        if (!this.running) {
            return;
        }

        window.requestAnimationFrame((new_time) => this.loop(new_time));
        this.displayBoids();

        // If we hit the refresh rate
        if(time_elapsed - this.lastFetchDate >= REFRESH_DELAY) {
            debug(`Refreshing after ${(time_elapsed - this.lastFetchDate)}`);
            this.lastFetchDate = time_elapsed;
            this.fetchUpdateAndRender(false);
        }
    }
}

function updateTrace(canvas) {
    canvas.trace = trace.checked;
}

function updateSlow() {
    REFRESH_DELAY = slow.checked? SLOW_REFRESH_DELAY: FAST_REFRESH_DELAY;
}

function Run() {
    const canvas = new Canvas(document.getElementById("canvas"));
    const trace = document.getElementById("trace");
    trace.addEventListener("change", () => updateTrace(canvas));
    updateTrace(canvas);
    const slow = document.getElementById("slow");
    slow.addEventListener("change", updateSlow);
    updateSlow();

    const app = new App(canvas);

    window.Jitter = 2;
    window.MouseX = window.MouseY = 0;

    const legendFlock = [{"x": 1, "y": 5, "vx": 1, "vy": 0}];
    new Canvas(document.getElementById("legendMine"))
        .render(legendFlock);
    new Canvas(document.getElementById("legendReference"))
        .renderReference(legendFlock);
}

window.onload = () => Run();

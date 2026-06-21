let currentTabId = null;
let currentStatus = 'idle';
let currentSteps = [];

const actionIcons = {
    click: '👆',
    input: '⌨️',
    select: '📋',
    extract: '📥',
    navigate: '🌐',
    scroll: '📜',
    hover: '🖱️',
    screenshot: '📷',
    wait: '⏳',
    press: '⌨️',
    check: '☑️',
    uncheck: '🔲'
};

document.addEventListener('DOMContentLoaded', async () => {
    const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
    if (tabs[0]) {
        currentTabId = tabs[0].id;
        document.getElementById('current-url').textContent = tabs[0].url || '-';
        updateStatus();
    }

    document.getElementById('btn-start').addEventListener('click', startRecording);
    document.getElementById('btn-pause').addEventListener('click', pauseRecording);
    document.getElementById('btn-resume').addEventListener('click', resumeRecording);
    document.getElementById('btn-stop').addEventListener('click', stopRecording);
    document.getElementById('btn-clear').addEventListener('click', clearSteps);

    setInterval(updateStatus, 1000);
});

async function sendMessageToContent(message) {
    try {
        return await chrome.tabs.sendMessage(currentTabId, message);
    } catch (e) {
        console.error('发送消息到content script失败:', e);
        if (message.type === 'START_RECORDING') {
            await injectContentScript();
            try {
                return await chrome.tabs.sendMessage(currentTabId, message);
            } catch (e2) {
                console.error('重试失败:', e2);
                throw e2;
            }
        }
        throw e;
    }
}

async function injectContentScript() {
    try {
        await chrome.scripting.executeScript({
            target: { tabId: currentTabId },
            files: ['content.js']
        });
        await chrome.scripting.insertCSS({
            target: { tabId: currentTabId },
            files: ['content.css']
        });
        await new Promise(resolve => setTimeout(resolve, 500));
    } catch (e) {
        console.error('注入content script失败:', e);
        throw e;
    }
}

async function startRecording() {
    try {
        await sendMessageToContent({ type: 'START_RECORDING' });
        currentStatus = 'recording';
        updateUI();
    } catch (e) {
        showError('无法开始录制，请刷新页面后重试');
    }
}

async function pauseRecording() {
    try {
        const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
        if (tabs[0]) {
            chrome.tabs.sendMessage(tabs[0].id, { type: 'TOGGLE_PAUSE' });
        }
        currentStatus = 'paused';
        updateUI();
    } catch (e) {
        console.error(e);
    }
}

async function resumeRecording() {
    try {
        const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
        if (tabs[0]) {
            chrome.tabs.sendMessage(tabs[0].id, { type: 'TOGGLE_PAUSE' });
        }
        currentStatus = 'recording';
        updateUI();
    } catch (e) {
        console.error(e);
    }
}

async function stopRecording() {
    try {
        await sendMessageToContent({ type: 'STOP_RECORDING' });
        currentStatus = 'idle';
        updateUI();
    } catch (e) {
        showError('停止录制失败');
    }
}

async function clearSteps() {
    try {
        await sendMessageToContent({ type: 'CLEAR_STEPS' });
        currentSteps = [];
        updateStepsList();
    } catch (e) {
        console.error(e);
    }
}

async function updateStatus() {
    try {
        const response = await sendMessageToContent({ type: 'GET_STATUS' });
        if (response) {
            if (response.recording) {
                currentStatus = response.paused ? 'paused' : 'recording';
            } else {
                currentStatus = 'idle';
            }
            currentSteps = response.steps || [];
            updateUI();
            updateStepsList();
        }
    } catch (e) {
    }
}

function updateUI() {
    const statusBadge = document.getElementById('status-badge');
    const btnStart = document.getElementById('btn-start');
    const btnPause = document.getElementById('btn-pause');
    const btnResume = document.getElementById('btn-resume');
    const btnStop = document.getElementById('btn-stop');
    const stepCount = document.getElementById('step-count');

    statusBadge.className = 'status-badge';
    if (currentStatus === 'recording') {
        statusBadge.classList.add('status-recording');
        statusBadge.innerHTML = '<span class="dot"></span>录制中';
        btnStart.style.display = 'none';
        btnPause.style.display = 'flex';
        btnResume.style.display = 'none';
        btnStop.style.display = 'flex';
    } else if (currentStatus === 'paused') {
        statusBadge.classList.add('status-paused');
        statusBadge.innerHTML = '<span class="dot"></span>已暂停';
        btnStart.style.display = 'none';
        btnPause.style.display = 'none';
        btnResume.style.display = 'flex';
        btnStop.style.display = 'flex';
    } else {
        statusBadge.classList.add('status-idle');
        statusBadge.innerHTML = '<span class="dot"></span>空闲';
        btnStart.style.display = 'flex';
        btnPause.style.display = 'none';
        btnResume.style.display = 'none';
        btnStop.style.display = 'none';
    }

    stepCount.textContent = currentSteps.length;
}

function updateStepsList() {
    const stepsList = document.getElementById('steps-list');

    if (currentSteps.length === 0) {
        stepsList.innerHTML = '<div class="empty-state">暂无步骤，点击开始录制</div>';
        return;
    }

    stepsList.innerHTML = currentSteps.slice(-8).map((step, index) => {
        const realIndex = currentSteps.length > 8 ? currentSteps.length - 8 + index : index;
        const icon = actionIcons[step.action] || '❓';
        const target = step.selector || step.url || (step.value ? `= ${step.value}` : '');
        return `
            <div class="step-item">
                <span class="step-index">${realIndex + 1}</span>
                <span class="step-icon">${icon}</span>
                <span class="step-action">${step.action}</span>
                <span class="step-target" title="${target}">${target}</span>
            </div>
        `;
    }).join('');
}

function showError(message) {
    const statusSection = document.querySelector('.status-section');
    const errorDiv = document.createElement('div');
    errorDiv.style.cssText = `
        background: #fff1f0;
        color: #ff4d4f;
        padding: 8px 12px;
        border-radius: 4px;
        margin-top: 12px;
        font-size: 12px;
        border: 1px solid #ffa39e;
    `;
    errorDiv.textContent = message;
    statusSection.appendChild(errorDiv);
    setTimeout(() => errorDiv.remove(), 3000);
}

chrome.runtime.onMessage.addListener((request, sender) => {
    if (request.type === 'STEP_RECORDED') {
        currentSteps = request.steps || [];
        updateUI();
        updateStepsList();
    } else if (request.type === 'RECORDING_STOPPED') {
        currentStatus = 'idle';
        currentSteps = request.steps || [];
        updateUI();
        updateStepsList();

        if (currentSteps.length > 0) {
            const jsonContent = JSON.stringify({ steps: currentSteps }, null, 2);
            chrome.storage.local.set({
                lastRecordedScript: jsonContent,
                lastRecordedUrl: request.url,
                lastRecordedTime: Date.now()
            });
        }
    }
});

chrome.runtime.onInstalled.addListener(() => {
    console.log('RPA录制助手扩展已安装');

    chrome.storage.local.set({
        isRecording: false,
        steps: [],
        recordedScripts: []
    });

    chrome.contextMenus.create({
        id: 'rpa-extract',
        title: '📥 RPA - 提取此元素数据',
        contexts: ['all']
    });

    chrome.contextMenus.create({
        id: 'rpa-screenshot',
        title: '📷 RPA - 截取当前页面',
        contexts: ['all']
    });

    chrome.contextMenus.create({
        id: 'rpa-wait',
        title: '⏳ RPA - 插入等待步骤',
        contexts: ['all']
    });

    chrome.contextMenus.create({
        id: 'rpa-start',
        title: '🎬 RPA - 开始录制',
        contexts: ['all']
    });

    chrome.contextMenus.create({
        id: 'rpa-stop',
        title: '⏹️ RPA - 停止录制',
        contexts: ['all']
    });
});

chrome.contextMenus.onClicked.addListener(async (info, tab) => {
    if (!tab.id) return;

    try {
        switch (info.menuItemId) {
            case 'rpa-start':
                chrome.tabs.sendMessage(tab.id, { type: 'START_RECORDING' });
                break;
            case 'rpa-stop':
                chrome.tabs.sendMessage(tab.id, { type: 'STOP_RECORDING' });
                break;
            case 'rpa-extract':
                chrome.tabs.sendMessage(tab.id, {
                    type: 'EXTRACT_ELEMENT',
                    x: info.pageX,
                    y: info.pageY
                });
                break;
            case 'rpa-screenshot':
                chrome.tabs.captureVisibleTab(tab.windowId, { format: 'png' }, (dataUrl) => {
                    chrome.tabs.sendMessage(tab.id, {
                        type: 'ADD_CUSTOM_STEP',
                        step: {
                            action: 'screenshot',
                            name: `screenshot_${Date.now()}`,
                            fullPage: false,
                            dataUrl: dataUrl
                        }
                    });
                });
                break;
            case 'rpa-wait':
                chrome.tabs.sendMessage(tab.id, {
                    type: 'ADD_CUSTOM_STEP',
                    step: {
                        action: 'wait',
                        seconds: 2
                    }
                });
                break;
        }
    } catch (e) {
        console.error('处理右键菜单失败:', e);
    }
});

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    switch (request.type) {
        case 'STEP_RECORDED':
            chrome.storage.local.get('recordedScripts', (result) => {
                const scripts = result.recordedScripts || [];
                if (scripts.length > 0) {
                    const lastScript = scripts[scripts.length - 1];
                    if (!lastScript.completed) {
                        lastScript.steps = request.steps;
                        lastScript.updatedAt = Date.now();
                        chrome.storage.local.set({ recordedScripts: scripts });
                    }
                }
            });
            break;

        case 'RECORDING_STARTED':
            chrome.storage.local.get('recordedScripts', (result) => {
                const scripts = result.recordedScripts || [];
                scripts.push({
                    id: Date.now(),
                    url: request.url,
                    title: request.title,
                    steps: [],
                    completed: false,
                    createdAt: Date.now(),
                    updatedAt: Date.now()
                });
                chrome.storage.local.set({ recordedScripts: scripts });
            });
            break;

        case 'RECORDING_STOPPED':
            chrome.storage.local.get('recordedScripts', (result) => {
                const scripts = result.recordedScripts || [];
                if (scripts.length > 0) {
                    const lastScript = scripts[scripts.length - 1];
                    if (!lastScript.completed) {
                        lastScript.steps = request.steps;
                        lastScript.completed = true;
                        lastScript.updatedAt = Date.now();
                        chrome.storage.local.set({ recordedScripts: scripts });
                    }
                }
            });
            break;
    }

    return true;
});

chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
    if (changeInfo.status === 'complete' && tab.url) {
        chrome.tabs.sendMessage(tabId, {
            type: 'PAGE_UPDATED',
            url: tab.url,
            title: tab.title
        }).catch(() => {});
    }
});

chrome.commands.onCommand.addListener(async (command) => {
    const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tabs[0] || !tabs[0].id) return;

    switch (command) {
        case 'start-recording':
            chrome.tabs.sendMessage(tabs[0].id, { type: 'START_RECORDING' });
            break;
        case 'stop-recording':
            chrome.tabs.sendMessage(tabs[0].id, { type: 'STOP_RECORDING' });
            break;
        case 'pause-recording':
            chrome.tabs.sendMessage(tabs[0].id, { type: 'TOGGLE_PAUSE' });
            break;
    }
});

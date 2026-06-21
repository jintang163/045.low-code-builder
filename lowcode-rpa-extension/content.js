(function() {
    'use strict';

    let isRecording = false;
    let recordedSteps = [];
    let highlightedElement = null;
    let overlayDiv = null;
    let statusBar = null;

    const actionIcons = {
        click: '👆',
        input: '⌨️',
        select: '📋',
        extract: '📥',
        navigate: '🌐',
        scroll: '📜',
        hover: '🖱️',
        screenshot: '📷',
        wait: '⏳'
    };

    function log(message, type = 'info') {
        if (statusBar) {
            statusBar.textContent = message;
            if (type === 'success') {
                statusBar.style.background = '#52c41a';
            } else if (type === 'error') {
                statusBar.style.background = '#ff4d4f';
            } else {
                statusBar.style.background = '#1677ff';
            }
        }
        console.log('[RPA Recorder]', message);
    }

    function generateSelector(element) {
        if (!element) return null;

        if (element.id) {
            return '#' + CSS.escape(element.id);
        }

        if (element.getAttribute && element.getAttribute('data-testid')) {
            return `[data-testid="${element.getAttribute('data-testid')}"]`;
        }

        if (element.getAttribute && element.getAttribute('data-id')) {
            return `[data-id="${element.getAttribute('data-id')}"]`;
        }

        if (element.name) {
            let selector = element.tagName.toLowerCase() + '[name="' + element.name + '"]';
            let count = document.querySelectorAll(selector).length;
            if (count === 1) return selector;
        }

        if (element.className && typeof element.className === 'string') {
            let classes = element.className.trim().split(/\s+/).filter(c => c && !c.includes('ng-') && !c.includes('active') && !c.includes('hover'));
            if (classes.length > 0) {
                let selector = element.tagName.toLowerCase() + '.' + classes.map(CSS.escape).join('.');
                let count = document.querySelectorAll(selector).length;
                if (count === 1) return selector;
            }
        }

        let selector = element.tagName.toLowerCase();
        let parent = element.parentElement;
        if (parent) {
            let siblings = Array.from(parent.children).filter(c => c.tagName === element.tagName);
            if (siblings.length > 1) {
                let index = siblings.indexOf(element) + 1;
                selector += `:nth-of-type(${index})`;
            }
            let parentSelector = generateSimpleSelector(parent);
            if (parentSelector) {
                selector = parentSelector + ' > ' + selector;
            }
        }

        try {
            let testCount = document.querySelectorAll(selector).length;
            if (testCount > 1) {
                selector = getXPath(element);
            }
        } catch (e) {
            selector = getXPath(element);
        }

        return selector;
    }

    function generateSimpleSelector(element) {
        if (!element) return null;
        if (element.id) return '#' + CSS.escape(element.id);
        return element.tagName.toLowerCase();
    }

    function getXPath(element) {
        let parts = [];
        while (element && element.nodeType === Node.ELEMENT_NODE) {
            let sibling = element;
            let count = 1;
            while ((sibling = sibling.previousElementSibling)) {
                if (sibling.tagName === element.tagName) count++;
            }
            let tagName = element.tagName.toLowerCase();
            parts.unshift(count > 1 ? `${tagName}[${count}]` : tagName);
            element = element.parentElement;
        }
        return '//' + parts.join('/');
    }

    function isInteractiveElement(element) {
        if (!element || element.tagName === 'HTML' || element.tagName === 'BODY') return false;

        const interactiveTags = ['A', 'BUTTON', 'INPUT', 'SELECT', 'TEXTAREA', 'OPTION', 'LABEL'];
        if (interactiveTags.includes(element.tagName)) return true;

        const role = element.getAttribute && element.getAttribute('role');
        if (role && ['button', 'link', 'checkbox', 'radio', 'textbox', 'combobox', 'option', 'menuitem'].includes(role)) {
            return true;
        }

        const style = window.getComputedStyle(element);
        if (style.cursor === 'pointer') return true;
        if (style.pointerEvents === 'none') return false;

        const onclick = element.getAttribute && element.getAttribute('onclick');
        if (onclick) return true;

        return false;
    }

    function findClosestInteractive(element) {
        let current = element;
        while (current && current !== document.body) {
            if (isInteractiveElement(current)) {
                return current;
            }
            current = current.parentElement;
        }
        return element;
    }

    function highlightElement(element) {
        if (!element) return;

        removeHighlight();

        const rect = element.getBoundingClientRect();
        overlayDiv = document.createElement('div');
        overlayDiv.style.cssText = `
            position: fixed;
            top: ${rect.top + window.scrollY}px;
            left: ${rect.left + window.scrollX}px;
            width: ${rect.width}px;
            height: ${rect.height}px;
            border: 2px solid #1677ff;
            background: rgba(22, 119, 255, 0.1);
            pointer-events: none;
            z-index: 2147483646;
            box-sizing: border-box;
            transition: all 0.1s ease;
        `;

        const label = document.createElement('div');
        const selector = generateSelector(element);
        label.textContent = selector;
        label.style.cssText = `
            position: absolute;
            top: -28px;
            left: 0;
            background: #1677ff;
            color: white;
            padding: 2px 8px;
            font-size: 12px;
            font-family: Consolas, monospace;
            border-radius: 3px;
            white-space: nowrap;
            max-width: 400px;
            overflow: hidden;
            text-overflow: ellipsis;
        `;
        overlayDiv.appendChild(label);

        document.body.appendChild(overlayDiv);
        highlightedElement = element;
    }

    function removeHighlight() {
        if (overlayDiv && overlayDiv.parentNode) {
            overlayDiv.parentNode.removeChild(overlayDiv);
        }
        overlayDiv = null;
        highlightedElement = null;
    }

    function createStatusBar() {
        if (statusBar) return;

        statusBar = document.createElement('div');
        statusBar.style.cssText = `
            position: fixed;
            top: 0;
            right: 0;
            background: #1677ff;
            color: white;
            padding: 8px 16px;
            font-size: 13px;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            z-index: 2147483647;
            border-bottom-left-radius: 8px;
            box-shadow: -2px 2px 8px rgba(0,0,0,0.2);
        `;
        statusBar.innerHTML = `
            <div style="display: flex; align-items: center; gap: 12px;">
                <span style="display: flex; align-items: center; gap: 6px;">
                    <span style="width: 8px; height: 8px; background: #ff4d4f; border-radius: 50%; animation: pulse 1s infinite;"></span>
                    RPA录制中
                </span>
                <span id="rpa-step-count">步骤: 0</span>
                <button id="rpa-pause-btn" style="background: rgba(255,255,255,0.2); border: none; color: white; padding: 2px 8px; border-radius: 4px; cursor: pointer; font-size: 12px;">暂停</button>
                <button id="rpa-stop-btn" style="background: #ff4d4f; border: none; color: white; padding: 2px 8px; border-radius: 4px; cursor: pointer; font-size: 12px;">停止</button>
            </div>
            <style>@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }</style>
        `;
        document.body.appendChild(statusBar);

        statusBar.querySelector('#rpa-pause-btn').addEventListener('click', togglePause);
        statusBar.querySelector('#rpa-stop-btn').addEventListener('click', stopRecording);
    }

    function removeStatusBar() {
        if (statusBar && statusBar.parentNode) {
            statusBar.parentNode.removeChild(statusBar);
        }
        statusBar = null;
    }

    function updateStepCount() {
        const countEl = document.querySelector('#rpa-step-count');
        if (countEl) {
            countEl.textContent = `步骤: ${recordedSteps.length}`;
        }
    }

    let isPaused = false;
    function togglePause() {
        isPaused = !isPaused;
        const btn = document.querySelector('#rpa-pause-btn');
        if (btn) {
            btn.textContent = isPaused ? '继续' : '暂停';
        }
        log(isPaused ? '录制已暂停' : '录制继续', isPaused ? 'info' : 'success');
    }

    function addStep(step) {
        if (isPaused) return;

        recordedSteps.push(step);
        updateStepCount();
        log(`${actionIcons[step.action] || ''} 已记录: ${step.action} ${step.selector || step.url || ''}`, 'success');

        chrome.runtime.sendMessage({
            type: 'STEP_RECORDED',
            step: step,
            steps: recordedSteps
        });
    }

    function handleClick(event) {
        if (!isRecording || isPaused) return;
        if (event.ctrlKey || event.metaKey) return;

        let target = findClosestInteractive(event.target);
        if (!target) return;

        event.preventDefault();
        event.stopPropagation();

        const selector = generateSelector(target);
        const step = {
            action: 'click',
            selector: selector,
            tagName: target.tagName,
            timeout: 30000
        };

        addStep(step);

        setTimeout(() => {
            target.click();
        }, 300);
    }

    function handleInput(event) {
        if (!isRecording || isPaused) return;

        const target = event.target;
        if (!target || !target.tagName) return;

        const tagName = target.tagName.toLowerCase();
        if (tagName !== 'input' && tagName !== 'textarea') return;

        if (event.inputType === 'insertText' || event.type === 'change') {
            const selector = generateSelector(target);
            const value = target.value;

            const existingIndex = recordedSteps.findIndex(s => s.action === 'input' && s.selector === selector);
            if (existingIndex >= 0 && Date.now() - (recordedSteps[existingIndex].timestamp || 0) < 2000) {
                recordedSteps[existingIndex].value = value;
                recordedSteps[existingIndex].timestamp = Date.now();
                updateStepCount();
                log(`⌨️ 已更新输入: ${selector} = ${value.substring(0, 30)}${value.length > 30 ? '...' : ''}`, 'success');
            } else {
                const step = {
                    action: 'input',
                    selector: selector,
                    value: value,
                    tagName: target.tagName,
                    inputType: target.type,
                    timeout: 30000,
                    timestamp: Date.now()
                };
                addStep(step);
            }
        }
    }

    function handleChange(event) {
        if (!isRecording || isPaused) return;

        const target = event.target;
        if (!target || !target.tagName) return;

        const tagName = target.tagName.toLowerCase();

        if (tagName === 'select') {
            const selector = generateSelector(target);
            const value = target.value;
            const step = {
                action: 'select',
                selector: selector,
                value: value,
                timeout: 30000
            };
            addStep(step);
        } else if (tagName === 'input' && (target.type === 'checkbox' || target.type === 'radio')) {
            const selector = generateSelector(target);
            const step = {
                action: target.checked ? 'check' : 'uncheck',
                selector: selector,
                timeout: 30000
            };
            addStep(step);
        } else {
            handleInput(event);
        }
    }

    function handleKeydown(event) {
        if (!isRecording || isPaused) return;

        if (event.key === 'Enter' && event.target) {
            const tagName = event.target.tagName.toLowerCase();
            if (tagName === 'input' || tagName === 'textarea') {
                const selector = generateSelector(event.target);
                const step = {
                    action: 'press',
                    selector: selector,
                    value: 'Enter',
                    timeout: 30000
                };
                addStep(step);
            }
        }
    }

    function handleMouseOver(event) {
        if (!isRecording || isPaused) return;
        if (event.ctrlKey || event.metaKey) return;

        let target = findClosestInteractive(event.target);
        if (target && target !== highlightedElement) {
            highlightElement(target);
        }
    }

    function handleMouseOut(event) {
        if (!isRecording) return;
        removeHighlight();
    }

    function handleUrlChange() {
        if (!isRecording) return;

        const url = window.location.href;
        const lastNavigateStep = [...recordedSteps].reverse().find(s => s.action === 'navigate');
        if (!lastNavigateStep || lastNavigateStep.url !== url) {
            const step = {
                action: 'navigate',
                url: url,
                timeout: 60000
            };
            addStep(step);
        }
    }

    function handleExtract(event) {
        if (!isRecording || !event.shiftKey || !event.ctrlKey) return;

        event.preventDefault();
        event.stopPropagation();

        let target = event.target;
        const selector = generateSelector(target);
        const text = (target.innerText || target.textContent || '').trim();

        const step = {
            action: 'extract',
            selector: selector,
            fieldName: `field_${recordedSteps.length + 1}`,
            extractType: 'text',
            timeout: 30000,
            extractedPreview: text.substring(0, 50)
        };
        addStep(step);

        log('提示: 按住 Shift+Ctrl 点击元素可提取数据', 'info');
    }

    function handleKeydownGlobal(event) {
        if (!isRecording) return;

        if (event.ctrlKey && event.shiftKey && event.key === 'E') {
            event.preventDefault();
            const step = {
                action: 'screenshot',
                name: `screenshot_${recordedSteps.length + 1}`,
                fullPage: false
            };
            addStep(step);
        }

        if (event.ctrlKey && event.shiftKey && event.key === 'W') {
            event.preventDefault();
            const step = {
                action: 'wait',
                seconds: 2
            };
            addStep(step);
        }

        if (event.key === 'Escape') {
            event.preventDefault();
            togglePause();
        }
    }

    function startRecording() {
        if (isRecording) return;

        isRecording = true;
        isPaused = false;
        recordedSteps = [];

        document.addEventListener('click', handleClick, true);
        document.addEventListener('input', handleInput, true);
        document.addEventListener('change', handleChange, true);
        document.addEventListener('keydown', handleKeydown, true);
        document.addEventListener('mouseover', handleMouseOver, true);
        document.addEventListener('mouseout', handleMouseOut, true);
        document.addEventListener('keydown', handleKeydownGlobal);
        document.addEventListener('click', handleExtract, true);

        createStatusBar();
        handleUrlChange();

        log('录制已开始，可点击页面元素进行录制', 'success');
        log('快捷键: Shift+Ctrl点击=提取数据 | Ctrl+Shift+E=截图 | Ctrl+Shift+W=等待2秒 | Esc=暂停', 'info');
    }

    function stopRecording() {
        isRecording = false;
        isPaused = false;

        document.removeEventListener('click', handleClick, true);
        document.removeEventListener('input', handleInput, true);
        document.removeEventListener('change', handleChange, true);
        document.removeEventListener('keydown', handleKeydown, true);
        document.removeEventListener('mouseover', handleMouseOver, true);
        document.removeEventListener('mouseout', handleMouseOut, true);
        document.removeEventListener('keydown', handleKeydownGlobal);
        document.removeEventListener('click', handleExtract, true);

        removeHighlight();
        removeStatusBar();

        chrome.runtime.sendMessage({
            type: 'RECORDING_STOPPED',
            steps: recordedSteps,
            url: window.location.href
        });

        log(`录制已停止，共记录 ${recordedSteps.length} 个步骤`, 'success');

        setTimeout(() => {
            showExportDialog();
        }, 500);
    }

    function showExportDialog() {
        const dialog = document.createElement('div');
        dialog.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: white;
            border-radius: 8px;
            box-shadow: 0 4px 24px rgba(0,0,0,0.2);
            z-index: 2147483647;
            padding: 24px;
            width: 500px;
            max-width: 90vw;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
        `;

        const jsonContent = JSON.stringify({ steps: recordedSteps }, null, 2);

        dialog.innerHTML = `
            <h3 style="margin: 0 0 16px 0; font-size: 18px;">🎬 录制完成</h3>
            <p style="margin: 0 0 16px 0; color: #666;">共记录 ${recordedSteps.length} 个操作步骤</p>
            <div style="margin-bottom: 16px;">
                <label style="display: block; margin-bottom: 8px; font-weight: 500;">脚本JSON：</label>
                <textarea id="rpa-json" readonly style="width: 100%; height: 200px; padding: 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-family: Consolas, monospace; font-size: 12px; resize: vertical;">${jsonContent}</textarea>
            </div>
            <div style="display: flex; gap: 12px; justify-content: flex-end;">
                <button id="rpa-copy-btn" style="padding: 8px 16px; border: 1px solid #d9d9d9; background: white; border-radius: 4px; cursor: pointer;">复制JSON</button>
                <button id="rpa-download-btn" style="padding: 8px 16px; border: none; background: #1677ff; color: white; border-radius: 4px; cursor: pointer;">下载脚本</button>
                <button id="rpa-close-btn" style="padding: 8px 16px; border: 1px solid #d9d9d9; background: white; border-radius: 4px; cursor: pointer;">关闭</button>
            </div>
        `;

        document.body.appendChild(dialog);

        dialog.querySelector('#rpa-copy-btn').addEventListener('click', () => {
            const textarea = dialog.querySelector('#rpa-json');
            textarea.select();
            document.execCommand('copy');
            log('已复制到剪贴板', 'success');
        });

        dialog.querySelector('#rpa-download-btn').addEventListener('click', () => {
            const blob = new Blob([jsonContent], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'rpa-script.json';
            a.click();
            URL.revokeObjectURL(url);
        });

        dialog.querySelector('#rpa-close-btn').addEventListener('click', () => {
            document.body.removeChild(dialog);
        });
    }

    chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
        switch (request.type) {
            case 'START_RECORDING':
                startRecording();
                sendResponse({ success: true, status: 'recording' });
                break;
            case 'STOP_RECORDING':
                stopRecording();
                sendResponse({ success: true, steps: recordedSteps });
                break;
            case 'GET_STATUS':
                sendResponse({
                    recording: isRecording,
                    paused: isPaused,
                    stepCount: recordedSteps.length,
                    steps: recordedSteps
                });
                break;
            case 'GET_STEPS':
                sendResponse({ steps: recordedSteps });
                break;
            case 'CLEAR_STEPS':
                recordedSteps = [];
                updateStepCount();
                sendResponse({ success: true });
                break;
        }
        return true;
    });

    window.addEventListener('hashchange', handleUrlChange);
    window.addEventListener('popstate', handleUrlChange);

    const originalPushState = history.pushState;
    history.pushState = function() {
        originalPushState.apply(this, arguments);
        setTimeout(handleUrlChange, 100);
    };

    const originalReplaceState = history.replaceState;
    history.replaceState = function() {
        originalReplaceState.apply(this, arguments);
        setTimeout(handleUrlChange, 100);
    };

})();

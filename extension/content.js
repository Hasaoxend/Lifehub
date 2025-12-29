// LifeHub Content Script
// Detects login forms and handles autofill

(function() {
  'use strict';
  
  let dropdownActive = false;
  let currentDropdown = null;
  
  // Inject styles for dropdown
  function injectStyles() {
    if (document.getElementById('lifehub-dropdown-styles')) return;
    
    const style = document.createElement('style');
    style.id = 'lifehub-dropdown-styles';
    style.textContent = `
      .lifehub-icon-wrapper {
        position: absolute !important;
        right: 8px !important;
        top: 50% !important;
        transform: translateY(-50%) !important;
        cursor: pointer !important;
        z-index: 99999 !important;
        opacity: 0.8 !important;
        transition: opacity 0.2s !important;
        pointer-events: auto !important;
        width: 20px !important;
        height: 20px !important;
        display: flex !important;
        align-items: center !important;
        justify-content: center !important;
      }
      .lifehub-icon-wrapper:hover {
        opacity: 1 !important;
      }
      .lifehub-dropdown {
        position: fixed !important;
        width: 260px !important;
        max-height: 280px !important;
        overflow-y: auto !important;
        background: #1e1e2e !important;
        border: 1px solid #6366f1 !important;
        border-radius: 10px !important;
        box-shadow: 0 8px 30px rgba(0,0,0,0.5) !important;
        z-index: 2147483647 !important;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif !important;
      }
      .lifehub-dropdown * {
        box-sizing: border-box !important;
      }
      .lifehub-dropdown-header {
        padding: 10px 14px !important;
        border-bottom: 1px solid rgba(255,255,255,0.1) !important;
        display: flex !important;
        align-items: center !important;
        gap: 8px !important;
        background: #1e1e2e !important;
        border-radius: 10px 10px 0 0 !important;
      }
      .lifehub-dropdown-header svg {
        color: #6366f1 !important;
      }
      .lifehub-dropdown-header span {
        font-size: 13px !important;
        font-weight: 600 !important;
        color: #f8fafc !important;
      }
      .lifehub-dropdown-content {
        background: #1e1e2e !important;
      }
      .lifehub-dropdown-item {
        padding: 10px 14px !important;
        cursor: pointer !important;
        border-bottom: 1px solid rgba(255,255,255,0.05) !important;
        transition: background 0.15s !important;
        background: #1e1e2e !important;
      }
      .lifehub-dropdown-item:hover {
        background: #2e2e4e !important;
      }
      .lifehub-dropdown-item:last-child {
        border-bottom: none !important;
      }
      .lifehub-dropdown-item-name {
        font-size: 12px !important;
        font-weight: 500 !important;
        color: #f8fafc !important;
        margin-bottom: 2px !important;
      }
      .lifehub-dropdown-item-user {
        font-size: 10px !important;
        color: #94a3b8 !important;
      }
      .lifehub-dropdown-empty {
        padding: 16px !important;
        text-align: center !important;
        color: #64748b !important;
        font-size: 11px !important;
        background: #1e1e2e !important;
      }
      .lifehub-dropdown-footer {
        padding: 8px 14px !important;
        border-top: 1px solid rgba(255,255,255,0.1) !important;
        text-align: center !important;
        background: #1e1e2e !important;
        border-radius: 0 0 10px 10px !important;
      }
      .lifehub-dropdown-footer a {
        font-size: 10px !important;
        color: #6366f1 !important;
        text-decoration: none !important;
      }
      .lifehub-dropdown-footer a:hover {
        text-decoration: underline !important;
      }
      /* Save Credentials Popup */
      .lifehub-save-popup {
        position: fixed !important;
        top: 20px !important;
        right: 20px !important;
        width: 320px !important;
        background: #1e1e2e !important;
        border: 1px solid #6366f1 !important;
        border-radius: 12px !important;
        box-shadow: 0 10px 40px rgba(0,0,0,0.5) !important;
        z-index: 2147483647 !important;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif !important;
        animation: lifehub-slide-in 0.3s ease !important;
      }
      @keyframes lifehub-slide-in {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
      }
      .lifehub-save-popup-header {
        padding: 14px 16px !important;
        border-bottom: 1px solid rgba(255,255,255,0.1) !important;
        display: flex !important;
        align-items: center !important;
        justify-content: space-between !important;
      }
      .lifehub-save-popup-title {
        display: flex !important;
        align-items: center !important;
        gap: 10px !important;
      }
      .lifehub-save-popup-title span {
        font-size: 14px !important;
        font-weight: 600 !important;
        color: #f8fafc !important;
      }
      .lifehub-save-popup-close {
        background: none !important;
        border: none !important;
        color: #64748b !important;
        cursor: pointer !important;
        padding: 4px !important;
        display: flex !important;
      }
      .lifehub-save-popup-close:hover {
        color: #f8fafc !important;
      }
      .lifehub-save-popup-body {
        padding: 16px !important;
      }
      .lifehub-save-popup-info {
        background: rgba(99, 102, 241, 0.1) !important;
        border-radius: 8px !important;
        padding: 12px !important;
        margin-bottom: 12px !important;
      }
      .lifehub-save-popup-label {
        font-size: 10px !important;
        color: #64748b !important;
        text-transform: uppercase !important;
        margin-bottom: 4px !important;
      }
      .lifehub-save-popup-value {
        font-size: 13px !important;
        color: #f8fafc !important;
        word-break: break-all !important;
      }
      .lifehub-save-popup-actions {
        display: flex !important;
        gap: 10px !important;
        margin-top: 12px !important;
      }
      .lifehub-save-popup-btn {
        flex: 1 !important;
        padding: 10px 16px !important;
        border-radius: 8px !important;
        font-size: 13px !important;
        font-weight: 500 !important;
        cursor: pointer !important;
        border: none !important;
        transition: all 0.2s !important;
      }
      .lifehub-save-popup-btn.primary {
        background: #6366f1 !important;
        color: white !important;
      }
      .lifehub-save-popup-btn.primary:hover {
        background: #4f46e5 !important;
      }
      .lifehub-save-popup-btn.secondary {
        background: rgba(255,255,255,0.1) !important;
        color: #94a3b8 !important;
      }
      .lifehub-save-popup-btn.secondary:hover {
        background: rgba(255,255,255,0.15) !important;
      }
    `;
    document.head.appendChild(style);
  }
  
  // Find login forms
  function findLoginForms() {
    const forms = [];
    
    // Find all password fields
    const passwordInputs = document.querySelectorAll(
      'input[type="password"], input[autocomplete*="password"]'
    );
    
    passwordInputs.forEach(passwordInput => {
      const form = passwordInput.closest('form') || document.body;
      
      // Try to find username field
      let usernameInput = null;
      
      // Check siblings and form children
      const possibleUsernames = form.querySelectorAll(`
        input[type="email"],
        input[type="text"][autocomplete*="user"],
        input[type="text"][autocomplete*="email"],
        input[name*="user"],
        input[name*="email"],
        input[name*="login"],
        input[id*="user"],
        input[id*="email"],
        input[id*="login"],
        input[type="text"]
      `);
      
      // Find the closest text input before password
      for (const input of possibleUsernames) {
        if (input !== passwordInput && isVisible(input)) {
          // Check if this input comes before password input in DOM
          const position = passwordInput.compareDocumentPosition(input);
          if (position & Node.DOCUMENT_POSITION_PRECEDING) {
            usernameInput = input;
          }
        }
      }
      
      // If no username found before, take first visible text input
      if (!usernameInput) {
        for (const input of possibleUsernames) {
          if (input !== passwordInput && isVisible(input)) {
            usernameInput = input;
            break;
          }
        }
      }
      
      if (usernameInput || passwordInput) {
        forms.push({
          form,
          usernameInput,
          passwordInput
        });
      }
    });
    
    return forms;
  }
  
  function isVisible(element) {
    if (!element) return false;
    const style = window.getComputedStyle(element);
    return style.display !== 'none' && 
           style.visibility !== 'hidden' && 
           style.opacity !== '0' &&
           element.offsetParent !== null;
  }
  
  // Fill credentials into form
  function fillForm(username, password) {
    const forms = findLoginForms();
    
    if (forms.length === 0) {
      console.log('LifeHub: No login forms found');
      return false;
    }
    
    const { usernameInput, passwordInput } = forms[0];
    
    if (usernameInput && username) {
      setInputValue(usernameInput, username);
    }
    
    if (passwordInput && password) {
      setInputValue(passwordInput, password);
    }
    
    console.log('LifeHub: Credentials filled');
    return true;
  }
  
  function setInputValue(input, value) {
    // Focus the input
    input.focus();
    
    // Dispatch events to trigger React/Vue/Angular change detection
    const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
      window.HTMLInputElement.prototype, 'value'
    ).set;
    
    nativeInputValueSetter.call(input, value);
    
    // Trigger input event
    input.dispatchEvent(new Event('input', { bubbles: true }));
    input.dispatchEvent(new Event('change', { bubbles: true }));
    
    // For React 16+
    const tracker = input._valueTracker;
    if (tracker) {
      tracker.setValue('');
    }
    
    input.dispatchEvent(new Event('input', { bubbles: true }));
  }
  
  // Close dropdown when clicking outside
  function handleOutsideClick(e) {
    if (currentDropdown && !currentDropdown.contains(e.target) && 
        !e.target.closest('.lifehub-icon-wrapper')) {
      closeDropdown();
    }
  }
  
  // Close dropdown on scroll (only if scrolling outside dropdown)
  function handleScroll(e) {
    // Don't close if scrolling inside the dropdown
    if (currentDropdown && currentDropdown.contains(e.target)) {
      return;
    }
    closeDropdown();
  }
  
  function closeDropdown() {
    if (currentDropdown) {
      currentDropdown.remove();
      currentDropdown = null;
      dropdownActive = false;
      document.removeEventListener('click', handleOutsideClick);
      window.removeEventListener('scroll', handleScroll, true);
    }
  }
  
  // Show dropdown with accounts
  async function showDropdown(wrapper) {
    if (dropdownActive) {
      closeDropdown();
      return;
    }
    
    // Request accounts from background
    chrome.runtime.sendMessage({ action: 'getAccounts' }, (response) => {
      // Check if authentication required
      if (response && response.requiresAuth) {
        createAuthRequiredDropdown(wrapper, response.message);
        return;
      }
      
      if (!response || !response.accounts) {
        createDropdown(wrapper, []);
        return;
      }
      
      // Filter accounts matching current domain
      const currentDomain = window.location.hostname.replace('www.', '');
      const matchingAccounts = response.accounts.filter(acc => {
        const accDomain = (acc.website || '').replace('www.', '').replace(/https?:\/\//, '');
        return accDomain.includes(currentDomain) || currentDomain.includes(accDomain);
      });
      
      createDropdown(wrapper, matchingAccounts.length > 0 ? matchingAccounts : response.accounts.slice(0, 5));
    });
  }
  
  // Create dropdown showing login required message
  function createAuthRequiredDropdown(wrapper, message) {
    closeDropdown();
    
    const dropdown = document.createElement('div');
    dropdown.className = 'lifehub-dropdown';
    
    dropdown.innerHTML = `
      <div class="lifehub-dropdown-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M12 2L4 7V11C4 16.5 7.84 21.74 12 23C16.16 21.74 20 16.5 20 11V7L12 2Z" 
                fill="#6366f1" stroke="white" stroke-width="1"/>
        </svg>
        <span>LifeHub</span>
      </div>
      <div class="lifehub-dropdown-content">
        <div class="lifehub-dropdown-empty" style="padding: 20px;">
          <div style="margin-bottom: 10px;">🔒</div>
          ${message || 'Vui lòng đăng nhập để sử dụng tính năng tự động điền.'}
        </div>
      </div>
      <div class="lifehub-dropdown-footer">
        <a href="https://lifehub-project-d8c97.web.app" target="_blank" class="lifehub-open-web">Mở LifeHub (Web)</a>
      </div>
    `;
    
    // Position dropdown using fixed positioning
    const wrapperRect = wrapper.getBoundingClientRect();
    dropdown.style.top = (wrapperRect.bottom + 5) + 'px';
    dropdown.style.left = Math.max(10, wrapperRect.right - 260) + 'px';
    
    document.body.appendChild(dropdown);
    currentDropdown = dropdown;
    dropdownActive = true;
    
    // Close on outside click
    setTimeout(() => {
      document.addEventListener('click', handleOutsideClick);
      window.addEventListener('scroll', handleScroll, true);
    }, 100);
  }
  
  function createDropdown(wrapper, accounts) {
    closeDropdown();
    
    const dropdown = document.createElement('div');
    dropdown.className = 'lifehub-dropdown';
    
    dropdown.innerHTML = `
      <div class="lifehub-dropdown-header">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M12 2L4 7V11C4 16.5 7.84 21.74 12 23C16.16 21.74 20 16.5 20 11V7L12 2Z" 
                fill="#6366f1" stroke="white" stroke-width="1"/>
        </svg>
        <span>LifeHub Autofill</span>
      </div>
      <div class="lifehub-dropdown-content">
        ${accounts.length === 0 ? 
          `<div class="lifehub-dropdown-empty">Không tìm thấy tài khoản phù hợp.<br>Mở extension để thêm tài khoản.</div>` :
          accounts.map((acc, i) => `
            <div class="lifehub-dropdown-item" data-index="${i}">
              <div class="lifehub-dropdown-item-name">${escapeHtml(acc.name || acc.website || 'Tài khoản')}</div>
              <div class="lifehub-dropdown-item-user">${escapeHtml(acc.username || acc.email || '')}</div>
            </div>
          `).join('')
        }
      </div>
      <div class="lifehub-dropdown-footer">
        <a href="https://lifehub-project-d8c97.web.app" target="_blank" class="lifehub-open-web">Mở LifeHub (Web)</a>
      </div>
    `;
    
    // Position dropdown using fixed positioning
    const wrapperRect = wrapper.getBoundingClientRect();
    dropdown.style.cssText = `
      top: ${wrapperRect.bottom + 5}px !important;
      left: ${Math.max(10, wrapperRect.right - 260)}px !important;
    `;
    
    document.body.appendChild(dropdown);
    currentDropdown = dropdown;
    dropdownActive = true;
    
    // Add click handlers
    dropdown.querySelectorAll('.lifehub-dropdown-item').forEach((item, index) => {
      item.addEventListener('click', () => {
        const acc = accounts[index];
        if (acc) {
          // Request password decryption and fill
          chrome.runtime.sendMessage({ 
            action: 'fillCredentials', 
            accountId: acc.id 
          }, (response) => {
            if (response && response.username && response.password) {
              fillForm(response.username, response.password);
            }
            closeDropdown();
          });
        }
      });
    });
    
    // Close on outside click
    setTimeout(() => {
      document.addEventListener('click', handleOutsideClick);
      window.addEventListener('scroll', handleScroll, true);
    }, 100);
  }
  
  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;')
              .replace(/</g, '&lt;')
              .replace(/>/g, '&gt;')
              .replace(/"/g, '&quot;');
  }
  
  // Add LifeHub icon to email/username fields only (NOT password fields)
  function addAutofillIcons() {
    // Strategy 1: Find inputs with specific attributes
    const emailInputs = new Set(document.querySelectorAll(`
      input[type="email"],
      input[type="text"][autocomplete*="user"],
      input[type="text"][autocomplete*="email"],
      input[type="tel"],
      input[name*="user" i],
      input[name*="email" i],
      input[name*="login" i],
      input[name*="student" i],
      input[name*="phone" i],
      input[name*="account" i],
      input[name*="masv" i],
      input[name*="mssv" i],
      input[id*="user" i],
      input[id*="email" i],
      input[id*="login" i],
      input[id*="account" i],
      input[id*="phone" i],
      input[placeholder*="email" i],
      input[placeholder*="user" i],
      input[placeholder*="phone" i],
      input[placeholder*="đăng nhập" i],
      input[placeholder*="tài khoản" i],
      input[placeholder*="sinh viên" i],
      input[placeholder*="mã sv" i]
    `));
    
    // Strategy 2: Find text inputs that appear before password fields
    const passwordInputs = document.querySelectorAll('input[type="password"]');
    passwordInputs.forEach(pwdInput => {
      const form = pwdInput.closest('form') || pwdInput.parentElement?.parentElement?.parentElement;
      if (form) {
        const textInputs = form.querySelectorAll('input[type="text"], input[type="email"], input[type="tel"]');
        textInputs.forEach(input => {
          const position = pwdInput.compareDocumentPosition(input);
          if (position & Node.DOCUMENT_POSITION_PRECEDING) {
            emailInputs.add(input);
          }
        });
      }
    });
    
    emailInputs.forEach(input => {
      // Skip if already has icon or is a password field
      if (input.dataset.lifehubIcon || input.type === 'password') return;
      if (!isVisible(input)) return;
      
      input.dataset.lifehubIcon = 'true';
      
      // Get input position
      const inputRect = input.getBoundingClientRect();
      if (inputRect.width === 0 || inputRect.height === 0) return;
      
      // Create icon element
      const wrapper = document.createElement('div');
      wrapper.className = 'lifehub-icon-wrapper';
      wrapper.innerHTML = `
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M12 2L4 7V11C4 16.5 7.84 21.74 12 23C16.16 21.74 20 16.5 20 11V7L12 2Z" 
                fill="#6366f1" stroke="white" stroke-width="1.5"/>
        </svg>
      `;
      
      // Position icon using input's parent as reference
      const parent = input.parentElement;
      if (parent) {
        const parentStyle = window.getComputedStyle(parent);
        if (parentStyle.position === 'static') {
          parent.style.position = 'relative';
        }
        
        // Calculate position relative to input within parent
        const parentRect = parent.getBoundingClientRect();
        const rightOffset = parentRect.right - inputRect.right + 8;
        const topOffset = inputRect.top - parentRect.top + (inputRect.height / 2);
        
        wrapper.style.cssText = `
          position: absolute !important;
          right: ${Math.max(rightOffset, 8)}px !important;
          top: ${topOffset}px !important;
          transform: translateY(-50%) !important;
          cursor: pointer !important;
          z-index: 99999 !important;
          opacity: 0.8 !important;
          width: 18px !important;
          height: 18px !important;
          display: flex !important;
          align-items: center !important;
          justify-content: center !important;
          pointer-events: auto !important;
        `;
        
        // Add padding to input so text doesn't overlap with icon
        const currentPadding = parseInt(window.getComputedStyle(input).paddingRight) || 0;
        if (currentPadding < 28) {
          input.style.paddingRight = '30px';
        }
        
        wrapper.addEventListener('mouseover', () => wrapper.style.opacity = '1');
        wrapper.addEventListener('mouseout', () => wrapper.style.opacity = '0.8');
        wrapper.addEventListener('click', (e) => {
          e.stopPropagation();
          e.preventDefault();
          showDropdown(wrapper);
        });
        parent.appendChild(wrapper);
      }
    });
  }
  
  // Listen for messages from popup
  chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.action === 'fill') {
      const success = fillForm(message.username, message.password);
      sendResponse({ success });
    }
    return true;
  });
  
  // Initialize
  function init() {
    injectStyles();
    
    // Add icons after page load
    setTimeout(addAutofillIcons, 1000);
    
    // Watch for dynamic content
    const observer = new MutationObserver(() => {
      addAutofillIcons();
    });
    
    observer.observe(document.body, {
      childList: true,
      subtree: true
    });
    
    // Listen for form submissions to offer saving credentials
    setupFormSubmitListener();
    
    // Check for pending save from previous page
    checkPendingCredentials();
  }
  
  // Check if there are pending credentials to save from form submission
  function checkPendingCredentials() {
    chrome.storage.local.get(['lifehub_pending_save'], (result) => {
      const pending = result.lifehub_pending_save;
      if (pending && pending.timestamp) {
        // Only show if less than 30 seconds old
        const age = Date.now() - pending.timestamp;
        if (age < 30000) {
          // Clear pending first
          chrome.storage.local.remove('lifehub_pending_save');
          // Show popup after short delay to let page settle
          setTimeout(() => {
            showSaveCredentialsPopup(pending.website, pending.username, pending.password);
          }, 500);
        } else {
          // Too old, clear it
          chrome.storage.local.remove('lifehub_pending_save');
        }
      }
    });
  }
  
  // Detect form submissions and offer to save credentials
  let savePopup = null;
  
  function setupFormSubmitListener() {
    document.addEventListener('submit', handleFormSubmit, true);
    
    // Also listen for button clicks on login buttons
    document.addEventListener('click', (e) => {
      const btn = e.target.closest('button[type="submit"], input[type="submit"], button:not([type])');  
      if (btn) {
        const form = btn.closest('form');
        if (form) {
          handleFormSubmit({ target: form, preventDefault: () => {} }, true);
        }
      }
    }, true);
  }
  
  function handleFormSubmit(e, isButtonClick = false) {
    const form = e.target;
    if (!form || (form.tagName !== 'FORM' && !isButtonClick)) return;
    
    // Find all password inputs in this form or near the button
    const container = form.tagName === 'FORM' ? form : (form.closest('div') || document.body);
    const passwordInput = container.querySelector('input[type="password"]');
    if (!passwordInput || !passwordInput.value) return;
    
    // Improved Username Discovery: Score candidates based on type, name, and proximity
    let bestUsernameInput = null;
    let highestScore = -1;
    
    const possibleInputs = container.querySelectorAll('input[type="text"], input[type="email"], input[type="tel"], input:not([type])');
    
    for (const input of possibleInputs) {
      if (input === passwordInput || !isVisible(input) || !input.value || input.value.length < 3) continue;
      
      let score = 0;
      const id = (input.id || '').toLowerCase();
      const name = (input.name || '').toLowerCase();
      const placeholder = (input.placeholder || '').toLowerCase();
      const type = (input.type || 'text').toLowerCase();
      
      // Level 1: Strong indicators
      if (type === 'email') score += 50;
      if (id.includes('user') || name.includes('user')) score += 40;
      if (id.includes('login') || name.includes('login')) score += 30;
      if (id.includes('email') || name.includes('email')) score += 30;
      
      // Level 2: Site-specific or placeholder indicators
      if (placeholder.includes('tên') || placeholder.includes('email') || placeholder.includes('user')) score += 20;
      
      // Proximity: Inputs closer to password field get a small boost
      const position = passwordInput.compareDocumentPosition(input);
      if (position & Node.DOCUMENT_POSITION_PRECEDING) {
        score += 10;
      }
      
      if (score > highestScore) {
        highestScore = score;
        bestUsernameInput = input;
      }
    }
    
    // Fallback: If no good candidates, use the one immediately preceding the password
    if (!bestUsernameInput && highestScore === -1) {
      for (const input of possibleInputs) {
        const position = passwordInput.compareDocumentPosition(input);
        if (position & Node.DOCUMENT_POSITION_PRECEDING) {
          bestUsernameInput = input;
        }
      }
    }
    
    if (!bestUsernameInput || !bestUsernameInput.value) return;
    
    const username = bestUsernameInput.value;
    const password = passwordInput.value;
    const website = window.location.hostname;
    
    console.log('LifeHub: Captured credentials for', website, '(score:', highestScore, ')');
    
    // Store pending credentials
    chrome.storage.local.set({
      lifehub_pending_save: {
        website,
        username,
        password,
        timestamp: Date.now()
      }
    });
  }
  
  function showSaveCredentialsPopup(website, username, password) {
    console.log('LifeHub: Showing save popup for', website, username);
    
    // Check if account already exists to show "Update" instead of "Save"
    try {
      chrome.runtime.sendMessage({
        action: 'checkAccountExists',
        data: { website, username }
      }, (response) => {
        if (chrome.runtime.lastError) {
          console.warn('LifeHub: checkAccountExists failed:', chrome.runtime.lastError);
          renderSavePopup(website, username, password, false);
          return;
        }
        const isUpdate = response && response.exists;
        renderSavePopup(website, username, password, isUpdate);
      });
    } catch (e) {
      console.error('LifeHub: sendMessage error:', e);
      renderSavePopup(website, username, password, false);
    }
  }

  function renderSavePopup(website, username, password, isUpdate) {
    // Remove existing popup
    if (savePopup) {
      savePopup.remove();
    }
    
    savePopup = document.createElement('div');
    savePopup.className = 'lifehub-save-popup';
    savePopup.innerHTML = `
      <div class="lifehub-save-popup-header">
        <div class="lifehub-save-popup-title">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L4 7V11C4 16.5 7.84 21.74 12 23C16.16 21.74 20 16.5 20 11V7L12 2Z" 
                  fill="#6366f1" stroke="white" stroke-width="1.5"/>
          </svg>
          <span>${isUpdate ? 'Cập nhật mật khẩu?' : 'Lưu tài khoản này?'}</span>
        </div>
        <button class="lifehub-save-popup-close">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>
      <div class="lifehub-save-popup-body">
        <div class="lifehub-save-popup-info">
          <div class="lifehub-save-popup-label">Website</div>
          <div class="lifehub-save-popup-value">${escapeHtml(website)}</div>
        </div>
        <div class="lifehub-save-popup-info">
          <div class="lifehub-save-popup-label">Tài khoản</div>
          <div class="lifehub-save-popup-value">${escapeHtml(username)}</div>
        </div>
        <div class="lifehub-save-popup-info">
          <div class="lifehub-save-popup-label">Mật khẩu</div>
          <div class="lifehub-save-popup-value">••••••••</div>
        </div>
        <div class="lifehub-save-popup-actions">
          <button class="lifehub-save-popup-btn secondary" data-action="cancel">Không, cảm ơn</button>
          <button class="lifehub-save-popup-btn primary" data-action="save">${isUpdate ? 'Cập nhật' : 'Lưu lại'}</button>
        </div>
      </div>
    `;
    
    document.body.appendChild(savePopup);
    
    // Event handlers
    savePopup.querySelector('.lifehub-save-popup-close').addEventListener('click', () => {
      savePopup.remove();
      savePopup = null;
    });
    
    savePopup.querySelector('[data-action="cancel"]').addEventListener('click', () => {
      savePopup.remove();
      savePopup = null;
    });
    
    savePopup.querySelector('[data-action="save"]').addEventListener('click', () => {
      const btn = savePopup.querySelector('[data-action="save"]');
      const originalText = btn.textContent;
      btn.disabled = true;
      btn.textContent = 'Đang xử lý...';

      // Send to background script to save
      chrome.runtime.sendMessage({
        action: 'saveCredentials',
        data: { website, username, password }
      }, (response) => {
        if (response && response.success) {
          // Success - show confirmation
          savePopup.querySelector('.lifehub-save-popup-body').innerHTML = `
            <div style="text-align: center; padding: 20px;">
              <div style="font-size: 24px; color: #10b981; margin-bottom: 10px;">✅</div>
              <div style="color: #f8fafc; font-weight: 500;">${response.message || 'Đã lưu thành công!'}</div>
              ${response.syncing ? '<div style="color: #94a3b8; font-size: 13px; margin-top: 8px;">Mở extension để đồng bộ lên Cloud.</div>' : ''}
            </div>
          `;
          
          setTimeout(() => {
            if (savePopup) {
              savePopup.remove();
              savePopup = null;
            }
          }, 2000);
        } else if (response && response.requiresAuth) {
          // Not authenticated - show error message
          savePopup.querySelector('.lifehub-save-popup-body').innerHTML = `
            <div style="text-align: center; padding: 20px;">
              <div style="font-size: 24px; margin-bottom: 10px;">🔒</div>
              <div style="color: #f8fafc; margin-bottom: 15px;">${response.message || 'Vui lòng đăng nhập để lưu.'}</div>
              <a href="https://lifehub-project-d8c97.web.app" target="_blank" 
                 style="display: inline-block; padding: 8px 16px; background: #6366f1; color: white; border-radius: 6px; text-decoration: none; font-weight: 500; font-size: 13px;">
                Mở LifeHub (Web)
              </a>
            </div>
          `;
          setTimeout(() => {
            if (savePopup) {
              savePopup.remove();
              savePopup = null;
            }
          }, 5000);
        } else {
          btn.disabled = false;
          btn.textContent = originalText;
          alert('Lỗi: ' + (response?.message || 'Không thể lưu tài khoản.'));
        }
      });
    });
    
    // Auto-hide after 30 seconds
    setTimeout(() => {
      if (savePopup) {
        savePopup.remove();
        savePopup = null;
      }
    }, 30000);
  }
  
  // Run when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

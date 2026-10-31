/**
 * Blood Donor Network - Unified Single-Page Dashboard Engine
 * Handles hash routing, sidebar drawer, panel switching without scrolling,
 * dropdowns, modals, and instant search filters.
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. Initialize Lucide Line Icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }

    // 2. Setup Single-Page Hash Routing & Sidebar Navigation
    initDashboardRouter();

    // 3. Setup Responsive Mobile Drawer
    initSidebarDrawer();

    // 4. Setup Topbar Dropdowns (Notifications & Profile)
    initTopbarDropdowns();

    // 5. Setup Live Global Search
    initGlobalPanelSearch();

    // 6. Setup Modal Dialogs
    initModals();
});

/**
 * Single-Page Router: Activates panels based on sidebar clicks and URL hashes.
 */
function initDashboardRouter() {
    const navItems = document.querySelectorAll('.nav-item[data-target]');
    if (!navItems.length) return;

    // Handle clicks on sidebar nav items
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const targetPanelId = item.getAttribute('data-target');
            const hash = item.getAttribute('href').replace('#', '');
            
            // Switch panel
            switchPanel(targetPanelId, item);

            // Update URL hash without scrolling
            if (history.pushState) {
                history.pushState(null, null, '#/' + hash.replace(/^\//, ''));
            } else {
                window.location.hash = '#/' + hash.replace(/^\//, '');
            }

            // Close mobile drawer if open
            closeMobileSidebar();
        });
    });

    // Handle browser back/forward buttons and direct URL hash loading
    window.addEventListener('hashchange', () => {
        handleHashRoute();
    });

    // Initial route resolution on page load
    handleHashRoute();
}

/**
 * Resolves current URL hash and activates the corresponding panel.
 */
function handleHashRoute() {
    const hash = window.location.hash.replace(/^#\/?/, '').trim();
    if (!hash) {
        // Default to first nav item / overview panel
        const firstItem = document.querySelector('.nav-item[data-target]');
        if (firstItem) {
            switchPanel(firstItem.getAttribute('data-target'), firstItem);
        }
        return;
    }

    // Check if a nav item matches this hash
    let matchedItem = document.querySelector(`.nav-item[href="#${hash}"]`) ||
                      document.querySelector(`.nav-item[href="#/${hash}"]`) ||
                      document.querySelector(`.nav-item[data-target="panel-${hash}"]`);

    // Handle aliases (e.g. #profile-section -> panel-profile, #inbox-section -> panel-inbox)
    if (!matchedItem) {
        if (hash.includes('profile')) {
            matchedItem = document.querySelector('.nav-item[data-target="panel-profile"]') ||
                          document.querySelector('.nav-item[data-target="panel-settings"]');
        } else if (hash.includes('inbox') || hash.includes('notif')) {
            matchedItem = document.querySelector('.nav-item[data-target="panel-inbox"]');
        } else if (hash.includes('demand') || hash.includes('request')) {
            matchedItem = document.querySelector('.nav-item[data-target="panel-requests"]') ||
                          document.querySelector('.nav-item[data-target="panel-demands"]');
        } else if (hash.includes('match')) {
            matchedItem = document.querySelector('.nav-item[data-target="panel-matching"]');
        } else if (hash.includes('stock') || hash.includes('reserve')) {
            matchedItem = document.querySelector('.nav-item[data-target="panel-stock"]') ||
                          document.querySelector('.nav-item[data-target="panel-reserves"]');
        }
    }

    if (matchedItem) {
        switchPanel(matchedItem.getAttribute('data-target'), matchedItem);
    } else {
        const firstItem = document.querySelector('.nav-item[data-target]');
        if (firstItem) {
            switchPanel(firstItem.getAttribute('data-target'), firstItem);
        }
    }
}

/**
 * Switches the active panel and updates sidebar styling & topbar title.
 */
function switchPanel(panelId, activeNavItem) {
    if (!panelId) return;

    // 1. Hide all panels
    const allPanels = document.querySelectorAll('.dash-panel');
    allPanels.forEach(panel => {
        panel.classList.remove('active');
    });

    // 2. Show target panel
    const targetPanel = document.getElementById(panelId);
    if (targetPanel) {
        targetPanel.classList.add('active');
    }

    // 3. Update active sidebar item
    const allNavItems = document.querySelectorAll('.nav-item');
    allNavItems.forEach(item => {
        item.classList.remove('active');
        item.removeAttribute('aria-current');
    });

    if (activeNavItem) {
        activeNavItem.classList.add('active');
        activeNavItem.setAttribute('aria-current', 'page');

        // Update Topbar Title Display
        const titleDisplay = document.getElementById('pageTitleDisplay');
        const itemText = activeNavItem.querySelector('span:not(.nav-badge)');
        if (titleDisplay && itemText) {
            titleDisplay.textContent = itemText.textContent.trim();
        }
    }

    // 4. Scroll content area to top smoothly
    const contentArea = document.getElementById('dashContentArea');
    if (contentArea) {
        contentArea.scrollTop = 0;
    }

    // 5. Re-render Lucide icons inside newly visible elements
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }

    // 6. Trigger window resize for Chart.js canvases inside the panel
    window.dispatchEvent(new Event('resize'));
}

/**
 * Handles responsive mobile sidebar drawer and backdrop.
 */
function initSidebarDrawer() {
    const toggleBtn = document.getElementById('sidebarToggleBtn');
    const closeBtn = document.getElementById('sidebarCloseBtn');
    const sidebar = document.getElementById('dashSidebar');
    const backdrop = document.getElementById('sidebarBackdrop');

    if (toggleBtn && sidebar && backdrop) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.add('open');
            backdrop.classList.add('active');
            toggleBtn.setAttribute('aria-expanded', 'true');
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', closeMobileSidebar);
    }

    if (backdrop) {
        backdrop.addEventListener('click', closeMobileSidebar);
    }

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeMobileSidebar();
            closeAllDropdowns();
            closeAllModals();
        }
    });
}

function closeMobileSidebar() {
    const sidebar = document.getElementById('dashSidebar');
    const backdrop = document.getElementById('sidebarBackdrop');
    const toggleBtn = document.getElementById('sidebarToggleBtn');

    if (sidebar) sidebar.classList.remove('open');
    if (backdrop) backdrop.classList.remove('active');
    if (toggleBtn) toggleBtn.setAttribute('aria-expanded', 'false');
}

/**
 * Topbar Dropdowns (Notification Bell & Profile Menu)
 */
function initTopbarDropdowns() {
    const notifBtn = document.getElementById('notifBtn');
    const notifMenu = document.getElementById('notifMenu');
    const profileBtn = document.getElementById('profileBtn');
    const profileMenu = document.getElementById('profileMenu');

    if (notifBtn && notifMenu) {
        notifBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            if (profileMenu) profileMenu.classList.remove('show');
            notifMenu.classList.toggle('show');
            notifBtn.setAttribute('aria-expanded', notifMenu.classList.contains('show'));
        });
    }

    if (profileBtn && profileMenu) {
        profileBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            if (notifMenu) notifMenu.classList.remove('show');
            profileMenu.classList.toggle('show');
            profileBtn.setAttribute('aria-expanded', profileMenu.classList.contains('show'));
        });
    }

    document.addEventListener('click', (e) => {
        if (!e.target.closest('.topbar-dropdown')) {
            closeAllDropdowns();
        }
    });
}

function closeAllDropdowns() {
    document.querySelectorAll('.dropdown-menu').forEach(menu => menu.classList.remove('show'));
    document.querySelectorAll('.topbar-icon-btn, .topbar-profile-btn').forEach(btn => {
        btn.setAttribute('aria-expanded', 'false');
    });
}

/**
 * Global Instant Search across current active panel tables
 */
function initGlobalPanelSearch() {
    const searchInput = document.getElementById('globalPanelSearch');
    if (!searchInput) return;

    // Keyboard shortcut (Ctrl+K or /)
    document.addEventListener('keydown', (e) => {
        if ((e.ctrlKey && e.key.toLowerCase() === 'k') || (e.key === '/' && document.activeElement !== searchInput && !['INPUT', 'SELECT', 'TEXTAREA'].includes(document.activeElement.tagName))) {
            e.preventDefault();
            searchInput.focus();
            searchInput.select();
        }
    });

    let debounceTimer = null;
    searchInput.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            const query = searchInput.value.toLowerCase().trim();
            const activePanel = document.querySelector('.dash-panel.active');
            if (!activePanel) return;

            const tables = activePanel.querySelectorAll('.data-table tbody');
            tables.forEach(tbody => {
                const rows = tbody.querySelectorAll('tr');
                let matchCount = 0;
                rows.forEach(row => {
                    const text = row.textContent.toLowerCase();
                    if (!query || text.includes(query)) {
                        row.style.display = '';
                        matchCount++;
                    } else {
                        row.style.display = 'none';
                    }
                });
            });
        }, 120);
    });
}

/**
 * Table Search Filter Helper
 */
function filterTable(inputId, tableBodyId) {
    const input = document.getElementById(inputId);
    const tableBody = document.getElementById(tableBodyId);
    if (!input || !tableBody) return;

    const query = input.value.toLowerCase().trim();
    const rows = tableBody.getElementsByTagName('tr');

    for (let i = 0; i < rows.length; i++) {
        const text = rows[i].textContent.toLowerCase();
        if (!query || text.includes(query)) {
            rows[i].style.display = '';
        } else {
            rows[i].style.display = 'none';
        }
    }
}

/**
 * Modal Dialog Open / Close Helpers
 */
function initModals() {
    document.querySelectorAll('[data-modal-open]').forEach(btn => {
        btn.addEventListener('click', () => {
            const modalId = btn.getAttribute('data-modal-open');
            openModal(modalId);
        });
    });

    document.querySelectorAll('[data-modal-close]').forEach(btn => {
        btn.addEventListener('click', () => {
            const modal = btn.closest('.modal-overlay');
            if (modal) modal.classList.remove('show');
        });
    });
}

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('show');
        const firstInput = modal.querySelector('input:not([type="hidden"]), select, textarea');
        if (firstInput) firstInput.focus();
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('show');
    }
}

function closeAllModals() {
    document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('show'));
}

/**
 * Urgency Filter for Blood Demands Tables
 * Supports: ALL, CRITICAL, HIGH, NORMAL (Medium), LOW
 */
function filterByUrgency(btn, urgencyVal, tbodyId) {
    if (!btn || !tbodyId) return;

    // 1. Update active styling in button group
    const bar = btn.closest('.urgency-filter-bar');
    if (bar) {
        bar.querySelectorAll('.urgency-pill').forEach(b => {
            b.classList.remove('active');
        });
        btn.classList.add('active');
    }

    // 2. Filter rows in target table body
    const tbody = document.getElementById(tbodyId);
    if (!tbody) return;

    const rows = tbody.querySelectorAll('tr[data-urgency]');
    let visibleCount = 0;

    rows.forEach(row => {
        const rowUrgency = (row.getAttribute('data-urgency') || '').toUpperCase();
        let matches = false;

        if (urgencyVal === 'ALL') {
            matches = true;
        } else if (urgencyVal === 'CRITICAL' && rowUrgency === 'CRITICAL') {
            matches = true;
        } else if (urgencyVal === 'HIGH' && rowUrgency === 'HIGH') {
            matches = true;
        } else if (urgencyVal === 'NORMAL' && (rowUrgency === 'NORMAL' || rowUrgency === 'MEDIUM')) {
            matches = true;
        } else if (urgencyVal === 'LOW' && rowUrgency === 'LOW') {
            matches = true;
        }

        if (matches) {
            row.style.display = '';
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });

    // 3. Clear existing empty state rows if present
    const existingEmpty = tbody.querySelectorAll('.urgency-empty-row');
    existingEmpty.forEach(el => el.remove());

    // 4. Show empty state if visibleCount is 0
    if (visibleCount === 0) {
        const emptyRow = document.createElement('tr');
        emptyRow.className = 'urgency-empty-row';
        const colSpan = tbody.closest('table').querySelectorAll('thead th').length || 7;
        const filterName = btn.textContent.trim();
        emptyRow.innerHTML = `<td colspan="${colSpan}" style="text-align: center; color: var(--slate-muted); padding: 2rem; font-size: 0.9rem;">No blood demands found matching <strong>${filterName}</strong> urgency.</td>`;
        tbody.appendChild(emptyRow);
    }
}

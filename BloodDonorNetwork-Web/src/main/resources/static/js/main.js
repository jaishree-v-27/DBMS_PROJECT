/**
 * Clinical Web Application JavaScript
 * Powers Lucide icons, Tab switches, FAQ Accordion, Interactive Compatibility Matrix, and Table Filters.
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. Initialize Lucide Icons
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }

    // 2. Setup Tab Switching (e.g. Admin Dashboard)
    initTabs();

    // 3. Setup FAQ Accordion (Landing Page)
    initFaqAccordion();

    // 4. Setup Interactive Transfusion Compatibility Widget
    initCompatibilityWidget();
});

/**
 * Handles Tab navigation switching between panes.
 */
function initTabs() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    if (!tabButtons.length) return;

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-tab');
            if (!targetId) return;

            // Deactivate all buttons in this tab group
            const container = btn.closest('.tab-nav') || document;
            container.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));

            // Deactivate all panes
            document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));

            // Activate current
            btn.classList.add('active');
            const targetPane = document.getElementById(targetId);
            if (targetPane) {
                targetPane.classList.add('active');
            }

            // Re-render Lucide icons inside newly visible tab
            if (typeof lucide !== 'undefined') {
                lucide.createIcons();
            }
        });
    });

    // Check if URL has a hash for specific tab (e.g. #requests-tab)
    if (window.location.hash) {
        const hash = window.location.hash.replace('#', '');
        let targetBtn = null;
        if (hash === 'requests-tab') {
            targetBtn = document.querySelector('.tab-btn[data-tab="tab-requests"]');
        } else {
            targetBtn = document.querySelector(`.tab-btn[data-tab="${hash}"]`);
        }
        if (targetBtn) {
            targetBtn.click();
        }
    }
}

/**
 * Toggles FAQ accordion items smoothly.
 */
function initFaqAccordion() {
    const faqHeaders = document.querySelectorAll('.faq-header');
    faqHeaders.forEach(header => {
        header.addEventListener('click', () => {
            const item = header.closest('.faq-item');
            if (!item) return;

            const isOpen = item.classList.contains('open');

            // Optional: close other open items
            document.querySelectorAll('.faq-item.open').forEach(other => {
                if (other !== item) other.classList.remove('open');
            });

            if (isOpen) {
                item.classList.remove('open');
            } else {
                item.classList.add('open');
            }

            if (typeof lucide !== 'undefined') {
                lucide.createIcons();
            }
        });
    });
}

/**
 * Compatibility Matrix clinical table logic.
 */
const COMPATIBILITY_RULES = {
    'O-': {
        canReceive: ['O-'],
        canGive: ['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+']
    },
    'O+': {
        canReceive: ['O+', 'O-'],
        canGive: ['O+', 'A+', 'B+', 'AB+']
    },
    'A-': {
        canReceive: ['A-', 'O-'],
        canGive: ['A-', 'A+', 'AB-', 'AB+']
    },
    'A+': {
        canReceive: ['A+', 'A-', 'O+', 'O-'],
        canGive: ['A+', 'AB+']
    },
    'B-': {
        canReceive: ['B-', 'O-'],
        canGive: ['B-', 'B+', 'AB-', 'AB+']
    },
    'B+': {
        canReceive: ['B+', 'B-', 'O+', 'O-'],
        canGive: ['B+', 'AB+']
    },
    'AB-': {
        canReceive: ['AB-', 'A-', 'B-', 'O-'],
        canGive: ['AB-', 'AB+']
    },
    'AB+': {
        canReceive: ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'],
        canGive: ['AB+']
    }
};

function initCompatibilityWidget() {
    const pills = document.querySelectorAll('.blood-pill');
    if (!pills.length) return;

    pills.forEach(pill => {
        pill.addEventListener('click', () => {
            pills.forEach(p => p.classList.remove('selected'));
            pill.classList.add('selected');

            const group = pill.getAttribute('data-group');
            updateCompatibilityResults(group);
        });
    });
}

function updateCompatibilityResults(group) {
    const rule = COMPATIBILITY_RULES[group];
    if (!rule) return;

    const receiveContainer = document.getElementById('canReceiveChips');
    const giveContainer = document.getElementById('canGiveChips');

    if (receiveContainer) {
        receiveContainer.innerHTML = rule.canReceive
            .map(bg => `<span class="bg-badge" style="font-size: 0.95rem; padding: 0.3rem 0.75rem;">${bg}</span>`)
            .join(' ');
    }

    if (giveContainer) {
        giveContainer.innerHTML = rule.canGive
            .map(bg => `<span class="bg-badge" style="font-size: 0.95rem; padding: 0.3rem 0.75rem; background: #e0f2fe; color: #0369a1; border-color: #7dd3fc;">${bg}</span>`)
            .join(' ');
    }
}

/**
 * Client-side table search filter.
 */
function filterTable(inputId, tableBodyId) {
    const input = document.getElementById(inputId);
    const tableBody = document.getElementById(tableBodyId);
    if (!input || !tableBody) return;

    const query = input.value.toLowerCase().trim();
    const rows = tableBody.getElementsByTagName('tr');

    for (let i = 0; i < rows.length; i++) {
        const text = rows[i].textContent.toLowerCase();
        if (text.includes(query)) {
            rows[i].style.display = '';
        } else {
            rows[i].style.display = 'none';
        }
    }
}

let comboTypingTimer;
const comboDoneTypingInterval = 500;

function normalizeComboImagePath(path) {
    if (!path || path === '-') return '';
    if (path.startsWith('http') || path.startsWith('data:')) return path;

    let cleanPath = path.trim();
    if (cleanPath.startsWith('/')) cleanPath = cleanPath.substring(1);

    if (cleanPath.startsWith('uploads/combos/')) return cleanPath.substring('uploads/combos/'.length);
    if (cleanPath.startsWith('uploads/')) return cleanPath.substring('uploads/'.length);
    if (cleanPath.startsWith('combos/')) return cleanPath.substring('combos/'.length);

    return cleanPath;
}

function resolveImageSources(path) {
    const cleanPath = normalizeComboImagePath(path);
    if (!cleanPath) return { primary: '', fallback: '' };

    if (cleanPath.startsWith('http') || cleanPath.startsWith('data:')) {
        return { primary: cleanPath, fallback: '' };
    }

    // If DB stores plain file name from ComboDataSeeder, prefer static /img first.
    if (!String(path).includes('/') && !String(path).startsWith('uploads')) {
        return {
            primary: '/img/' + cleanPath,
            fallback: '/combos/' + cleanPath
        };
    }

    return {
        primary: '/combos/' + cleanPath,
        fallback: '/img/' + cleanPath
    };
}

document.addEventListener('DOMContentLoaded', () => {
    const msg = sessionStorage.getItem('successMessage');
    if (msg) {
        if (typeof showToast === 'function') showToast(msg, 'success');
        sessionStorage.removeItem('successMessage');
    }

    loadCombos();

    const searchInput = document.getElementById('searchComboInput');
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            clearTimeout(comboTypingTimer);
            comboTypingTimer = setTimeout(loadCombos, comboDoneTypingInterval);
        });
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') e.preventDefault();
        });
    }
});

async function loadCombos() {
    try {
        const token = sessionStorage.getItem('jwtToken');
        const searchInput = document.getElementById('searchComboInput');
        const keyword = searchInput ? searchInput.value.trim() : '';

        let url = '/api/admin/combos';
        if (keyword) url += `?name=${encodeURIComponent(keyword)}`;

        const response = await fetch(url, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const combos = await response.json();
        const tbody = document.querySelector('#comboTable tbody');
        if (!tbody) return;

        tbody.innerHTML = '';

        if (!Array.isArray(combos) || combos.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted py-5"><i class="fa-solid fa-box-open fs-2 mb-3 text-warning"></i><br>Menu is currently empty.</td></tr>`;
            return;
        }

        combos.forEach((combo) => {
            const safeName = escapeHtml(combo.name || '-');
            const safeDesc = escapeHtml(combo.description || '-');
            const safePrice = Number(combo.price || 0).toLocaleString('vi-VN') + ' VND';

            let imageHtml = `<span class="badge bg-light text-muted border px-2 py-1"><i class="fa-solid fa-image-slash me-1"></i> No Image</span>`;
            if (combo.image && combo.image !== '-') {
                const src = resolveImageSources(combo.image);
                imageHtml = `<img src="${src.primary}" data-fallback="${src.fallback}" alt="combo" class="img-thumbnail shadow-sm" style="width: 70px; height: 70px; object-fit: cover; border-radius: 12px; transition: 0.3s;" onerror="if(this.dataset.fallback){const fb=this.dataset.fallback;this.dataset.fallback='';this.src=fb;}else{this.outerHTML='<div class=\\'d-flex align-items-center justify-content-center bg-light border shadow-sm\\' style=\\'width: 70px; height: 70px; border-radius: 12px;\\'><i class=\\'fa-solid fa-burger text-warning fs-3\\'></i></div>';}">`;
            }

            tbody.innerHTML += `
                <tr>
                    <td class="align-middle">
                        <span class="badge bg-light text-secondary border shadow-sm px-2 py-1">ID: #${combo.id}</span>
                    </td>
                    <td class="align-middle fw-bold text-dark fs-6">${safeName}</td>
                    <td class="align-middle text-muted" style="max-width: 300px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" title="${safeDesc}">${safeDesc}</td>
                    <td class="align-middle fw-bold text-success fs-6">${safePrice}</td>
                    <td class="align-middle">${imageHtml}</td>
                    <td class="align-middle text-end pe-4">
                        <a class="btn tech-btn tech-btn-outline-amber px-3 py-1 shadow-sm" style="font-size: 0.8rem;" href="/admin/combos/edit?id=${combo.id}">
                            <i class="fa-solid fa-pen-to-square"></i> Edit
                        </a>
                    </td>
                </tr>
            `;
        });
    } catch (error) {
        console.error('Error loading combos:', error);
    }
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}
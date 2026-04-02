let comboTypingTimer;
const comboDoneTypingInterval = 500;

document.addEventListener('DOMContentLoaded', () => {
    loadCombos();

    const searchInput = document.getElementById('searchComboInput');
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            clearTimeout(comboTypingTimer);
            comboTypingTimer = setTimeout(loadCombos, comboDoneTypingInterval);
        });

        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
            }
        });
    }
});

async function loadCombos() {
    try {
        const token = localStorage.getItem('jwtToken');
        const searchInput = document.getElementById('searchComboInput');
        const keyword = searchInput ? searchInput.value.trim() : '';

        let url = '/api/admin/combos';
        if (keyword) {
            url += `?name=${encodeURIComponent(keyword)}`;
        }

        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const combos = await response.json();

        const tbody = document.querySelector('#comboTable tbody');
        if (!tbody) return;

        tbody.innerHTML = '';

        if (!Array.isArray(combos) || combos.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted py-4">No combos found.</td></tr>`;
            return;
        }

        combos.forEach((combo) => {
            const safeImage = escapeHtml(combo.image ? combo.image : '-');
            const safeDesc = escapeHtml(combo.description ? combo.description : '-');
            const safePrice = Number(combo.price || 0).toLocaleString('en-US');
            const safeName = escapeHtml(combo.name || '-');

            tbody.innerHTML += `
                <tr>
                    <td class="align-middle fw-bold">${combo.id}</td>
                    <td class="align-middle">${safeName}</td>
                    <td class="align-middle">${safeDesc}</td>
                    <td class="align-middle">${safePrice}</td>
                    <td class="align-middle">${safeImage}</td>
                    <td class="align-middle text-end pe-4">
                        <a class="btn btn-sm btn-primary" href="/admin/combos/edit?id=${combo.id}">
                            <i class="fa-solid fa-pen"></i> Edit
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

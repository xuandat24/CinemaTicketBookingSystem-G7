let typingTimer;
const doneTypingInterval = 500; // Wait 500ms (0.5 seconds)

document.addEventListener('DOMContentLoaded', () => {
    loadCategories();

    const searchInput = document.getElementById('searchCategoryInput');
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            clearTimeout(typingTimer);
            typingTimer = setTimeout(() => {
                loadCategories();
            }, doneTypingInterval);
        });

        searchInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
            }
        });
    }
});

async function loadCategories() {
    try {
        const token = sessionStorage.getItem('jwtToken');
        const searchInput = document.getElementById('searchCategoryInput');
        const keyword = searchInput ? searchInput.value.trim() : '';

        let url = '/api/admin/categories';
        if (keyword !== '') {
            url += `?name=${encodeURIComponent(keyword)}`;
        }

        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const categories = await response.json();
        const tbody = document.querySelector('#categoryTable tbody');
        if (!tbody) return;

        tbody.innerHTML = '';

        if (categories.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-5"><i class="fa-solid fa-box-open fs-2 mb-2"></i><br>No categories found matching "${keyword}".</td></tr>`;
            return;
        }

        categories.forEach(cat => {
            // Improve UI for a more modern look:
            tbody.innerHTML += `
                <tr>
                    <td class="align-middle fw-bold text-secondary">#${cat.categoryId}</td>
                    <td class="align-middle">
                        <a href="/admin/movies/search?categoryId=${cat.categoryId}" class="text-decoration-none fw-bold text-dark fs-6" title="View all movies in this category">
                            ${cat.name}
                        </a>
                    </td>
                    <td class="align-middle">
                        <span class="badge bg-success-subtle text-success px-3 py-2 rounded-pill fw-bold border border-success">
                            <i class="fa-solid fa-film me-1"></i> ${cat.movieCount} Movies
                        </span>
                    </td>
                    <td class="align-middle text-end pe-4">
                        <a href="/admin/categories/edit?id=${cat.categoryId}" class="btn tech-btn tech-btn-outline shadow-sm" style="padding: 6px 15px !important; font-size: 0.75rem;">
                            <i class="fa-solid fa-pen-to-square me-1"></i> Edit
                        </a>
                    </td>
                </tr>
            `;
        });
    } catch (error) {
        console.error("Error loading categories:", error);
    }
}
document.addEventListener('DOMContentLoaded', () => {
    loadCategories();
});

async function loadCategories() {
    try {
        const response = await fetch('/api/admin/categories');
        const categories = await response.json();

        const tbody = document.querySelector('#categoryTable tbody');
        tbody.innerHTML = '';

        if (categories.length === 0) {
            tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted py-4">No categories found.</td></tr>`;
            return;
        }

        categories.forEach(cat => {
                    tbody.innerHTML += `
                        <tr>
                            <td class="align-middle fw-bold">${cat.categoryId}</td>
                            <td class="align-middle">
                                <a href="/admin/movies/search?categoryId=${cat.categoryId}" class="text-decoration-none fw-bold text-primary" title="View all movies in this category">
                                    <i class="fa-solid fa-link fa-sm"></i> ${cat.name}
                                </a>
                            </td>
                            <td class="align-middle">
                                <span class="badge bg-info text-dark">${cat.movieCount} Movies</span>
                            </td>
                            <td class="align-middle text-end pe-4">
                                <a href="/admin/categories/edit?id=${cat.categoryId}" class="btn btn-sm btn-primary">
                                    <i class="fa-solid fa-pen-to-square"></i> Edit
                                </a>
                            </td>
                        </tr>
                    `;
                });
    } catch (error) {
        console.error("Error loading categories:", error);
    }
}
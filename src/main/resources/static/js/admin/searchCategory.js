let typingTimer;
const doneTypingInterval = 500; // Đợi 500ms (0.5 giây)

document.addEventListener('DOMContentLoaded', () => {
    loadCategories();

    // Áp dụng Live Search (Debounce) cho thanh tìm kiếm Category
    const searchInput = document.getElementById('searchCategoryInput');
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            clearTimeout(typingTimer);
            typingTimer = setTimeout(() => {
                loadCategories();
            }, doneTypingInterval);
        });

        // Chặn tải lại trang nếu người dùng quen tay ấn Enter
        searchInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
            }
        });
    }
});

// Giữ nguyên hàm async function loadCategories() ở dưới...

async function loadCategories() {
    try {
        const token = localStorage.getItem('jwtToken');

        // 1. Lấy giá trị từ ô tìm kiếm
        const searchInput = document.getElementById('searchCategoryInput');
        const keyword = searchInput ? searchInput.value.trim() : '';

        // 2. Gắn từ khóa vào URL nếu có
        let url = '/api/admin/categories';
        if (keyword !== '') {
            url += `?name=${encodeURIComponent(keyword)}`;
        }

        // 3. Gọi API
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
            tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">No categories found matching "${keyword}".</td></tr>`;
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
                        <a href="/admin/categories/edit?id=${cat.categoryId}" class="btn btn-sm btn-primary shadow-sm">
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
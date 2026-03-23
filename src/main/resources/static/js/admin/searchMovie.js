// Khai báo 2 biến toàn cục để quản lý thời gian chờ (Debounce)
let typingTimer;
const doneTypingInterval = 500; // Đợi 500ms (0.5 giây) sau khi ngừng gõ mới tìm kiếm

document.addEventListener('DOMContentLoaded', async () => {
    await loadCategoriesForFilter();

    const urlParams = new URLSearchParams(window.location.search);

    const keyword = urlParams.get('keyword');
    if (keyword) {
        document.getElementById('searchTitle').value = keyword;
    }

    const categoryIdFromUrl = urlParams.get('categoryId');
    if (categoryIdFromUrl) {
        document.getElementById('filterCategory').value = categoryIdFromUrl;
    }

    // === 1. LIVE SEARCH CHO Ô NHẬP TÊN PHIM (DEBOUNCE) ===
    const searchInput = document.getElementById('searchTitle');
    if (searchInput) {
        // Lắng nghe sự kiện 'input' (mỗi khi gõ, xóa, dán chữ...)
        searchInput.addEventListener('input', function () {
            // Bước 1: Xóa lệnh tìm kiếm cũ nếu người dùng vẫn tiếp tục gõ
            clearTimeout(typingTimer);

            // Bước 2: Hẹn giờ 0.5 giây sau mới chạy hàm applyFilter()
            typingTimer = setTimeout(() => {
                applyFilter();
            }, doneTypingInterval);
        });

        // Vẫn chặn phím Enter để form không bị tải lại trang nếu người dùng quen tay ấn Enter
        searchInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                // Không cần gọi applyFilter() ở đây nữa vì sự kiện 'input' ở trên đã lo rồi
            }
        });
    }

    // === 2. TÌM KIẾM NGAY KHI ĐỔI THỂ LOẠI / NGÀY THÁNG / SẮP XẾP ===
    // (Bấm chọn là lọc ngay, không cần ấn nút Filter)
    const filterElements = ['filterCategory', 'fromDate', 'toDate', 'sortBy'];
    filterElements.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener('change', function() {
                applyFilter();
            });
        }
    });

    loadMovies();
});

async function loadCategoriesForFilter() {
    try {
        const token = localStorage.getItem('jwtToken'); // Lấy Token
        const response = await fetch('/api/admin/categories', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}` // Bắt buộc đính kèm Token
            }
        });
        const categories = await response.json();
        const select = document.getElementById('filterCategory');
        categories.forEach(cat => {
            select.innerHTML += `<option value="${cat.categoryId}">${cat.name}</option>`;
        });
    } catch (error) {
        console.error("Error loading categories:", error);
    }
}

function applyFilter() {
    loadMovies();
}

async function loadMovies() {
    try {
        const title = document.getElementById('searchTitle').value.trim();
        const categoryId = document.getElementById('filterCategory').value;
        const fromDate = document.getElementById('fromDate').value;
        const toDate = document.getElementById('toDate').value;
        const sortBy = document.getElementById('sortBy').value;

        let apiUrl = `/api/admin/movies?sortBy=${sortBy}`;
        if (title) apiUrl += `&title=${encodeURIComponent(title)}`;
        if (categoryId) apiUrl += `&categoryId=${categoryId}`;
        if (fromDate) apiUrl += `&fromDate=${fromDate}`;
        if (toDate) apiUrl += `&toDate=${toDate}`;

        const tableTitle = document.getElementById('tableTitle');
        if (tableTitle) {
            tableTitle.innerHTML = title ? `Search Results for: <span class="text-primary">"${title}"</span>` : 'Current Movies';
        }

        const token = localStorage.getItem('jwtToken');
        const response = await fetch(apiUrl, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const movies = await response.json();

        const tbody = document.querySelector('#movieTable tbody');
        if (!tbody) return;

        tbody.innerHTML = '';

        if (movies.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-muted py-5">
                        <i class="fa-solid fa-magnifying-glass mb-2 fs-3"></i><br>
                        No movies found matching your criteria.
                    </td>
                </tr>`;
            return;
        }

        movies.forEach(movie => {
            let statusBadge = 'bg-secondary';
            if (movie.status === 'Now Playing') statusBadge = 'bg-success';
            if (movie.status === 'Coming Soon') statusBadge = 'bg-warning text-dark';
            if (movie.status === 'Pending') statusBadge = 'bg-info text-dark';
            if (movie.status === 'Disabled') statusBadge = 'bg-dark';

            let categoryTags = movie.categoryNames && movie.categoryNames.length > 0
                ? movie.categoryNames.map(name => `<span class="badge bg-light text-dark border me-1">${name}</span>`).join('')
                : '<span class="text-muted small">No category</span>';

            tbody.innerHTML += `
                <tr>
                    <td class="align-middle fw-bold">${movie.movieId}</td>
                    <td class="align-middle">
                        <div class="d-flex align-items-center">
                            ${movie.bannerPath ? `<img src="${movie.bannerPath}" alt="poster" style="width: 45px; height: 65px; object-fit: cover; border-radius: 4px; margin-right: 12px;">` : ''}
                            <div>
                                <div class="fw-bold text-dark mb-1">${movie.title}</div>
                                <div>${categoryTags}</div>
                            </div>
                        </div>
                    </td>
                    <td class="align-middle text-muted fw-semibold">
                        <i class="fa-regular fa-calendar-days me-1"></i> ${movie.releaseDate || 'N/A'}
                    </td>
                    <td class="align-middle">
                        <span class="badge ${statusBadge}">${movie.status}</span>
                    </td>
                    <td class="align-middle text-end pe-4">
                        <a href="/admin/movies/edit?id=${movie.movieId}" class="btn btn-sm btn-primary shadow-sm">
                            <i class="fa-solid fa-pen-to-square"></i> Edit
                        </a>
                    </td>
                </tr>
            `;
        });
    } catch (error) {
        alert("Failed to fetch movies from the server.");
    }
}
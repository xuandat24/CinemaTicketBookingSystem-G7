let allMovies = [];
let filteredMovies = [];
let currentPage = 1;
const itemsPerPage = 8; // Số lượng phim trên 1 trang (để 8 tạo 2 hàng x 4 phim cực đẹp)

let typingTimer;
const doneTypingInterval = 500;

document.addEventListener('DOMContentLoaded', async () => {
    await loadCategories();
    await loadMovies();

    const textInputs = ['searchTitle', 'searchLanguage'];
    textInputs.forEach(id => {
        const inputEle = document.getElementById(id);
        if (inputEle) {
            inputEle.addEventListener('input', function () {
                clearTimeout(typingTimer);
                typingTimer = setTimeout(() => { applyFilters(); }, doneTypingInterval);
            });
            inputEle.addEventListener('keypress', function (e) {
                if (e.key === 'Enter') e.preventDefault();
            });
        }
    });

    const filterElements = ['filterCategory', 'sortBy', 'filterStatus'];
    filterElements.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener('change', function() { applyFilters(); });
        }
    });
});

async function loadCategories() {
    try {
        const res = await fetch('/api/public/categories');
        const categories = await res.json();
        const select = document.getElementById('filterCategory');
        categories.forEach(cat => {
            select.innerHTML += `<option value="${cat.categoryId}">${cat.name}</option>`;
        });
    } catch (error) { console.error("Lỗi tải danh mục:", error); }
}

async function loadMovies() {
    try {
        const res = await fetch('/api/public/movies');
        if (!res.ok) throw new Error("Network response was not ok");
        allMovies = await res.json();
        filteredMovies = [...allMovies];
        restoreSearchState();
        applyFilters();
    } catch (error) {
        document.getElementById('movieListContainer').innerHTML = `<div class="col-12 text-center text-danger py-5"><h5>Không thể tải danh sách phim. Vui lòng kiểm tra máy chủ.</h5></div>`;
    }
}

function applyFilters() {
    const title = document.getElementById('searchTitle').value.toLowerCase();
    const categoryId = document.getElementById('filterCategory').value;
    const language = document.getElementById('searchLanguage').value.toLowerCase();
    const status = document.getElementById('filterStatus').value;
    const sortBy = document.getElementById('sortBy').value;

    filteredMovies = allMovies.filter(movie => {
        const matchTitle = movie.title.toLowerCase().includes(title);
        const matchLanguage = (movie.language || '').toLowerCase().includes(language);
        const matchStatus = status === '' || movie.status === status;

        let matchCategory = true;
        if (categoryId !== '') {
            matchCategory = movie.categoryIds && movie.categoryIds.includes(parseInt(categoryId));
        }
        return matchTitle && matchLanguage && matchStatus && matchCategory;
    });

    if (sortBy === 'newest') {
        filteredMovies.sort((a, b) => new Date(b.releaseDate) - new Date(a.releaseDate));
    } else if (sortBy === 'oldest') {
        filteredMovies.sort((a, b) => new Date(a.releaseDate) - new Date(b.releaseDate));
    }

    currentPage = 1;
    renderMovies();
    renderPagination();
}

function renderMovies() {
    const container = document.getElementById('movieListContainer');
    container.innerHTML = '';

    if (filteredMovies.length === 0) {
        container.innerHTML = `<div class="col-12 text-center py-5"><h5 class="text-muted"><i class="fa fa-film fa-2x mb-3 d-block"></i>Không tìm thấy bộ phim nào phù hợp.</h5></div>`;
        return;
    }

    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const moviesToShow = filteredMovies.slice(startIndex, endIndex);

    moviesToShow.forEach(movie => {
        // Nếu không có ảnh, dùng ảnh mặc định
        const banner = movie.bannerPath ? movie.bannerPath : 'https://via.placeholder.com/300x450?text=No+Image';

        let statusBadge = "bg-secondary";
        if (movie.status === 'Now Playing') statusBadge = "bg-success";
        else if (movie.status === 'Coming Soon') statusBadge = "bg-warning text-dark";

        const categories = movie.categoryNames && movie.categoryNames.length > 0
            ? movie.categoryNames.join(', ')
            : 'Chưa phân loại';

        // Vẽ Khung Card hiển thị đầy đủ
        container.innerHTML += `
            <div class="col-lg-3 col-md-4 col-sm-6 mb-4">
                <div class="card h-100 border-0 shadow-sm movie-card" style="border-radius: 12px; overflow: hidden; cursor: pointer; transition: transform 0.2s;" onclick="goToDetail(${movie.movieId})" onmouseover="this.style.transform='translateY(-5px)'" onmouseout="this.style.transform='translateY(0)'">
                    <div class="position-relative">
                        <img src="${banner}" class="card-img-top" alt="${movie.title}" style="height: 380px; object-fit: cover;">
                        <span class="position-absolute top-0 end-0 badge ${statusBadge} m-2 px-3 py-2 shadow">${movie.status}</span>
                    </div>
                    <div class="card-body d-flex flex-column bg-white">
                        <h5 class="card-title fw-bold text-dark text-truncate" title="${movie.title}">${movie.title}</h5>
                        <p class="text-muted small mb-2 text-truncate"><i class="fa fa-tags text-primary"></i> ${categories}</p>
                        <div class="mt-auto d-flex justify-content-between align-items-center border-top pt-3">
                            <span class="fw-bold text-danger"><i class="fa fa-clock-o"></i> ${movie.duration || '?'} Phút</span>
                            <span class="text-muted small"><i class="fa fa-calendar"></i> ${movie.releaseDate || 'N/A'}</span>
                        </div>
                    </div>
                </div>
            </div>
        `;
    });
}

function renderPagination() {
    const totalPages = Math.ceil(filteredMovies.length / itemsPerPage);
    const pagination = document.getElementById('paginationContainer');
    pagination.innerHTML = '';

    if (totalPages <= 1) return;

    let html = '';
    for (let i = 1; i <= totalPages; i++) {
        html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="javascript:void(0);" onclick="changePage(${i})">${i}</a>
                 </li>`;
    }
    pagination.innerHTML = html;
}

function changePage(page) {
    currentPage = page;
    renderMovies();
    renderPagination();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function goToDetail(movieId) {
    saveSearchState();
    window.location.href = `/detail?id=${movieId}`;
}

function saveSearchState() {
    sessionStorage.setItem('movieSearchState', JSON.stringify({
        title: document.getElementById('searchTitle').value,
        categoryId: document.getElementById('filterCategory').value,
        status: document.getElementById('filterStatus').value,
        language: document.getElementById('searchLanguage').value,
        sortBy: document.getElementById('sortBy').value,
        page: currentPage,
        scrollY: window.scrollY
    }));
}

function restoreSearchState() {
    const stateStr = sessionStorage.getItem('movieSearchState');
    if (stateStr) {
        const state = JSON.parse(stateStr);
        document.getElementById('searchTitle').value = state.title || '';
        document.getElementById('filterCategory').value = state.categoryId || '';
        const statusInput = document.getElementById('filterStatus');
        if (statusInput) statusInput.value = state.status || '';
        const langInput = document.getElementById('searchLanguage');
        if (langInput) langInput.value = state.language || '';
        document.getElementById('sortBy').value = state.sortBy || 'newest';
        currentPage = state.page || 1;

        setTimeout(() => {
            window.scrollTo(0, state.scrollY || 0);
            sessionStorage.removeItem('movieSearchState');
        }, 100);
    }
}
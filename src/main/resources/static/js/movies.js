let allMovies = [];
let filteredMovies = [];
let currentPage = 1;
const itemsPerPage = 20;

document.addEventListener('DOMContentLoaded', async () => {
    await loadCategories();

    // CHỈ PHỤC HỒI 1 LẦN KHI LOAD TRANG
    restoreSearchState();

    // Tự động lọc khi gõ chữ (Title, Language)
    let typingTimer;
    const textInputs = ['searchTitle', 'searchLanguage'];
    textInputs.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener('input', () => {
                clearTimeout(typingTimer);
                typingTimer = setTimeout(applyFilters, 500); // Đợi 0.5s sau khi ngừng gõ mới lọc
            });
            element.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') e.preventDefault();
            });
        }
    });

    // Tự động lọc khi đổi Dropdown (Category, Status, Sort)
    const dropdownInputs = ['filterCategory', 'filterStatus', 'sortBy'];
    dropdownInputs.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener('change', applyFilters);
        }
    });

    await loadMovies();

    // CHỈ CUỘN TRANG 1 LẦN KHI LOAD
    restoreScrollPosition();
});

async function loadCategories() {
    try {
        const res = await fetch('/api/public/categories');
        const categories = await res.json();
        const select = document.getElementById('filterCategory');
        if (select) {
            categories.forEach(cat => {
                select.innerHTML += `<option value="${cat.categoryId}">${cat.name}</option>`;
            });
        }
    } catch (e) { console.error("Error loading categories", e); }
}

async function loadMovies() {
    try {
        const titleEle = document.getElementById('searchTitle');
        const catEle = document.getElementById('filterCategory');
        const langEle = document.getElementById('searchLanguage');
        const statusEle = document.getElementById('filterStatus');
        const sortEle = document.getElementById('sortBy');

        const title = titleEle ? titleEle.value.trim() : '';
        const categoryId = catEle ? catEle.value : '';
        const language = langEle ? langEle.value.trim() : '';
        const status = statusEle ? statusEle.value : '';
        const sortBy = sortEle ? sortEle.value : 'releaseDateDesc';

        // NỐI ĐẦY ĐỦ CÁC THAM SỐ VÀO API
        let apiUrl = `/api/public/movies?sortBy=${sortBy}`;
        if(title) apiUrl += `&title=${encodeURIComponent(title)}`;
        if(categoryId) apiUrl += `&categoryId=${categoryId}`;
        if(language) apiUrl += `&language=${encodeURIComponent(language)}`;
        if(status) apiUrl += `&status=${encodeURIComponent(status)}`;

        // Hiển thị loading mượt mà
        document.getElementById('movieListContainer').innerHTML = `
            <div class="col-12 text-center py-5">
                <div class="spinner-border text-primary" role="status"></div>
                <p class="mt-2 text-muted">Loading movies...</p>
            </div>`;

        const res = await fetch(apiUrl);
        if (!res.ok) throw new Error("Failed to fetch movies");

        filteredMovies = await res.json();

        renderMovies();
        renderPagination();

    } catch (e) {
        console.error("Error loading movies", e);
        document.getElementById('movieListContainer').innerHTML = `<div class="col-12 text-center py-5"><h5 class="text-danger">Failed to load movies. Please try again later.</h5></div>`;
    }
}

function applyFilters() {
    currentPage = 1;
    loadMovies();
}
function renderMovies() {
    const container = document.getElementById('movieListContainer');
    container.innerHTML = '';

    if (filteredMovies.length === 0) {
        container.innerHTML = `<div class="col-12 text-center py-5"><h5 class="text-muted">No movies found!</h5></div>`;
        return;
    }

    // Logic Phân trang
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const moviesToShow = filteredMovies.slice(startIndex, endIndex);

    moviesToShow.forEach(movie => {
        const catNames = movie.categoryNames ? movie.categoryNames.join(', ') : 'Category';

        container.innerHTML += `
        <div class="col-md-3 col-sm-6 mb-4">
           <div class="trend_2i position-relative shadow-sm rounded overflow-hidden"
                style="cursor:pointer; transition: transform 0.3s;"
                onclick="goToDetail(${movie.movieId})"
                onmouseover="this.style.transform='scale(1.03)'"
                onmouseout="this.style.transform='scale(1)'">
             <img src="${movie.bannerPath || '/img/placeholder.jpg'}" class="w-100" style="height: 460px; object-fit: cover;" alt="${movie.title}">
             <div class="position-absolute bottom-0 w-100 p-3" style="background: linear-gradient(to top, rgba(0,0,0,0.95) 0%, rgba(0,0,0,0.5) 60%, transparent 100%);">

                <h6 class="text-white fw-bold text-truncate mb-1" style="font-size: 1.1rem;" title="${movie.title}">${movie.title}</h6>

                <small class="text-light d-block text-truncate mb-1" style="font-size: 0.85rem;" title="${catNames} | ${movie.language}">
                    ${catNames} <span class="mx-1 text-white-50">|</span> <span class="text-warning fw-bold">${movie.language || 'N/A'}</span>
                </small>

                <small class="fw-bold" style="color: #eb7a18; font-size: 0.85rem;">
                    <i class="fa fa-clock-o me-1"></i> ${movie.duration} Mins
                </small>
             </div>
           </div>
        </div>
        `;
    });
}

function renderPagination() {
    const container = document.getElementById('paginationContainer');
    container.innerHTML = '';
    const totalPages = Math.ceil(filteredMovies.length / itemsPerPage);

    if (totalPages <= 1) return; // Nếu chỉ có 1 trang thì ẩn luôn thanh phân trang

    // Nút Previous (Quay lại)
    if (currentPage > 1) {
        container.innerHTML += `<li class="page-item"><a class="page-link text-primary fw-bold" href="#" onclick="changePage(${currentPage - 1}); return false;">&laquo;</a></li>`;
    } else {
        container.innerHTML += `<li class="page-item disabled"><a class="page-link text-muted">&laquo;</a></li>`;
    }

    // Các số trang (1, 2, 3...)
    for (let i = 1; i <= totalPages; i++) {
        if (i === currentPage) {
            container.innerHTML += `<li class="page-item active"><a class="page-link fw-bold">${i}</a></li>`;
        } else {
            container.innerHTML += `<li class="page-item"><a class="page-link text-dark" href="#" onclick="changePage(${i}); return false;">${i}</a></li>`;
        }
    }

    // Nút Next (Tiếp theo)
    if (currentPage < totalPages) {
        container.innerHTML += `<li class="page-item"><a class="page-link text-primary fw-bold" href="#" onclick="changePage(${currentPage + 1}); return false;">&raquo;</a></li>`;
    } else {
        container.innerHTML += `<li class="page-item disabled"><a class="page-link text-muted">&raquo;</a></li>`;
    }
}

function changePage(page) {
    currentPage = page;
    renderMovies();
    renderPagination();
    window.scrollTo({ top: document.getElementById('trend').offsetTop - 50, behavior: 'smooth' });
}

function goToDetail(movieId) {
    sessionStorage.setItem('movieSearchState', JSON.stringify({
        title: document.getElementById('searchTitle').value,
        categoryId: document.getElementById('filterCategory').value,
        sortBy: document.getElementById('sortBy').value,
        page: currentPage,
        scrollY: window.scrollY
    }));

    // 2. Chuyển hướng
    window.location.href = `/detail?id=${movieId}`;
}

function restoreSearchState() {
    const stateStr = sessionStorage.getItem('movieSearchState');
    if (stateStr) {
        const state = JSON.parse(stateStr);
        document.getElementById('searchTitle').value = state.title || '';
        document.getElementById('filterCategory').value = state.categoryId || '';

        const langInput = document.getElementById('searchLanguage');
        if (langInput) langInput.value = state.language || '';

        const statusInput = document.getElementById('filterStatus');
        if (statusInput) statusInput.value = state.status || '';

        if (state.sortBy) {
            if (state.sortBy === 'newest') document.getElementById('sortBy').value = 'releaseDateDesc';
            else if (state.sortBy === 'oldest') document.getElementById('sortBy').value = 'releaseDateAsc';
            else document.getElementById('sortBy').value = state.sortBy;
        } else {
            document.getElementById('sortBy').value = 'releaseDateDesc';
        }

        currentPage = state.page || 1;
    }
}

function restoreScrollPosition() {
    const stateStr = sessionStorage.getItem('movieSearchState');
    if (stateStr) {
        const state = JSON.parse(stateStr);
        setTimeout(() => {
            window.scrollTo(0, state.scrollY || 0);
            sessionStorage.removeItem('movieSearchState');
        }, 100);
    }
}
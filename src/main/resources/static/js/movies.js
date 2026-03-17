let allMovies = [];
let filteredMovies = [];
let currentPage = 1;
const itemsPerPage = 20; // Số phim trên 1 trang

document.addEventListener('DOMContentLoaded', async () => {
    await loadCategories();
    await loadMovies();
});

async function loadCategories() {
    try {
        const res = await fetch('/api/public/categories');
        const categories = await res.json();
        const select = document.getElementById('filterCategory');
        categories.forEach(cat => {
            select.innerHTML += `<option value="${cat.categoryId}">${cat.name}</option>`;
        });
    } catch (e) { console.error("Error loading categories", e); }
}

async function loadMovies() {
    try {
        restoreSearchState();

        const title = document.getElementById('searchTitle').value.trim();
        const categoryId = document.getElementById('filterCategory').value;
        const sortBy = document.getElementById('sortBy').value;

        let apiUrl = `/api/public/movies?sortBy=${sortBy}`;
        if(title) apiUrl += `&title=${encodeURIComponent(title)}`;
        if(categoryId) apiUrl += `&categoryId=${categoryId}`;

        const res = await fetch(apiUrl);
        if (!res.ok) throw new Error("Failed to fetch movies");

        const data = await res.json();

        // Dữ liệu lúc này đã được Backend lọc cực chuẩn (và loại bỏ sẵn phim Disabled), chỉ việc in ra
        filteredMovies = data;

        renderMovies();
        renderPagination();
        restoreScrollPosition();

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

                <small class="text-light d-block text-truncate mb-1" style="font-size: 0.85rem;" title="${catNames}">${catNames}</small>

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

    if (totalPages <= 1) return;

    // Nút Prev
    if (currentPage > 1) {
        container.innerHTML += `<li style="cursor:pointer;" onclick="changePage(${currentPage - 1})"><a><i class="fa fa-chevron-left"></i></a></li>`;
    }

    for (let i = 1; i <= totalPages; i++) {
        const activeClass = i === currentPage ? 'class="act"' : '';
        container.innerHTML += `<li style="cursor:pointer;" onclick="changePage(${i})"><a ${activeClass}>${i}</a></li>`;
    }

    // Nút Next
    if (currentPage < totalPages) {
        container.innerHTML += `<li style="cursor:pointer;" onclick="changePage(${currentPage + 1})"><a><i class="fa fa-chevron-right"></i></a></li>`;
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
        document.getElementById('sortBy').value = state.sortBy || 'newest';
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
let allMovies = [];
let filteredMovies = [];
let currentPage = 1;
const itemsPerPage = 10; // 10 phim / 1 trang

let typingTimer;
const doneTypingInterval = 500;

document.addEventListener('DOMContentLoaded', async () => {
    await loadCategories();
    await loadMovies();

    // Sự kiện tìm kiếm (Live Search)
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

    // Sự kiện Dropdown
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
        if (res.ok) {
            const categories = await res.json();
            const select = document.getElementById('filterCategory');
            if (select) {
                categories.forEach(cat => {
                    select.innerHTML += `<option value="${cat.categoryId}">${cat.name}</option>`;
                });
            }
        }
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
        document.getElementById('movieListContainer').innerHTML =
            `<div class="col-12 text-center text-danger py-5">
                <h5><i class="fa-solid fa-triangle-exclamation me-2"></i>Không thể tải danh sách phim. Vui lòng kiểm tra lại kết nối.</h5>
             </div>`;
    }
}

function applyFilters() {
    const title = document.getElementById('searchTitle') ? document.getElementById('searchTitle').value.toLowerCase() : '';
    const categoryId = document.getElementById('filterCategory') ? document.getElementById('filterCategory').value : '';
    const language = document.getElementById('searchLanguage') ? document.getElementById('searchLanguage').value.toLowerCase() : '';
    const status = document.getElementById('filterStatus') ? document.getElementById('filterStatus').value : '';
    const sortBy = document.getElementById('sortBy') ? document.getElementById('sortBy').value : 'newest';

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

    const totalPages = Math.ceil(filteredMovies.length / itemsPerPage);
    if (currentPage > totalPages && totalPages > 0) currentPage = totalPages;
    if (totalPages === 0) currentPage = 1;

    renderMovies();
    renderPagination();
}

function renderMovies() {
    const container = document.getElementById('movieListContainer');
    if (!container) return;

    container.innerHTML = '';

    if (filteredMovies.length === 0) {
        container.innerHTML = `<div class="col-12 text-center py-5">
            <h5 class="text-muted"><i class="fa fa-film fa-2x mb-3 d-block"></i>Không tìm thấy bộ phim nào phù hợp.</h5>
        </div>`;
        return;
    }

    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const moviesToShow = filteredMovies.slice(startIndex, endIndex);

    moviesToShow.forEach(movie => {
        let banner = 'https://via.placeholder.com/300x450?text=No+Image';
        if (movie.bannerPath) {
            banner = movie.bannerPath.startsWith('/') || movie.bannerPath.startsWith('http') ? movie.bannerPath : '/' + movie.bannerPath;
        }

        let statusBadge = "bg-secondary";
        if (movie.status === 'Now Playing') statusBadge = "bg-success";
        else if (movie.status === 'Coming Soon') statusBadge = "bg-warning text-dark";

        const categories = movie.categoryNames && movie.categoryNames.length > 0
            ? movie.categoryNames.join(', ')
            : 'Chưa phân loại';

        // VŨ KHÍ BÍ MẬT: Đoạn script tự động gọi OMDb nếu ảnh bị hỏng 404
        let omdbFallback = `fetch('https://www.omdbapi.com/?t=${encodeURIComponent(movie.title)}&apikey=9b0b8e47')` +
            `.then(res=>res.json()).then(data=>{ if(data.Poster && data.Poster!=='N/A') this.src=data.Poster; else this.src='https://via.placeholder.com/300x450?text=Image+Not+Found'; })` +
            `.catch(()=>this.src='https://via.placeholder.com/300x450?text=Image+Not+Found');`;

        container.innerHTML += `
            <div class="col-lg-3 col-md-4 col-sm-6 mb-4">
                <div class="card h-100 border-0 shadow-sm movie-card" style="border-radius: 12px; overflow: hidden; cursor: pointer; transition: transform 0.2s;" onclick="goToDetail(${movie.movieId})" onmouseover="this.style.transform='translateY(-5px)'" onmouseout="this.style.transform='translateY(0)'">
                    <div class="position-relative">
                        <img src="${banner}" 
                             onerror="if(!this.hasAttribute('data-retried')){ this.setAttribute('data-retried', 'true'); ${omdbFallback} } else { this.src='https://via.placeholder.com/300x450?text=Image+Not+Found'; }" 
                             class="card-img-top" alt="${movie.title}" style="height: 380px; object-fit: cover;">
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
    if (!pagination) return;

    pagination.innerHTML = '';

    // FIX LỖI PHÂN TRANG: Luôn hiển thị thanh phân trang dù chỉ có 1 trang để user biết
    if (totalPages === 0) return;

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
        title: document.getElementById('searchTitle') ? document.getElementById('searchTitle').value : '',
        categoryId: document.getElementById('filterCategory') ? document.getElementById('filterCategory').value : '',
        status: document.getElementById('filterStatus') ? document.getElementById('filterStatus').value : '',
        language: document.getElementById('searchLanguage') ? document.getElementById('searchLanguage').value : '',
        sortBy: document.getElementById('sortBy') ? document.getElementById('sortBy').value : 'newest',
        page: currentPage,
        scrollY: window.scrollY
    }));
}

function restoreSearchState() {
    const stateStr = sessionStorage.getItem('movieSearchState');
    if (stateStr) {
        const state = JSON.parse(stateStr);
        if(document.getElementById('searchTitle')) document.getElementById('searchTitle').value = state.title || '';
        if(document.getElementById('filterCategory')) document.getElementById('filterCategory').value = state.categoryId || '';
        if(document.getElementById('filterStatus')) document.getElementById('filterStatus').value = state.status || '';
        if(document.getElementById('searchLanguage')) document.getElementById('searchLanguage').value = state.language || '';
        if(document.getElementById('sortBy')) document.getElementById('sortBy').value = state.sortBy || 'newest';

        currentPage = state.page || 1;

        setTimeout(() => {
            window.scrollTo(0, state.scrollY || 0);
            sessionStorage.removeItem('movieSearchState');
        }, 100);
    }
}
let allMovies = [];
let filteredMovies = [];
let currentPage = 1;
const itemsPerPage = 20;

const DEFAULT_SORT = 'releaseDateDesc';

document.addEventListener('DOMContentLoaded', async () => {
    await loadCategories();
    await loadLanguages();

    restoreSearchState();

    let typingTimer;
    const titleInput = document.getElementById('searchTitle');
    if (titleInput) {
        titleInput.addEventListener('input', () => {
            clearTimeout(typingTimer);
            typingTimer = setTimeout(applyFilters, 500);
        });
        titleInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') e.preventDefault();
        });
    }

    const dropdownInputs = ['filterCategory', 'filterLanguage', 'filterStatus', 'sortBy'];
    dropdownInputs.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener('change', applyFilters);
        }
    });

    const resetBtn = document.getElementById('resetFiltersBtn');
    if (resetBtn) {
        resetBtn.addEventListener('click', resetFilters);
    }

    await loadMovies();
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
    } catch (e) {
        console.error('Error loading categories', e);
    }
}

async function loadLanguages(selectedLanguage = '') {
    try {
        const res = await fetch('/api/public/movies?sortBy=releaseDateDesc');
        if (!res.ok) throw new Error('Failed to fetch languages');

        const movies = await res.json();
        const languages = [...new Set((movies || [])
            .map(m => (m.language || '').trim())
            .filter(l => l.length > 0))].sort((a, b) => a.localeCompare(b));

        const select = document.getElementById('filterLanguage');
        if (!select) return;

        select.innerHTML = '<option value="">-- All Languages --</option>';
        languages.forEach(lang => {
            select.innerHTML += `<option value="${lang}">${lang}</option>`;
        });

        if (selectedLanguage) {
            select.value = selectedLanguage;
        }
    } catch (e) {
        console.error('Error loading languages', e);
    }
}

async function loadMovies() {
    try {
        const title = document.getElementById('searchTitle')?.value.trim() || '';
        const categoryId = document.getElementById('filterCategory')?.value || '';
        const language = document.getElementById('filterLanguage')?.value || '';
        const status = document.getElementById('filterStatus')?.value || '';
        const sortBy = document.getElementById('sortBy')?.value || DEFAULT_SORT;

        let apiUrl = `/api/public/movies?sortBy=${sortBy}`;
        if (title) apiUrl += `&title=${encodeURIComponent(title)}`;
        if (categoryId) apiUrl += `&categoryId=${categoryId}`;
        if (language) apiUrl += `&language=${encodeURIComponent(language)}`;
        if (status) apiUrl += `&status=${encodeURIComponent(status)}`;

        document.getElementById('movieListContainer').innerHTML = `
            <div class="col-12 text-center py-5">
                <div class="spinner-border text-primary" role="status"></div>
                <p class="mt-2 text-muted">Loading movies...</p>
            </div>`;

        const res = await fetch(apiUrl);
        if (!res.ok) throw new Error('Failed to fetch movies');

        filteredMovies = await res.json();

        renderMovies();
        renderPagination();
    } catch (e) {
        console.error('Error loading movies', e);
        document.getElementById('movieListContainer').innerHTML = `<div class="col-12 text-center py-5"><h5 class="text-danger">Failed to load movies. Please try again later.</h5></div>`;
    }
}

async function resetFilters() {
    const titleInput = document.getElementById('searchTitle');
    const categoryInput = document.getElementById('filterCategory');
    const statusInput = document.getElementById('filterStatus');
    const sortInput = document.getElementById('sortBy');

    if (titleInput) titleInput.value = '';
    if (categoryInput) categoryInput.value = '';
    if (statusInput) statusInput.value = '';
    if (sortInput) sortInput.value = DEFAULT_SORT;

    await loadLanguages('');

    currentPage = 1;
    sessionStorage.removeItem('movieSearchState');
    await loadMovies();
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

    if (totalPages <= 1) return;

    if (currentPage > 1) {
        container.innerHTML += `<li class="page-item"><a class="page-link text-primary fw-bold" href="#" onclick="changePage(${currentPage - 1}); return false;">&laquo;</a></li>`;
    } else {
        container.innerHTML += `<li class="page-item disabled"><a class="page-link text-muted">&laquo;</a></li>`;
    }

    for (let i = 1; i <= totalPages; i++) {
        if (i === currentPage) {
            container.innerHTML += `<li class="page-item active"><a class="page-link fw-bold">${i}</a></li>`;
        } else {
            container.innerHTML += `<li class="page-item"><a class="page-link text-dark" href="#" onclick="changePage(${i}); return false;">${i}</a></li>`;
        }
    }

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
        language: document.getElementById('filterLanguage').value,
        status: document.getElementById('filterStatus').value,
        sortBy: document.getElementById('sortBy').value,
        page: currentPage,
        scrollY: window.scrollY
    }));

    window.location.href = `/detail?id=${movieId}`;
}

function restoreSearchState() {
    const stateStr = sessionStorage.getItem('movieSearchState');
    if (stateStr) {
        const state = JSON.parse(stateStr);

        document.getElementById('searchTitle').value = state.title || '';
        document.getElementById('filterCategory').value = state.categoryId || '';

        const langInput = document.getElementById('filterLanguage');
        if (langInput) langInput.value = state.language || '';

        const statusInput = document.getElementById('filterStatus');
        if (statusInput) statusInput.value = state.status || '';

        if (state.sortBy) {
            if (state.sortBy === 'newest') document.getElementById('sortBy').value = 'releaseDateDesc';
            else if (state.sortBy === 'oldest') document.getElementById('sortBy').value = 'releaseDateAsc';
            else document.getElementById('sortBy').value = state.sortBy;
        } else {
            document.getElementById('sortBy').value = DEFAULT_SORT;
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

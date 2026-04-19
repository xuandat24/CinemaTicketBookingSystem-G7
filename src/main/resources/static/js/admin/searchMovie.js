let typingTimer;
const doneTypingInterval = 500;
let allMovies = [];
let currentPage = 1;
const itemsPerPage = 10;
const DEFAULT_SORT = 'idDesc';

document.addEventListener('DOMContentLoaded', async () => {
    await loadCategoriesForFilter();
    await loadLanguagesForFilter();
    restoreSearchState();

    const urlParams = new URLSearchParams(window.location.search);
    const categoryIdFromUrl = urlParams.get('categoryId');
    if (categoryIdFromUrl) {
        document.getElementById('filterCategory').value = categoryIdFromUrl;
    }

    setupLiveSearch('searchTitle');

    const filterElements = ['filterCategory', 'filterLanguage', 'fromDate', 'toDate', 'sortBy', 'filterStatus'];
    filterElements.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener('change', applyFilter);
        }
    });

    const resetBtn = document.getElementById('resetFiltersBtn');
    if (resetBtn) {
        resetBtn.addEventListener('click', resetFilters);
    }

    loadMovies();
});

function setupLiveSearch(elementId) {
    const inputEle = document.getElementById(elementId);
    if (inputEle) {
        inputEle.addEventListener('input', function () {
            clearTimeout(typingTimer);
            typingTimer = setTimeout(() => { applyFilter(); }, doneTypingInterval);
        });
        inputEle.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') e.preventDefault();
        });
    }
}

async function loadCategoriesForFilter() {
    try {
        const token = sessionStorage.getItem('jwtToken');
        const response = await fetch('/api/admin/categories', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) throw new Error('Cannot fetch categories');

        const categories = await response.json();
        const select = document.getElementById('filterCategory');
        if (!select) return;

        categories.forEach(cat => {
            select.innerHTML += `<option value="${cat.categoryId}">${cat.name}</option>`;
        });
    } catch (error) {
        console.error('Error loading categories:', error);
    }
}

async function loadLanguagesForFilter(selectedLanguage = '') {
    try {
        const token = sessionStorage.getItem('jwtToken');
        const response = await fetch('/api/admin/movies?sortBy=idDesc', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) throw new Error('Cannot fetch languages');

        const movies = await response.json();
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
    } catch (error) {
        console.error('Error loading languages:', error);
    }
}

function applyFilter() {
    currentPage = 1;
    loadMovies();
}

async function resetFilters() {
    const titleInput = document.getElementById('searchTitle');
    const categoryInput = document.getElementById('filterCategory');
    const languageInput = document.getElementById('filterLanguage');
    const statusInput = document.getElementById('filterStatus');
    const fromDateInput = document.getElementById('fromDate');
    const toDateInput = document.getElementById('toDate');
    const sortInput = document.getElementById('sortBy');

    if (titleInput) titleInput.value = '';
    if (categoryInput) categoryInput.value = '';
    if (languageInput) languageInput.value = '';
    if (statusInput) statusInput.value = '';
    if (fromDateInput) fromDateInput.value = '';
    if (toDateInput) toDateInput.value = '';
    if (sortInput) sortInput.value = DEFAULT_SORT;

    sessionStorage.removeItem('adminMovieSearchState');
    currentPage = 1;

    await loadLanguagesForFilter('');
    await loadMovies();
}

async function loadMovies() {
    try {
        const title = document.getElementById('searchTitle').value.trim();
        const categoryId = document.getElementById('filterCategory').value;
        const language = document.getElementById('filterLanguage').value;
        const status = document.getElementById('filterStatus').value;
        const fromDate = document.getElementById('fromDate').value;
        const toDate = document.getElementById('toDate').value;
        const sortBy = document.getElementById('sortBy').value || DEFAULT_SORT;

        let apiUrl = `/api/admin/movies?sortBy=${sortBy}`;
        if (title) apiUrl += `&title=${encodeURIComponent(title)}`;
        if (categoryId) apiUrl += `&categoryId=${categoryId}`;
        if (language) apiUrl += `&language=${encodeURIComponent(language)}`;
        if (status) apiUrl += `&status=${encodeURIComponent(status)}`;
        if (fromDate) apiUrl += `&fromDate=${fromDate}`;
        if (toDate) apiUrl += `&toDate=${toDate}`;

        const token = sessionStorage.getItem('jwtToken');
        const response = await fetch(apiUrl, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) throw new Error('Network response was not ok');

        allMovies = await response.json();
        renderMovies();
        renderPagination();

    } catch (error) {
        alert('Failed to fetch movies from the server.');
    }
}

// ĐÃ SỬA: Thay đổi UI HTML sinh ra cho đẹp hơn, logic vòng lặp giữ nguyên
function renderMovies() {
    const tbody = document.querySelector('#movieTable tbody');
    tbody.innerHTML = '';

    if (allMovies.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-5"><i class="fa-solid fa-box-open fs-2 mb-2"></i><br>No movies found matching your criteria.</td></tr>`;
        document.getElementById('tableInfo').innerText = 'Showing 0 movies';
        return;
    }

    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const moviesToShow = allMovies.slice(startIndex, endIndex);

    document.getElementById('tableInfo').innerText = `Showing ${startIndex + 1} to ${Math.min(endIndex, allMovies.length)} of ${allMovies.length} movies`;
    
    moviesToShow.forEach(movie => {
        // Style Status Badge
        let statusBadge = 'bg-secondary';
        if (movie.status === 'Now Playing') statusBadge = 'bg-success';
        else if (movie.status === 'Coming Soon') statusBadge = 'bg-warning text-dark';
        else if (movie.status === 'Pending') statusBadge = 'bg-info text-dark';
        else if (movie.status === 'Disabled') statusBadge = 'bg-danger';

        // Make it a pill with a light shadow
        statusBadge += ' rounded-pill px-3 py-2 shadow-sm fw-bold';

        // Style Category Badge
        let categoryTags = movie.categoryNames && movie.categoryNames.length > 0
            ? movie.categoryNames.map(name => `<span class="badge bg-light text-dark border me-1 mb-1 px-2 py-1 shadow-sm">${name}</span>`).join('')
            : '<span class="badge bg-light text-muted border border-dashed px-2 py-1">No category</span>';

        const imagePath = movie.posterPath || movie.bannerPath || 'https://placehold.co/45x65/eeeeee/999999?text=No+Img';

        tbody.innerHTML += `
            <tr>
                <td class="fw-bold text-secondary">#${movie.movieId}</td>
                <td>
                    <div class="d-flex align-items-center">
                        <img src="${imagePath}" alt="poster" style="width: 50px; height: 75px; object-fit: cover; border-radius: 6px; margin-right: 15px; box-shadow: 0 4px 10px rgba(0,0,0,0.15);">
                        <div>
                            <div class="fw-bold text-dark mb-1 fs-6">${movie.title}</div>
                            <div class="mb-1">${categoryTags}</div>
                            <div class="text-muted small"><i class="fa-solid fa-globe text-primary me-1"></i> ${movie.language || 'Unknown'}</div>
                        </div>
                    </div>
                </td>
                <td class="text-muted fw-bold">
                    <i class="fa-regular fa-calendar-days text-info me-1"></i> ${movie.releaseDate || 'N/A'}
                </td>
                <td>
                    <span class="badge ${statusBadge}">${movie.status}</span>
                </td>
                <td class="text-end">
                    <a href="/admin/movies/edit?id=${movie.movieId}" class="btn tech-btn tech-btn-primary" onclick="saveSearchState()">
                        <i class="fa-solid fa-pen-to-square me-1"></i> Edit Config
                    </a>
                </td>
            </tr>
        `;
    });
}

function renderPagination() {
    const totalPages = Math.ceil(allMovies.length / itemsPerPage);
    const paginationContainer = document.getElementById('paginationContainer');
    paginationContainer.innerHTML = '';

    if (totalPages <= 1) return;

    if (currentPage > 1) {
        paginationContainer.innerHTML += `<li class="page-item"><a class="page-link" href="#" onclick="changePage(${currentPage - 1}); return false;"><i class="fa-solid fa-chevron-left"></i></a></li>`;
    } else {
        paginationContainer.innerHTML += `<li class="page-item disabled"><a class="page-link text-muted"><i class="fa-solid fa-chevron-left"></i></a></li>`;
    }

    for (let i = 1; i <= totalPages; i++) {
        if (i === currentPage) {
            paginationContainer.innerHTML += `<li class="page-item active"><a class="page-link">${i}</a></li>`;
        } else {
            paginationContainer.innerHTML += `<li class="page-item"><a class="page-link" href="#" onclick="changePage(${i}); return false;">${i}</a></li>`;
        }
    }

    if (currentPage < totalPages) {
        paginationContainer.innerHTML += `<li class="page-item"><a class="page-link" href="#" onclick="changePage(${currentPage + 1}); return false;"><i class="fa-solid fa-chevron-right"></i></a></li>`;
    } else {
        paginationContainer.innerHTML += `<li class="page-item disabled"><a class="page-link text-muted"><i class="fa-solid fa-chevron-right"></i></a></li>`;
    }
}

function changePage(page) {
    currentPage = page;
    renderMovies();
    renderPagination();
    // Smooth scroll to the top of the table
    document.querySelector('.tech-card').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function saveSearchState() {
    sessionStorage.setItem('adminMovieSearchState', JSON.stringify({
        title: document.getElementById('searchTitle').value,
        categoryId: document.getElementById('filterCategory').value,
        language: document.getElementById('filterLanguage').value,
        status: document.getElementById('filterStatus').value,
        fromDate: document.getElementById('fromDate').value,
        toDate: document.getElementById('toDate').value,
        sortBy: document.getElementById('sortBy').value,
        page: currentPage
    }));
}

function restoreSearchState() {
    const stateStr = sessionStorage.getItem('adminMovieSearchState');
    if (stateStr) {
        const state = JSON.parse(stateStr);
        if (state.title) document.getElementById('searchTitle').value = state.title;
        if (state.categoryId) document.getElementById('filterCategory').value = state.categoryId;
        if (state.language) document.getElementById('filterLanguage').value = state.language;
        if (state.status) document.getElementById('filterStatus').value = state.status;
        if (state.fromDate) document.getElementById('fromDate').value = state.fromDate;
        if (state.toDate) document.getElementById('toDate').value = state.toDate;
        if (state.sortBy) document.getElementById('sortBy').value = state.sortBy;
        if (state.page) currentPage = state.page;
    }
}
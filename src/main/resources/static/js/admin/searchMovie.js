let typingTimer;
const doneTypingInterval = 500;

document.addEventListener('DOMContentLoaded', async () => {
    await loadCategoriesForFilter();

    restoreSearchState();

    const urlParams = new URLSearchParams(window.location.search);
    const categoryIdFromUrl = urlParams.get('categoryId');
    if (categoryIdFromUrl) {
        document.getElementById('filterCategory').value = categoryIdFromUrl;
    }

    setupLiveSearch('searchTitle');

    setupLiveSearch('searchLanguage');

    const filterElements = ['filterCategory', 'fromDate', 'toDate', 'sortBy', 'filterStatus'];
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
        const token = localStorage.getItem('jwtToken'); // LẤY TOKEN
        const response = await fetch('/api/admin/categories', {
            headers: { 'Authorization': `Bearer ${token}` } // GÀI TOKEN VÀO HEADER
        });
        if (!response.ok) throw new Error("Cannot fetch categories");

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
        const language = document.getElementById('searchLanguage').value.trim();
        const status = document.getElementById('filterStatus').value;
        const fromDate = document.getElementById('fromDate').value;
        const toDate = document.getElementById('toDate').value;
        const sortBy = document.getElementById('sortBy').value;

        let apiUrl = `/api/admin/movies?sortBy=${sortBy}`;
        if (title) apiUrl += `&title=${encodeURIComponent(title)}`;
        if (categoryId) apiUrl += `&categoryId=${categoryId}`;
        if (language) apiUrl += `&language=${encodeURIComponent(language)}`;
        if (status) apiUrl += `&status=${encodeURIComponent(status)}`;
        if (fromDate) apiUrl += `&fromDate=${fromDate}`;
        if (toDate) apiUrl += `&toDate=${toDate}`;

        const token = localStorage.getItem('jwtToken'); // LẤY TOKEN
        const response = await fetch(apiUrl, {
            headers: { 'Authorization': `Bearer ${token}` } // GÀI TOKEN VÀO HEADER
        });

        if (!response.ok) throw new Error("Network response was not ok");
        const movies = await response.json();

        const tbody = document.querySelector('#movieTable tbody');
        tbody.innerHTML = '';

        if (movies.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-4">No movies found matching your criteria.</td></tr>`;
            return;
        }

        movies.forEach(movie => {
            let statusBadge = "bg-secondary";
            if (movie.status === 'Now Playing') statusBadge = "bg-success";
            else if (movie.status === 'Coming Soon') statusBadge = "bg-warning text-dark";
            else if (movie.status === 'Pending') statusBadge = "bg-info text-dark";
            else if (movie.status === 'Disabled') statusBadge = "bg-danger";

            let categoryTags = movie.categoryNames && movie.categoryNames.length > 0
                ? movie.categoryNames.map(name => `<span class="badge bg-light text-dark border me-1">${name}</span>`).join('')
                : '<span class="badge bg-light text-muted border border-dashed">No category</span>';

            tbody.innerHTML += `
                <tr>
                    <td class="align-middle fw-bold">${movie.movieId}</td>
                    <td class="align-middle">
                        <div class="d-flex align-items-center">
                            ${movie.bannerPath ? `<img src="${movie.bannerPath}" alt="poster" style="width: 45px; height: 65px; object-fit: cover; border-radius: 4px; margin-right: 12px;">` : ''}
                            <div>
                                <div class="fw-bold text-dark mb-1">${movie.title}</div>
                                <div class="mb-1">${categoryTags}</div>
                                <div class="text-muted small"><i class="fa-solid fa-globe"></i> ${movie.language || 'Unknown'}</div>
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
                        <a href="/admin/movies/edit?id=${movie.movieId}" class="btn btn-sm btn-primary shadow-sm" onclick="saveSearchState()">
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

function saveSearchState() {
    sessionStorage.setItem('adminMovieSearchState', JSON.stringify({
        title: document.getElementById('searchTitle').value,
        categoryId: document.getElementById('filterCategory').value,
        language: document.getElementById('searchLanguage').value,
        status: document.getElementById('filterStatus').value,
        fromDate: document.getElementById('fromDate').value,
        toDate: document.getElementById('toDate').value,
        sortBy: document.getElementById('sortBy').value
    }));
}

function restoreSearchState() {
    const stateStr = sessionStorage.getItem('adminMovieSearchState');
    if (stateStr) {
        const state = JSON.parse(stateStr);
        if (state.title) document.getElementById('searchTitle').value = state.title;
        if (state.categoryId) document.getElementById('filterCategory').value = state.categoryId;
        if (state.language) document.getElementById('searchLanguage').value = state.language;
        if (state.status) document.getElementById('filterStatus').value = state.status;
        if (state.fromDate) document.getElementById('fromDate').value = state.fromDate;
        if (state.toDate) document.getElementById('toDate').value = state.toDate;
        if (state.sortBy) document.getElementById('sortBy').value = state.sortBy;
    }
}
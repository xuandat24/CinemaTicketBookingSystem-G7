const urlParams = new URLSearchParams(window.location.search);
const movieId = urlParams.get('id');

let selectedCategoryIds = [];

document.addEventListener('DOMContentLoaded', async () => {
    if (!movieId) {
        alert("Invalid Movie ID!");
        window.location.href = '/admin/movies/search';
        return;
    }
    await loadCategoryDropdown();
    await loadMovieData();
});

async function loadMovieData() {
    try {
        const token = localStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/movies/${movieId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            throw new Error(`Server returned status ${response.status}`);
        }

        const movie = await response.json();

        // 1. Hàm điền dữ liệu an toàn (Chỉ điền nếu thẻ HTML tồn tại)
        const setVal = (id, val) => {
            const el = document.getElementById(id);
            if (el) el.value = val;
        };

        setVal('editMovieId', movie.movieId);
        setVal('movieTitle', movie.title);
        setVal('movieDesc', movie.description);
        setVal('movieDuration', movie.duration);
        setVal('movieReleaseDate', movie.releaseDate);
        setVal('movieStatus', movie.status);
        setVal('movieDirector', movie.director || '');
        setVal('movieActors', movie.actors || '');
        setVal('movieRating', movie.rating || 0.0);
        setVal('movieLanguage', movie.language || 'Unknown');

        // 2. Load Categories
        if (movie.categoryIds) {
            selectedCategoryIds = movie.categoryIds.map(String);
            renderCategoryTags();
        }

        // 3. Hiển thị Banner Preview an toàn
        if (movie.bannerPath && document.getElementById('currentBannerContainer')) {
            document.getElementById('currentBannerContainer').classList.remove('d-none');
            if(document.getElementById('currentBannerImg')) document.getElementById('currentBannerImg').src = movie.bannerPath;
        }

        // 4. Hiển thị Poster Preview an toàn
        if (movie.posterPath && document.getElementById('currentPosterContainer')) {
            document.getElementById('currentPosterContainer').classList.remove('d-none');
            if(document.getElementById('currentPosterImg')) document.getElementById('currentPosterImg').src = movie.posterPath;
        }

        // 5. Hiển thị Trailer Preview an toàn (Phân biệt Youtube và File cứng)
        if (movie.trailerPath && document.getElementById('currentTrailerContainer')) {
            document.getElementById('currentTrailerContainer').classList.remove('d-none');
            const wrapper = document.getElementById('trailerMediaWrapper');

            if (wrapper) {
                if (movie.trailerPath.includes('youtube.com') || movie.trailerPath.includes('youtu.be')) {
                    wrapper.innerHTML = `<iframe style="width: 100%; height: 120px; border-radius: 5px;" src="${movie.trailerPath}" frameborder="0" allowfullscreen></iframe>`;
                } else {
                    wrapper.innerHTML = `<video controls style="width: 100%; max-height: 120px; border-radius: 5px; background: #000;"><source src="${movie.trailerPath}" type="video/mp4"></video>`;
                }
            }
        }

    } catch (error) {
        console.error("Lỗi chi tiết:", error);
        alert("Lỗi tải dữ liệu phim: " + error.message + "\n(Vui lòng mở F12 -> Console để xem chi tiết)");
        // Tạm thời comment dòng dưới để bạn ở lại trang đọc lỗi nếu có
        // window.location.href = '/admin/movies/search';
    }
}

async function loadCategoryDropdown() {
    const token = localStorage.getItem('jwtToken');
    const response = await fetch('/api/admin/categories', {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    const categories = await response.json();
    const select = document.getElementById('movieCategorySelect');
    if(select) {
        select.innerHTML = '<option value="">-- Select category to add --</option>';
        categories.forEach(c => select.innerHTML += `<option value="${c.categoryId}">${c.name}</option>`);
    }
}

function addCategoryTag() {
    const select = document.getElementById('movieCategorySelect');
    const id = select.value;
    if (id && !selectedCategoryIds.includes(id)) {
        selectedCategoryIds.push(id);
        renderCategoryTags();
    }
    select.value = "";
}

function removeCategoryTag(id) {
    selectedCategoryIds = selectedCategoryIds.filter(catId => catId !== id.toString());
    renderCategoryTags();
}

function renderCategoryTags() {
    const container = document.getElementById('selectedCategoryTags');
    if(!container) return;

    container.innerHTML = '';
    const select = document.getElementById('movieCategorySelect');

    selectedCategoryIds.forEach(id => {
        let name = Array.from(select.options).find(opt => opt.value === id)?.text || 'Unknown';
        container.innerHTML += `
            <span class="badge bg-primary text-white border me-2 mb-2 p-2" style="cursor:pointer;" onclick="removeCategoryTag('${id}')" title="Click to remove">
                ${name} <i class="fa-solid fa-xmark ms-1"></i>
            </span>
        `;
    });
}

const editForm = document.getElementById('editMovieForm');
if (editForm) {
    editForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (selectedCategoryIds.length === 0) return alert("Please add at least one category tag!");

        const btnSubmit = document.getElementById('btnUpdate');
        const originalText = btnSubmit.innerHTML;
        btnSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Updating...';
        btnSubmit.disabled = true;

        const formData = new FormData();
        formData.append('movieId', document.getElementById('editMovieId').value);
        formData.append('title', document.getElementById('movieTitle').value.trim());
        formData.append('description', document.getElementById('movieDesc').value.trim());
        formData.append('duration', document.getElementById('movieDuration').value);
        formData.append('releaseDate', document.getElementById('movieReleaseDate').value);
        formData.append('status', document.getElementById('movieStatus').value);
        formData.append('director', document.getElementById('movieDirector').value.trim());
        formData.append('actors', document.getElementById('movieActors').value.trim());
        formData.append('rating', document.getElementById('movieRating').value);
        formData.append('language', document.getElementById('movieLanguage').value.trim());

        selectedCategoryIds.forEach(id => formData.append('categoryIds', id));

        // Lấy File upload cứng
        const bannerFile = document.getElementById('movieBanner') ? document.getElementById('movieBanner').files[0] : null;
        const posterFile = document.getElementById('moviePosterFile') ? document.getElementById('moviePosterFile').files[0] : null;
        const trailerFile = document.getElementById('movieTrailer') ? document.getElementById('movieTrailer').files[0] : null;

        // Lấy Text URL
        const bannerUrl = document.getElementById('bannerUrl') ? document.getElementById('bannerUrl').value.trim() : "";
        const posterUrl = document.getElementById('posterUrl') ? document.getElementById('posterUrl').value.trim() : "";
        const trailerUrl = document.getElementById('movieTrailerUrl') ? document.getElementById('movieTrailerUrl').value.trim() : "";

        if (bannerFile) formData.append('bannerFile', bannerFile);
        if (posterFile) formData.append('posterFile', posterFile);
        if (trailerFile) formData.append('trailerFile', trailerFile);

        if (bannerUrl) formData.append('bannerUrl', bannerUrl);
        if (posterUrl) formData.append('posterUrl', posterUrl);
        if (trailerUrl) formData.append('trailerPath', trailerUrl); // Mapping chuẩn về backend

        try {
            const token = localStorage.getItem('jwtToken');
            const response = await fetch(`/api/admin/movies/${movieId}`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${token}` },
                body: formData
            });
            const data = await response.json();

            if (response.ok) {
                alert("Success: " + data.message);
                window.location.href = '/admin/movies/search';
            } else {
                alert("Error: " + (data.message || "Failed to update movie."));
            }
        } catch (error) {
            alert("Server connection error!");
        } finally {
            btnSubmit.innerHTML = originalText;
            btnSubmit.disabled = false;
        }
    });
}

const btnDelete = document.getElementById('btnDeleteMovie');
if (btnDelete) {
    btnDelete.addEventListener('click', async () => {
        if(!confirm("DANGER: Are you absolutely sure you want to delete this movie? This action will disable the movie.")) return;
        try{
            const token = localStorage.getItem('jwtToken');
            const response = await fetch(`/api/admin/movies/${movieId}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            const data = await response.json();

            if(response.ok){
                alert("Success: " + data.message);
                window.location.href = '/admin/movies/search';
            } else {
                alert("Error: " + (data.message || "Failed to delete movie"));
            }
        } catch (error) {
            alert("There is some error when trying to delete movie");
        }
    });
}
const urlParams = new URLSearchParams(window.location.search);
const comboId = urlParams.get('id');

function normalizeComboImagePath(path) {
    if (!path || path === '-') return '';
    if (path.startsWith('http') || path.startsWith('data:')) return path;

    let cleanPath = path.trim();
    if (cleanPath.startsWith('/')) cleanPath = cleanPath.substring(1);

    if (cleanPath.startsWith('uploads/combos/')) return cleanPath.substring('uploads/combos/'.length);
    if (cleanPath.startsWith('uploads/')) return cleanPath.substring('uploads/'.length);
    if (cleanPath.startsWith('combos/')) return cleanPath.substring('combos/'.length);

    return cleanPath;
}

function resolveImageSources(path) {
    const cleanPath = normalizeComboImagePath(path);
    if (!cleanPath) return { primary: '', fallback: '' };

    if (cleanPath.startsWith('http') || cleanPath.startsWith('data:')) {
        return { primary: cleanPath, fallback: '' };
    }

    // If DB stores plain filename from ComboDataSeeder, prefer static /img first.
    if (!String(path).includes('/') && !String(path).startsWith('uploads')) {
        return {
            primary: '/img/' + cleanPath,
            fallback: '/combos/' + cleanPath
        };
    }

    return {
        primary: '/combos/' + cleanPath,
        fallback: '/img/' + cleanPath
    };
}

document.addEventListener('DOMContentLoaded', async () => {
    if (!comboId) {
        if (typeof showToast === 'function') showToast('Combo ID not found!', 'error');
        setTimeout(() => { window.location.href = '/admin/combos'; }, 1500);
        return;
    }

    const priceInput = document.getElementById('editComboPrice');
    if (priceInput) {
        priceInput.addEventListener('focus', (e) => { e.target.value = e.target.value.replace(/\D/g, ''); });
        priceInput.addEventListener('input', (e) => { e.target.value = e.target.value.replace(/\D/g, ''); });
        priceInput.addEventListener('blur', (e) => {
            const val = e.target.value.replace(/\D/g, '');
            if (val) e.target.value = Number(val).toLocaleString('vi-VN');
        });
    }

    const imageFileInput = document.getElementById('editComboImageFile');
    if (imageFileInput) imageFileInput.addEventListener('change', previewSelectedImage);

    await loadComboData();
});

async function loadComboData() {
    try {
        const token = sessionStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/combos/${comboId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Combo not found');

        const combo = await response.json();
        document.getElementById('editComboName').value = combo.name;
        document.getElementById('editComboDescription').value = combo.description || '';
        if (combo.price) document.getElementById('editComboPrice').value = Number(combo.price).toLocaleString('vi-VN');

        const imagePathInput = document.getElementById('editComboImagePath');
        const previewContainer = document.getElementById('editComboImagePreviewContainer');
        const previewImg = document.getElementById('editComboImagePreview');

        if (combo.image && combo.image.trim() !== '') {
            imagePathInput.value = combo.image;
            const src = resolveImageSources(combo.image);
            previewImg.src = src.primary;
            previewImg.dataset.fallback = src.fallback || '';
            previewImg.onerror = function () {
                const fb = this.dataset.fallback || '';
                if (fb) {
                    this.dataset.fallback = '';
                    this.src = fb;
                    return;
                }
                this.onerror = null;
                this.outerHTML = '<div class="alert alert-danger p-2 m-0 mt-2 text-center"><i class="fa-solid fa-image-slash"></i> Image not found on server</div>';
            };
            previewContainer.classList.remove('d-none');
        } else {
            imagePathInput.value = '';
            previewContainer.classList.add('d-none');
        }
    } catch (error) {
        if (typeof showToast === 'function') showToast('Failed to load combo data!', 'error');
        setTimeout(() => { window.location.href = '/admin/combos'; }, 1500);
    }
}

document.getElementById('editComboForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const errorEle = document.getElementById('formValidationError');
    if (errorEle) errorEle.style.display = 'none';

    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;

    const showError = (msg) => {
        if (errorEle) {
            errorEle.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> ERROR: ${msg}`;
            errorEle.style.display = 'block';
        } else if (typeof showToast === 'function') {
            showToast(msg, 'error');
        }
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
    };

    const nameRegex = /^[\p{L}0-9\s]+$/u;
    const descRegex = /^[\p{L}0-9\s.,!?()\-\/&:+%'"]+$/u;

    const name = document.getElementById('editComboName').value.trim();
    if (!name) return showError('Combo name must not be empty!');
    if (!nameRegex.test(name)) return showError('Combo name can ONLY contain letters, numbers, and spaces!');

    const desc = document.getElementById('editComboDescription').value.trim();
    if (!desc) return showError('Combo description must not be empty!');
    if (!descRegex.test(desc)) return showError('Description contains invalid special characters!');

    const rawPrice = document.getElementById('editComboPrice').value;
    const priceNumber = parseInt(rawPrice.replace(/\D/g, ''), 10);
    if (isNaN(priceNumber) || priceNumber < 10000) return showError('Combo price must be at least 10,000 VND!');

    submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i> Updating...';
    submitBtn.disabled = true;

    try {
        let finalImagePath = document.getElementById('editComboImagePath').value;
        const imageFileInput = document.getElementById('editComboImageFile');
        const selectedFile = imageFileInput && imageFileInput.files ? imageFileInput.files[0] : null;

        if (selectedFile) finalImagePath = await uploadComboImage(selectedFile, name);

        const payload = { name: name, description: desc, price: priceNumber, image: finalImagePath };
        const token = sessionStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/combos/${comboId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            sessionStorage.setItem('successMessage', 'Combo updated successfully!');
            setTimeout(() => { window.location.href = '/admin/combos'; }, 800);
        } else {
            const data = await response.json();
            showError(data.message || 'Failed to update combo.');
        }
    } catch (error) {
        showError('Server connection error!');
    }
});

document.getElementById('btnDeleteCombo').addEventListener('click', async () => {
    if (!confirm('WARNING: Delete this combo? This action cannot be undone!')) return;
    const btnDelete = document.getElementById('btnDeleteCombo');
    btnDelete.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Deleting...';
    btnDelete.disabled = true;

    try {
        const token = sessionStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/combos/${comboId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (response.ok) {
            sessionStorage.setItem('successMessage', 'Combo deleted successfully!');
            setTimeout(() => { window.location.href = '/admin/combos'; }, 800);
        } else {
            if (typeof showToast === 'function') showToast('Delete failed!', 'error');
            btnDelete.innerHTML = '<i class="fa-solid fa-trash-can"></i> Delete';
            btnDelete.disabled = false;
        }
    } catch (error) {
        if (typeof showToast === 'function') showToast('Server error!', 'error');
    }
});

async function uploadComboImage(file, comboName) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('name', comboName || 'combo');

    const token = sessionStorage.getItem('jwtToken');
    const response = await fetch('/api/admin/combos/upload-image', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData
    });
    if (!response.ok) throw new Error('Image upload error');
    const text = await response.text();
    try {
        const data = JSON.parse(text);
        return data.imagePath || data.imageUrl || data.image || text;
    } catch (e) {
        return text;
    }
}

function previewSelectedImage() {
    const input = document.getElementById('editComboImageFile');
    const previewContainer = document.getElementById('editComboImagePreviewContainer');
    const previewImg = document.getElementById('editComboImagePreview');
    if (!input || !previewContainer || !previewImg) return;
    const file = input.files && input.files[0] ? input.files[0] : null;
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (e) => {
        previewImg.src = e.target.result;
        previewImg.onerror = null;
        previewContainer.classList.remove('d-none');
    };
    reader.readAsDataURL(file);
}

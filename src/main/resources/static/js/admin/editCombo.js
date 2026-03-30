const urlParams = new URLSearchParams(window.location.search);
const comboId = urlParams.get('id');

let currentImagePath = '';

document.addEventListener('DOMContentLoaded', async () => {
    if (!comboId) {
        alert('Invalid Combo ID!');
        window.location.href = '/admin/combos';
        return;
    }

    const fileInput = document.getElementById('editComboImageFile');
    if (fileInput) {
        fileInput.addEventListener('change', previewSelectedImage);
    }

    await loadComboData();
});

async function loadComboData() {
    try {
        const token = localStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/combos/${comboId}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) throw new Error('Combo not found');

        const combo = await response.json();
        document.getElementById('editComboName').value = combo.name || '';
        document.getElementById('editComboDescription').value = combo.description || '';
        document.getElementById('editComboPrice').value = combo.price || 0;

        currentImagePath = combo.image || '';
        document.getElementById('editComboImagePath').value = currentImagePath || '-';
        renderImagePreview(currentImagePath);
    } catch (error) {
        alert('Failed to load combo data!');
        window.location.href = '/admin/combos';
    }
}

document.getElementById('editComboForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Updating...';
    submitBtn.disabled = true;

    try {
        const imageFileInput = document.getElementById('editComboImageFile');
        const selectedFile = imageFileInput && imageFileInput.files ? imageFileInput.files[0] : null;
        let nextImagePath = currentImagePath;

        if (selectedFile) {
            nextImagePath = await uploadComboImage(selectedFile, document.getElementById('editComboName').value.trim());
        }

        const payload = {
            name: document.getElementById('editComboName').value.trim(),
            description: document.getElementById('editComboDescription').value.trim(),
            price: parseInt(document.getElementById('editComboPrice').value, 10) || 0,
            image: nextImagePath || ''
        };

        const token = localStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/combos/${comboId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (response.ok) {
            alert('Success: ' + data.message);
            window.location.href = '/admin/combos';
        } else {
            alert('Error: ' + (data.message || 'Failed to update combo.'));
        }
    } catch (error) {
        console.error('Error:', error);
        alert(error.message || 'Server connection error!');
    } finally {
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
    }
});

document.getElementById('btnDeleteCombo').addEventListener('click', async () => {
    if (!confirm('WARNING: Are you sure you want to delete this combo?')) return;

    try {
        const token = localStorage.getItem('jwtToken');
        const response = await fetch(`/api/admin/combos/${comboId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const data = await response.json();

        if (response.ok) {
            alert('Success: ' + data.message);
            window.location.href = '/admin/combos';
        } else {
            alert('Error: ' + (data.message || 'Failed to delete combo.'));
        }
    } catch (error) {
        alert('Server connection error!');
    }
});

async function uploadComboImage(file, comboName) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('name', comboName || 'combo');

    const token = localStorage.getItem('jwtToken');
    const uploadResponse = await fetch('/api/admin/combos/upload-image', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`
        },
        body: formData
    });

    const uploadData = await uploadResponse.json();
    if (!uploadResponse.ok) {
        throw new Error(uploadData.message || 'Image upload failed.');
    }

    currentImagePath = uploadData.imagePath || '';
    document.getElementById('editComboImagePath').value = currentImagePath || '-';
    renderImagePreview(currentImagePath);

    return currentImagePath;
}

function previewSelectedImage() {
    const input = document.getElementById('editComboImageFile');
    const file = input && input.files ? input.files[0] : null;

    if (!file) {
        renderImagePreview(currentImagePath);
        return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
        const preview = document.getElementById('editComboImagePreview');
        const container = document.getElementById('editComboImagePreviewContainer');
        if (!preview || !container) return;
        preview.src = event.target && event.target.result ? event.target.result : '';
        container.classList.remove('d-none');
    };
    reader.readAsDataURL(file);
}

function renderImagePreview(path) {
    const preview = document.getElementById('editComboImagePreview');
    const container = document.getElementById('editComboImagePreviewContainer');
    if (!preview || !container) return;

    if (!path) {
        preview.src = '';
        container.classList.add('d-none');
        return;
    }

    preview.src = path;
    container.classList.remove('d-none');
}

document.addEventListener('DOMContentLoaded', () => {
    const imageFileInput = document.getElementById('comboImageFile');
    if (imageFileInput) {
        imageFileInput.addEventListener('change', previewSelectedImage);
    }
});

document.getElementById('addComboForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Saving...';
    submitBtn.disabled = true;

    try {
        let imagePath = '';
        const imageFileInput = document.getElementById('comboImageFile');
        const selectedFile = imageFileInput && imageFileInput.files ? imageFileInput.files[0] : null;

        if (selectedFile) {
            imagePath = await uploadComboImage(selectedFile, document.getElementById('comboName').value.trim());
        }

        const payload = {
            name: document.getElementById('comboName').value.trim(),
            description: document.getElementById('comboDescription').value.trim(),
            price: parseInt(document.getElementById('comboPrice').value, 10) || 0,
            image: imagePath
        };

        const token = localStorage.getItem('jwtToken');
        const response = await fetch('/api/admin/combos', {
            method: 'POST',
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
            alert('Error: ' + (data.message || 'Failed to add combo.'));
        }
    } catch (error) {
        console.error('Error:', error);
        alert(error.message || 'Server connection error!');
    } finally {
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
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

    return uploadData.imagePath || '';
}

function previewSelectedImage() {
    const imageFileInput = document.getElementById('comboImageFile');
    const previewContainer = document.getElementById('comboImagePreviewContainer');
    const preview = document.getElementById('comboImagePreview');

    if (!imageFileInput || !previewContainer || !preview) return;

    const file = imageFileInput.files && imageFileInput.files[0] ? imageFileInput.files[0] : null;
    if (!file) {
        preview.src = '';
        previewContainer.classList.add('d-none');
        return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
        preview.src = event.target && event.target.result ? event.target.result : '';
        previewContainer.classList.remove('d-none');
    };
    reader.readAsDataURL(file);
}

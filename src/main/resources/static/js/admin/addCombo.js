document.addEventListener('DOMContentLoaded', () => {
    const priceInput = document.getElementById("comboPrice");
    if(priceInput) {
        priceInput.addEventListener("focus", (e) => { e.target.value = e.target.value.replace(/\D/g, ""); });
        priceInput.addEventListener("input", (e) => { e.target.value = e.target.value.replace(/\D/g, ""); });
        priceInput.addEventListener("blur", (e) => {
            let val = e.target.value.replace(/\D/g, "");
            if(val) e.target.value = Number(val).toLocaleString("vi-VN");
        });
    }
    const imageFileInput = document.getElementById('comboImageFile');
    if (imageFileInput) imageFileInput.addEventListener('change', previewSelectedImage);
});

document.getElementById('addComboForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    let errorEle = document.getElementById("formValidationError");
    if(errorEle) errorEle.style.display = "none";

    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;

    const showError = (msg) => {
        if (errorEle) {
            errorEle.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> ERROR: ${msg}`;
            errorEle.style.display = "block";
        } else if(typeof showToast === 'function') showToast(msg, "error");
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
    };

    // EXTREMELY STRICT FILTER (STRICT REGEX)
    const nameRegex = /^[\p{L}0-9\s]+$/u;
    const descRegex = /^[\p{L}0-9\s.,!?()\-\/&:+%'"]+$/u;

    const name = document.getElementById('comboName').value.trim();
    if (!name) return showError("Combo name must not be empty!");
    if (!nameRegex.test(name)) return showError("Combo name can ONLY contain letters, numbers, and spaces!");

    const desc = document.getElementById('comboDescription').value.trim();
    if (!desc) return showError("Combo description must not be empty!");
    if (!descRegex.test(desc)) return showError("Description contains invalid special characters!");

    let rawPrice = document.getElementById('comboPrice').value;
    let priceNumber = parseInt(rawPrice.replace(/\D/g, ""), 10);
    // VALIDATE MINIMUM PRICE (PREVENT 1 VND INPUT)
    if (isNaN(priceNumber) || priceNumber < 10000) {
        return showError("Combo price must be at least 10,000 VND!");
    }

    submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i> Saving...';
    submitBtn.disabled = true;

    try {
        let imagePath = '';
        const imageFileInput = document.getElementById('comboImageFile');
        const selectedFile = imageFileInput && imageFileInput.files ? imageFileInput.files[0] : null;

        if (selectedFile) imagePath = await uploadComboImage(selectedFile, name);

        const payload = { name: name, description: desc, price: priceNumber, image: imagePath };
        const token = sessionStorage.getItem('jwtToken');
        const response = await fetch('/api/admin/combos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            sessionStorage.setItem("successMessage", "Combo added successfully!");
            setTimeout(() => { window.location.href = '/admin/combos'; }, 800);
        } else {
            const data = await response.json();
            showError(data.message || 'Failed to add combo.');
        }
    } catch (error) { showError('Server connection error!'); }
});

async function uploadComboImage(file, comboName) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('name', comboName || 'combo');

    const token = sessionStorage.getItem('jwtToken');
    const uploadResponse = await fetch('/api/admin/combos/upload-image', {
        method: 'POST', headers: { 'Authorization': `Bearer ${token}` }, body: formData
    });

    if (!uploadResponse.ok) throw new Error('Error uploading image to server.');
    const text = await uploadResponse.text();
    try { const data = JSON.parse(text); return data.imagePath || data.imageUrl || data.image || text; }
    catch (e) { return text; }
}

function previewSelectedImage() {
    const input = document.getElementById('comboImageFile');
    const previewContainer = document.getElementById('comboImagePreviewContainer');
    const preview = document.getElementById('comboImagePreview');
    if (!input || !previewContainer || !preview) return;
    const file = input.files && input.files[0] ? input.files[0] : null;
    if (!file) { preview.src = ''; previewContainer.classList.add('d-none'); return; }
    const reader = new FileReader();
    reader.onload = (e) => { preview.src = e.target.result; previewContainer.classList.remove('d-none'); };
    reader.readAsDataURL(file);
}

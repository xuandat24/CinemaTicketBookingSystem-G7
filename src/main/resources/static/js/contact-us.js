/* =========================================
   XỬ LÝ GỬI API FEEDBACK TỪ KHÁCH HÀNG
========================================= */
document.addEventListener('DOMContentLoaded', function () {
    const contactForm = document.getElementById('contactForm');

    if (contactForm) {
        contactForm.addEventListener('submit', async function(e) {
            e.preventDefault(); // Ngăn trình duyệt tải lại trang

            const btnSubmit = document.getElementById('btnSubmit');
            const name = document.getElementById('fbName').value.trim();
            const email = document.getElementById('fbEmail').value.trim();
            const message = document.getElementById('fbMessage').value.trim();

            // Hiển thị trạng thái đang gửi
            btnSubmit.innerHTML = '<i class="fa fa-spinner fa-spin me-2"></i> Sending...';
            btnSubmit.disabled = true;

            try {
                // Gọi API gửi Feedback
                const response = await fetch('/api/public/feedbacks', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ name, email, message })
                });

                if (response.ok) {
                    // Ẩn form và hiện thông báo thành công
                    document.getElementById('contactForm').classList.add('d-none');
                    document.getElementById('successAlert').classList.remove('d-none');
                } else {
                    alert("An error occurred on the server, please try again later!");
                    btnSubmit.innerHTML = '<i class="fa fa-paper-plane me-2"></i> Send Message';
                    btnSubmit.disabled = false;
                }
            } catch (error) {
                alert("Could not connect to the server! Please check your network.");
                btnSubmit.innerHTML = '<i class="fa fa-paper-plane me-2"></i> Send Message';
                btnSubmit.disabled = false;
            }
        });
    }
});
let topMoviesChartInstance = null;
let combosChartInstance = null;
let peakHoursChartInstance = null;
let typingTimer;

document.getElementById('occupancyMovieSearch').addEventListener('input', () => {
    clearTimeout(typingTimer);
    typingTimer = setTimeout(loadOccupancy, 500); // Đợi 500ms sau khi ngừng gõ mới tải dữ liệu
});

document.addEventListener("DOMContentLoaded", () => {
    updateDashboard();
    loadOccupancy();
});

async function updateDashboard() {
    const timeFilter = document.querySelector('input[name="timeFilter"]:checked').value;

    try {
        const response = await fetch(`/api/admin/dashboard?filter=${timeFilter}`);
        if (!response.ok) throw new Error("Error loading dashboard data");

        const dashboardData = await response.json();
        renderDashboard(dashboardData);

    } catch (error) {
        console.error("Dashboard error:", error);
    }
}

function renderDashboard(data) {
    // 1. Update KPI Text Cards
    document.getElementById('totalRevenue').innerText = data.totalRevenue || "0 VND";
    document.getElementById('totalTickets').innerText = data.totalTickets || 0;
    document.getElementById('topMovieName').innerText = data.topMovieName || "No data available";
    document.getElementById('totalCombos').innerText = data.totalCombos || 0;

    // 2. Render Top Movies Chart (Bar Chart)
    const ctxTopMovies = document.getElementById('topMoviesChart').getContext('2d');
    if (topMoviesChartInstance) topMoviesChartInstance.destroy();
    topMoviesChartInstance = new Chart(ctxTopMovies, {
        type: 'bar',
        data: {
            labels: data.topMovies.labels,
            datasets: [{
                label: 'Revenue (million VND)',
                data: data.topMovies.data,
                backgroundColor: 'rgba(54, 162, 235, 0.6)',
                borderColor: 'rgba(54, 162, 235, 1)',
                borderWidth: 1,
                borderRadius: 5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false, // BẮT BUỘC PHẢI CÓ ĐỂ CHỐNG PHÌNH TO
            scales: { y: { beginAtZero: true } }
        }
    });

    // 3. Render Combos Chart (Doughnut Chart)
    const ctxCombos = document.getElementById('combosChart').getContext('2d');
    if (combosChartInstance) combosChartInstance.destroy();
    combosChartInstance = new Chart(ctxCombos, {
        type: 'doughnut',
        data: {
            labels: data.combos.labels,
            datasets: [{
                data: data.combos.data,
                backgroundColor: ['#ffcd56', '#ff6384', '#36a2eb', '#4bc0c0', '#9966ff'],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false // BẮT BUỘC PHẢI CÓ ĐỂ CHỐNG PHÌNH TO
        }
    });

    // 4. Render Peak Hours Chart (Line Chart)
    const ctxPeak = document.getElementById('peakHoursChart').getContext('2d');
    if (peakHoursChartInstance) peakHoursChartInstance.destroy();
    peakHoursChartInstance = new Chart(ctxPeak, {
        type: 'line',
        data: {
            labels: data.peakHours.labels,
            datasets: [{
                label: 'Tickets Booked',
                data: data.peakHours.data,
                fill: true,
                backgroundColor: 'rgba(75, 192, 192, 0.2)',
                borderColor: 'rgba(75, 192, 192, 1)',
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false, // BẮT BUỘC PHẢI CÓ ĐỂ CHỐNG PHÌNH TO
            scales: { y: { beginAtZero: true, suggestedMax: 5 } }
        }
    });
}

// Lắng nghe sự kiện khi người dùng bấm Tab Ngày/Tuần/Tháng để load lại bảng này
document.querySelectorAll('input[name="timeFilter"]').forEach(radio => {
    radio.addEventListener('change', () => {
        loadOccupancy(); // Load lại bảng lấp đầy
    });
});

// Lấy dữ liệu và vẽ bảng Tỉ lệ lấp đầy
async function loadOccupancy() {
    const timeFilter = document.querySelector('input[name="timeFilter"]:checked').value;
    const keyword = document.getElementById('occupancyMovieSearch').value;

    // Nối URL
    let url = `/api/admin/dashboard/occupancy?filter=${timeFilter}`;
    if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;

    try {
        const res = await fetch(url);
        const data = await res.json();
        const tbody = document.getElementById('occupancyTableBody');
        tbody.innerHTML = '';

        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No showtimes found</td></tr>';
            return;
        }

        data.forEach(item => {
        let colorClass = '';

        if (item.occupancyRate >= 80) {
            // >= 80% : Màu xanh
            colorClass = 'occupancy-high';
        } else if (item.occupancyRate >= 60) {
            // >= 60% và < 80% : Màu vàng
            colorClass = 'occupancy-medium';
        } else {
            // < 60% : Màu đỏ
            colorClass = 'occupancy-low';
        }

        tbody.innerHTML += `
            <tr>
                <td class="fw-bold text-dark">${item.movieName}</td>
                <td><span class="badge bg-light text-dark border"><i class="fa-regular fa-clock me-1"></i>${item.time}</span></td>
                <td>${item.roomName}</td>
                <td class="text-center fw-bold">${item.bookedSeats} / ${item.totalSeats}</td>
                <td>
                    <div class="d-flex align-items-center">
                        <span class="me-2 fw-bold" style="width: 45px;">${item.occupancyRate}%</span>
                        <div class="progress flex-grow-1" style="height: 10px;">
                            <div class="progress-bar ${colorClass}" role="progressbar" style="width: ${item.occupancyRate}%;"></div>
                        </div>
                    </div>
                </td>
            </tr>
        `;
    });
    } catch (e) {
        console.error("Error loading occupancy data", e);
    }
}
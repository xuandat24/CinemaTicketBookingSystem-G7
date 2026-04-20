// TRICK 1: HIDE THE PAGE IMMEDIATELY TO PREVENT PEEKING
document.documentElement.style.opacity = '0';
document.documentElement.style.pointerEvents = 'none';

document.addEventListener("DOMContentLoaded", async function () {
    localStorage.removeItem("jwtToken");
    localStorage.removeItem("username");

    const token = sessionStorage.getItem("jwtToken");

    if (!token || token === "null" || token === "undefined") {
        fetch("/api/auth/logout", { method: "POST" }).catch(()=>{});
        window.location.href = "/login";
        return;
    }

    try {
        const response = await fetch("/api/users/my-profile", {
            method: "GET",
            headers: { "Authorization": "Bearer " + token, "Cache-Control": "no-cache" }
        });

        if (response.ok) {
            const userProfile = await response.json();
            if (userProfile.roleId !== 1) {
                alert("Access Denied! This area is for Admins only.");
                window.location.href = "/";
            } else {
                // TRICK 2: IF ADMIN -> SHOW THE PAGE SMOOTHLY
                document.documentElement.style.transition = 'opacity 0.3s ease';
                document.documentElement.style.opacity = '1';
                document.documentElement.style.pointerEvents = 'auto';

                // ĐÃ THÊM: TỰ ĐỘNG CẬP NHẬT ẢNH VÀ THÔNG TIN PROFILE Ở GÓC DƯỚI SIDEBAR
                const adminNameEl = document.getElementById("admin-sidebar-name");
                const adminEmailEl = document.getElementById("admin-sidebar-email");
                const adminAvatarEl = document.getElementById("admin-sidebar-avatar");

                if (adminNameEl) {
                    let fName = userProfile.firstName || "";
                    let lName = userProfile.lastName || "";
                    let fullName = `${fName} ${lName}`.trim();
                    if(!fullName) fullName = userProfile.userName || "Admin";
                    adminNameEl.innerText = fullName;
                }
                if (adminEmailEl) {
                    adminEmailEl.innerText = userProfile.email || "admin@ctbs.com";
                }
                if (adminAvatarEl) {
                    if (userProfile.avatar && userProfile.avatar !== "null") {
                        adminAvatarEl.src = userProfile.avatar;
                    } else {
                        let fullName = adminNameEl ? adminNameEl.innerText : "Admin";
                        adminAvatarEl.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(fullName)}&background=d96c2c&color=fff`;
                    }
                }
            }
        } else {
            sessionStorage.clear();
            window.location.href = "/login";
        }
    } catch (error) {
        sessionStorage.clear();
        window.location.href = "/login";
    }

    // ======================================================
    // LOGOUT EVENT SPECIFIC TO ADMIN (FORCE REDIRECT)
    // ======================================================
    const adminLogoutBtn = document.getElementById("adminLogoutBtn");
    if (adminLogoutBtn) {
        adminLogoutBtn.addEventListener("click", function(e) {
            e.preventDefault();
            if (confirm("Confirm logout from the admin system?")) {
                const t = sessionStorage.getItem("jwtToken");
                sessionStorage.clear();
                localStorage.clear();

                if (t && t !== "null") {
                    fetch("/api/auth/logout", {
                        method: "POST",
                        headers: { "Authorization": "Bearer " + t }
                    }).finally(() => {
                        window.location.href = "/login"; // Force Redirect
                    });
                } else {
                    window.location.href = "/login";
                }
            }
        });
    }
});
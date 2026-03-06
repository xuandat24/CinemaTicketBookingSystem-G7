document.getElementById("loginForm").addEventListener("submit", async function(e){

    e.preventDefault()

    const email = document.getElementById("email").value
    const password = document.getElementById("password").value

    const response = await fetch("/api/auth/login",{

        method:"POST",

        headers:{
            "Content-Type":"application/json"
        },

        body:JSON.stringify({
            email:email,
            password:password
        })

    })

    if(!response.ok){

        document.getElementById("errorMsg").innerText="Login failed"

        return
    }

    const data = await response.json()

    localStorage.setItem("token",data.token)

    window.location.href="/"

})

function logout(){

    localStorage.removeItem("token")

    window.location.href="/login"

}
function checkLogin(){

    const token = localStorage.getItem("token")

    if(!token){

        window.location.href="/login"

    }

}
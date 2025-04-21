document.addEventListener("DOMContentLoaded", function () {
    console.log("Document is ready!");

    const loginForm = document.querySelector("form");
    if (loginForm) {
        loginForm.addEventListener("submit", function (event) {
            const username = document.getElementById("username").value;
            const password = document.getElementById("password").value;

            if (!username || !password) {
                event.preventDefault();
                alert("Please fill in all fields");
            }
        });
    }
});

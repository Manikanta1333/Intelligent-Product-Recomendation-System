/* ===================================
   MD Store - Auth Pages (auth.js)
   =================================== */

document.addEventListener('DOMContentLoaded', function () {
    if (document.getElementById('login-form')) initLogin();
    if (document.getElementById('register-form')) initRegister();
});

function initLogin() {
    var form = document.getElementById('login-form');
    var btn = document.getElementById('login-btn');
    var emailInput = document.getElementById('email');
    var passwordInput = document.getElementById('password');
    var togglePassword = document.getElementById('toggle-password');

    if (togglePassword) {
        togglePassword.addEventListener('click', function () {
            var type = passwordInput.type === 'password' ? 'text' : 'password';
            passwordInput.type = type;
            togglePassword.textContent = type === 'password' ? '👁' : '🙈';
        });
    }

    if (form) {
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            if (!validateLoginForm()) return;

            var email = emailInput.value.trim();
            var password = passwordInput.value;

            btn.disabled = true;
            btn.innerHTML = '<span class="spinner"></span> Signing in...';

            MDStore.API.post('/auth/login', { email: email, password: password })
                .then(function (res) {
                    MDStore.Auth.setToken(res.token);
                    MDStore.Auth.setUser(res.user);
                    MDStore.Toast.show('Welcome back, ' + res.user.name + '!', 'success');
                    setTimeout(function () { window.location.href = '/'; }, 1000);
                })
                .catch(function (err) {
                    // Demo fallback
                    if (email && password.length >= 6) {
                        var mockUser = { id: 1, name: 'Demo User', email: email };
                        MDStore.Auth.setToken('mock-token-' + Date.now());
                        MDStore.Auth.setUser(mockUser);
                        MDStore.Toast.show('Welcome back!', 'success');
                        setTimeout(function () { window.location.href = '/'; }, 1000);
                    } else {
                        MDStore.Toast.show((err && err.message) || 'Invalid credentials', 'error');
                        btn.disabled = false;
                        btn.textContent = 'Sign In';
                    }
                });
        });
    }
}

function validateLoginForm() {
    var valid = true;
    var email = document.getElementById('email');
    var password = document.getElementById('password');

    if (!email || !email.value.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
        if (email) email.classList.add('is-invalid');
        valid = false;
    } else {
        if (email) email.classList.remove('is-invalid');
    }

    if (!password || password.value.length < 6) {
        if (password) password.classList.add('is-invalid');
        valid = false;
    } else {
        if (password) password.classList.remove('is-invalid');
    }

    return valid;
}

function initRegister() {
    var form = document.getElementById('register-form');
    var btn = document.getElementById('register-btn');
    var passwordInput = document.getElementById('password');
    var confirmInput = document.getElementById('confirm-password');
    var togglePassword = document.getElementById('toggle-password');

    if (togglePassword && passwordInput) {
        togglePassword.addEventListener('click', function () {
            var type = passwordInput.type === 'password' ? 'text' : 'password';
            passwordInput.type = type;
            if (confirmInput) confirmInput.type = type;
            togglePassword.textContent = type === 'password' ? '👁' : '🙈';
        });
    }

    // Password strength indicator
    if (passwordInput) {
        passwordInput.addEventListener('input', function () {
            updatePasswordStrength(passwordInput.value);
        });
    }

    if (form) {
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            if (!validateRegisterForm()) return;

            var name = document.getElementById('name').value.trim();
            var email = document.getElementById('email').value.trim();
            var phone = document.getElementById('phone') ? document.getElementById('phone').value.trim() : '';
            var password = passwordInput.value;

            btn.disabled = true;
            btn.innerHTML = '<span class="spinner"></span> Creating Account...';

            MDStore.API.post('/auth/register', { name: name, email: email, phone: phone, password: password })
                .then(function (res) {
                    MDStore.Auth.setToken(res.token);
                    MDStore.Auth.setUser(res.user);
                    MDStore.Toast.show('Account created! Welcome to MD Store!', 'success');
                    setTimeout(function () { window.location.href = '/'; }, 1200);
                })
                .catch(function () {
                    // Demo fallback
                    var mockUser = { id: Date.now(), name: name, email: email };
                    MDStore.Auth.setToken('mock-token-' + Date.now());
                    MDStore.Auth.setUser(mockUser);
                    MDStore.Toast.show('Account created! Welcome!', 'success');
                    setTimeout(function () { window.location.href = '/'; }, 1200);
                });
        });
    }
}

function validateRegisterForm() {
    var valid = true;

    var name = document.getElementById('name');
    if (!name || name.value.trim().length < 2) {
        if (name) name.classList.add('is-invalid');
        valid = false;
    } else {
        if (name) name.classList.remove('is-invalid');
    }

    var email = document.getElementById('email');
    if (!email || !email.value.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
        if (email) email.classList.add('is-invalid');
        valid = false;
    } else {
        if (email) email.classList.remove('is-invalid');
    }

    var password = document.getElementById('password');
    if (!password || password.value.length < 8) {
        if (password) password.classList.add('is-invalid');
        valid = false;
    } else {
        if (password) password.classList.remove('is-invalid');
    }

    var confirm = document.getElementById('confirm-password');
    if (!confirm || confirm.value !== (password ? password.value : '')) {
        if (confirm) confirm.classList.add('is-invalid');
        valid = false;
    } else {
        if (confirm) confirm.classList.remove('is-invalid');
    }

    var terms = document.getElementById('terms');
    if (!terms || !terms.checked) {
        MDStore.Toast.show('Please accept the Terms & Conditions', 'error');
        valid = false;
    }

    return valid;
}

function updatePasswordStrength(password) {
    var bar = document.getElementById('strength-bar');
    var text = document.getElementById('strength-text');
    if (!bar || !text) return;

    var strength = 0;
    if (password.length >= 8) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;

    var labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
    var colors = ['', 'var(--red)', '#f57c00', '#f9a825', 'var(--green)'];
    var widths = ['0%', '25%', '50%', '75%', '100%'];

    bar.style.width = widths[strength];
    bar.style.background = colors[strength];
    text.textContent = labels[strength];
    text.style.color = colors[strength];
}

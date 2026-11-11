/* ===================================
   MD Store - API Utility (api.js)
   =================================== */

var MDStore = MDStore || {};

MDStore.API = (function () {
    var BASE_URL = '/api';

    function request(method, endpoint, body) {
        var options = {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        };
        var token = MDStore.Auth.getToken();
        if (token) {
            options.headers['Authorization'] = 'Bearer ' + token;
        }
        if (body) {
            options.body = JSON.stringify(body);
        }
        return fetch(BASE_URL + endpoint, options)
            .then(function (res) {
                if (!res.ok) {
                    return res.json().then(function (err) { throw err; });
                }
                return res.json();
            });
    }

    return {
        get:    function (endpoint)       { return request('GET',    endpoint); },
        post:   function (endpoint, body) { return request('POST',   endpoint, body); },
        put:    function (endpoint, body) { return request('PUT',    endpoint, body); },
        delete: function (endpoint)       { return request('DELETE', endpoint); }
    };
})();

/* ---- Auth Helpers ---- */
MDStore.Auth = (function () {
    return {
        getToken:    function () { return localStorage.getItem('md_token'); },
        setToken:    function (t) { localStorage.setItem('md_token', t); },
        removeToken: function () { localStorage.removeItem('md_token'); },
        getUser:     function () { var u = localStorage.getItem('md_user'); return u ? JSON.parse(u) : null; },
        setUser:     function (u) { localStorage.setItem('md_user', JSON.stringify(u)); },
        removeUser:  function () { localStorage.removeItem('md_user'); },
        isLoggedIn:  function () { return !!this.getToken(); },
        logout:      function () {
            this.removeToken();
            this.removeUser();
            MDStore.Cart.clear();
            window.location.href = '/login';
        }
    };
})();

/* ---- Per-user key helper ---- */
// window.currentUserId is set by Thymeleaf before this script loads.
// Returns a storage key scoped to the logged-in user, e.g. "md_cart_42".
// Guests get a generic key that we never display or persist between logins.
function _userKey(base) {
    var uid = (window.currentUserId != null && window.currentUserId !== '') ? window.currentUserId : 'guest';
    return base + '_' + uid;
}

function _mergeGuestCartIntoUserCart(cart, base) {
    var uid = (window.currentUserId != null && window.currentUserId !== '') ? window.currentUserId : null;
    if (uid == null) return cart;

    var guestKey = base + '_guest';
    var guestCart = localStorage.getItem(guestKey);
    if (!guestCart) return cart;

    JSON.parse(guestCart).forEach(function (guestItem) {
        var existing = cart.find(function (item) { return item.id === guestItem.id; });
        if (existing) existing.quantity += guestItem.quantity;
        else cart.push(guestItem);
    });

    localStorage.setItem(base + '_' + uid, JSON.stringify(cart));
    localStorage.removeItem(guestKey);
    return cart;
}

/* ---- Cart Helpers ---- */
MDStore.Cart = (function () {
    var BASE = 'md_cart';

    function key()     { return _userKey(BASE); }
    function getCart() {
        var c = localStorage.getItem(key());
        return _mergeGuestCartIntoUserCart(c ? JSON.parse(c) : [], BASE);
    }
    function saveCart(cart) { localStorage.setItem(key(), JSON.stringify(cart)); updateBadge(); }

    function updateBadge() {
        if (!window.isLoggedIn) {
            document.querySelectorAll('.cart-badge').forEach(function (b) { b.style.display = 'none'; });
            return;
        }
        var total = getCart().reduce(function (s, i) { return s + i.quantity; }, 0);
        document.querySelectorAll('.cart-badge').forEach(function (b) {
            b.textContent = total;
            b.style.display = total > 0 ? 'flex' : 'none';
        });
    }

    return {
        get: getCart,
        add: function (product, qty) {
            var cart = getCart();
            qty = qty || 1;
            var existing = cart.find(function (i) { return i.id === product.id; });
            if (existing) { existing.quantity += qty; }
            else { cart.push({ id: product.id, name: product.name, brand: product.brand, price: product.price, image: product.image, quantity: qty }); }
            saveCart(cart);
            MDStore.Toast.show('Added to cart!', 'success');
        },
        remove:    function (id)      { saveCart(getCart().filter(function (i) { return i.id !== id; })); },
        updateQty: function (id, qty) {
            var cart = getCart();
            var item = cart.find(function (i) { return i.id === id; });
            if (item) { item.quantity = Math.max(1, qty); }
            saveCart(cart);
        },
        total: function () { return getCart().reduce(function (s, i) { return s + i.price * i.quantity; }, 0); },
        count: function () { return getCart().reduce(function (s, i) { return s + i.quantity; }, 0); },
        clear: function () { localStorage.removeItem(key()); updateBadge(); },
        init:  function () { updateBadge(); }
    };
})();

/* ---- Wishlist Helpers ---- */
MDStore.Wishlist = (function () {
    var BASE = 'md_wishlist';

    function key()     { return _userKey(BASE); }
    function getList() { var w = localStorage.getItem(key()); return w ? JSON.parse(w) : []; }
    function saveList(list) { localStorage.setItem(key(), JSON.stringify(list)); updateBadge(); }

    function updateBadge() {
        if (!window.isLoggedIn) {
            document.querySelectorAll('.wishlist-badge').forEach(function (b) { b.style.display = 'none'; });
            return;
        }
        var len = getList().length;
        document.querySelectorAll('.wishlist-badge').forEach(function (b) {
            b.textContent = len;
            b.style.display = len > 0 ? 'flex' : 'none';
        });
    }

    return {
        get: getList,
        toggle: function (product) {
            var list = getList();
            var idx  = list.findIndex(function (i) { return i.id === product.id; });
            if (idx > -1) { list.splice(idx, 1); MDStore.Toast.show('Removed from wishlist'); }
            else          { list.push(product);  MDStore.Toast.show('Added to wishlist!', 'success'); }
            saveList(list);
            return idx === -1;
        },
        has:    function (id) { return getList().some(function (i) { return i.id === id; }); },
        remove: function (id) { saveList(getList().filter(function (i) { return i.id !== id; })); },
        clear:  function ()   { localStorage.removeItem(key()); updateBadge(); },
        init:   function ()   { updateBadge(); }
    };
})();

/* ---- Toast Notifications ---- */
MDStore.Toast = (function () {
    var container;
    function init() {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    return {
        show: function (message, type) {
            if (!container) init();
            var toast = document.createElement('div');
            toast.className = 'toast' + (type ? ' ' + type : '');
            toast.textContent = message;
            container.appendChild(toast);
            requestAnimationFrame(function () {
                requestAnimationFrame(function () { toast.classList.add('show'); });
            });
            setTimeout(function () {
                toast.classList.remove('show');
                setTimeout(function () { toast.remove(); }, 400);
            }, 3000);
        }
    };
})();

/* ---- URL Params ---- */
MDStore.Params = {
    get: function (key) { return new URLSearchParams(window.location.search).get(key); }
};

/* ---- Format Helpers ---- */
MDStore.Format = {
    price: function (p) { return '₹' + Number(p).toLocaleString('en-IN', { maximumFractionDigits: 2 }); },
    date:  function (d) { return new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }); }
};

/* ---- Stars ---- */
MDStore.Stars = function (rating) {
    var full  = Math.floor(rating);
    var half  = rating % 1 >= 0.5 ? 1 : 0;
    var empty = 5 - full - half;
    return '★'.repeat(full) + (half ? '½' : '') + '☆'.repeat(empty);
};

/* ---- Skeleton Loader ---- */
MDStore.Skeleton = {
    productCard: function () {
        return '<div class="product-card" style="pointer-events:none"><div class="product-img-wrap skeleton" style="aspect-ratio:1"></div><div class="product-info"><div class="skeleton" style="height:12px;width:60%;margin-bottom:8px"></div><div class="skeleton" style="height:16px;margin-bottom:8px"></div><div class="skeleton" style="height:12px;width:80%;margin-bottom:12px"></div><div class="skeleton" style="height:36px"></div></div></div>';
    },
    grid: function (n) {
        var html = '';
        for (var i = 0; i < (n || 8); i++) { html += MDStore.Skeleton.productCard(); }
        return html;
    }
};

/* ---- Init on DOMContentLoaded ---- */
document.addEventListener('DOMContentLoaded', function () {
    MDStore.Cart.init();
    MDStore.Wishlist.init();
    updateHeaderUser();
});

function updateHeaderUser() {
    var user = MDStore.Auth.getUser();
    var signInLinks = document.querySelectorAll('.sign-in-link');
    signInLinks.forEach(function (el) {
        if (user) {
            el.innerHTML = '<span style="font-size:0.72rem;font-weight:600">' + user.name.split(' ')[0] + '</span>';
            el.href = '/profile';
        }
    });
}

/* ---- Auth Guard — redirect guests to /login on cart/wishlist actions ---- */
document.addEventListener('click', function (e) {
    if (window.isLoggedIn) return;
    var btn = e.target.closest('.add-to-cart-btn, .wishlist-btn, [data-action="add-to-cart"], [data-action="add-to-wishlist"]');
    if (!btn) return;
    e.preventDefault();
    e.stopImmediatePropagation();
    window.location.href = '/login';
}, true);

/* ---- Logout interceptor — clear this user's localStorage before server logout ---- */
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('a[href="/logout"]').forEach(function (link) {
        link.addEventListener('click', function (e) {
            // Clear current user's cart and wishlist from localStorage before logout
            MDStore.Cart.clear();
            MDStore.Wishlist.clear();
            // Navigation to /logout proceeds normally (server invalidates session)
        });
    });
});
/* ===================================
   MD Store - Home Page (home.js)
   =================================== */

document.addEventListener('DOMContentLoaded', function () {
    loadFeaturedProducts();
    loadCategories();
    initHeroAnimations();
});

/* ---- Mock Data (replace with fetch when backend ready) ---- */
var MOCK_PRODUCTS = [
    { id: 1, name: 'Premium Wireless Headphones', brand: 'SoundMax', price: 4999, oldPrice: 8999, image: '', category: 'Electronics', rating: 4.5, reviews: 1240, badge: 'hot' },
    { id: 2, name: 'Smart Fitness Tracker Band', brand: 'FitPro', price: 2499, oldPrice: 3999, image: '', category: 'Electronics', rating: 4.2, reviews: 870, badge: 'new' },
    { id: 3, name: 'Organic Cotton Kurta Set', brand: 'EthnicWear', price: 1299, oldPrice: 2299, image: '', category: 'Fashion', rating: 4.6, reviews: 543, badge: '' },
    { id: 4, name: 'Baby Soft Plush Toy Bear', brand: 'TinyJoy', price: 799, oldPrice: 1299, image: '', category: 'Baby Products', rating: 4.8, reviews: 328, badge: 'new' },
    { id: 5, name: 'Yoga Mat Premium Anti-Slip', brand: 'FlexFit', price: 1599, oldPrice: 2499, image: '', category: 'Sports', rating: 4.4, reviews: 692, badge: '' },
    { id: 6, name: 'Vitamin C Face Serum 30ml', brand: 'GlowUp', price: 899, oldPrice: 1499, image: '', category: 'Beauty', rating: 4.7, reviews: 1890, badge: 'hot' },
    { id: 7, name: 'Stainless Steel Water Bottle 1L', brand: 'HydroKing', price: 599, oldPrice: 999, image: '', category: 'Sports', rating: 4.3, reviews: 2100, badge: '' },
    { id: 8, name: 'Wireless Mechanical Keyboard', brand: 'KeyPro', price: 5499, oldPrice: 8499, image: '', category: 'Electronics', rating: 4.6, reviews: 410, badge: 'hot' }
];

var MOCK_CATEGORIES = [
    { id: 'electronics', name: 'Electronics', icon: '📱', count: 234 },
    { id: 'fashion', name: 'Fashion', icon: '👗', count: 512 },
    { id: 'baby', name: 'Baby Products', icon: '🍼', count: 183 },
    { id: 'sports', name: 'Sports', icon: '⚽', count: 297 },
    { id: 'beauty', name: 'Beauty', icon: '💄', count: 341 },
    { id: 'home', name: 'Home & Kitchen', icon: '🏠', count: 428 }
];

var PRODUCT_ICONS = ['📱', '⌚', '👗', '🍼', '⚽', '💄', '🎧', '⌨️'];

function loadFeaturedProducts() {
    var container = document.getElementById('featured-products');
    if (!container) return;

    container.innerHTML = MDStore.Skeleton.grid(8);

    setTimeout(function () {
        MDStore.API.get('/products?featured=true')
            .then(function (data) { renderProducts(container, data); })
            .catch(function () { renderProducts(container, MOCK_PRODUCTS); });
    }, 500);
}

function loadCategories() {
    var container = document.getElementById('categories-container');
    if (!container) return;

    MDStore.API.get('/categories')
        .then(function (data) { renderCategories(container, data); })
        .catch(function () { renderCategories(container, MOCK_CATEGORIES); });
}

function renderProducts(container, products) {
    if (!products || products.length === 0) {
        container.innerHTML = '<div class="empty-state"><div class="empty-icon">📦</div><h3>No Products Found</h3></div>';
        return;
    }
    var html = products.map(function (p, idx) { return buildProductCard(p, idx); }).join('');
    container.innerHTML = html;
    bindProductActions(container);
}

function buildProductCard(p, idx) {
    var discount = p.oldPrice ? Math.round((1 - p.price / p.oldPrice) * 100) : 0;
    var isWishlisted = MDStore.Wishlist.has(p.id);
    var imgContent = p.image
        ? '<img src="' + p.image + '" alt="' + p.name + '" loading="lazy">'
        : '<div class="product-img-placeholder">' + (PRODUCT_ICONS[idx % PRODUCT_ICONS.length]) + '</div>';

    return '<div class="product-card" data-id="' + p.id + '">' +
        (p.badge ? '<span class="product-badge ' + p.badge + '">' + p.badge.toUpperCase() + '</span>' : '') +
        '<div class="product-img-wrap">' +
        imgContent +
        '<button class="wishlist-btn ' + (isWishlisted ? 'active' : '') + '" data-product-id="' + p.id + '" title="Wishlist">♥</button>' +
        '</div>' +
        '<div class="product-info">' +
        '<div class="product-brand">' + p.brand + '</div>' +
        '<div class="product-name">' + p.name + '</div>' +
        '<div class="product-rating"><span class="stars">' + MDStore.Stars(p.rating) + '</span><span class="rating-count">(' + (p.reviews || 0) + ')</span></div>' +
        '<div class="product-price-row">' +
        '<span class="product-price">' + MDStore.Format.price(p.price) + '</span>' +
        (p.oldPrice ? '<span class="product-price-old">' + MDStore.Format.price(p.oldPrice) + '</span>' : '') +
        (discount > 0 ? '<span class="product-discount">' + discount + '% off</span>' : '') +
        '</div>' +
        '<div class="product-actions">' +
        '<button class="btn btn-primary btn-sm add-to-cart-btn" data-product=\'' + JSON.stringify({ id: p.id, name: p.name, brand: p.brand, price: p.price, image: p.image || '' }) + '\'>🛒 Add</button>' +
        '<a href="/product-detail?id=' + p.id + '" class="btn btn-outline btn-sm">View</a>' +
        '</div>' +
        '</div>' +
        '</div>';
}

function bindProductActions(container) {
    container.querySelectorAll('.add-to-cart-btn').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            var product = JSON.parse(btn.getAttribute('data-product'));
            MDStore.Cart.add(product, 1);
            btn.textContent = '✓ Added';
            btn.style.background = 'var(--dark-yellow)';
            setTimeout(function () {
                btn.innerHTML = '🛒 Add';
                btn.style.background = '';
            }, 1500);
        });
    });

    container.querySelectorAll('.wishlist-btn').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            var cardEl = btn.closest('.product-card');
            var id = parseInt(btn.getAttribute('data-product-id'));
            var product = MOCK_PRODUCTS.find(function (p) { return p.id === id; }) || { id: id };
            var added = MDStore.Wishlist.toggle(product);
            btn.classList.toggle('active', added);
        });
    });
}

function renderCategories(container, cats) {
    var html = cats.map(function (c) {
        return '<a href="/products?category=' + c.id + '" class="category-card">' +
            '<span class="cat-icon">' + c.icon + '</span>' +
            '<div class="cat-name">' + c.name + '</div>' +
            '</a>';
    }).join('');
    container.innerHTML = html;
}

function initHeroAnimations() {
    var hero = document.querySelector('.hero-section');
    if (!hero) return;
    var elements = hero.querySelectorAll('.hero-eyebrow, .hero-title, .hero-desc, .hero-btns, .hero-img-card');
    elements.forEach(function (el, i) {
        el.style.opacity = '0';
        el.style.transform = 'translateY(24px)';
        el.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
        el.style.transitionDelay = (i * 0.1) + 's';
        setTimeout(function () {
            el.style.opacity = '1';
            el.style.transform = 'translateY(0)';
        }, 80);
    });
}

/* ===================================
   MD Store - Products Page (products.js)
   =================================== */

document.addEventListener('DOMContentLoaded', function () {
    loadProducts();
    bindFilters();
});

var allProducts = [];
var currentPage = 1;
var perPage = 12;

var MOCK_ALL_PRODUCTS = [
    { id: 1, name: 'Premium Wireless Headphones', brand: 'SoundMax', price: 4999, oldPrice: 8999, image: '', category: 'electronics', rating: 4.5, reviews: 1240, badge: 'hot' },
    { id: 2, name: 'Smart Fitness Tracker Band', brand: 'FitPro', price: 2499, oldPrice: 3999, image: '', category: 'electronics', rating: 4.2, reviews: 870, badge: 'new' },
    { id: 3, name: 'Organic Cotton Kurta Set', brand: 'EthnicWear', price: 1299, oldPrice: 2299, image: '', category: 'fashion', rating: 4.6, reviews: 543, badge: '' },
    { id: 4, name: 'Baby Soft Plush Toy Bear', brand: 'TinyJoy', price: 799, oldPrice: 1299, image: '', category: 'baby', rating: 4.8, reviews: 328, badge: 'new' },
    { id: 5, name: 'Yoga Mat Premium Anti-Slip', brand: 'FlexFit', price: 1599, oldPrice: 2499, image: '', category: 'sports', rating: 4.4, reviews: 692, badge: '' },
    { id: 6, name: 'Vitamin C Face Serum 30ml', brand: 'GlowUp', price: 899, oldPrice: 1499, image: '', category: 'beauty', rating: 4.7, reviews: 1890, badge: 'hot' },
    { id: 7, name: 'Stainless Steel Water Bottle 1L', brand: 'HydroKing', price: 599, oldPrice: 999, image: '', category: 'sports', rating: 4.3, reviews: 2100, badge: '' },
    { id: 8, name: 'Wireless Mechanical Keyboard', brand: 'KeyPro', price: 5499, oldPrice: 8499, image: '', category: 'electronics', rating: 4.6, reviews: 410, badge: 'hot' },
    { id: 9, name: 'Men\'s Running Shoes', brand: 'SpeedRunner', price: 3299, oldPrice: 5499, image: '', category: 'sports', rating: 4.3, reviews: 785, badge: '' },
    { id: 10, name: 'Baby Walker Adjustable', brand: 'SafeStep', price: 2199, oldPrice: 3499, image: '', category: 'baby', rating: 4.5, reviews: 212, badge: 'new' },
    { id: 11, name: 'Moisturizing Night Cream', brand: 'SkinLux', price: 749, oldPrice: 1199, image: '', category: 'beauty', rating: 4.4, reviews: 1023, badge: '' },
    { id: 12, name: 'Women\'s Ethnic Saree', brand: 'SariHouse', price: 2899, oldPrice: 4999, image: '', category: 'fashion', rating: 4.7, reviews: 320, badge: 'hot' },
    { id: 13, name: '4K Action Camera 20MP', brand: 'VisionPro', price: 8999, oldPrice: 14999, image: '', category: 'electronics', rating: 4.5, reviews: 567, badge: '' },
    { id: 14, name: 'Cricket Bat Kashmir Willow', brand: 'SportsKing', price: 1899, oldPrice: 3299, image: '', category: 'sports', rating: 4.2, reviews: 445, badge: '' },
    { id: 15, name: 'Baby Diaper Bag Waterproof', brand: 'BabyBliss', price: 1299, oldPrice: 1999, image: '', category: 'baby', rating: 4.6, reviews: 198, badge: 'new' },
    { id: 16, name: 'Hair Growth Serum 50ml', brand: 'TressMax', price: 1199, oldPrice: 1899, image: '', category: 'beauty', rating: 4.3, reviews: 876, badge: '' }
];

var PRODUCT_ICONS = { electronics: '📱', fashion: '👗', baby: '🍼', sports: '⚽', beauty: '💄', default: '🛒' };

function loadProducts() {
    var container = document.getElementById('products-grid');
    var categoryFilter = MDStore.Params.get('category') || '';
    var searchFilter = MDStore.Params.get('q') || '';

    if (container) {
        container.innerHTML = MDStore.Skeleton.grid(12);
    }

    if (categoryFilter) {
        var activeCat = document.querySelector('[data-cat="' + categoryFilter + '"]');
        if (activeCat) activeCat.classList.add('checked');
    }

    if (searchFilter) {
        var searchInput = document.getElementById('page-search');
        if (searchInput) searchInput.value = searchFilter;
    }

    setTimeout(function () {
        MDStore.API.get('/products' + (categoryFilter ? '?category=' + categoryFilter : ''))
            .then(function (data) {
                allProducts = data;
                filterAndRender();
            })
            .catch(function () {
                allProducts = MOCK_ALL_PRODUCTS;
                if (categoryFilter) {
                    allProducts = allProducts.filter(function (p) { return p.category === categoryFilter; });
                }
                if (searchFilter) {
                    allProducts = allProducts.filter(function (p) {
                        return p.name.toLowerCase().includes(searchFilter.toLowerCase()) ||
                            p.brand.toLowerCase().includes(searchFilter.toLowerCase());
                    });
                }
                filterAndRender();
            });
    }, 400);
}

function filterAndRender() {
    var container = document.getElementById('products-grid');
    var countEl = document.getElementById('product-count');
    if (!container) return;

    var filtered = applyFilters(allProducts);
    if (countEl) countEl.textContent = filtered.length + ' products';

    var start = (currentPage - 1) * perPage;
    var paginated = filtered.slice(start, start + perPage);

    if (paginated.length === 0) {
        container.innerHTML = '<div class="empty-state"><div class="empty-icon">🔍</div><h3>No Products Found</h3><p>Try adjusting your filters or search terms.</p></div>';
    } else {
        container.innerHTML = paginated.map(function (p, idx) { return buildProductCard(p, idx); }).join('');
        bindProductCardActions(container);
    }

    renderPagination(filtered.length);
}

function applyFilters(products) {
    var sortBy = document.getElementById('sort-select') ? document.getElementById('sort-select').value : 'default';
    var minPrice = document.getElementById('min-price') ? parseFloat(document.getElementById('min-price').value) || 0 : 0;
    var maxPrice = document.getElementById('max-price') ? parseFloat(document.getElementById('max-price').value) || Infinity : Infinity;

    var result = products.filter(function (p) {
        return p.price >= minPrice && p.price <= maxPrice;
    });

    if (sortBy === 'price-asc') result.sort(function (a, b) { return a.price - b.price; });
    else if (sortBy === 'price-desc') result.sort(function (a, b) { return b.price - a.price; });
    else if (sortBy === 'rating') result.sort(function (a, b) { return b.rating - a.rating; });
    else if (sortBy === 'newest') result.reverse();

    return result;
}

function buildProductCard(p, idx) {
    var discount = p.oldPrice ? Math.round((1 - p.price / p.oldPrice) * 100) : 0;
    var isWishlisted = MDStore.Wishlist.has(p.id);
    var icon = PRODUCT_ICONS[p.category] || PRODUCT_ICONS.default;
    var imgContent = p.image
        ? '<img src="' + p.image + '" alt="' + p.name + '" loading="lazy">'
        : '<div class="product-img-placeholder">' + icon + '</div>';

    return '<div class="product-card" data-id="' + p.id + '">' +
        (p.badge ? '<span class="product-badge ' + p.badge + '">' + p.badge.toUpperCase() + '</span>' : '') +
        '<div class="product-img-wrap">' + imgContent +
        '<button class="wishlist-btn ' + (isWishlisted ? 'active' : '') + '" data-product-id="' + p.id + '">♥</button>' +
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

function bindProductCardActions(container) {
    container.querySelectorAll('.add-to-cart-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var product = JSON.parse(btn.getAttribute('data-product'));
            MDStore.Cart.add(product, 1);
            var orig = btn.innerHTML;
            btn.textContent = '✓ Added';
            setTimeout(function () { btn.innerHTML = orig; }, 1500);
        });
    });
    container.querySelectorAll('.wishlist-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var id = parseInt(btn.getAttribute('data-product-id'));
            var product = MOCK_ALL_PRODUCTS.find(function (p) { return p.id === id; }) || { id: id };
            var added = MDStore.Wishlist.toggle(product);
            btn.classList.toggle('active', added);
        });
    });
}

function renderPagination(total) {
    var container = document.getElementById('pagination');
    if (!container) return;
    var pages = Math.ceil(total / perPage);
    if (pages <= 1) { container.innerHTML = ''; return; }
    var html = '';
    if (currentPage > 1) html += '<button class="page-btn" data-page="' + (currentPage - 1) + '">‹</button>';
    for (var i = 1; i <= pages; i++) {
        html += '<button class="page-btn ' + (i === currentPage ? 'active' : '') + '" data-page="' + i + '">' + i + '</button>';
    }
    if (currentPage < pages) html += '<button class="page-btn" data-page="' + (currentPage + 1) + '">›</button>';
    container.innerHTML = html;
    container.querySelectorAll('.page-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            currentPage = parseInt(btn.getAttribute('data-page'));
            filterAndRender();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    });
}

function bindFilters() {
    var sortSelect = document.getElementById('sort-select');
    if (sortSelect) sortSelect.addEventListener('change', function () { currentPage = 1; filterAndRender(); });

    var applyBtn = document.getElementById('apply-filter');
    if (applyBtn) applyBtn.addEventListener('click', function () { currentPage = 1; filterAndRender(); });

    var clearBtn = document.getElementById('clear-filter');
    if (clearBtn) clearBtn.addEventListener('click', function () {
        var minP = document.getElementById('min-price');
        var maxP = document.getElementById('max-price');
        if (minP) minP.value = '';
        if (maxP) maxP.value = '';
        currentPage = 1;
        filterAndRender();
    });

    var pageSearch = document.getElementById('page-search');
    if (pageSearch) {
        pageSearch.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                var q = pageSearch.value.trim().toLowerCase();
                allProducts = MOCK_ALL_PRODUCTS.filter(function (p) {
                    return !q || p.name.toLowerCase().includes(q) || p.brand.toLowerCase().includes(q);
                });
                currentPage = 1;
                filterAndRender();
            }
        });
    }
}

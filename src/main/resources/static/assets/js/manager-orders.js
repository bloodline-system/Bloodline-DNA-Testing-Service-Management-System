// Manager Orders JavaScript

class OrderManager {
    constructor() {
        this.currentStatus = 'all';
        this.currentPage = 1;
        this.itemsPerPage = 10;
        this.orders = [];
        this.filteredOrders = [];

        this.init();
    }

    init() {
        this.loadSampleData();
        this.bindEvents();
        this.renderOrders();
        this.updateCounts();
    }

    // Sample data for demonstration
    loadSampleData() {
        this.orders = [
            {
                id: 'ORD-2024-001',
                customer: 'John Smith',
                email: 'john.smith@email.com',
                testType: 'Paternity Test',
                orderDate: '2024-06-25',
                status: 'pending',
                priority: 'high',
                amount: '$299.00'
            },
            {
                id: 'ORD-2024-002',
                customer: 'Emily Johnson',
                email: 'emily.j@email.com',
                testType: 'Ancestry Test',
                orderDate: '2024-06-24',
                status: 'in-progress',
                priority: 'medium',
                amount: '$199.00'
            },
            {
                id: 'ORD-2024-003',
                customer: 'Michael Brown',
                email: 'michael.brown@email.com',
                testType: 'Health Test',
                orderDate: '2024-06-23',
                status: 'result-available',
                priority: 'high',
                amount: '$399.00'
            },
            {
                id: 'ORD-2024-004',
                customer: 'Sarah Davis',
                email: 'sarah.davis@email.com',
                testType: 'Relationship Test',
                orderDate: '2024-06-22',
                status: 'completed',
                priority: 'low',
                amount: '$249.00'
            },
            {
                id: 'ORD-2024-005',
                customer: 'David Wilson',
                email: 'david.wilson@email.com',
                testType: 'Paternity Test',
                orderDate: '2024-06-21',
                status: 'cancelled',
                priority: 'medium',
                amount: '$299.00'
            }
        ];

        // Add more sample data
        for (let i = 6; i <= 50; i++) {
            const statuses = ['pending', 'in-progress', 'result-available', 'completed', 'cancelled'];
            const priorities = ['high', 'medium', 'low'];
            const testTypes = ['Paternity Test', 'Ancestry Test', 'Health Test', 'Relationship Test'];

            this.orders.push({
                id: `ORD-2024-${String(i).padStart(3, '0')}`,
                customer: `Customer ${i}`,
                email: `customer${i}@email.com`,
                testType: testTypes[Math.floor(Math.random() * testTypes.length)],
                orderDate: new Date(2024, 5, Math.floor(Math.random() * 28) + 1).toISOString().split('T')[0],
                status: statuses[Math.floor(Math.random() * statuses.length)],
                priority: priorities[Math.floor(Math.random() * priorities.length)],
                amount: `$${(Math.random() * 300 + 199).toFixed(2)}`
            });
        }
    }

    bindEvents() {
        // Tab navigation
        document.querySelectorAll('[data-status]').forEach(tab => {
            tab.addEventListener('click', (e) => {
                this.switchTab(e.target.dataset.status);
            });
        });

        // Sidebar navigation (update existing sidebar links)
        this.updateSidebarNavigation();

        // Search functionality
        document.getElementById('searchInput').addEventListener('input', (e) => {
            this.searchOrders(e.target.value);
        });

        // Filter functionality
        document.getElementById('dateFilter').addEventListener('change', () => {
            this.applyFilters();
        });

        document.getElementById('testTypeFilter').addEventListener('change', () => {
            this.applyFilters();
        });

        // Select all checkbox
        document.getElementById('selectAll').addEventListener('change', (e) => {
            this.toggleSelectAll(e.target.checked);
        });
    }

    updateSidebarNavigation() {
        // Update sidebar order management links to use tab switching instead of page navigation
        const sidebarLinks = document.querySelectorAll('.side-menu__item');

        sidebarLinks.forEach(link => {
            const text = link.textContent.trim().toLowerCase();
            let status = '';

            switch(text) {
                case 'pending':
                    status = 'pending';
                    break;
                case 'in progress':
                    status = 'in-progress';
                    break;
                case 'result available':
                    status = 'result-available';
                    break;
                case 'completed':
                    status = 'completed';
                    break;
                case 'cancelled':
                    status = 'cancelled';
                    break;
            }

            if (status) {
                link.addEventListener('click', (e) => {
                    e.preventDefault();
                    this.switchTab(status);
                });
            }
        });
    }

    switchTab(status) {
        this.currentStatus = status;
        this.currentPage = 1;

        // Update active tab
        document.querySelectorAll('[data-status]').forEach(tab => {
            tab.classList.remove('active');
        });
        document.querySelector(`[data-status="${status}"]`).classList.add('active');

        this.filterOrders();
        this.renderOrders();
    }

    filterOrders() {
        if (this.currentStatus === 'all') {
            this.filteredOrders = [...this.orders];
        } else {
            this.filteredOrders = this.orders.filter(order => order.status === this.currentStatus);
        }
    }

    searchOrders(query) {
        if (!query) {
            this.filterOrders();
        } else {
            this.filteredOrders = this.orders.filter(order =>
                order.id.toLowerCase().includes(query.toLowerCase()) ||
                order.customer.toLowerCase().includes(query.toLowerCase()) ||
                order.email.toLowerCase().includes(query.toLowerCase()) ||
                order.testType.toLowerCase().includes(query.toLowerCase())
            );

            if (this.currentStatus !== 'all') {
                this.filteredOrders = this.filteredOrders.filter(order => order.status === this.currentStatus);
            }
        }

        this.currentPage = 1;
        this.renderOrders();
    }

    applyFilters() {
        // Apply date and test type filters
        this.filterOrders();

        const dateFilter = document.getElementById('dateFilter').value;
        const testTypeFilter = document.getElementById('testTypeFilter').value;

        if (testTypeFilter !== 'All Test Types') {
            this.filteredOrders = this.filteredOrders.filter(order => order.testType === testTypeFilter);
        }

        // Add date filtering logic here based on dateFilter value

        this.currentPage = 1;
        this.renderOrders();
    }

    renderOrders() {
        const tbody = document.getElementById('ordersTableBody');
        const startIndex = (this.currentPage - 1) * this.itemsPerPage;
        const endIndex = startIndex + this.itemsPerPage;
        const ordersToShow = this.filteredOrders.slice(startIndex, endIndex);

        tbody.innerHTML = '';

        ordersToShow.forEach(order => {
            const row = this.createOrderRow(order);
            tbody.appendChild(row);
        });

        this.updatePagination();
    }

    createOrderRow(order) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>
                <input type="checkbox" class="form-check-input order-checkbox" value="${order.id}">
            </td>
            <td>
                <strong>${order.id}</strong>
            </td>
            <td>
                <div>
                    <strong>${order.customer}</strong><br>
                    <small class="text-muted">${order.email}</small>
                </div>
            </td>
            <td>${order.testType}</td>
            <td>${this.formatDate(order.orderDate)}</td>
            <td>
                <span class="status-badge status-${order.status}">
                    ${this.formatStatus(order.status)}
                </span>
            </td>
            <td>
                <span class="priority-${order.priority}">
                    ${order.priority.charAt(0).toUpperCase() + order.priority.slice(1)}
                </span>
            </td>
            <td>
                <button class="btn btn-sm btn-outline-primary action-btn" onclick="orderManager.viewOrder('${order.id}')">
                    <i class="ri-eye-line"></i> View
                </button>
                <button class="btn btn-sm btn-outline-success action-btn" onclick="orderManager.updateStatus('${order.id}')">
                    <i class="ri-edit-line"></i> Update
                </button>
            </td>
        `;
        return row;
    }

    formatStatus(status) {
        const statusMap = {
            'pending': 'Pending',
            'in-progress': 'In Progress',
            'result-available': 'Result Available',
            'completed': 'Completed',
            'cancelled': 'Cancelled'
        };
        return statusMap[status] || status;
    }

    formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }

    updatePagination() {
        const totalPages = Math.ceil(this.filteredOrders.length / this.itemsPerPage);
        const pagination = document.getElementById('pagination');

        pagination.innerHTML = '';

        // Previous button
        const prevItem = document.createElement('li');
        prevItem.className = `page-item ${this.currentPage === 1 ? 'disabled' : ''}`;
        prevItem.innerHTML = `<a class="page-link" href="#" onclick="orderManager.goToPage(${this.currentPage - 1})">Previous</a>`;
        pagination.appendChild(prevItem);

        // Page numbers
        for (let i = 1; i <= totalPages; i++) {
            if (i === 1 || i === totalPages || (i >= this.currentPage - 2 && i <= this.currentPage + 2)) {
                const pageItem = document.createElement('li');
                pageItem.className = `page-item ${i === this.currentPage ? 'active' : ''}`;
                pageItem.innerHTML = `<a class="page-link" href="#" onclick="orderManager.goToPage(${i})">${i}</a>`;
                pagination.appendChild(pageItem);
            } else if (i === this.currentPage - 3 || i === this.currentPage + 3) {
                const ellipsis = document.createElement('li');
                ellipsis.className = 'page-item disabled';
                ellipsis.innerHTML = '<span class="page-link">...</span>';
                pagination.appendChild(ellipsis);
            }
        }

        // Next button
        const nextItem = document.createElement('li');
        nextItem.className = `page-item ${this.currentPage === totalPages ? 'disabled' : ''}`;
        nextItem.innerHTML = `<a class="page-link" href="#" onclick="orderManager.goToPage(${this.currentPage + 1})">Next</a>`;
        pagination.appendChild(nextItem);

        // Update showing entries
        const start = (this.currentPage - 1) * this.itemsPerPage + 1;
        const end = Math.min(this.currentPage * this.itemsPerPage, this.filteredOrders.length);

        document.getElementById('showing-start').textContent = start;
        document.getElementById('showing-end').textContent = end;
        document.getElementById('total-entries').textContent = this.filteredOrders.length;
    }

    goToPage(page) {
        const totalPages = Math.ceil(this.filteredOrders.length / this.itemsPerPage);
        if (page >= 1 && page <= totalPages) {
            this.currentPage = page;
            this.renderOrders();
        }
    }

    updateCounts() {
        const counts = {
            all: this.orders.length,
            pending: this.orders.filter(o => o.status === 'pending').length,
            'in-progress': this.orders.filter(o => o.status === 'in-progress').length,
            'result-available': this.orders.filter(o => o.status === 'result-available').length,
            completed: this.orders.filter(o => o.status === 'completed').length,
            cancelled: this.orders.filter(o => o.status === 'cancelled').length
        };

        // Update tab badges
        document.querySelectorAll('[data-status]').forEach(tab => {
            const badge = tab.querySelector('.badge');
            const status = tab.dataset.status;
            if (badge && counts[status] !== undefined) {
                badge.textContent = counts[status];
            }
        });

        // Update statistics cards
        document.getElementById('all-count').textContent = counts.all;
        document.getElementById('pending-count').textContent = counts.pending;
        document.getElementById('progress-count').textContent = counts['in-progress'];
        document.getElementById('result-count').textContent = counts['result-available'];
        document.getElementById('completed-count').textContent = counts.completed;
        document.getElementById('cancelled-count').textContent = counts.cancelled;
    }

    viewOrder(orderId) {
        const order = this.orders.find(o => o.id === orderId);
        if (order) {
            // Populate modal with order details
            const modalContent = document.getElementById('orderDetailsContent');
            modalContent.innerHTML = `
                <div class="row">
                    <div class="col-md-6">
                        <h6>Order Information</h6>
                        <p><strong>Order ID:</strong> ${order.id}</p>
                        <p><strong>Test Type:</strong> ${order.testType}</p>
                        <p><strong>Order Date:</strong> ${this.formatDate(order.orderDate)}</p>
                        <p><strong>Amount:</strong> ${order.amount}</p>
                        <p><strong>Status:</strong> <span class="status-badge status-${order.status}">${this.formatStatus(order.status)}</span></p>
                        <p><strong>Priority:</strong> <span class="priority-${order.priority}">${order.priority}</span></p>
                    </div>
                    <div class="col-md-6">
                        <h6>Customer Information</h6>
                        <p><strong>Name:</strong> ${order.customer}</p>
                        <p><strong>Email:</strong> ${order.email}</p>
                    </div>
                </div>
            `;

            // Show modal
            const modal = new bootstrap.Modal(document.getElementById('orderDetailsModal'));
            modal.show();
        }
    }

    updateStatus(orderId) {
        // Implement status update functionality
        console.log('Update status for order:', orderId);
    }

    toggleSelectAll(checked) {
        document.querySelectorAll('.order-checkbox').forEach(checkbox => {
            checkbox.checked = checked;
        });
    }
}

// Initialize the order manager when the page loads
document.addEventListener('DOMContentLoaded', function() {
    window.orderManager = new OrderManager();
});
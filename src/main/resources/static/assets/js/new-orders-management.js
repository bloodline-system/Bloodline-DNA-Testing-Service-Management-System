/* filepath: d:\source\Java\web-project\dna-testing\Bloodline-DNA-Testing-Service-Management-System\src\main\resources\static\assets\js\new-orders-management.js */

// New Orders Management JavaScript
class NewOrdersManager {
    constructor() {
        this.newOrders = [];
        this.availableStaff = [];
        this.filteredOrders = [];
        this.selectedStaffId = null;
        this.selectedOrderId = null;
        this.currentAssignmentType = 'sample-collection';

        this.init();
    }

    init() {
        this.loadNewOrders();
        this.loadAvailableStaff();
        this.bindEvents();
        this.updateStatistics();
    }

    // Load new orders (mock data - replace with actual API call)
    loadNewOrders() {
        // Simulate API call
        this.newOrders = [
            {
                id: 'ORD-2024-NEW-001',
                customer: 'Alice Johnson',
                email: 'alice.johnson@email.com',
                phone: '+1 (555) 123-4567',
                testType: 'Paternity Test',
                orderDate: '2024-06-28',
                priority: 'high',
                amount: '$299.00',
                status: 'new',
                appointmentDate: '2024-07-01',
                notes: 'Urgent case - customer requested expedited processing'
            },
            {
                id: 'ORD-2024-NEW-002',
                customer: 'Robert Wilson',
                email: 'robert.wilson@email.com',
                phone: '+1 (555) 234-5678',
                testType: 'Ancestry Test',
                orderDate: '2024-06-28',
                priority: 'medium',
                amount: '$199.00',
                status: 'new',
                appointmentDate: '2024-07-02',
                notes: ''
            },
            {
                id: 'ORD-2024-NEW-003',
                customer: 'Maria Garcia',
                email: 'maria.garcia@email.com',
                phone: '+1 (555) 345-6789',
                testType: 'Health Test',
                orderDate: '2024-06-27',
                priority: 'high',
                amount: '$399.00',
                status: 'new',
                appointmentDate: '2024-06-30',
                notes: 'Customer has family history concerns'
            },
            {
                id: 'ORD-2024-NEW-004',
                customer: 'James Thompson',
                email: 'james.thompson@email.com',
                phone: '+1 (555) 456-7890',
                testType: 'Relationship Test',
                orderDate: '2024-06-27',
                priority: 'low',
                amount: '$249.00',
                status: 'new',
                appointmentDate: '2024-07-03',
                notes: 'Sibling relationship verification'
            },
            {
                id: 'ORD-2024-NEW-005',
                customer: 'Sandra Lee',
                email: 'sandra.lee@email.com',
                phone: '+1 (555) 567-8901',
                testType: 'Paternity Test',
                orderDate: '2024-06-26',
                priority: 'medium',
                amount: '$299.00',
                status: 'new',
                appointmentDate: '2024-07-01',
                notes: 'Court-ordered test'
            }
        ];

        this.filteredOrders = [...this.newOrders];
        this.renderOrders();
        this.updateStatistics();
    }

    // Load available staff (mock data - replace with actual API call)
    loadAvailableStaff() {
        // Simulate API call to /manager/staff/available
        this.availableStaff = [
            {
                id: 1,
                username: 'staff_001',
                fullName: 'Dr. Emily Chen',
                email: 'emily.chen@dnalab.com',
                phoneNumber: '+1 (555) 001-0001',
                profileImageUrl: '/assets/images/faces/1.jpg',
                specialization: 'Sample Collection',
                experience: '5 years',
                currentWorkload: 3,
                maxWorkload: 8,
                isActive: true,
                lastLogin: '2024-06-28T09:00:00'
            },
            {
                id: 2,
                username: 'staff_002',
                fullName: 'Dr. Michael Rodriguez',
                email: 'michael.rodriguez@dnalab.com',
                phoneNumber: '+1 (555) 002-0002',
                profileImageUrl: '/assets/images/faces/2.jpg',
                specialization: 'Result Analysis',
                experience: '7 years',
                currentWorkload: 2,
                maxWorkload: 6,
                isActive: true,
                lastLogin: '2024-06-28T08:30:00'
            },
            {
                id: 3,
                username: 'staff_003',
                fullName: 'Dr. Sarah Johnson',
                email: 'sarah.johnson@dnalab.com',
                phoneNumber: '+1 (555) 003-0003',
                profileImageUrl: '/assets/images/faces/3.jpg',
                specialization: 'Sample Collection',
                experience: '3 years',
                currentWorkload: 5,
                maxWorkload: 8,
                isActive: true,
                lastLogin: '2024-06-28T10:15:00'
            },
            {
                id: 4,
                username: 'staff_004',
                fullName: 'Dr. David Kim',
                email: 'david.kim@dnalab.com',
                phoneNumber: '+1 (555) 004-0004',
                profileImageUrl: '/assets/images/faces/4.jpg',
                specialization: 'Result Analysis',
                experience: '6 years',
                currentWorkload: 1,
                maxWorkload: 7,
                isActive: true,
                lastLogin: '2024-06-28T07:45:00'
            },
            {
                id: 5,
                username: 'staff_005',
                fullName: 'Dr. Lisa Wang',
                email: 'lisa.wang@dnalab.com',
                phoneNumber: '+1 (555) 005-0005',
                profileImageUrl: '/assets/images/faces/5.jpg',
                specialization: 'Sample Collection',
                experience: '4 years',
                currentWorkload: 4,
                maxWorkload: 8,
                isActive: true,
                lastLogin: '2024-06-28T09:30:00'
            }
        ];

        this.updateStatistics();
    }

    bindEvents() {
        // Search functionality
        document.getElementById('searchNewOrders').addEventListener('input', (e) => {
            this.searchOrders(e.target.value);
        });

        // Filter functionality
        document.getElementById('testTypeFilter').addEventListener('change', () => {
            this.applyFilters();
        });

        document.getElementById('priorityFilter').addEventListener('change', () => {
            this.applyFilters();
        });

        // Assignment type radio buttons
        document.querySelectorAll('input[name="assignmentType"]').forEach(radio => {
            radio.addEventListener('change', (e) => {
                this.currentAssignmentType = e.target.value;
                this.filterStaffByAssignmentType();
            });
        });

        // Modal events
        document.getElementById('staffAssignmentModal').addEventListener('hidden.bs.modal', () => {
            this.resetAssignmentForm();
        });
    }

    searchOrders(query) {
        if (!query.trim()) {
            this.filteredOrders = [...this.newOrders];
        } else {
            this.filteredOrders = this.newOrders.filter(order =>
                order.id.toLowerCase().includes(query.toLowerCase()) ||
                order.customer.toLowerCase().includes(query.toLowerCase()) ||
                order.email.toLowerCase().includes(query.toLowerCase()) ||
                order.testType.toLowerCase().includes(query.toLowerCase())
            );
        }
        this.renderOrders();
    }

    applyFilters() {
        let filtered = [...this.newOrders];

        const testTypeFilter = document.getElementById('testTypeFilter').value;
        const priorityFilter = document.getElementById('priorityFilter').value;

        if (testTypeFilter) {
            filtered = filtered.filter(order => order.testType === testTypeFilter);
        }

        if (priorityFilter) {
            filtered = filtered.filter(order => order.priority === priorityFilter);
        }

        this.filteredOrders = filtered;
        this.renderOrders();
    }

    renderOrders() {
        const tbody = document.getElementById('newOrdersTableBody');
        const emptyState = document.getElementById('emptyState');

        if (this.filteredOrders.length === 0) {
            tbody.innerHTML = '';
            emptyState.style.display = 'block';
            return;
        }

        emptyState.style.display = 'none';
        tbody.innerHTML = '';

        this.filteredOrders.forEach(order => {
            const row = this.createOrderRow(order);
            tbody.appendChild(row);
        });
    }

    createOrderRow(order) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>
                <div class="d-flex align-items-center">
                    <div class="test-type-icon test-type-${this.getTestTypeClass(order.testType)}">
                        <i class="${this.getTestTypeIcon(order.testType)}"></i>
                    </div>
                    <div>
                        <strong>${order.id}</strong><br>
                        <small class="text-muted">Order Date: ${this.formatDate(order.orderDate)}</small>
                    </div>
                </div>
            </td>
            <td>
                <div>
                    <strong>${order.customer}</strong><br>
                    <small class="text-muted">${order.email}</small><br>
                    <small class="text-muted">${order.phone}</small>
                </div>
            </td>
            <td>
                <span class="fw-semibold">${order.testType}</span><br>
                <small class="text-muted">Appointment: ${this.formatDate(order.appointmentDate)}</small>
            </td>
            <td>
                <span class="priority-badge priority-${order.priority}">
                    ${order.priority.toUpperCase()}
                </span>
            </td>
            <td>${this.formatDate(order.orderDate)}</td>
            <td><strong>${order.amount}</strong></td>
            <td>
                <div class="assignment-status unassigned">
                    <i class="ri-time-line me-1"></i>
                    Unassigned
                </div>
                <button class="btn btn-sm btn-primary" onclick="newOrdersManager.openAssignmentModal('${order.id}')">
                    <i class="ri-user-add-line me-1"></i>Assign Staff
                </button>
            </td>
            <td>
                <div class="btn-group btn-group-sm" role="group">
                    <button class="btn btn-outline-info action-btn" onclick="newOrdersManager.viewOrderDetails('${order.id}')" 
                            data-bs-toggle="tooltip" title="View Details">
                        <i class="ri-eye-line"></i>
                    </button>
                </div>
            </td>
        `;
        return row;
    }

    getTestTypeClass(testType) {
        const typeMap = {
            'Paternity Test': 'paternity',
            'Ancestry Test': 'ancestry',
            'Health Test': 'health',
            'Relationship Test': 'relationship'
        };
        return typeMap[testType] || 'paternity';
    }

    getTestTypeIcon(testType) {
        const iconMap = {
            'Paternity Test': 'ri-heart-pulse-line',
            'Ancestry Test': 'ri-earth-line',
            'Health Test': 'ri-microscope-line',
            'Relationship Test': 'ri-group-line'
        };
        return iconMap[testType] || 'ri-test-tube-line';
    }

    formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }

    updateStatistics() {
        document.getElementById('new-orders-count').textContent = this.newOrders.length;
        document.getElementById('available-staff-count').textContent = this.availableStaff.filter(s => s.isActive).length;
        document.getElementById('assigned-today-count').textContent = '0'; // This would come from API
        document.getElementById('pending-assignment-count').textContent = this.newOrders.length;
    }

    openAssignmentModal(orderId) {
        this.selectedOrderId = orderId;
        const order = this.newOrders.find(o => o.id === orderId);

        if (!order) return;

        // Populate order information
        this.populateOrderInfo(order);

        // Reset assignment type to default
        document.getElementById('sampleCollection').checked = true;
        this.currentAssignmentType = 'sample-collection';

        // Load and display staff
        this.renderStaffSelection();

        // Show modal
        const modal = new bootstrap.Modal(document.getElementById('staffAssignmentModal'));
        modal.show();
    }

    populateOrderInfo(order) {
        const orderInfoSection = document.getElementById('orderInfoSection');
        orderInfoSection.innerHTML = `
            <div class="row">
                <div class="col-md-6">
                    <h6 class="text-primary mb-3">Order Information</h6>
                    <div class="info-row">
                        <span class="info-label">Order ID:</span>
                        <span class="info-value">${order.id}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Test Type:</span>
                        <span class="info-value">${order.testType}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Priority:</span>
                        <span class="info-value priority-badge priority-${order.priority}">${order.priority.toUpperCase()}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Amount:</span>
                        <span class="info-value">${order.amount}</span>
                    </div>
                </div>
                <div class="col-md-6">
                    <h6 class="text-primary mb-3">Customer Information</h6>
                    <div class="info-row">
                        <span class="info-label">Name:</span>
                        <span class="info-value">${order.customer}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Email:</span>
                        <span class="info-value">${order.email}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Phone:</span>
                        <span class="info-value">${order.phone}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Appointment:</span>
                        <span class="info-value">${this.formatDate(order.appointmentDate)}</span>
                    </div>
                </div>
            </div>
            ${order.notes ? `
                <div class="mt-3">
                    <h6 class="text-primary">Special Notes</h6>
                    <div class="alert alert-info">
                        <i class="ri-information-line me-2"></i>${order.notes}
                    </div>
                </div>
            ` : ''}
        `;
    }

    renderStaffSelection() {
        const container = document.getElementById('staffSelectionContainer');
        const noStaffDiv = document.getElementById('noStaffAvailable');

        // Filter staff based on assignment type
        const filteredStaff = this.filterStaffByAssignmentType();

        if (filteredStaff.length === 0) {
            container.innerHTML = '';
            noStaffDiv.style.display = 'block';
            return;
        }

        noStaffDiv.style.display = 'none';
        container.innerHTML = '';

        filteredStaff.forEach(staff => {
            const staffCard = this.createStaffCard(staff);
            container.appendChild(staffCard);
        });
    }

    filterStaffByAssignmentType() {
        return this.availableStaff.filter(staff => {
            if (!staff.isActive) return false;

            if (this.currentAssignmentType === 'sample-collection') {
                return staff.specialization === 'Sample Collection' && staff.currentWorkload < staff.maxWorkload;
            } else if (this.currentAssignmentType === 'result-analysis') {
                return staff.specialization === 'Result Analysis' && staff.currentWorkload < staff.maxWorkload;
            }

            return false;
        });
    }

    createStaffCard(staff) {
        const col = document.createElement('div');
        col.className = 'col-md-6';

        const workloadPercentage = Math.round((staff.currentWorkload / staff.maxWorkload) * 100);
        const isNearCapacity = workloadPercentage > 80;

        col.innerHTML = `
            <div class="staff-card" data-staff-id="${staff.id}" onclick="newOrdersManager.selectStaff(${staff.id})">
                <div class="d-flex align-items-center">
                    <div class="staff-avatar">
                        ${this.getInitials(staff.fullName)}
                    </div>
                    <div class="staff-info">
                        <div class="staff-name">${staff.fullName}</div>
                        <div class="staff-role">
                            <i class="ri-user-star-line me-1"></i>${staff.specialization}
                        </div>
                        <div class="d-flex align-items-center justify-content-between">
                            <div class="staff-status available">
                                <i class="ri-checkbox-circle-line me-1"></i>Available
                            </div>
                            <small class="text-muted">
                                Workload: ${staff.currentWorkload}/${staff.maxWorkload}
                                ${isNearCapacity ? '<i class="ri-alert-line text-warning ms-1"></i>' : ''}
                            </small>
                        </div>
                        <div class="mt-2">
                            <small class="text-muted">
                                <i class="ri-time-line me-1"></i>Experience: ${staff.experience}
                            </small>
                        </div>
                    </div>
                </div>
            </div>
        `;

        return col;
    }

    getInitials(fullName) {
        return fullName.split(' ').map(name => name.charAt(0)).join('').toUpperCase();
    }

    selectStaff(staffId) {
        // Remove previous selection
        document.querySelectorAll('.staff-card').forEach(card => {
            card.classList.remove('selected');
        });

        // Add selection to clicked card
        const selectedCard = document.querySelector(`[data-staff-id="${staffId}"]`);
        if (selectedCard) {
            selectedCard.classList.add('selected');
        }

        this.selectedStaffId = staffId;

        // Enable confirm button
        document.getElementById('confirmAssignmentBtn').disabled = false;
    }

    resetAssignmentForm() {
        this.selectedStaffId = null;
        this.selectedOrderId = null;

        document.querySelectorAll('.staff-card').forEach(card => {
            card.classList.remove('selected');
        });

        document.getElementById('assignmentNotes').value = '';
        document.getElementById('confirmAssignmentBtn').disabled = true;
    }

    viewOrderDetails(orderId) {
        const order = this.newOrders.find(o => o.id === orderId);
        if (!order) return;

        const modalContent = document.getElementById('orderDetailsContent');
        modalContent.innerHTML = `
            <div class="order-info-card">
                <h6><i class="ri-file-info-line me-2"></i>Order Information</h6>
                <div class="row">
                    <div class="col-md-6">
                        <div class="info-row">
                            <span class="info-label">Order ID:</span>
                            <span class="info-value">${order.id}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Test Type:</span>
                            <span class="info-value">${order.testType}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Order Date:</span>
                            <span class="info-value">${this.formatDate(order.orderDate)}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Priority:</span>
                            <span class="info-value">${order.priority.toUpperCase()}</span>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="info-row">
                            <span class="info-label">Customer:</span>
                            <span class="info-value">${order.customer}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Email:</span>
                            <span class="info-value">${order.email}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Phone:</span>
                            <span class="info-value">${order.phone}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-label">Amount:</span>
                            <span class="info-value">${order.amount}</span>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="row">
                <div class="col-md-12">
                    <h6 class="text-primary mb-3">Appointment Details</h6>
                    <div class="card">
                        <div class="card-body">
                            <div class="d-flex align-items-center mb-3">
                                <i class="ri-calendar-line text-primary me-3 fs-18"></i>
                                <div>
                                    <strong>Scheduled Date:</strong> ${this.formatDate(order.appointmentDate)}<br>
                                    <small class="text-muted">Sample collection appointment</small>
                                </div>
                            </div>
                            ${order.notes ? `
                                <div class="mt-3">
                                    <strong>Special Instructions:</strong>
                                    <div class="alert alert-info mt-2">
                                        <i class="ri-information-line me-2"></i>${order.notes}
                                    </div>
                                </div>
                            ` : ''}
                        </div>
                    </div>
                </div>
            </div>
        `;

        const modal = new bootstrap.Modal(document.getElementById('orderDetailsModal'));
        modal.show();
    }
}

// Global function for staff assignment confirmation
function confirmStaffAssignment() {
    if (!window.newOrdersManager.selectedStaffId || !window.newOrdersManager.selectedOrderId) {
        Swal.fire({
            icon: 'warning',
            title: 'Selection Required',
            text: 'Please select a staff member to assign.'
        });
        return;
    }

    const orderId = window.newOrdersManager.selectedOrderId;
    const staffId = window.newOrdersManager.selectedStaffId;
    const assignmentType = window.newOrdersManager.currentAssignmentType;
    const notes = document.getElementById('assignmentNotes').value;

    // Show loading state
    const confirmBtn = document.getElementById('confirmAssignmentBtn');
    const originalText = confirmBtn.innerHTML;
    confirmBtn.innerHTML = '<span class="loading-spinner"></span> Assigning...';
    confirmBtn.disabled = true;

    // Show loading dialog
    Swal.fire({
        title: 'Assigning Staff...',
        text: 'Please wait while we assign the staff member to this order.',
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading()
        }
    });

    // Simulate API call (replace with actual implementation)
    setTimeout(() => {
        // Here you would make the actual API call
        // Example: POST /manager/orders/${orderId}/assign-staff
        const assignmentData = {
            orderId: orderId,
            staffId: staffId,
            assignmentType: assignmentType,
            notes: notes
        };

        console.log('Assignment data:', assignmentData);

        // Remove the order from new orders list
        const orderIndex = window.newOrdersManager.newOrders.findIndex(o => o.id === orderId);
        if (orderIndex > -1) {
            window.newOrdersManager.newOrders.splice(orderIndex, 1);
        }

        // Update staff workload
        const staff = window.newOrdersManager.availableStaff.find(s => s.id === staffId);
        if (staff) {
            staff.currentWorkload += 1;
        }

        // Refresh the display
        window.newOrdersManager.filteredOrders = [...window.newOrdersManager.newOrders];
        window.newOrdersManager.renderOrders();
        window.newOrdersManager.updateStatistics();

        Swal.fire({
            icon: 'success',
            title: 'Assignment Successful!',
            text: 'Staff has been successfully assigned to the order.',
            showConfirmButton: true,
            confirmButtonText: 'OK'
        }).then(() => {
            // Close modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('staffAssignmentModal'));
            if (modal) modal.hide();
        });
    }, 2000);

    // Restore button after delay
    setTimeout(() => {
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
    }, 2000);
}

// Initialize the new orders manager when the page loads
document.addEventListener('DOMContentLoaded', function() {
    window.newOrdersManager = new NewOrdersManager();
});
# Manager Order and Report Management

This document describes the manager functionality for handling service orders and system reports in the Bloodline DNA Testing Service Management System.

## Overview

Managers have access to comprehensive order management and reporting capabilities through dedicated REST APIs. The system provides endpoints for viewing, updating, and assigning orders, as well as creating and managing system reports.

## Order Management

### Endpoints

#### 1. Get All Orders
- **URL**: `GET /api/v1/manager/orders`
- **Description**: Retrieve all service orders with status counts and statistics
- **Response**: List of orders with pagination support and status breakdown

#### 2. Get New Orders
- **URL**: `GET /api/v1/manager/orders/new`
- **Description**: Retrieve unassigned orders with available staff information
- **Response**: New orders count, available staff count, and assignment data

#### 3. Update Order Status
- **URL**: `PATCH /api/v1/manager/orders/{orderId}/status`
- **Description**: Update the status of a specific order
- **Parameters**:
  - `status`: New status (PENDING, CONFIRMED, ASSIGNED, COMPLETED, CANCELLED)
  - `notes` (optional): Additional notes for the status change

#### 4. Assign Staff to Order
- **URL**: `POST /api/v1/manager/orders/{orderId}/assign-staff`
- **Description**: Assign collection and analysis staff to an order
- **Parameters**:
  - `collectStaffId`: ID of staff for sample collection
  - `analysisStaffId`: ID of staff for analysis
  - `assignmentType` (optional): Type of assignment
  - `notes` (optional): Assignment notes

## Report Management

### Endpoints

#### 1. Get All Reports
- **URL**: `GET /api/v1/manager/reports`
- **Description**: Retrieve all system reports with filtering and pagination
- **Parameters**:
  - `page`: Page number (default: 0)
  - `size`: Page size (default: 20)
  - `status`: Filter by status (all, GENERATED, APPROVED, REJECTED)
  - `generatedByRole`: Filter by role (all, MANAGER, STAFF, etc.)
  - `search`: Search in report name, category, or user name
  - `sortBy`: Sort field (createdAt, reportName, etc.)
  - `sortDir`: Sort direction (asc, desc)

#### 2. Get Report by ID
- **URL**: `GET /api/v1/manager/reports/{reportId}`
- **Description**: Retrieve detailed information for a specific report

#### 3. Create New Report
- **URL**: `POST /api/v1/manager/reports`
- **Description**: Create a new system report
- **Body**:
  ```json
  {
    "reportName": "Monthly Sales Report",
    "reportCategory": "Sales",
    "reportType": "FINANCIAL",
    "reportData": "Report content/data"
  }
  ```

#### 4. Update Report Status
- **URL**: `PATCH /api/v1/manager/reports/{reportId}/status`
- **Description**: Update the status of a report
- **Body**:
  ```json
  {
    "status": "APPROVED"
  }
  ```

## Authentication

All endpoints require MANAGER role authentication. Include the JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

## Response Format

All responses follow a standard API wrapper format:

```json
{
  "code": 200,
  "message": "Success message",
  "data": {
    // Response data
  },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

## Error Handling

- **401 Unauthorized**: Invalid or missing authentication
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource not found
- **400 Bad Request**: Invalid request parameters
- **500 Internal Server Error**: Server error

## Testing

Comprehensive unit tests are provided in:
- `ManagerOrderControllerTest.java`
- `ManagerReportControllerTest.java`

Run tests with:
```bash
mvn test -Dtest=ManagerOrderControllerTest,ManagerReportControllerTest
```

## Postman Collection

A complete Postman collection is available at:
`postman/manager-order-report-api-collection.json`

This collection includes:
- Authentication flow
- All API endpoints
- Test scripts for validation
- Environment variables setup

## Key Features

### Order Management
- Real-time order status tracking
- Staff availability checking
- Automated notifications
- Status transition validation
- Email notifications for status changes

### Report Management
- Multiple report types and categories
- Role-based access control
- Advanced filtering and search
- Report statistics and analytics
- Status workflow management

## Dependencies

- Spring Boot Web
- Spring Security
- JPA/Hibernate
- JWT Authentication
- Email service
- Notification service

## Future Enhancements

- Bulk order operations
- Advanced reporting with charts
- Integration with external systems
- Mobile app support
- Real-time notifications via WebSocket
# DNA Testing Service API Documentation

## Base URL

```
http://localhost:8080/api/mock/service
```

## Service Types

### Get All Service Types

- **Method**: GET
- **Endpoint**: `/service-types`
- **Response**: List of service types

### Create Service Type

- **Method**: POST
- **Endpoint**: `/service-types`
- **Request Body**:

```json
{
  "typeName": "PATERNITY"
}
```

- **Response**: Created service type

## Medical Services

### Get All Medical Services

- **Method**: GET
- **Endpoint**: `/medical-services`
- **Response**: List of medical services

### Get Medical Service by ID

- **Method**: GET
- **Endpoint**: `/medical-services/{id}`
- **Response**: Medical service details

### Create Medical Service

- **Method**: POST
- **Endpoint**: `/medical-services`
- **Request Body**:

```json
{
  "serviceName": "Paternity DNA Test",
  "serviceCategory": "DNA_TESTING",
  "testType": "PATERNITY",
  "serviceTypeId": 1,
  "participants": 2,
  "executionTimeDays": 7,
  "basePrice": 299.99,
  "currentPrice": 249.99,
  "isAvailable": true,
  "serviceDescription": "Standard paternity test to determine biological father",
  "featureAssignments": [
    {
      "featureId": 1,
      "isAvailable": true
    },
    {
      "featureId": 2,
      "isAvailable": true
    }
  ]
}
```

- **Response**: Created medical service

### Delete Medical Service

- **Method**: DELETE
- **Endpoint**: `/medical-services/{id}`
- **Response**: 204 No Content

## Service Features

### Get All Service Features

- **Method**: GET
- **Endpoint**: `/service-features`
- **Response**: List of service features

### Create Service Feature

- **Method**: POST
- **Endpoint**: `/service-features`
- **Request Body**:

```json
{
  "featureName": "Confidential Results",
  "isAvailable": true
}
```

- **Response**: Created service feature

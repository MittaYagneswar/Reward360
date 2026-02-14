# Postman Testing Guide - Fraud_MS & CustomerMs Integration

## Prerequisites
1. Start **Eureka Discovery Server** on port 8761
2. Start **CustomerMs** on port 8081
3. Start **Fraud_MS** on port 8082

## Step-by-Step Testing Instructions

---

## STEP 1: Register a User in CustomerMs

### Endpoint
```
POST http://localhost:8081/api/users/register
```

### Headers
```
Content-Type: application/json
```

### Body (raw JSON)
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "password": "password123",
  "phoneNumber": "1234567890",
  "tier": "SILVER",
  "address": "123 Main Street",
  "city": "New York",
  "state": "NY",
  "zipCode": "10001",
  "country": "USA",
  "dateOfBirth": "1990-01-15"
}
```

### Expected Response (200 OK)
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "1234567890",
  "tier": "SILVER",
  "address": "123 Main Street",
  "city": "New York",
  "state": "NY",
  "zipCode": "10001",
  "country": "USA",
  "dateOfBirth": "1990-01-15"
}
```

**Note:** Save the `id` from the response (e.g., `1`). You'll use this as `userId`.

---

## STEP 2: Create Transactions in CustomerMs

### Endpoint
```
POST http://localhost:8081/api/users/claim
```

### Headers
```
Content-Type: application/json
```

### Body (raw JSON) - Transaction 1
```json
{
  "userId": 1,
  "externalId": "EXT-TXN-001",
  "type": "PURCHASE",
  "pointsEarned": 100,
  "pointsRedeemed": 0,
  "store": "Amazon",
  "date": "2026-02-10",
  "expiry": "2027-02-10",
  "note": "Electronics purchase"
}
```

### Expected Response (201 Created)
```json
{
  "id": 1,
  "externalId": "EXT-TXN-001",
  "type": "PURCHASE",
  "pointsEarned": 100,
  "pointsRedeemed": 0,
  "store": "Amazon",
  "date": "2026-02-10",
  "expiry": "2027-02-10",
  "note": "Electronics purchase"
}
```

### Body (raw JSON) - Transaction 2
```json
{
  "userId": 1,
  "externalId": "EXT-TXN-002",
  "type": "REDEMPTION",
  "pointsEarned": 0,
  "pointsRedeemed": 50,
  "store": "Walmart",
  "date": "2026-02-11",
  "expiry": "2027-02-11",
  "note": "Redeemed points for groceries"
}
```

### Body (raw JSON) - Transaction 3
```json
{
  "userId": 1,
  "externalId": "EXT-TXN-003",
  "type": "CLAIM",
  "pointsEarned": 200,
  "pointsRedeemed": 0,
  "store": "Best Buy",
  "date": "2026-02-12",
  "expiry": "2027-02-12",
  "note": "Bonus points claim"
}
```

---

## STEP 3: Verify Transactions in CustomerMs (Optional)

### Endpoint
```
GET http://localhost:8081/api/users/transactions/user/1
```

### Headers
```
None required
```

### Expected Response (200 OK)
```json
[
  {
    "id": 1,
    "externalId": "EXT-TXN-001",
    "type": "PURCHASE",
    "pointsEarned": 100,
    "pointsRedeemed": 0,
    "store": "Amazon",
    "date": "2026-02-10",
    "expiry": "2027-02-10",
    "note": "Electronics purchase"
  },
  {
    "id": 2,
    "externalId": "EXT-TXN-002",
    "type": "REDEMPTION",
    "pointsEarned": 0,
    "pointsRedeemed": 50,
    "store": "Walmart",
    "date": "2026-02-11",
    "expiry": "2027-02-11",
    "note": "Redeemed points for groceries"
  },
  {
    "id": 3,
    "externalId": "EXT-TXN-003",
    "type": "CLAIM",
    "pointsEarned": 200,
    "pointsRedeemed": 0,
    "store": "Best Buy",
    "date": "2026-02-12",
    "expiry": "2027-02-12",
    "note": "Bonus points claim"
  }
]
```

---

## STEP 4: Test Feign Client Integration from Fraud_MS

### Endpoint (THE MAIN TEST!)
```
GET http://localhost:8082/api/v1/transactions/customer/1
```

### Headers
```
None required
```

### Expected Response (200 OK)
This will fetch customer transactions from CustomerMs via Feign client:
```json
[
  {
    "id": 1,
    "externalId": "EXT-TXN-001",
    "type": "PURCHASE",
    "pointsEarned": 100,
    "pointsRedeemed": 0,
    "store": "Amazon",
    "date": "2026-02-10",
    "expiry": "2027-02-10",
    "note": "Electronics purchase",
    "userId": 1
  },
  {
    "id": 2,
    "externalId": "EXT-TXN-002",
    "type": "REDEMPTION",
    "pointsEarned": 0,
    "pointsRedeemed": 50,
    "store": "Walmart",
    "date": "2026-02-11",
    "expiry": "2027-02-11",
    "note": "Redeemed points for groceries",
    "userId": 1
  },
  {
    "id": 3,
    "externalId": "EXT-TXN-003",
    "type": "CLAIM",
    "pointsEarned": 200,
    "pointsRedeemed": 0,
    "store": "Best Buy",
    "date": "2026-02-12",
    "expiry": "2027-02-12",
    "note": "Bonus points claim",
    "userId": 1
  }
]
```

**✅ SUCCESS!** If you get this response, the Feign client is working correctly! Fraud_MS successfully called CustomerMs through Eureka discovery.

---

## STEP 5: Test Fallback (Resilience Test)

1. **Stop CustomerMs** (shut down the service)
2. Make the same request:

### Endpoint
```
GET http://localhost:8082/api/v1/transactions/customer/1
```

### Expected Response (200 OK)
```json
[]
```

The fallback returns an empty array instead of throwing an error. Check the Fraud_MS console logs - you should see:
```
CustomerMs service is unavailable. Returning empty transaction list for userId: 1
```

---

## Additional Test Cases

### Test Case 1: Non-existent User
```
GET http://localhost:8082/api/v1/transactions/customer/999
```
**Expected:** Empty array `[]`

### Test Case 2: Get Fraud_MS Transactions
```
GET http://localhost:8082/api/v1/transactions
```
**Expected:** List of fraud transactions from Fraud_MS database

### Test Case 3: Create Fraud Transaction
```
POST http://localhost:8082/api/v1/transactions
Content-Type: application/json

{
  "transactionId": "FRD-001",
  "accountId": "ACC1001",
  "amount": 1500.00,
  "currency": "USD",
  "merchantName": "Apple Store",
  "merchantCategory": "ELECTRONICS",
  "paymentMethod": "CREDIT_CARD",
  "location": "New York, USA",
  "description": "iPhone purchase",
  "riskLevel": "LOW",
  "status": "CLEARED",
  "userId": 1
}
```

---

## Eureka Dashboard Verification

Visit: `http://localhost:8761`

You should see:
- **CUSTOMERMS** - UP (1 instance)
- **FRAUD-DETECTION-SERVICE** - UP (1 instance)

---

## Troubleshooting

### Issue: Connection refused
**Solution:** Make sure all services are running:
- Eureka: `http://localhost:8761`
- CustomerMs: `http://localhost:8081`
- Fraud_MS: `http://localhost:8082`

### Issue: Empty response from Feign client
**Possible causes:**
1. Services not registered with Eureka - Check Eureka dashboard
2. No data in CustomerMs database - Create user and transactions first
3. Wrong userId - Use the actual userId from Step 1

### Issue: 404 Not Found
**Solution:** Double-check the endpoint URL and make sure the service is running

---

## Quick Test Summary

1. ✅ Register user in CustomerMs → Get userId
2. ✅ Create 3 transactions in CustomerMs using that userId
3. ✅ Call Fraud_MS Feign endpoint: `/api/v1/transactions/customer/{userId}`
4. ✅ Verify you get the same transactions back
5. ✅ Stop CustomerMs and test fallback behavior

**That's it!** Your microservices are communicating through Eureka with Feign client! 🎉

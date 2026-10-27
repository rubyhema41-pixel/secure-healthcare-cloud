# Secure Healthcare Data Management - ABSE + Cloud (AWS)

backend/  : Spring Boot 3, Java 17, MySQL, AWS S3
frontend/ : React 18 + Vite + Bootstrap

## Run locally
1. Start MySQL (user root / password root, or set DB_USER and DB_PASS).
2. AWS: create an S3 bucket; set S3_BUCKET and AWS_REGION; run `aws configure` (or use an IAM role on EC2).
3. Make two keys with `openssl rand -base64 32` and set MASTER_KEY and INDEX_KEY.
4. Backend:  cd backend && mvn spring-boot:run          (http://localhost:8080)
5. Frontend: cd frontend && npm install && npm run dev   (http://localhost:5173)
6. Login as admin / admin123 (set ADMIN_PASSWORD to change it). Register one Owner, one User and one Authority.

## Demo flow
Owner uploads a file (policy "doctor,cardiology", keywords "diabetes")
-> a Data User with attributes "doctor,cardiology" searches "diabetes" and sends a request
-> Authority approves -> User downloads (hash checked, then decrypted) -> Admin views the audit log.

## Deploy on AWS
- RDS MySQL (set DB_URL, DB_USER, DB_PASS) and an S3 bucket with public access blocked.
- Backend: `mvn package`, run the jar on EC2 with an IAM role allowing s3:GetObject and s3:PutObject on the bucket.
- Frontend: `VITE_API_URL=https://<backend>/api npm run build`, upload dist/ to S3 static hosting or CloudFront.
- Use HTTPS (ACM certificate + load balancer) and set CORS_ORIGIN to the frontend URL.

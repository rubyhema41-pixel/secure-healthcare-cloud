package com.healthcare.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
/** Stores ONLY encrypted files in AWS S3. */
@Service
public class StorageService {
    private final S3Client s3;
    private final String bucket;
    public StorageService(@Value("${app.s3.bucket}") String bucket, @Value("${app.s3.region}") String region) {
        this.bucket = bucket;
        this.s3 = S3Client.builder().region(Region.of(region)).build(); // credentials: IAM role / env vars
    }
    public void put(String key, byte[] data) {
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).serverSideEncryption(ServerSideEncryption.AES256).build(),
                RequestBody.fromBytes(data));
    }
    public byte[] get(String key) {
        return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
    }
}

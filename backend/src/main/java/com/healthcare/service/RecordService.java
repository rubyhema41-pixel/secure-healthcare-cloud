package com.healthcare.service;
import com.healthcare.model.*;
import com.healthcare.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecordService {
    private final EhrRecordRepository records; private final AccessRequestRepository requests;
    private final AuditLogRepository logs; private final CryptoService crypto; private final StorageService storage;
    public RecordService(EhrRecordRepository r, AccessRequestRepository q, AuditLogRepository l, CryptoService c, StorageService s) {
        records = r; requests = q; logs = l; crypto = c; storage = s;
    }
    public void audit(String user, String action, String detail) {
        AuditLog a = new AuditLog(); a.username = user; a.action = action; a.detail = detail; logs.save(a);
    }

    /** DATA OWNER: encrypt -> hash -> store in S3 -> save metadata in MySQL */
    public EhrRecord upload(User owner, MultipartFile file, String policy, String keywords) throws Exception {
        CryptoService.Encrypted enc = crypto.encrypt(file.getBytes());
        String key = "ehr/" + UUID.randomUUID();
        storage.put(key, enc.cipher());
        EhrRecord r = new EhrRecord();
        r.ownerId = owner.id; r.fileName = file.getOriginalFilename(); r.s3Key = key; r.policy = policy;
        r.sha256 = crypto.sha256Hex(enc.cipher()); r.wrappedKey = enc.wrappedKey();
        r.keywordTokens = Arrays.stream(keywords.split(",")).filter(k -> !k.isBlank())
                .map(crypto::keywordToken).collect(Collectors.joining(","));
        records.save(r);
        audit(owner.username, "UPLOAD", "record " + r.id + " (" + r.fileName + ")");
        return r;
    }
    /** DATA USER: search by keyword token; only records whose policy the user satisfies are shown */
    public List<EhrRecord> search(User u, String keyword) {
        String token = crypto.keywordToken(keyword);
        audit(u.username, "SEARCH", "encrypted keyword search");
        return records.findAll().stream()
                .filter(r -> r.keywordTokens != null && Arrays.asList(r.keywordTokens.split(",")).contains(token))
                .filter(r -> crypto.policySatisfied(r.policy, u.attributes))
                .toList();
    }
    public AccessRequest request(User u, Long recordId) {
        EhrRecord r = records.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));
        if (!crypto.policySatisfied(r.policy, u.attributes))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your attributes do not satisfy the record policy");
        AccessRequest q = new AccessRequest();
        q.recordId = recordId; q.userId = u.id; q.username = u.username; q.fileName = r.fileName;
        requests.save(q);
        audit(u.username, "REQUEST", "record " + recordId);
        return q;
    }
    public List<AccessRequest> pending() { return requests.findByStatus(AccessRequest.Status.PENDING); }

    /** AUTHORITY: approve or reject */
    public AccessRequest decide(User authority, Long requestId, boolean approve) {
        AccessRequest q = requests.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        q.status = approve ? AccessRequest.Status.APPROVED : AccessRequest.Status.REJECTED;
        requests.save(q);
        audit(authority.username, approve ? "APPROVE" : "REJECT", "request " + requestId + " by " + q.username);
        return q;
    }
    /** DATA USER: download only if approved; verify hash; then decrypt */
    public EhrRecord approvedRecord(User u, Long recordId) {
        if (!requests.existsByRecordIdAndUserIdAndStatus(recordId, u.id, AccessRequest.Status.APPROVED))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access not approved by the Authority");
        return records.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));
    }
    public byte[] download(User u, EhrRecord r) throws Exception {
        byte[] cipher = storage.get(r.s3Key);
        if (!crypto.sha256Hex(cipher).equals(r.sha256)) {
            audit(u.username, "TAMPER_DETECTED", "record " + r.id);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Integrity check failed: record was modified");
        }
        audit(u.username, "DOWNLOAD", "record " + r.id);
        return crypto.decrypt(cipher, r.wrappedKey);
    }
}

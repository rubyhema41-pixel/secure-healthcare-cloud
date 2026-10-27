package com.healthcare.web;
import com.healthcare.model.*;
import com.healthcare.repo.*;
import com.healthcare.service.RecordService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController @RequestMapping("/api")
public class ApiController {
    private final UserRepository users; private final AuditLogRepository logs;
    private final RecordService svc; private final PasswordEncoder enc;
    public ApiController(UserRepository u, AuditLogRepository l, RecordService s, PasswordEncoder e) { users = u; logs = l; svc = s; enc = e; }

    private User me(Authentication a) { return users.findByUsername(a.getName()).orElseThrow(); }

    // ---------- Registration / login ----------
    public record RegisterReq(String username, String password, Role role, String attributes) {}
    @PostMapping("/auth/register")
    public User register(@RequestBody RegisterReq r) {
        if (r.role() == Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot self-register as Admin");
        if (users.findByUsername(r.username()).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Username taken");
        User u = new User(); u.username = r.username(); u.passwordHash = enc.encode(r.password());
        u.role = r.role(); u.attributes = r.attributes() == null ? "" : r.attributes();
        users.save(u); svc.audit(u.username, "REGISTER", u.role.name()); return u;
    }
    @GetMapping("/auth/me") public User whoAmI(Authentication a) { svc.audit(a.getName(), "LOGIN", ""); return me(a); }

    // ---------- Data Owner ----------
    @PreAuthorize("hasRole('DATA_OWNER')")
    @PostMapping(value = "/records", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EhrRecord upload(Authentication a, @RequestParam MultipartFile file, @RequestParam String policy,
                            @RequestParam String keywords) throws Exception { return svc.upload(me(a), file, policy, keywords); }

    // ---------- Data User ----------
    @PreAuthorize("hasRole('DATA_USER')")
    @GetMapping("/records/search")
    public List<EhrRecord> search(Authentication a, @RequestParam String keyword) { return svc.search(me(a), keyword); }

    @PreAuthorize("hasRole('DATA_USER')")
    @PostMapping("/records/{id}/request")
    public AccessRequest request(Authentication a, @PathVariable Long id) { return svc.request(me(a), id); }

    @PreAuthorize("hasRole('DATA_USER')")
    @GetMapping("/records/{id}/download")
    public ResponseEntity<byte[]> download(Authentication a, @PathVariable Long id) throws Exception {
        User u = me(a); EhrRecord r = svc.approvedRecord(u, id); byte[] data = svc.download(u, r);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + r.fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(data);
    }

    // ---------- Authority ----------
    @PreAuthorize("hasRole('AUTHORITY')")
    @GetMapping("/requests/pending") public List<AccessRequest> pending() { return svc.pending(); }

    @PreAuthorize("hasRole('AUTHORITY')")
    @PostMapping("/requests/{id}/decision")
    public AccessRequest decide(Authentication a, @PathVariable Long id, @RequestParam boolean approve) { return svc.decide(me(a), id, approve); }

    // ---------- Admin ----------
    @PreAuthorize("hasRole('ADMIN')") @GetMapping("/admin/users") public List<User> allUsers() { return users.findAll(); }
    @PreAuthorize("hasRole('ADMIN')") @GetMapping("/admin/logs") public List<AuditLog> allLogs() { return logs.findAllByOrderByIdDesc(); }
}

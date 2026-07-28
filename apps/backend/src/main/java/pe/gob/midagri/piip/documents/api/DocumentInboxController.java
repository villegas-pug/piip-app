package pe.gob.midagri.piip.documents.api;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.gob.midagri.piip.documents.domain.DocumentState;
import pe.gob.midagri.piip.documents.persistence.DocumentRepository;
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.portfolio.persistence.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentInboxController {
    private final PortfolioRecordRepository records; private final DocumentRepository documents; private final LocalAuthorizationService authorization;
    public DocumentInboxController(PortfolioRecordRepository records, DocumentRepository documents, LocalAuthorizationService authorization) { this.records = records; this.documents = documents; this.authorization = authorization; }
    @GetMapping @Transactional(readOnly = true)
    public List<DossierSummary> list() {
        LocalAccessContext access = authorization.requireAuthenticatedRole();
        return records.findAll().stream().filter(record -> access.coversExecutingUnit(record.getExecutingUnit().getId(), record.getExecutingUnit().getInstitution().getId())).map(record -> {
            var slots = documents.findByRecordIdOrderByType(record.getId());
            long loaded = slots.stream().filter(item -> item.getState() == DocumentState.LOADED).count();
            long pending = slots.stream().filter(item -> item.getState() == DocumentState.PENDING).count();
            long notApplicable = slots.stream().filter(item -> item.getState() == DocumentState.NOT_APPLICABLE).count();
            return new DossierSummary(record.getRecordType().label(), record.getCode(), record.getName(), record.getExecutingUnit().getName(), record.getStatus().label(), loaded, pending, notApplicable, record.getUpdatedAt());
        }).toList();
    }
    public record DossierSummary(String recordType, String code, String name, String unit, String status, long loadedCount, long pendingCount, long notApplicableCount, Instant lastActivity) {}
}

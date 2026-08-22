package pe.gob.midagri.piip.documents.application;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.documents.domain.DocumentState;
import pe.gob.midagri.piip.documents.persistence.DocumentRepository;
import pe.gob.midagri.piip.identity.application.*;
import pe.gob.midagri.piip.portfolio.persistence.*;
import pe.gob.midagri.piip.organization.application.OrganizationReadModels.OrganizationalUnitView;

@Service
public class DocumentInboxService {
    private final PortfolioRecordRepository records;
    private final DocumentRepository documents;
    private final LocalAuthorizationService authorization;
    private final ResponsibleUnitRepository responsibleUnits;
    public DocumentInboxService(PortfolioRecordRepository records, DocumentRepository documents,
            LocalAuthorizationService authorization, ResponsibleUnitRepository responsibleUnits) {
        this.records = records; this.documents = documents; this.authorization = authorization; this.responsibleUnits = responsibleUnits;
    }
    @Transactional(readOnly = true)
    public List<DossierSummary> list() {
        return list(null);
    }

    @Transactional(readOnly = true)
    public List<DossierSummary> list(Long executingUnitId) {
        LocalAccessContext access = authorization.requireAuthenticatedRole();
        if (executingUnitId != null) authorization.requireReadableUnit(executingUnitId);
        var source = executingUnitId == null ? records.findAll() : records.findByExecutingUnit_IdOrderByUpdatedAtDesc(executingUnitId);
        return source.stream()
            .filter(record -> access.coversExecutingUnit(record.getExecutingUnit().getId(), record.getExecutingUnit().getInstitution().getId()))
            .map(record -> {
                var slots = documents.findByRecordIdOrderByTypeDisplayOrderAscTypeCodeAsc(record.getId());
                List<OrganizationalUnitView> units = responsibleUnits.findByRecordIdOrderByDisplayOrder(record.getId()).stream()
                    .map(ResponsibleUnitEntity::getOrganizationalUnit)
                    .map(unit -> new OrganizationalUnitView(unit.getId(), unit.getCode(), unit.getName(), unit.isActive(), unit.getAcronym(),
                        unit.getParent() == null ? null : unit.getParent().getId(), unit.getExecutingUnit().getId())).toList();
                return new DossierSummary(record.getRecordType().label(), record.getCode(), record.getName(), record.getExecutingUnit().getName(), record.getStatus().label(),
                    slots.stream().filter(item -> item.getState() == DocumentState.LOADED).count(),
                    slots.stream().filter(item -> item.getState() == DocumentState.PENDING).count(),
                    slots.stream().filter(item -> item.getState() == DocumentState.NOT_APPLICABLE).count(), record.getUpdatedAt(),
                    record.getRecordType().name(), record.getExecutingUnit().getId(), units);
            }).toList();
    }
    public record DossierSummary(String recordType, String code, String name, String unit, String status,
        long loadedCount, long pendingCount, long notApplicableCount, Instant lastActivity,
        String recordTypeCode, Long executingUnitId, List<OrganizationalUnitView> organizationalUnits) {}
}

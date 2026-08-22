package pe.gob.midagri.piip.portfolio.application;

import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.identity.application.LocalAccessContext;
import pe.gob.midagri.piip.identity.domain.RoleCode;
import pe.gob.midagri.piip.portfolio.api.PortfolioDtos.PortfolioRecordResponse;
import pe.gob.midagri.piip.portfolio.domain.PortfolioStatus;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.shared.api.PageResponse;

@Service
public class PortfolioQueryService {
    private final PortfolioRecordRepository records;
    private final PortfolioApplicationSupport support;
    private final PortfolioReadModelAssembler assembler;

    public PortfolioQueryService(PortfolioRecordRepository records, PortfolioApplicationSupport support,
            PortfolioReadModelAssembler assembler) {
        this.records = records;
        this.support = support;
        this.assembler = assembler;
    }

    @Transactional(readOnly = true)
    public PageResponse<PortfolioRecordResponse> list(RecordType type, String query, String status, Long executingUnitId,
            int page, int size, String sort, String direction) {
        LocalAccessContext access = support.authorization().requireAuthenticatedRole();
        Specification<PortfolioRecordEntity> specification = (root, ignored, builder) -> builder.equal(root.get("recordType"), type);
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, ignored, builder) -> builder.or(
                builder.like(builder.lower(root.get("code")), pattern),
                builder.like(builder.lower(root.get("name")), pattern),
                builder.like(builder.lower(root.get("responsible")), pattern)));
        }
        PortfolioStatus parsedStatus = support.parseStatus(status);
        if (parsedStatus != null) specification = specification.and((root, ignored, builder) -> builder.equal(root.get("status"), parsedStatus));
        if (executingUnitId != null) specification = specification.and((root, ignored, builder) -> builder.equal(root.get("executingUnit").get("id"), executingUnitId));
        specification = specification.and(support.scopeSpecification(access));
        String sortProperty = java.util.Set.of("code", "name", "startDate", "updatedAt").contains(sort) ? sort : "updatedAt";
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(sortDirection, sortProperty));
        return PageResponse.from(records.findAll(specification, pageable).map(assembler::toResponse));
    }

    @Transactional(readOnly = true)
    public PortfolioRecordResponse get(String code) {
        return assembler.toResponse(support.readAllowed(records, code));
    }

    @Transactional(readOnly = true)
    public List<PortfolioRecordResponse> eligibleInitiatives() {
        LocalAccessContext actor = support.authorization().require(RoleCode.ADMINISTRADOR_PIIP);
        return records.findByRecordTypeAndStatusOrderByUpdatedAtDesc(RecordType.INITIATIVE, PortfolioStatus.INITIATIVE_APPROVED).stream()
            .filter(record -> actor.coversExecutingUnit(RoleCode.ADMINISTRADOR_PIIP,
                record.getExecutingUnit().getId(), record.getExecutingUnit().getInstitution().getId()))
            .filter(record -> !records.existsByOriginRecordId(record.getId()))
            .map(assembler::toResponse).toList();
    }
}

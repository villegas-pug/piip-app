package pe.gob.midagri.piip.documents.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.midagri.piip.documents.persistence.DocumentEntity;
import pe.gob.midagri.piip.documents.persistence.DocumentRepository;
import pe.gob.midagri.piip.documents.persistence.DocumentTypeRepository;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;
import pe.gob.midagri.piip.shared.application.error.NotFoundException;

@Service
public class PortfolioDocumentService {
    private final PortfolioRecordRepository records;
    private final DocumentRepository documents;
    private final DocumentTypeRepository documentTypes;

    public PortfolioDocumentService(PortfolioRecordRepository records, DocumentRepository documents,
            DocumentTypeRepository documentTypes) {
        this.records = records;
        this.documents = documents;
        this.documentTypes = documentTypes;
    }

    @Transactional
    public void initializeSlots(Long recordId) {
        var record = records.findById(recordId).orElseThrow(() -> new NotFoundException("Registro inexistente"));
        documentTypes.findByActiveTrueOrderByDisplayOrderAscCodeAsc().stream()
            .filter(type -> documents.findByRecordIdAndTypeId(recordId, type.getId()).isEmpty())
            .forEach(type -> documents.save(new DocumentEntity(record, type)));
    }
}

package pe.gob.midagri.piip.documents.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.midagri.piip.documents.persistence.DocumentEntity;
import pe.gob.midagri.piip.documents.persistence.DocumentRepository;
import pe.gob.midagri.piip.documents.persistence.DocumentTypeEntity;
import pe.gob.midagri.piip.documents.persistence.DocumentTypeRepository;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordEntity;
import pe.gob.midagri.piip.portfolio.persistence.PortfolioRecordRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioDocumentServiceTest {
    @Mock PortfolioRecordRepository records;
    @Mock DocumentRepository documents;
    @Mock DocumentTypeRepository documentTypes;

    @Test
    void createsOnlyMissingSlotsForActiveDocumentTypes() {
        PortfolioRecordEntity record = org.mockito.Mockito.mock(PortfolioRecordEntity.class);
        DocumentTypeEntity type = org.mockito.Mockito.mock(DocumentTypeEntity.class);
        when(records.findById(1L)).thenReturn(Optional.of(record));
        when(documentTypes.findByActiveTrueOrderByDisplayOrderAscCodeAsc()).thenReturn(List.of(type));
        when(type.getId()).thenReturn(2L);
        when(documents.findByRecordIdAndTypeId(1L, 2L)).thenReturn(Optional.empty());

        new PortfolioDocumentService(records, documents, documentTypes).initializeSlots(1L);

        verify(documents).save(any(DocumentEntity.class));
    }
}

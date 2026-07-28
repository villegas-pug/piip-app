package pe.gob.midagri.piip.portfolio.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import pe.gob.midagri.piip.portfolio.domain.RecordType;
import pe.gob.midagri.piip.portfolio.persistence.*;

@Service
public class CodeGeneratorService {
    private final CodeCounterRepository counters;
    public CodeGeneratorService(CodeCounterRepository counters) { this.counters = counters; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized String next(RecordType type, int year) {
        CodeCounterEntity counter = counters.findForUpdate(type, year).orElseGet(() -> counters.saveAndFlush(new CodeCounterEntity(type, year)));
        return "%s-%03d-%04d".formatted(type.prefix(), counter.next(), year);
    }
}

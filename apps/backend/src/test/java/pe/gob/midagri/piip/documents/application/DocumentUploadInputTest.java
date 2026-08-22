package pe.gob.midagri.piip.documents.application;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentUploadInputTest {
    @Test
    void defersByteReadingUntilApplicationNeedsIt() {
        AtomicInteger reads = new AtomicInteger();
        DocumentUploadInput input = new DocumentUploadInput("a.pdf", "application/pdf", 1,
            () -> { reads.incrementAndGet(); return new byte[] {1}; });
        assertThat(reads).hasValue(0);
        assertThat(input.bytes().get()).containsExactly((byte) 1);
        assertThat(reads).hasValue(1);
    }
}

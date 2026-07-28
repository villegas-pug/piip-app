package pe.gob.midagri.piip.portfolio.persistence;

import jakarta.persistence.*;
import pe.gob.midagri.piip.portfolio.domain.RecordType;

@Entity
@Table(name = "CONTADOR_CODIGO", uniqueConstraints = @UniqueConstraint(name = "UK_CONTADOR_TIPO_ANIO", columnNames = {"TIPO_REGISTRO", "ANIO"}))
public class CodeCounterEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CONTADOR") private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "TIPO_REGISTRO", length = 20, nullable = false) private RecordType recordType;
    @Column(name = "ANIO", nullable = false) private int year;
    @Column(name = "ULTIMO_NUMERO", nullable = false) private int lastNumber;
    @Version @Column(name = "VERSION", nullable = false) private long version;

    protected CodeCounterEntity() {}
    public CodeCounterEntity(RecordType recordType, int year) { this.recordType = recordType; this.year = year; }
    public int next() { return ++lastNumber; }
}

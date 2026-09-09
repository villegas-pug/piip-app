from pathlib import Path

from reportlab.lib.colors import HexColor, white
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase.pdfmetrics import stringWidth
from reportlab.pdfgen import canvas


OUTPUT = Path("output/pdf/guion-demostracion-en-vivo-piip.pdf")
PAGE_W, PAGE_H = A4
NAVY = HexColor("#103B5C")
BLUE = HexColor("#1E73B7")
TEAL = HexColor("#1CA6A3")
GREEN = HexColor("#4D9B64")
GOLD = HexColor("#E5A642")
INK = HexColor("#173042")
MUTED = HexColor("#587080")
LINE = HexColor("#C9DCE6")
PALE = HexColor("#EEF5F8")
PALE_BLUE = HexColor("#E8F2FA")
PALE_GREEN = HexColor("#EDF7F0")


def rounded(c, x, y, w, h, fill, radius=10, stroke=None):
    c.setFillColor(fill)
    c.setStrokeColor(stroke or fill)
    c.roundRect(x, y, w, h, radius, fill=1, stroke=1 if stroke else 0)


def text(c, value, x, y, size=10, color=INK, font="Helvetica"):
    c.setFillColor(color)
    c.setFont(font, size)
    c.drawString(x, y, value)


def centered(c, value, x, y, size=10, color=INK, font="Helvetica"):
    c.setFillColor(color)
    c.setFont(font, size)
    c.drawCentredString(x, y, value)


def wrap(c, value, font, size, max_width):
    words = value.split()
    lines, current = [], ""
    for word in words:
        candidate = f"{current} {word}".strip()
        if stringWidth(candidate, font, size) <= max_width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def paragraph(c, value, x, y, max_width, size=8, leading=9.4, color=MUTED, font="Helvetica"):
    for line in wrap(c, value, font, size, max_width):
        text(c, line, x, y, size, color, font)
        y -= leading
    return y


def check(c, x, y, label, color):
    c.setFillColor(color)
    c.circle(x, y + 2, 4, fill=1, stroke=0)
    text(c, label, x + 11, y - 1, 7.5, INK)


def step(c, number, title, timing, show, say, x, y, color):
    rounded(c, x, y, 523, 62, white, 9, LINE)
    c.setFillColor(color)
    c.circle(x + 20, y + 41, 13, fill=1, stroke=0)
    centered(c, str(number), x + 20, y + 37, 9.4, white, "Helvetica-Bold")
    text(c, title, x + 43, y + 42, 9.8, INK, "Helvetica-Bold")
    rounded(c, x + 176, y + 35, 45, 15, PALE_BLUE, 8)
    centered(c, timing, x + 198.5, y + 40, 6.8, BLUE, "Helvetica-Bold")
    text(c, "MUESTRA", x + 43, y + 25, 6.5, color, "Helvetica-Bold")
    paragraph(c, show, x + 98, y + 25, 397, 7.4, 8.2, INK)
    text(c, "DI", x + 43, y + 10, 6.5, color, "Helvetica-Bold")
    paragraph(c, f'“{say}”', x + 61, y + 10, 434, 7.4, 8.2, MUTED)


def state_card(c, x, title, accent, lines):
    rounded(c, x, 105, 253, 107, white, 10, LINE)
    text(c, title, x + 15, 193, 8.8, accent, "Helvetica-Bold")
    y = 178
    for label, description in lines:
        text(c, label, x + 15, y, 7.2, INK, "Helvetica-Bold")
        paragraph(c, description, x + 15, y - 10, 221, 6.9, 7.7, MUTED)
        y -= 19


def draw_pdf():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(OUTPUT), pagesize=A4)
    c.setTitle("PIIP - Ruta de demostracion en vivo")
    c.setAuthor("MIDAGRI - PIIP")
    c.setFillColor(HexColor("#F4F9FB"))
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)
    c.setFillColor(NAVY)
    c.rect(0, PAGE_H - 108, PAGE_W, 108, fill=1, stroke=0)
    c.setFillColor(TEAL)
    c.circle(PAGE_W - 30, PAGE_H - 23, 34, fill=1, stroke=0)
    c.setFillColor(BLUE)
    c.circle(PAGE_W - 5, PAGE_H - 76, 25, fill=1, stroke=0)

    rounded(c, 36, PAGE_H - 42, 111, 20, TEAL, 10)
    centered(c, "GUION PERSONAL", 91.5, PAGE_H - 36, 7, white, "Helvetica-Bold")
    text(c, "PIIP", 36, PAGE_H - 73, 25, white, "Helvetica-Bold")
    text(c, "Demostración en vivo - 5 a 7 minutos", 101, PAGE_H - 64, 14, white, "Helvetica-Bold")
    text(c, "Muestra el valor del registro, los expedientes y los estados sin cambiar decisiones.", 36, PAGE_H - 88, 8.2, HexColor("#D7EAF6"))

    rounded(c, 36, 684, 523, 42, PALE, 9, LINE)
    text(c, "PASO 0 - CONTEXTO", 51, 710, 7.4, GOLD, "Helvetica-Bold")
    paragraph(c, "Hoy el portafolio se consolida en un Excel transitorio. PIIP organiza este registro en una sola plataforma para facilitar su consulta y seguimiento.", 51, 697, 486, 7.9, 9.0, INK)

    text(c, "ANTES DE EMPEZAR", 36, 662, 9.7, NAVY, "Helvetica-Bold")
    for index, label in enumerate(("Aplicación abierta", "Sesión iniciada por el expositor", "Iniciativa y proyecto de demostración", "Archivos de prueba sin datos sensibles")):
        check(c, 53 + (index % 2) * 255, 638 - (index // 2) * 18, label, TEAL if index < 2 else BLUE)

    text(c, "RUTA DE DEMOSTRACIÓN", 36, 593, 9.7, NAVY, "Helvetica-Bold")
    text(c, "Muestra una idea por pantalla. Los estados se explican; no se modifican durante la demo.", 36, 581, 7.7, MUTED)

    step(c, 1, "Portafolio y estados", "50 s", "El listado y las etiquetas de iniciativa y proyecto.", "Cada estado indica en qué momento del recorrido se encuentra un registro.", 36, 510, BLUE)
    step(c, 2, "Nueva iniciativa y ficha", "75 s", "El formulario y la carga de la ficha inicial de prueba.", "La ficha sustenta la iniciativa antes de incorporarla al portafolio.", 36, 440, TEAL)
    step(c, 3, "Expediente de iniciativa", "60 s", "El detalle y sus documentos de iniciativa.", "La iniciativa tiene expediente propio para su ficha, evaluación y decisión.", 36, 370, GOLD)
    step(c, 4, "Proyecto y su expediente", "90 s", "Un proyecto existente y su documento de prueba.", "El proyecto tiene código y expediente propios; no copia los documentos de la iniciativa.", 36, 300, GREEN)
    step(c, 5, "Cierre del recorrido", "30 s", "El vínculo entre la iniciativa y el proyecto consultable.", "Ambos registros se relacionan, pero cada uno conserva sus documentos y seguimiento.", 36, 230, NAVY)

    text(c, "ESTADOS EN SIMPLE", 36, 214, 9.5, NAVY, "Helvetica-Bold")
    state_card(c, 36, "INICIATIVA", TEAL, (
        ("Presentado", "Registrada y pendiente de decisión."),
        ("Iniciativa aprobada", "Puede originar un proyecto vinculado."),
        ("No Admisible / Archivada", "Cierran el recorrido de la iniciativa."),
    ))
    state_card(c, 306, "PROYECTO", GREEN, (
        ("Proyecto en ejecución", "Trabajo y seguimiento en curso."),
        ("Producto aprobado", "Resultado aceptado antes del cierre."),
        ("No aprobado / Suspendido", "Requiere continuidad, ajuste o cancelación."),
        ("Cancelado / Finalizado", "Cierran el proyecto."),
    ))

    rounded(c, 36, 51, 523, 35, PALE_BLUE, 9, LINE)
    text(c, "PLAN DE RESPALDO", 51, 70, 7.1, BLUE, "Helvetica-Bold")
    text(c, "Si falla la sesión o conexión, abre la guía conceptual y explica esta misma ruta sin ejecutar acciones.", 51, 58, 7.5, INK)
    text(c, "PIIP - Ruta de demostración en vivo", 36, 20, 7, MUTED)
    text(c, "Uso del expositor", 478, 20, 7, MUTED)
    c.showPage()
    c.save()


if __name__ == "__main__":
    draw_pdf()

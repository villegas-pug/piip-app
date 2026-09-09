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
LIGHT = HexColor("#F8FBFC")
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


def paragraph(c, value, x, y, max_width, size=8.5, leading=10.5, color=MUTED, font="Helvetica"):
    for line in wrap(c, value, font, size, max_width):
        text(c, line, x, y, size, color, font)
        y -= leading
    return y


def check(c, x, y, label, color=TEAL):
    c.setFillColor(color)
    c.circle(x, y + 2, 4.5, fill=1, stroke=0)
    text(c, label, x + 12, y - 1, 8.2, INK)


def step(c, number, title, timing, show, say, x, y, color):
    rounded(c, x, y, 523, 86, white, 10, LINE)
    c.setFillColor(color)
    c.circle(x + 21, y + 60, 14, fill=1, stroke=0)
    centered(c, str(number), x + 21, y + 56, 10, white, "Helvetica-Bold")
    text(c, title, x + 44, y + 62, 11, INK, "Helvetica-Bold")
    rounded(c, x + 157, y + 52, 48, 16, PALE_BLUE, 8)
    centered(c, timing, x + 181, y + 57, 7.1, BLUE, "Helvetica-Bold")
    text(c, "MUESTRA", x + 44, y + 40, 7.1, color, "Helvetica-Bold")
    paragraph(c, show, x + 105, y + 40, 390, 8.0, 9.5, INK)
    text(c, "DI", x + 44, y + 18, 7.1, color, "Helvetica-Bold")
    paragraph(c, f'“{say}”', x + 70, y + 18, 425, 8.0, 9.5, MUTED)


def draw_pdf():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(OUTPUT), pagesize=A4)
    c.setTitle("PIIP - Ruta de demostracion en vivo")
    c.setAuthor("MIDAGRI - PIIP")

    c.setFillColor(HexColor("#F4F9FB"))
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)
    c.setFillColor(NAVY)
    c.rect(0, PAGE_H - 128, PAGE_W, 128, fill=1, stroke=0)
    c.setFillColor(TEAL)
    c.circle(PAGE_W - 28, PAGE_H - 29, 38, fill=1, stroke=0)
    c.setFillColor(BLUE)
    c.circle(PAGE_W - 4, PAGE_H - 89, 27, fill=1, stroke=0)

    rounded(c, 36, PAGE_H - 47, 99, 22, TEAL, 11)
    centered(c, "GUION PERSONAL", 85.5, PAGE_H - 40, 7.3, white, "Helvetica-Bold")
    text(c, "PIIP", 36, PAGE_H - 80, 28, white, "Helvetica-Bold")
    text(c, "Ruta de demostración en vivo", 107, PAGE_H - 69, 15, white, "Helvetica-Bold")
    text(c, "Recorrido mínimo sugerido: 3 minutos", 36, PAGE_H - 96, 9, HexColor("#D7EAF6"))

    rounded(c, 36, 646, 523, 48, PALE, 10, LINE)
    text(c, "PASO 0 - CONTEXTO", 52, 676, 8, GOLD, "Helvetica-Bold")
    paragraph(c, "Hoy el portafolio se consolida en un Excel transitorio. PIIP permite ordenar este registro en una sola plataforma para facilitar su consulta y seguimiento.", 52, 661, 484, 8.5, 10.5, INK)

    text(c, "ANTES DE EMPEZAR", 36, 622, 10.5, NAVY, "Helvetica-Bold")
    checks = [
        "Aplicación abierta",
        "Sesión iniciada por el expositor",
        "Registro de demostración identificado",
        "Datos sin información sensible",
    ]
    for idx, item in enumerate(checks):
        check(c, 53 + (idx % 2) * 250, 596 - (idx // 2) * 20, item, TEAL if idx < 2 else BLUE)

    text(c, "RUTA DE DEMOSTRACIÓN", 36, 548, 10.5, NAVY, "Helvetica-Bold")
    text(c, "Muestra un paso, di una idea y continúa. No expliques campos técnicos.", 36, 534, 8.2, MUTED)

    step(c, 1, "Portafolio", "25 s", "La pantalla de Inicio y el listado de iniciativas o proyectos disponibles.", "Aquí podemos consultar el portafolio institucional de la unidad seleccionada.", 36, 432, BLUE)
    step(c, 2, "Nueva iniciativa", "40 s", "El acceso a Nueva iniciativa, el formulario y la ficha inicial.", "El registro reúne la información necesaria para incorporar una iniciativa.", 36, 334, TEAL)
    step(c, 3, "Revisar y registrar", "40 s", "La revisión final y la confirmación del registro de demostración.", "Al confirmar, la iniciativa se incorpora al portafolio como Presentado.", 36, 236, GOLD)
    step(c, 4, "Consultar el detalle", "25 s", "El detalle de la iniciativa recién incorporada, con código y estado.", "La iniciativa ya quedó incorporada y puede consultarse en el portafolio.", 36, 138, GREEN)

    rounded(c, 36, 72, 253, 48, PALE_BLUE, 10, LINE)
    text(c, "NO FORMA PARTE DE LA DEMO MÍNIMA", 50, 102, 7.4, BLUE, "Helvetica-Bold")
    paragraph(c, "La aprobación y el proyecto vinculado son acciones posteriores y separadas.", 50, 88, 221, 7.7, 9.0, INK)

    rounded(c, 306, 72, 253, 48, PALE_GREEN, 10, LINE)
    text(c, "PLAN DE RESPALDO", 320, 102, 7.4, GREEN, "Helvetica-Bold")
    paragraph(c, "Si falla la sesión o conexión, abre la guía conceptual y explica esta misma ruta sin ejecutar acciones.", 320, 88, 221, 7.7, 9.0, INK)

    text(c, "Cierre sugerido: PIIP permite pasar de un registro transitorio a una gestión más ordenada y consultable.", 36, 45, 8.1, NAVY, "Helvetica-Bold")
    text(c, "PIIP - Ruta de demostración en vivo", 36, 20, 7.1, MUTED)
    text(c, "Uso del expositor", 478, 20, 7.1, MUTED)
    c.showPage()
    c.save()


if __name__ == "__main__":
    draw_pdf()

from pathlib import Path

from reportlab.lib.colors import HexColor, white
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase.pdfmetrics import stringWidth
from reportlab.pdfgen import canvas


OUTPUT = Path("output/pdf/ayuda-memoria-visual-piip-midagri.pdf")
PAGE_W, PAGE_H = A4

NAVY = HexColor("#103B5C")
BLUE = HexColor("#1E73B7")
TEAL = HexColor("#1CA6A3")
GREEN = HexColor("#4D9B64")
GOLD = HexColor("#E5A642")
INK = HexColor("#173042")
MUTED = HexColor("#5E7280")
PALE = HexColor("#EEF5F8")
PALE_BLUE = HexColor("#E8F2FA")
PALE_GREEN = HexColor("#EDF7F0")
LINE = HexColor("#C9DCE6")
LIGHT = HexColor("#F8FBFC")


def rounded(c, x, y, w, h, fill, radius=10, stroke=None):
    c.setFillColor(fill)
    c.setStrokeColor(stroke or fill)
    c.roundRect(x, y, w, h, radius, fill=1, stroke=1 if stroke else 0)


def text(c, value, x, y, size=10, color=INK, font="Helvetica"):
    c.setFont(font, size)
    c.setFillColor(color)
    c.drawString(x, y, value)


def centered(c, value, x, y, size=10, color=INK, font="Helvetica"):
    c.setFont(font, size)
    c.setFillColor(color)
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


def paragraph(c, value, x, y, max_width, size=8.8, leading=11, color=MUTED, font="Helvetica"):
    for line in wrap(c, value, font, size, max_width):
        text(c, line, x, y, size, color, font)
        y -= leading
    return y


def icon_target(c, x, y, color):
    c.setStrokeColor(color)
    c.setLineWidth(1.8)
    for r in (10, 6, 2):
        c.circle(x, y, r, stroke=1, fill=0)


def icon_folder(c, x, y, color):
    c.setStrokeColor(color)
    c.setFillColor(white)
    c.setLineWidth(1.6)
    c.roundRect(x - 11, y - 7, 22, 15, 2, fill=0, stroke=1)
    c.line(x - 10, y + 5, x - 2, y + 5)
    c.line(x - 2, y + 5, x, y + 8)
    c.line(x, y + 8, x + 5, y + 8)


def icon_user(c, x, y, color):
    c.setStrokeColor(color)
    c.setLineWidth(1.7)
    c.circle(x, y + 5, 4, stroke=1, fill=0)
    c.arc(x - 9, y - 11, x + 9, y + 4, 0, 180)


def icon_chart(c, x, y, color):
    c.setStrokeColor(color)
    c.setLineWidth(1.7)
    c.line(x - 10, y - 8, x - 10, y + 10)
    c.line(x - 10, y - 8, x + 11, y - 8)
    c.setFillColor(color)
    c.rect(x - 5, y - 6, 4, 8, fill=1, stroke=0)
    c.rect(x + 2, y - 6, 4, 13, fill=1, stroke=0)
    c.rect(x + 9, y - 6, 4, 18, fill=1, stroke=0)


def card(c, x, y, w, h, title, body, accent, icon):
    rounded(c, x, y, w, h, LIGHT, 11, LINE)
    c.setFillColor(accent)
    c.circle(x + 20, y + h - 20, 13, fill=1, stroke=0)
    icon(c, x + 20, y + h - 20, white)
    text(c, title, x + 40, y + h - 17, 10.5, INK, "Helvetica-Bold")
    paragraph(c, body, x + 15, y + h - 52, w - 30, 8.7, 10.7, MUTED)


def comparison_card(c, x, y, w, h):
    rounded(c, x, y, w, h, LIGHT, 11, LINE)
    c.setFillColor(GOLD)
    c.circle(x + 20, y + h - 20, 13, fill=1, stroke=0)
    icon_chart(c, x + 20, y + h - 20, white)
    text(c, "ANTES Y CON PIIP", x + 40, y + h - 17, 9.7, INK, "Helvetica-Bold")
    text(c, "HOY", x + 15, y + h - 41, 7.2, GOLD, "Helvetica-Bold")
    paragraph(c, "Excel transitorio: cada caso es una fila y los documentos se enlazan externamente.", x + 15, y + h - 52, w - 30, 7.3, 8.5, MUTED)
    text(c, "CON PIIP", x + 15, y + 22, 7.2, TEAL, "Helvetica-Bold")
    paragraph(c, "Registro organizado para consultar y dar seguimiento.", x + 15, y + 12, w - 30, 7.1, 8.2, INK)


def flow_step(c, index, title, caption, y, color):
    x = 53
    c.setFillColor(color)
    c.circle(x, y + 12, 15, fill=1, stroke=0)
    centered(c, str(index), x, y + 8, 10, white, "Helvetica-Bold")
    rounded(c, 78, y - 8, 440, 40, white, 9, LINE)
    text(c, title, 91, y + 13, 9.6, INK, "Helvetica-Bold")
    text(c, caption, 91, y, 8.1, MUTED)
    if index < 5:
        c.setStrokeColor(LINE)
        c.setLineWidth(2)
        c.line(x, y - 8, x, y - 22)
        c.setFillColor(LINE)
        c.setStrokeColor(LINE)
        c.line(x - 3, y - 18, x, y - 22)
        c.line(x + 3, y - 18, x, y - 22)


def draw_pdf():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(OUTPUT), pagesize=A4)
    c.setTitle("Ayuda memoria visual PIIP - MIDAGRI")
    c.setAuthor("MIDAGRI - PIIP")

    c.setFillColor(HexColor("#F4F9FB"))
    c.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)

    c.setFillColor(NAVY)
    c.rect(0, PAGE_H - 110, PAGE_W, 110, fill=1, stroke=0)
    c.setFillColor(TEAL)
    c.circle(PAGE_W - 38, PAGE_H - 27, 36, fill=1, stroke=0)
    c.setFillColor(BLUE)
    c.circle(PAGE_W - 3, PAGE_H - 69, 28, fill=1, stroke=0)

    rounded(c, 36, PAGE_H - 49, 67, 22, TEAL, 11)
    centered(c, "GUÍA RÁPIDA", 69.5, PAGE_H - 42, 7.5, white, "Helvetica-Bold")
    text(c, "PIIP", 36, PAGE_H - 78, 29, white, "Helvetica-Bold")
    text(c, "Portafolio Institucional de MIDAGRI", 106, PAGE_H - 68, 15, white, "Helvetica-Bold")
    text(c, "Registra y organiza iniciativas y proyectos; las decisiones las toman las personas.", 36, PAGE_H - 94, 9.0, HexColor("#D7EAF6"))

    card(c, 36, 600, 169, 97, "¿Qué es?", "Una plataforma que reúne la información de iniciativas y proyectos del portafolio institucional.", BLUE, icon_folder)
    card(c, 220, 600, 169, 97, "¿Para qué sirve?", "Para contar con información ordenada, consultable y útil para dar seguimiento.", TEAL, icon_target)
    comparison_card(c, 404, 600, 155, 97)

    text(c, "¿QUÉ PUEDE HACER LA PERSONA USUARIA?", 36, 575, 10.5, NAVY, "Helvetica-Bold")
    actions = [
        ("Registrar", "iniciativas y proyectos"),
        ("Cargar", "la información y ficha inicial"),
        ("Consultar", "el portafolio y sus registros"),
        ("Dar seguimiento", "a decisiones, tareas y avance"),
    ]
    x = 36
    colors = [BLUE, TEAL, GREEN, GOLD]
    for i, (verb, detail) in enumerate(actions):
        rounded(c, x, 530, 125, 32, PALE_BLUE if i % 2 == 0 else PALE_GREEN, 8)
        c.setFillColor(colors[i])
        c.circle(x + 14, 546, 5, fill=1, stroke=0)
        text(c, verb, x + 26, 548, 8.3, INK, "Helvetica-Bold")
        text(c, detail, x + 26, 537, 7.2, MUTED)
        x += 133

    text(c, "FLUJO GENERAL DE USO", 36, 502, 10.5, NAVY, "Helvetica-Bold")
    text(c, "Cada paso tiene un propósito claro. La aprobación y el proyecto vinculado son acciones separadas.", 36, 488, 8.2, MUTED)
    steps = [
        ("Registrar iniciativa", "Completar la información y adjuntar la ficha inicial.", BLUE),
        ("Queda presentada", "La iniciativa se incorpora al portafolio con su propio código.", TEAL),
        ("Registrar decisión", "La decisión se registra de forma separada.", GOLD),
        ("Iniciativa aprobada", "La iniciativa queda lista para originar un proyecto, si corresponde.", GREEN),
        ("Crear proyecto vinculado", "Se registra por separado, con código y expediente propios.", NAVY),
    ]
    y = 435
    for idx, (title, caption, color) in enumerate(steps, start=1):
        flow_step(c, idx, title, caption, y, color)
        y -= 51

    rounded(c, 36, 143, 255, 81, PALE, 12, LINE)
    text(c, "EJEMPLO SIMPLE", 51, 204, 10.2, NAVY, "Helvetica-Bold")
    paragraph(c, "María registra una iniciativa para mejorar un servicio. Adjunta la ficha y esta aparece como Presentado. Luego se registra la decisión. Si se aprueba, María puede crear un proyecto vinculado. Ambos registros quedan organizados y pueden consultarse en el portafolio.", 51, 189, 224, 8.5, 10.5, INK)

    rounded(c, 306, 143, 253, 81, NAVY, 12)
    text(c, "BENEFICIOS PARA MIDAGRI", 321, 204, 10.2, white, "Helvetica-Bold")
    benefits = ["Información en un solo lugar", "Consulta y seguimiento más simples", "Mejor trazabilidad de las decisiones"]
    yy = 188
    for benefit in benefits:
        c.setFillColor(TEAL)
        c.circle(325, yy + 2, 3, fill=1, stroke=0)
        text(c, benefit, 335, yy - 1, 8.4, white)
        yy -= 15

    rounded(c, 36, 42, 523, 82, white, 12, LINE)
    c.setFillColor(BLUE)
    c.circle(57, 99, 12, fill=1, stroke=0)
    icon_user(c, 57, 96, white)
    text(c, "CÓMO EXPLICARLO EN 1 MINUTO", 78, 102, 10.4, NAVY, "Helvetica-Bold")
    speech = ("PIIP es el Portafolio Institucional de MIDAGRI. Nos ayuda a reunir en un solo lugar "
              "la información de iniciativas y proyectos, para que sea más fácil registrarla, consultarla y darle seguimiento. "
              "Una persona registra una iniciativa y esta queda presentada en el portafolio. Después se registra la decisión. "
              "Si la iniciativa es aprobada y corresponde continuar, se crea un proyecto vinculado, pero como un registro separado. "
              "Así, MIDAGRI cuenta con información más ordenada, trazable y disponible para el seguimiento.")
    paragraph(c, speech, 78, 84, 463, 8.2, 10.0, INK)

    text(c, "PIIP - Portafolio Institucional de MIDAGRI", 36, 20, 7.2, MUTED)
    text(c, "Ayuda memoria para exposición", 430, 20, 7.2, MUTED)
    c.showPage()
    c.save()


if __name__ == "__main__":
    draw_pdf()

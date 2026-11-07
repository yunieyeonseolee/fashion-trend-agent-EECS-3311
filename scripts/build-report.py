#!/usr/bin/env python3
"""Render the reviewed Markdown report and committed diagrams to stage2-report.pdf.
Install documentation-only dependencies from scripts/report-requirements.txt first.
"""
from pathlib import Path
from html import escape
import re
import xml.etree.ElementTree as ET
import markdown
from PIL import Image as PILImage
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Image, LongTable, TableStyle, Preformatted

ROOT=Path(__file__).resolve().parents[1]
BASE='https://github.com/yunieyeonseolee/fashion-trend-agent-EECS-3311/blob/main/'
font_pairs=[
    (Path('/System/Library/Fonts/Supplemental/Arial.ttf'),Path('/System/Library/Fonts/Supplemental/Arial Bold.ttf')),
    (Path('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'),Path('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf')),
    (Path('C:/Windows/Fonts/arial.ttf'),Path('C:/Windows/Fonts/arialbd.ttf')),
]
font='Helvetica'
for regular,bold in font_pairs:
    if regular.exists() and bold.exists():
        pdfmetrics.registerFont(TTFont('Report',str(regular)))
        pdfmetrics.registerFont(TTFont('Report-Bold',str(bold)))
        pdfmetrics.registerFontFamily('Report',normal='Report',bold='Report-Bold',italic='Report',boldItalic='Report-Bold')
        font='Report';break
styles=getSampleStyleSheet()
styles.add(ParagraphStyle(name='Body',fontName=font,fontSize=9.5,leading=13.5,spaceAfter=8,splitLongWords=True))
styles.add(ParagraphStyle(name='Cell',parent=styles['Body'],fontSize=8,leading=11,spaceAfter=2))
styles.add(ParagraphStyle(name='Caption',parent=styles['Body'],fontSize=8.5,textColor=colors.HexColor('#5a5268')))
for name,size in [('Title',23),('Heading1',17),('Heading2',12)]:
    styles[name].fontName=font+'-Bold' if font=='Report' else 'Helvetica-Bold'
    styles[name].fontSize=size;styles[name].leading=size+5
    styles[name].textColor=colors.HexColor('#382653')
    styles[name].spaceBefore=12;styles[name].spaceAfter=9
page_width,page_height=A4
margin=42
width=page_width-2*margin
story=[]

def inline(node):
    result=escape(node.text or '')
    for child in node:
        inner=inline(child)
        if child.tag in ('strong','b'):result+='<b>'+inner+'</b>'
        elif child.tag in ('em','i'):result+='<i>'+inner+'</i>'
        elif child.tag=='code':result+='<font name="Courier">'+inner+'</font>'
        elif child.tag=='a':
            target=child.get('href','')
            if not target.startswith(('https://','http://','#')):target=BASE+target
            result+='<link href="'+escape(target,quote=True)+'" color="#513c86">'+inner+'</link>'
        elif child.tag=='br':result+='<br/>'
        else:result+=inner
        result+=escape(child.tail or '')
    return result

def add(node):
    if node.tag=='h1':story.append(Paragraph(inline(node),styles['Title']))
    elif node.tag=='h2':
        if not ''.join(node.itertext()).startswith('1.'):story.append(PageBreak())
        story.append(Paragraph(inline(node),styles['Heading1']))
    elif node.tag=='h3':
        if re.match(r'A[2-9]\d*\.',node.text or ''):story.append(PageBreak())
        story.append(Paragraph(inline(node),styles['Heading2']))
    elif node.tag=='p' and node.find('img') is not None:
        image=node.find('img');path=ROOT/image.get('src')
        w,h=PILImage.open(path).size
        scale=min(width/w,(page_height-150)/h)
        story.append(Image(str(path),width=w*scale,height=h*scale))
        story.append(Spacer(1,8));story.append(Paragraph(escape(image.get('alt','')),styles['Caption']))
    elif node.tag=='p':story.append(Paragraph(inline(node),styles['Body']))
    elif node.tag in ('ul','ol'):
        for i,item in enumerate(node.findall('li'),1):
            prefix=f'{i}. ' if node.tag=='ol' else '• '
            story.append(Paragraph(prefix+inline(item),styles['Body']))
    elif node.tag=='table':
        rows=[]
        for row in node.findall('.//tr'):
            rows.append([Paragraph(inline(cell),styles['Cell']) for cell in row])
        count=len(rows[0])
        ratios={3:[.25,.30,.45],4:[.48,.18,.12,.22]}.get(count,[1/count]*count)
        table=LongTable(rows,colWidths=[width*r for r in ratios],repeatRows=1,hAlign='LEFT')
        table.setStyle(TableStyle([
            ('BACKGROUND',(0,0),(-1,0),colors.HexColor('#eae4f2')),
            ('VALIGN',(0,0),(-1,-1),'TOP'),
            ('GRID',(0,0),(-1,-1),.4,colors.HexColor('#d5d0df')),
            ('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),
            ('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6),
        ]))
        story.append(table);story.append(Spacer(1,10))
    elif node.tag=='pre':story.append(Preformatted(''.join(node.itertext()),styles['Code']))
    else:
        for child in node:add(child)

def footer(canvas,doc):
    canvas.saveState();canvas.setFont(font,8);canvas.setFillColor(colors.HexColor('#6d647a'))
    canvas.drawString(margin,24,'EECS 3311 · Stage 2 · Review draft · November 7, 2026')
    canvas.drawRightString(page_width-margin,24,str(doc.page));canvas.restoreState()

html=markdown.markdown((ROOT/'stage2-report.md').read_text(),extensions=['tables','fenced_code'])
root=ET.fromstring('<root>'+html+'</root>')
for element in root:add(element)
doc=SimpleDocTemplate(str(ROOT/'stage2-report.pdf'),pagesize=A4,rightMargin=margin,leftMargin=margin,
                      topMargin=38,bottomMargin=42,title='Stage 2 Implementation Report — Review Draft',author='Yeonseo Lee')
doc.build(story,onFirstPage=footer,onLaterPages=footer)
print('Generated stage2-report.pdf')

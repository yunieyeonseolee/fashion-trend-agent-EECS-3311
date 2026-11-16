#!/usr/bin/env python3
"""Build the submission PDF from the current Word report, preserving text, links and figures.
Uses the documentation dependencies in scripts/report-requirements.txt.
"""
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
from html import escape
import xml.etree.ElementTree as ET
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Image, LongTable, TableStyle

ROOT=Path(__file__).resolve().parents[1]
NS={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main',
    'r':'http://schemas.openxmlformats.org/officeDocument/2006/relationships',
    'a':'http://schemas.openxmlformats.org/drawingml/2006/main'}
W='{'+NS['w']+'}'
R='{'+NS['r']+'}'
font='Helvetica'
for base,regular,bold in [('/System/Library/Fonts/Supplemental','Arial.ttf','Arial Bold.ttf'),('/usr/share/fonts/truetype/dejavu','DejaVuSans.ttf','DejaVuSans-Bold.ttf')]:
    if (Path(base)/regular).exists():
        pdfmetrics.registerFont(TTFont('Report',str(Path(base)/regular)))
        pdfmetrics.registerFont(TTFont('Report-Bold',str(Path(base)/bold)))
        pdfmetrics.registerFontFamily('Report',normal='Report',bold='Report-Bold',italic='Report',boldItalic='Report-Bold')
        font='Report';break
bold=font+'-Bold'
styles={
    'body':ParagraphStyle('body',fontName=font,fontSize=9.5,leading=13.5,spaceAfter=7),
    'cell':ParagraphStyle('cell',fontName=font,fontSize=8,leading=11,spaceAfter=3),
    'title':ParagraphStyle('title',fontName=bold,fontSize=22,leading=28,spaceAfter=16),
    'heading':ParagraphStyle('heading',fontName=bold,fontSize=16,leading=21,spaceBefore=15,spaceAfter=9,keepWithNext=True),
    'subheading':ParagraphStyle('subheading',fontName=bold,fontSize=12,leading=17,spaceBefore=12,spaceAfter=7,keepWithNext=True),
}
width=A4[0]-84
story=[]
with ZipFile(ROOT/'stage2-report.docx') as archive:
    doc=ET.fromstring(archive.read('word/document.xml'))
    rels={r.attrib['Id']:r.attrib['Target'] for r in ET.fromstring(archive.read('word/_rels/document.xml.rels'))}
    def inline(node):
        out=''
        for child in node:
            tag=child.tag.split('}')[-1]
            if tag=='t':out+=escape(child.text or '')
            elif tag=='tab':out+='    '
            elif tag in ('br','cr'):out+='<br/>'
            elif tag=='hyperlink':
                value=inline(child);url=rels.get(child.get(R+'id'),'')
                out+=f'<link href="{escape(url,quote=True)}" color="#513c86">{value}</link>' if url else value
            elif tag=='r':
                value=inline(child)
                if child.find('w:rPr/w:b',NS) is not None:value='<b>'+value+'</b>'
                out+=value
            elif tag not in ('pPr','rPr','drawing','pict'):out+=inline(child)
        return out
    def paragraph(p,cell=False):
        elements=[]
        text=inline(p)
        if text.strip():
            style=p.find('w:pPr/w:pStyle',NS)
            name=style.get(W+'val','') if style is not None else ''
            key='cell' if cell else 'title' if name=='Title' else 'heading' if name=='Heading1' else 'subheading' if name in ('Heading2','Heading3') else 'body'
            if not cell and p.find('w:pPr/w:pageBreakBefore',NS) is not None:elements.append(PageBreak())
            if p.find('w:pPr/w:numPr',NS) is not None:text='• '+text
            elements.append(Paragraph(text,styles[key]))
        for image in p.findall('.//a:blip',NS):
            target=rels[image.get(R+'embed')]
            item=Image(BytesIO(archive.read('word/'+target)))
            scale=min(width/item.imageWidth,590/item.imageHeight)
            item.drawWidth=item.imageWidth*scale;item.drawHeight=item.imageHeight*scale
            elements.extend([item,Spacer(1,8)])
        return elements
    for element in doc.find('w:body',NS):
        if element.tag==W+'p':story.extend(paragraph(element))
        elif element.tag==W+'tbl':
            rows=[]
            for row in element.findall('w:tr',NS):
                rows.append([sum((paragraph(p,True) for p in c.findall('w:p',NS)),[]) or '' for c in row.findall('w:tc',NS)])
            count=len(rows[0]);ratios={2:[.27,.73],3:[.25,.36,.39],4:[.28,.25,.24,.23],5:[.13,.13,.33,.23,.18]}.get(count,[1/count]*count)
            table=LongTable(rows,colWidths=[width*r for r in ratios],repeatRows=1,hAlign='LEFT')
            table.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#eae4f2')),('VALIGN',(0,0),(-1,-1),'TOP'),('GRID',(0,0),(-1,-1),.4,colors.HexColor('#d5d0df')),('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6)]))
            story.extend([table,Spacer(1,10)])
def footer(canvas,document):
    canvas.saveState();canvas.setFont(font,8);canvas.setFillColor(colors.HexColor('#6d647a'))
    canvas.drawString(42,24,'EECS 3311 · Stage 2 Implementation Report')
    canvas.drawRightString(A4[0]-42,24,str(document.page));canvas.restoreState()
SimpleDocTemplate(str(ROOT/'stage2-report.pdf'),pagesize=A4,leftMargin=42,rightMargin=42,topMargin=38,bottomMargin=42,
    title='Stage 2 Implementation Report',author='Yeonseo Lee').build(story,onFirstPage=footer,onLaterPages=footer)
print('Generated stage2-report.pdf from stage2-report.docx')

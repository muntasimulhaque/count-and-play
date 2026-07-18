from PIL import Image, ImageDraw, ImageFont
import math

EMO_FP="/tmp/NotoColorEmoji.ttf"
SANS="/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
EMO=ImageFont.truetype(EMO_FP,109)

def hx(s):
    s=s.lstrip("#")
    if len(s)==8:  # AARRGGBB
        a=int(s[0:2],16); r=int(s[2:4],16); g=int(s[4:6],16); b=int(s[6:8],16); return (r,g,b,a)
    r=int(s[0:2],16); g=int(s[2:4],16); b=int(s[4:6],16); return (r,g,b,255)

P=dict(
 TextBlue=hx("2A5D8F"), Q=hx("E67E22"), Ghost=hx("B8CFE0"),
 SkyTop=hx("AEE7FF"), SkyMid=hx("E8F9FF"), Mint=hx("D4F5D4"),
 OverlayTop=hx("7EC8E3"), OverlayBottom=hx("A8E6CF"),
 PlateABorder=hx("8ECAE6"), PlateBBorder=hx("F4A261"), GoneBorder=hx("BBBBBB"), SlotBorder=hx("E07A5F"),
 ZoneBg=hx("8CFFFFFF"), GoneBg=hx("40C8C8C8"),
 ChromeBtnBg=hx("F2FFFFFF"), ChromeBtnBorder=hx("8ECAE6"),
 Green=hx("7DDF7D"), GreenShadow=hx("56B856"),
 OrangeBtn=hx("F4A261"), OrangeBtnShadow=hx("D07F3F"),
 Blue=hx("8AB6F9"), BlueShadow=hx("5A8FE0"),
 Purple=hx("C8A2FF"), PurpleShadow=hx("A276E0"),
 Yellow=hx("FFB703"), YellowShadow=hx("D99502"),
 PromptGrey=hx("444444"), LabelGrey=hx("999999"), WrongPink=hx("FFD6D6"),
 SlotBg=hx("80FFFFFF"),
)

W,H=1080,1920

_emocache={}
def emoji(ch, px):
    key=(ch,px)
    if key in _emocache: return _emocache[key]
    n=len(ch)
    tmp=Image.new("RGBA",(160*max(1,n)+40,180),(0,0,0,0))
    d=ImageDraw.Draw(tmp)
    d.text((10,10),ch,font=EMO,embedded_color=True)
    bb=tmp.getbbox()
    g=tmp.crop(bb)
    w,h=g.size
    nw=max(1,int(round(w*px/h)))
    g=g.resize((nw,px),Image.LANCZOS)
    _emocache[key]=g
    return g

def bg_gradient(img, stops):
    # stops: list of (pos0-1, color)
    d=ImageDraw.Draw(img)
    for y in range(H):
        t=y/(H-1)
        # find segment
        for i in range(len(stops)-1):
            p0,c0=stops[i]; p1,c1=stops[i+1]
            if p0<=t<=p1:
                f=(t-p0)/(p1-p0) if p1>p0 else 0
                r=int(c0[0]+(c1[0]-c0[0])*f); g=int(c0[1]+(c1[1]-c0[1])*f); b=int(c0[2]+(c1[2]-c0[2])*f)
                d.line([(0,y),(W,y)],fill=(r,g,b)); break

def sans(px): return ImageFont.truetype(SANS,px)

def ctext(d, cx, y, text, font, fill, anchor="mm"):
    d.text((cx,y),text,font=font,fill=fill,anchor=anchor)

def paste_center(base, im, cx, cy):
    base.alpha_composite(im,(int(cx-im.width/2),int(cy-im.height/2)))

def chunky(base, x, y, w, h, bg, shadow, text=None, font=None, textcolor=(255,255,255,255), radius=40, emoji_lead=None):
    d=ImageDraw.Draw(base)
    sh=5
    d.rounded_rectangle([x,y,x+w,y+h],radius=radius,fill=shadow)
    d.rounded_rectangle([x,y,x+w,y+h-sh],radius=radius,fill=bg)
    cx=x+w/2; cy=y+(h-sh)/2
    if emoji_lead and text:
        em=emoji(emoji_lead,int(h*0.42))
        tb=d.textbbox((0,0),text,font=font); tw=tb[2]-tb[0]
        gap=int(h*0.12)
        total=em.width+gap+tw
        sx=cx-total/2
        paste_center(base,em,sx+em.width/2,cy)
        d.text((sx+em.width+gap,cy),text,font=font,fill=textcolor,anchor="lm")
    elif text:
        d.text((cx,cy),text,font=font,fill=textcolor,anchor="mm")
    elif emoji_lead:
        em=emoji(emoji_lead,int(h*0.5)); paste_center(base,em,cx,cy)

def dashed_round(base, x, y, w, h, color, radius=22, width=4, dash=18, gap=12, fill=None):
    d=ImageDraw.Draw(base)
    if fill is not None:
        d.rounded_rectangle([x,y,x+w,y+h],radius=radius,fill=fill)
    # straight edges dashed; corners solid arcs
    def dash_line(x0,y0,x1,y1):
        L=math.hypot(x1-x0,y1-y0); 
        if L==0: return
        ux=(x1-x0)/L; uy=(y1-y0)/L; pos=0
        while pos<L:
            e=min(pos+dash,L)
            d.line([(x0+ux*pos,y0+uy*pos),(x0+ux*e,y0+uy*e)],fill=color,width=width)
            pos+=dash+gap
    r=radius
    dash_line(x+r,y,x+w-r,y)           # top
    dash_line(x+r,y+h,x+w-r,y+h)       # bottom
    dash_line(x,y+r,x,y+h-r)           # left
    dash_line(x+w,y+r,x+w,y+h-r)       # right
    # corner arcs
    d.arc([x,y,x+2*r,y+2*r],180,270,fill=color,width=width)
    d.arc([x+w-2*r,y,x+w,y+2*r],270,360,fill=color,width=width)
    d.arc([x,y+h-2*r,x+2*r,y+h],90,180,fill=color,width=width)
    d.arc([x+w-2*r,y+h-2*r,x+w,y+h],0,90,fill=color,width=width)

def dashed_circle(base,cx,cy,rad,color,width=3,dash=10,gap=8,fill=None):
    d=ImageDraw.Draw(base)
    if fill is not None:
        d.ellipse([cx-rad,cy-rad,cx+rad,cy+rad],fill=fill)
    circ=2*math.pi*rad; step=(dash+gap); n=int(circ/step)
    da=dash/rad
    a=0
    while a<2*math.pi:
        d.arc([cx-rad,cy-rad,cx+rad,cy+rad],math.degrees(a),math.degrees(a+da),fill=color,width=width)
        a+=(dash+gap)/rad

def flow_emoji(base, items, box, item_px, gap=22):
    # items: list of emoji chars; box=(x,y,w,h); center a wrapped flow
    x,y,w,h=box
    rows=[]; cur=[]; cw=0
    widths=[emoji(it,item_px).width for it in items]
    for it,iw in zip(items,widths):
        if cur and cw+gap+iw>w:
            rows.append((cur,cw)); cur=[]; cw=0
        cur.append((it,iw)); cw+= (iw if not cur[:-1] else 0)
        if len(cur)>1: cw=sum(a[1] for a in cur)+gap*(len(cur)-1)
        else: cw=iw
    if cur: rows.append((cur,cw))
    rh=item_px+gap
    total_h=len(rows)*item_px+gap*(len(rows)-1)
    cy=y+(h-total_h)/2+item_px/2
    for row,rw in rows:
        cx=x+(w-rw)/2
        for it,iw in row:
            em=emoji(it,item_px); paste_center(base,em,cx+iw/2,cy)
            cx+=iw+gap
        cy+=item_px+gap

def header(base, mute=False, stars=0):
    d=ImageDraw.Draw(base)
    top=70
    dia=150
    # home
    for cx,icon in [(40+dia/2,"🏠"),(W-40-dia/2,("🔇" if mute else "🔊"))]:
        d.ellipse([cx-dia/2,top,cx+dia/2,top+dia],fill=P["ChromeBtnBg"])
        d.ellipse([cx-dia/2,top,cx+dia/2,top+dia],outline=P["ChromeBtnBorder"],width=5)
        em=emoji(icon,int(dia*0.5)); paste_center(base,em,cx,top+dia/2)
    if stars:
        st=emoji("⭐",70)
        sx=W/2-(stars*80)/2
        for i in range(stars):
            paste_center(base,st,sx+i*80+40,top+dia/2)
    return top+dia

def equation(base,y,parts):
    # parts: list of (text,color)
    d=ImageDraw.Draw(base); f=sans(96)
    widths=[d.textbbox((0,0),t,font=f)[2] for t,_ in parts]
    total=sum(widths); x=W/2-total/2
    for (t,c),wq in zip(parts,widths):
        d.text((x,y),t,font=f,fill=c,anchor="lm"); x+=wq
    return y

def prompt(base,y,text,color=None):
    d=ImageDraw.Draw(base); f=sans(52)
    d.text((W/2,y),text,font=f,fill=color or P["PromptGrey"],anchor="mm")

def save(base,name):
    base.convert("RGB").save(f"/tmp/shots/{name}",quality=95)
    print("saved",name)

# ---------------- Screen 1: Menu / title ----------------
def screen_menu():
    img=Image.new("RGBA",(W,H))
    bg_gradient(img,[(0,P["OverlayTop"]),(1,P["OverlayBottom"])])
    base=img
    d=ImageDraw.Draw(base)
    # three fruit
    row=["🍎","🍊","🥭"]; px=190; gap=40
    ws=[emoji(c,px).width for c in row]; tot=sum(ws)+gap*2; x=W/2-tot/2
    cy=430
    for c,w in zip(row,ws):
        paste_center(base,emoji(c,px),x+w/2,cy); x+=w+gap
    d.text((W/2,640),"Count & Play",font=sans(120),fill=(255,255,255,255),anchor="mm")
    # learn button (pill)
    bw,bh=640,180
    chunky(base,W/2-bw/2,820,bw,bh,P["Green"],P["GreenShadow"],text="Learn",font=sans(76),radius=90,emoji_lead="🧺")
    d.text((W/2,1050),"pick numbers, watch and do",font=sans(40),fill=(255,255,255,235),anchor="mm")
    chunky(base,W/2-bw/2,1140,bw,bh,P["Blue"],P["BlueShadow"],text="Quiz",font=sans(76),radius=90,emoji_lead="⭐")
    d.text((W/2,1370),"watch, count, answer",font=sans(40),fill=(255,255,255,235),anchor="mm")
    save(base,"01_menu.png")

# ---------------- Screen 2: Learn addition ----------------
def screen_add():
    img=Image.new("RGBA",(W,H)); bg_gradient(img,[(0,P["SkyTop"]),(0.6,P["SkyMid"]),(1,P["Mint"])]); base=img
    hb=header(base)
    equation(base,hb+90,[("4 + 3 = ",P["TextBlue"]),("?",P["Q"])])
    top=hb+180
    # two plates stacked
    mx=40; pw=W-2*mx; ph=560; gap=30
    dashed_round(base,mx,top,pw,ph,P["PlateABorder"],radius=30,width=5,fill=P["ZoneBg"])
    flow_emoji(base,["🍎"]*4,(mx+30,top+30,pw-60,ph-60),190,gap=40)
    top2=top+ph+gap
    dashed_round(base,mx,top2,pw,ph,P["PlateBBorder"],radius=30,width=5,fill=P["ZoneBg"])
    flow_emoji(base,["🍊"]*3,(mx+30,top2+30,pw-60,ph-60),190,gap=40)
    prompt(base,top2+ph+90,"Tap to put them together!")
    # bottom bar big button
    bw,bh=740,190
    chunky(base,W/2-bw/2,H-260,bw,bh,P["Green"],P["GreenShadow"],text="Put together!",font=sans(64),radius=44,emoji_lead="🧺")
    save(base,"02_learn_add.png")

# ---------------- Screen 3: Quiz ----------------
def screen_quiz():
    img=Image.new("RGBA",(W,H)); bg_gradient(img,[(0,P["SkyTop"]),(0.6,P["SkyMid"]),(1,P["Mint"])]); base=img
    hb=header(base,stars=3)
    equation(base,hb+90,[("5 + 3 = ",P["TextBlue"]),("?",P["Q"])])
    top=hb+180
    mx=40; pw=W-2*mx; ph=760
    dashed_round(base,mx,top,pw,ph,P["PlateABorder"],radius=30,width=5,fill=P["ZoneBg"])
    flow_emoji(base,["🎈"]*8,(mx+30,top+30,pw-60,ph-60),170,gap=36)
    prompt(base,top+ph+80,"How many altogether?")
    # three answer buttons
    bw=300; bh=300; gap=40; total=3*bw+2*gap; x=W/2-total/2; by=H-420
    for i,(n,correct) in enumerate([("7",False),("8",True),("9",False)]):
        bg=P["Green"] if correct else (255,255,255,255)
        sh=P["GreenShadow"] if correct else hx("C9DCE8")
        tc=(255,255,255,255) if correct else P["TextBlue"]
        chunky(base,x+i*(bw+gap),by,bw,bh,bg,sh,text=n,font=sans(150),textcolor=tc,radius=44)
    save(base,"03_quiz.png")

# ---------------- Screen 4: Subtraction take-away ----------------
def screen_sub():
    img=Image.new("RGBA",(W,H)); bg_gradient(img,[(0,P["SkyTop"]),(0.6,P["SkyMid"]),(1,P["Mint"])]); base=img
    hb=header(base)
    equation(base,hb+90,[("6 − 2 = ",P["TextBlue"]),("?",P["Q"])])
    top=hb+180
    mx=40; pw=W-2*mx
    ph=620
    dashed_round(base,mx,top,pw,ph,P["PlateABorder"],radius=30,width=5,fill=P["ZoneBg"])
    flow_emoji(base,["🍓"]*4,(mx+30,top+30,pw-60,ph-60),185,gap=40)
    # gone zone
    gy=top+ph+30; gh=360
    dashed_round(base,mx,gy,pw,gh,P["GoneBorder"],radius=30,width=5,fill=P["GoneBg"])
    d=ImageDraw.Draw(base)
    d.text((W/2,gy+55),"take away 2",font=sans(40),fill=P["LabelGrey"],anchor="mm")
    # two slots with ghost strawberries
    slot=190; sgap=60; tot=2*slot+sgap; sx=W/2-tot/2; syc=gy+gh/2+30
    for i in range(2):
        cx=sx+i*(slot+sgap)+slot/2
        dashed_circle(base,cx,syc,slot/2,P["SlotBorder"],width=4,fill=P["SlotBg"])
        gh_em=emoji("🍓",int(slot*0.8)).copy()
        gh_em.putalpha(gh_em.getchannel("A").point(lambda a:int(a*0.4)))
        paste_center(base,gh_em,cx,syc)
    prompt(base,H-150,"6 take away 2 leaves 4!")
    save(base,"04_learn_sub.png")

screen_menu(); screen_add(); screen_quiz(); screen_sub()
print("ALL DONE")

from PIL import Image, ImageDraw, ImageFont


def load_fonts():
    candidates = [
        "/System/Library/Fonts/PingFang.ttc",
        "/System/Library/Fonts/STHeiti Light.ttc",
        "/System/Library/Fonts/STHeiti Medium.ttc",
        "/Library/Fonts/Arial Unicode.ttf",
    ]
    for path in candidates:
        try:
            return (
                ImageFont.truetype(path, 32),
                ImageFont.truetype(path, 24),
                ImageFont.truetype(path, 22),
            )
        except Exception:
            continue
    fallback = ImageFont.load_default()
    return fallback, fallback, fallback


def text_center(draw, box, text, font):
    x0, y0, x1, y1 = box
    bbox = draw.textbbox((0, 0), text, font=font)
    w = bbox[2] - bbox[0]
    h = bbox[3] - bbox[1]
    return (x0 + x1) / 2 - w / 2, (y0 + y1) / 2 - h / 2


def mid_of(box):
    x0, y0, x1, y1 = box
    return (x0 + x1) / 2, (y0 + y1) / 2


def draw_rect(draw, name, cx, cy, font, w=220, h=90):
    x0, y0 = cx - w / 2, cy - h / 2
    x1, y1 = cx + w / 2, cy + h / 2
    draw.rectangle([x0, y0, x1, y1], outline="black", width=3, fill="white")
    draw.text(text_center(draw, (x0, y0, x1, y1), name, font), name, fill="black", font=font)
    return (x0, y0, x1, y1)


def draw_diamond(draw, name, cx, cy, font, w=170, h=110):
    pts = [(cx, cy - h / 2), (cx + w / 2, cy), (cx, cy + h / 2), (cx - w / 2, cy)]
    draw.polygon(pts, outline="black", width=3, fill="white")
    box = (cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
    draw.text(text_center(draw, box, name, font), name, fill="black", font=font)
    return box


def connect(draw, a_box, b_box, tiny_font, label=None, a_card=None, b_card=None, label_offset=(0, 0)):
    ax, ay = mid_of(a_box)
    bx, by = mid_of(b_box)
    draw.line([ax, ay, bx, by], fill="black", width=3)

    if label:
        mx, my = (ax + bx) / 2 + label_offset[0], (ay + by) / 2 + label_offset[1]
        tbox = draw.textbbox((0, 0), label, font=tiny_font)
        tw = tbox[2] - tbox[0]
        th = tbox[3] - tbox[1]
        draw.rectangle([mx - tw / 2 - 6, my - th / 2 - 4, mx + tw / 2 + 6, my + th / 2 + 4], fill="white")
        draw.text((mx - tw / 2, my - th / 2), label, fill="black", font=tiny_font)

    def draw_card(text, tx, ty):
        tbox = draw.textbbox((0, 0), text, font=tiny_font)
        tw = tbox[2] - tbox[0]
        th = tbox[3] - tbox[1]
        draw.rectangle([tx - tw / 2 - 6, ty - th / 2 - 4, tx + tw / 2 + 6, ty + th / 2 + 4], fill="white")
        draw.text((tx - tw / 2, ty - th / 2), text, fill="black", font=tiny_font)

    if a_card:
        tx, ty = ax + (bx - ax) * 0.15, ay + (by - ay) * 0.15
        draw_card(a_card, tx, ty)
    if b_card:
        tx, ty = ax + (bx - ax) * 0.85, ay + (by - ay) * 0.85
        draw_card(b_card, tx, ty)


def main():
    W, H = 2200, 1400
    img = Image.new("RGB", (W, H), "white")
    draw = ImageDraw.Draw(img)

    font, small, tiny = load_fonts()

    title = "系统实体联系图（功能全覆盖）"
    tbox = draw.textbbox((0, 0), title, font=font)
    draw.text((W / 2 - (tbox[2] - tbox[0]) / 2, 30), title, fill="black", font=font)

    user = draw_rect(draw, "用户", 250, 260, font)
    admin = draw_rect(draw, "管理员", 250, 1050, font)
    session = draw_rect(draw, "会话", 750, 260, font)
    message = draw_rect(draw, "消息", 1250, 260, font)
    file_up = draw_rect(draw, "上传文件", 1750, 260, font)
    record_file = draw_rect(draw, "聊天记录文件", 1250, 820, font)
    sensitive = draw_rect(draw, "敏感词", 750, 820, font)
    revoke = draw_rect(draw, "撤回记录", 1750, 820, font)
    log = draw_rect(draw, "操作日志", 1250, 1120, font)
    robot = draw_rect(draw, "机器人", 250, 650, font)

    join = draw_diamond(draw, "参与/加入", 500, 260, small)
    contain = draw_diamond(draw, "包含", 1000, 260, small)
    send = draw_diamond(draw, "发送", 750, 460, small)
    upload_rel = draw_diamond(draw, "上传", 1500, 260, small)
    archive = draw_diamond(draw, "归档", 1250, 560, small)
    hit = draw_diamond(draw, "命中", 1000, 820, small)
    revoke_rel = draw_diamond(draw, "撤回", 1500, 820, small)
    manage = draw_diamond(draw, "管理", 500, 1050, small)
    record_rel = draw_diamond(draw, "记录", 750, 1120, small)

    connect(draw, user, join, tiny, a_card="N", b_card="N")
    connect(draw, join, session, tiny)

    connect(draw, session, contain, tiny, a_card="1", b_card="N")
    connect(draw, contain, message, tiny)

    connect(draw, user, send, tiny, a_card="1", b_card="N")
    connect(draw, send, message, tiny)

    connect(draw, message, upload_rel, tiny, a_card="N", b_card="N")
    connect(draw, upload_rel, file_up, tiny)

    connect(draw, message, archive, tiny, a_card="N", b_card="N")
    connect(draw, archive, record_file, tiny)

    connect(draw, sensitive, hit, tiny, a_card="N", b_card="N")
    connect(draw, hit, message, tiny)

    connect(draw, user, revoke_rel, tiny, a_card="1", b_card="N")
    connect(draw, revoke_rel, revoke, tiny)
    connect(draw, message, revoke_rel, tiny, a_card="1", b_card="N", label_offset=(0, -22))

    connect(draw, robot, send, tiny, a_card="1", b_card="N")
    connect(draw, robot, join, tiny, a_card="N", b_card="N")

    connect(draw, admin, manage, tiny)
    for target in [user, session, message, sensitive, record_file, file_up]:
        connect(draw, manage, target, tiny)

    connect(draw, user, record_rel, tiny, a_card="1", b_card="N")
    connect(draw, admin, record_rel, tiny, a_card="1", b_card="N", label_offset=(0, 18))
    connect(draw, record_rel, log, tiny)

    out_path = "/Users/uc/Desktop/test/xechat/系统ER图-全功能.png"
    img.save(out_path)
    print(out_path)


if __name__ == "__main__":
    main()


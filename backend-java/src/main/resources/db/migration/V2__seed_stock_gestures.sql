-- The seven gestures MediaPipe's stock Gesture Recognizer outputs, the phrases the app maps them to
-- (android/.../recognition/GesturePhrases.kt), and that model as the active version.

INSERT INTO sign_category (slug, name_vi, sort_order)
VALUES ('basic_gestures', 'Cử chỉ cơ bản', 0);

INSERT INTO sign (category_id, slug, gloss_vi, description)
SELECT c.id, s.slug, s.gloss_vi, s.description
FROM sign_category c,
     (VALUES ('xin_chao', 'Xin chào', 'Bàn tay mở'),
             ('dong_y', 'Đồng ý', 'Ngón cái lên'),
             ('khong_dong_y', 'Không đồng ý', 'Ngón cái xuống'),
             ('tuyet_voi', 'Tuyệt vời', 'Chữ V'),
             ('cho_toi_hoi', 'Cho tôi hỏi', 'Ngón trỏ lên'),
             ('dung_lai', 'Dừng lại', 'Nắm tay'),
             ('toi_yeu_ban', 'Tôi yêu bạn', 'Cái, trỏ và út')) AS s (slug, gloss_vi, description)
WHERE c.slug = 'basic_gestures';

-- Same pinned URL and checksum as the app's downloadGestureModel task.
INSERT INTO model_version (name, version, asset_url, sha256, released_at, is_active)
VALUES ('mediapipe_gesture_recognizer', '1',
        'https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/1/gesture_recognizer.task',
        '97952348cf6a6a4915c2ea1496b4b37ebabc50cbbf80571435643c455f2b0482', now(), true);

INSERT INTO model_label (model_version_id, label, sign_id)
SELECT m.id, l.label, s.id
FROM model_version m,
     (VALUES ('Open_Palm', 'xin_chao'),
             ('Thumb_Up', 'dong_y'),
             ('Thumb_Down', 'khong_dong_y'),
             ('Victory', 'tuyet_voi'),
             ('Pointing_Up', 'cho_toi_hoi'),
             ('Closed_Fist', 'dung_lai'),
             ('ILoveYou', 'toi_yeu_ban')) AS l (label, sign_slug)
         JOIN sign s ON s.slug = l.sign_slug
WHERE m.name = 'mediapipe_gesture_recognizer' AND m.version = '1';

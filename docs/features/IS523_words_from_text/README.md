# IS523 | Слова из текста — где что лежит

Фича живёт в двух репозиториях. Точка истины по поведению и решениям —
репозиторий `lexeme` (`~/dev/lexeme`, в этом проекте виден как симлинк
`backend/`):

- бриф фичи с решениями Д1–Д27: `backend/docs/features/IS523_words_from_text/brief.md`
- фазы и подфичи: `backend/docs/features/IS523_words_from_text/phases.md`
- исследование поверхности: `backend/docs/features/IS523_words_from_text/research_surface.md`
- конвейер: `backend/docs/features/IS523_words_from_text/pipeline.md`
- бэкенд (подфича 2а, IS528): `backend/docs/features/IS523_words_from_text/IS528_backend_wiki/`
- спека поведения: `backend/spec/words-from-text.md`, контракт `backend/api/openapi.yaml`

Здесь, в PolyTrainer, остаются только подфичи приложения:

- `IS525_dictionary_languages/` — языки словаря (влита, релиз 0.1.16)
- `IS527_photo_words/` — захват фото и список слов (в ветке `IS523_words_from_text`)
- `IS529_app_gateway/` — шлюз к серверу и список слов из ответа: бриф,
  план, ручники, трасса данных первого снимка
- `IS532_add_words/` — добавление слов со снимка в словарь (подфича 6,
  первая итерация): бриф

Копии брифа, фаз, исследования и конвейера отсюда удалены 2026-10-11:
они расходились с оригиналом.

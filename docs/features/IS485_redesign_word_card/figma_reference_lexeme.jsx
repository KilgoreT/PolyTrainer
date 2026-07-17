// Референс из Figma MCP get_design_context, фрейм 5027:1299 «Карточка слова»
// (состояние: слово + одна сохранённая лексема; файл v3wJqLvvQLhG794zM1Zox5, снят 2026-07-15).
// НЕ переносить в проект как есть — источник размеров/цветов/теней. Asset-URL живут ~7 дней.

export default function Component() {
  return (
    <div className="bg-[#ede9f6] overflow-clip relative rounded-[40px] size-full" data-node-id="5027:1299" data-name="Карточка слова">
      {/* статусбар опущен */}
      {/* топбар: back 26px @ left 20, menu ⋮ 24px @ right 20, top ~50 */}
      <div className="absolute bg-white drop-shadow-[0px_2px_5px_rgba(50,40,90,0.06)] h-[90px] left-[18px] right-[18px] rounded-[18px] top-[94px]" data-name="Шапка слова">
        <div className="left-[20px] text-[#1a1a1e] text-[26px] font-bold">book</div>
        <div className="left-[20px] text-[#9a9aa2] text-[13.5px]">Добавлено <span className="font-bold text-[#1a1a1e]">3 июля 2026</span></div>
        <div className="left-[296px] rounded-[19px] size-[38px]" data-name="флаг словаря (круг 38)" />
      </div>
      <div className="absolute bg-white drop-shadow-[0px_2px_5px_rgba(50,40,90,0.06)] h-[135px] left-[18px] right-[18px] rounded-[18px] top-[200px]" data-name="Карточка лексемы">
        <div className="bg-[#4749b8] h-[30px] left-[20px] rounded-[9px] top-[18px] w-[97.25px]" data-name="чип Перевод ×">
          <span className="text-[14px] text-white font-bold left-[12px]">Перевод</span>
          <span data-name="иконка × 13px @ left 72" />
        </div>
        <div className="left-[20px] text-[#1a1a1e] text-[18px] top-[72px]">книга</div>
        <div className="left-[20px] top-[99px] size-[18px]" data-name="иконка корзины" />
        <div className="left-[46px] text-[#d64545] text-[15px]">Удалить</div>
      </div>
      <div className="absolute bg-[#c9c3ea] bottom-[34px] right-[24px] rounded-[19px] size-[60px]" data-name="FAB enabled">
        <div className="shadow-[0px_10px_22px_-6px_rgba(100,90,160,0.5)] rounded-[19px] size-[60px]" />
        <div className="size-[26px]" data-name="иконка документ+" />
      </div>
    </div>
  );
}

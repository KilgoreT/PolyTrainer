// Референс из Figma MCP get_design_context, фрейм 5027:1406 «Создание значения»
// (состояние: сохранённая лексема + открытый ПУСТОЙ черновик; файл v3wJqLvvQLhG794zM1Zox5, снят 2026-07-15).
// НЕ переносить в проект как есть — источник размеров/цветов/теней.

export default function Component() {
  return (
    <div className="bg-[#ede9f6] overflow-clip relative rounded-[40px] size-full" data-node-id="5027:1406" data-name="Создание значения">
      {/* статусбар/топбар/шапка слова — идентичны фрейму 5027:1299 */}
      <div className="absolute bg-white border border-[#dcd6f2] border-solid drop-shadow-[0px_2px_5px_rgba(50,40,90,0.06)] h-[205.3px] left-[18px] right-[18px] rounded-[18px] top-[200px]" data-name="Карточка ЧЕРНОВИКА (border выделяет)">
        <div className="bg-[#eeecef] h-[18px] right-[20px] rounded-[6px] top-[18px]" data-name="бейдж Черновик">
          <span className="text-[#9a9aa2] text-[11px] font-bold">Черновик</span>
        </div>
        <div className="left-[20px] text-[#8a8a90] text-[14.5px] top-[60px]">Выберите компонент, чтобы добавить значение</div>
        <div className="text-right text-[#9a9aa2] text-[12px] font-bold tracking-[0.24px] top-[93px]" data-name="лейбл секции">ДОБАВИТЬ КОМПОНЕНТ</div>
        <div className="bg-[#4749b8] h-[26px] rounded-[8px] top-[110px]" data-name="чип Перевод +">
          <span className="text-[13px] text-white">Перевод</span>
          <span data-name="иконка + 11px" />
        </div>
        <div className="bg-[#4749b8] h-[26px] rounded-[8px] top-[110px] right-[20px]" data-name="чип Пример + (кастомный компонент, образец)">
          <span className="text-[13px] text-white">Пример</span>
          <span data-name="иконка + 11px" />
        </div>
        <div className="bg-[#f0eef7] h-px top-[154px]" data-name="divider" />
        <div className="left-[46px] text-[#d64545] text-[15px] top-[178px]" data-name="+ корзина 18px @ left 20">Удалить значение</div>
      </div>
      <div className="absolute bg-white drop-shadow-[0px_2px_5px_rgba(50,40,90,0.06)] h-[139px] left-[18px] right-[18px] rounded-[18px] top-[417.3px]" data-name="Карточка сохранённой лексемы">
        <div className="bg-[#4749b8] h-[26px] left-[20px] rounded-[8px] top-[18px] w-[86.22px]" data-name="чип Перевод ×">
          <span className="text-[13px] text-white">Перевод</span>
          <span data-name="иконка × 11px" />
        </div>
        <div className="left-[20px] text-[#1a1a1e] text-[17px] top-[59.5px]">книга</div>
        <div className="bg-[#f0eef7] h-px top-[90px]" data-name="divider" />
        <div className="left-[46px] text-[#d64545] text-[15px] top-[113.5px]" data-name="+ корзина">Удалить значение</div>
      </div>
      <div className="absolute bg-[#d8d3ec] bottom-[34px] opacity-75 right-[24px] rounded-[19px] size-[60px]" data-name="FAB DISABLED (черновик открыт): без тени, opacity .75">
        <div className="size-[26px]" data-name="иконка документ+" />
      </div>
    </div>
  );
}

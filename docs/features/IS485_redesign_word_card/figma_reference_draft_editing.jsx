// Референс из Figma MCP get_design_context, фрейм 5027:1469 «Создание значения: редактирование»
// (состояние: черновик с ОТКРЫТЫМ инлайн-редактированием перевода + сохранённая лексема ниже;
//  файл v3wJqLvvQLhG794zM1Zox5, снят 2026-07-15).
// НЕ переносить в проект как есть — источник размеров/цветов/теней.

export default function Component() {
  return (
    <div className="bg-[#ede9f6] overflow-clip relative rounded-[40px] size-full" data-node-id="5027:1469" data-name="Создание значения: редактирование">
      {/* статусбар/топбар/шапка слова — идентичны фрейму 5027:1299 */}
      <div className="absolute bg-white border border-[#dcd6f2] border-solid drop-shadow-[0px_2px_5px_rgba(50,40,90,0.06)] h-[247px] left-[18px] right-[18px] rounded-[18px] top-[200px]" data-name="Карточка черновика (редактирование)">
        <div className="bg-[#eeecef] h-[18px] right-[20px] rounded-[6px] top-[18px]" data-name="бейдж Черновик" />
        <div className="bg-[#4749b8] h-[26px] left-[20px] rounded-[8px] top-[50px] w-[86.22px]" data-name="label-чип Перевод × (компонент добавлен в черновик)">
          <span className="text-[13px] text-white">Перевод</span>
          <span data-name="иконка × 11px" />
        </div>
        <div className="border-[#c2c1e8] border-b border-solid h-[26px] left-[20px] right-[20px] top-[82px]" data-name="ПОЛЕ ВВОДА: underline 1px #C2C1E8">
          <span className="text-[#1a1a1e] text-[17px]">кни</span>
          <div className="bg-[#4749b8] h-[20px] w-[2px] rounded-[1px]" data-name="курсор primary" />
        </div>
        <div className="text-right text-[#9a9aa2] text-[12px] font-bold tracking-[0.24px] top-[135px]">ДОБАВИТЬ КОМПОНЕНТ</div>
        <div className="bg-[#4749b8] h-[26px] rounded-[8px] top-[152px] right-[20px]" data-name="чип Пример + (Перевод исчез из ДОБАВИТЬ — уже занят, не multiple)">
          <span className="text-[13px] text-white">Пример</span>
        </div>
        <div className="bg-[#f0eef7] h-px top-[196px]" data-name="divider" />
        <div className="left-[46px] text-[#d64545] text-[15px] top-[219.5px]" data-name="+ корзина">Удалить значение</div>
      </div>
      <div className="absolute bg-white drop-shadow-[0px_2px_5px_rgba(50,40,90,0.06)] h-[139px] left-[18px] right-[18px] rounded-[18px] top-[459px]" data-name="Карточка сохранённой лексемы — как во фрейме 1406" />
      <div className="absolute bg-[#d8d3ec] bottom-[34px] opacity-75 right-[24px] rounded-[19px] size-[60px]" data-name="FAB DISABLED" />
    </div>
  );
}

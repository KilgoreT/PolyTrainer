// Референс из Figma MCP get_design_context, фрейм 5027:1587 «Диалог компонента»
// (диалог создания/редактирования компонента поверх затемнённого экрана списка;
//  файл v3wJqLvvQLhG794zM1Zox5, снят 2026-07-15). НЕ переносить как есть.

export default function Component() {
  return (
    <div className="bg-[#fcfcfa] rounded-[40px] size-full" data-node-id="5027:1587" data-name="Диалог компонента">
      {/* фон: размытый список + оверлей rgba(20,18,40,0.44) */}
      <div className="absolute bg-white inset-[22.54%_22px] rounded-[9px] shadow-[0px_24px_60px_-12px_rgba(20,18,40,0.5)]" data-name="Диалог">
        <div className="left-[24px] top-[38.5px] text-[#1a1a1e] text-[22px] font-bold" data-name="заголовок">Новый компонент</div>

        <div className="left-[24px] top-[81px] text-[#4749b8] text-[12px] font-bold tracking-[0.36px]" data-name="лейбл">НАЗВАНИЕ</div>
        <div className="bg-[#f5f4f0] border border-[#4749b8] h-[46px] left-[24px] right-[24px] top-[97px] rounded-[13px]" data-name="поле ввода имени (курсор primary 2×18)">
          <span className="left-[15px] text-[#1a1a1e] text-[16px]">Пример</span>
        </div>

        <div className="left-[24px] top-[174px] text-[#8a8a90] text-[12px] font-bold tracking-[0.36px]" data-name="лейбл">ТИП ЗНАЧЕНИЯ</div>
        <div className="bg-[#f3f1fc] border border-[#4749b8] h-[42px] left-[24px] right-[24px] top-[193px] rounded-[9px]" data-name="радио ВЫБРАН: Текст">
          <div className="border-2 border-[#4749b8] left-[14px] rounded-[12px] size-[24px]" data-name="radio outer">
            <div className="bg-[#4749b8] rounded-[5px] size-[10px]" data-name="radio dot" />
          </div>
          <span className="left-[48px] text-[#1a1a1e] text-[15px] font-bold">Текст</span>
        </div>
        <div className="bg-white border border-[#e6e4dc] h-[42px] left-[24px] right-[24px] top-[245px] rounded-[9px]" data-name="радио НЕ выбран: Изображение">
          <div className="border-2 border-[#c4c4cc] left-[14px] rounded-[12px] size-[24px]" data-name="radio empty" />
          <span className="left-[48px] text-[#6e6e76] text-[15px]">Изображение</span>
        </div>

        <div className="bg-[#4749b8] h-[24px] w-[24px] left-[24px] top-[316.56px] rounded-[7px]" data-name="чекбокс CHECKED (галочка SVG 15)" />
        <div className="left-[60px] top-[327.78px] text-[#1a1a1e] text-[14.5px]" data-name="лейбл чекбокса">Разрешить несколько значений на карточке</div>

        <div className="bg-[#f1f0ec] h-[52px] left-[24px] right-[179px] top-[374.12px] rounded-[14px]" data-name="кнопка Отменить">
          <span className="text-[#6e6e76] text-[15.5px] font-bold">Отменить</span>
        </div>
        <div className="bg-[#4749b8] h-[52px] left-[179px] right-[24px] top-[374.12px] rounded-[14px] shadow-[0px_8px_18px_-5px_rgba(71,73,184,0.5)]" data-name="кнопка Создать">
          <span className="text-white text-[15.5px] font-bold">Создать</span>
        </div>
      </div>
    </div>
  );
}

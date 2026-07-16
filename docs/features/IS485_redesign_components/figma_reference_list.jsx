// Референс из Figma MCP get_design_context, фрейм 5027:1534 «Компоненты»
// (экран списка компонентов словаря; файл v3wJqLvvQLhG794zM1Zox5, снят 2026-07-15).
// НЕ переносить как есть — источник размеров/цветов/теней. Asset-URL живут ~7 дней.

export default function Component() {
  return (
    <div className="bg-[#fcfcfa] rounded-[40px] size-full" data-node-id="5027:1534" data-name="Компоненты">
      {/* топбар: back 26px @ left 20 top 50; заголовок «Английский» 22 bold @ left 58 */}
      <div className="absolute inset-[96px_0_0_0]" data-name="Container (список)">
        <div className="bg-white border border-[#eceae4] drop-shadow-[0px_1px_1.5px_rgba(0,0,0,0.03)] h-[117px] left-[20px] right-[20px] rounded-[18px]" data-name="Карточка компонента">
          <div className="bg-[#e9e6fa] h-[42px] w-[42px] left-[18px] top-[18px] rounded-[12px]" data-name="иконка типа (квадрат 42, лавандовый бг, SVG 22)" />
          <div className="left-[72px] top-[29px] text-[#1a1a1e] text-[18px] font-bold" data-name="название">Пример</div>
          <div className="left-[72px] top-[50px] text-[#9a9aa2] text-[12.5px]" data-name="счётчик">Значений: 0</div>
          <div className="bg-[#f3f2ee] h-[34px] w-[34px] left-[256px] top-[18px] rounded-[10px]" data-name="кнопка edit (карандаш SVG 17)" />
          <div className="bg-[#f3f2ee] h-[34px] w-[34px] left-[296px] top-[18px] rounded-[10px]" data-name="кнопка delete (корзина SVG 17)" />
          <div className="bg-[#f1f0ec] h-[25px] left-[18px] top-[74px] rounded-[8px]" data-name="чип шаблона: иконка 13 + «Текст» 12.5 #6E6E76">
            <span className="left-[30px] text-[#6e6e76] text-[12.5px]">Текст</span>
          </div>
        </div>
        {/* вторая карточка top 133 — «Синонимы», идентична */}
      </div>
      <div className="absolute bg-[#4749b8] bottom-[30px] right-[22px] h-[54px] w-[132.7px] rounded-[17px]" data-name="Extended FAB «Создать»">
        <div className="shadow-[0px_12px_26px_-6px_rgba(71,73,184,0.55)] h-[54px] rounded-[17px]" />
        <div className="left-[22px] size-[22px]" data-name="иконка +" />
        <div className="left-[53px] text-white text-[16px] font-bold">Создать</div>
      </div>
    </div>
  );
}

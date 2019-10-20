package ru.kbakaras.e2.message;

/**
 * Признак того, как должен обрабатываться элемент при загрузке в
 * систему получатель.
 */
public enum E2ElementUse {
    /**
     * Вариант используется по умолчанию. Если элемент не существует,
     * он создаётся. Если элемент существует, то обновляется при условии,
     * что флаг changed установлен.
     */
    Load,

    /**
     * При данном варианте элемент не создаётся и не обновляется, но подразумевается,
     * что он должен существовать в системе получателе. Если на данный элемент будет
     * ссылаться какой-то атрибут, а элемента в системе не будет, должно выбрасываться
     * исключение.
     */
    Reference,

    /**
     * Этот вариант похож на <i>E2ElementUse.Load</i>. Элемент создаётся только
     * в случае, когда на него ссылается какой-то загружаемый атрибут. А обновляется
     * только при условии, что флаг changed установлен, а элемент существует.<br/><br/>
     *
     * Этот режим удобно использовать в случаях, когда мы хотим грузить в систему
     * получатель элемент, только в том случае, если он действительно используется
     * (на него хоть кто-то ссылается), а не любые, созданные в отправителе элементы.
     */
    Update
}

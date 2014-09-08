package model;

/**
 * Стратегия игрока может управлять хоккеистом посредством установки свойств объекта данного класса.
 */
public class Move {
    private double speedUp;
    private double turn;
    private ActionType action = ActionType.NONE;
    private double passPower = 1.0D;
    private double passAngle;
    private int teammateIndex = -1;

    /**
     * @return Возвращает текущее ускорение хоккеиста.
     */
    public double getSpeedUp() {
        return speedUp;
    }

    /**
     * Устанавливает ускорение хоккеиста.
     * <p/>
     * Ускорение является относительным и должно лежать в интервале от {@code -1.0} до {@code 1.0}.
     * Значения, выходящие за указанный интервал, будут приведены к ближайшей его границе.
     */
    public void setSpeedUp(double speedUp) {
        this.speedUp = speedUp;
    }

    /**
     * @return Возвращает текущий угол поворота хоккеиста.
     */
    public double getTurn() {
        return turn;
    }

    /**
     * Устанавливает угол поворота хоккеиста.
     * <p/>
     * Угол поворота задаётся в радианах относительно текущего направления хоккеиста и для хоккеиста
     * с базовым значением атрибута подвижность и максимальным запасом выносливости ограничен
     * интервалом от {@code -game.hockeyistTurnAngleFactor} до {@code game.hockeyistTurnAngleFactor}.
     * Значения, выходящие за указанный интервал, будут приведены к ближайшей его границе.
     * Положительные значения соответствуют повороту по часовой стрелке.
     */
    public void setTurn(double turn) {
        this.turn = turn;
    }

    /**
     * @return Возвращает текущее действие хоккеиста.
     */
    public ActionType getAction() {
        return action;
    }

    /**
     * Устанавливает действие хоккеиста.
     */
    public void setAction(ActionType action) {
        this.action = action;
    }

    /**
     * @return Возвращает текущую силу паса.
     */
    public double getPassPower() {
        return passPower;
    }

    /**
     * Устанавливает силу паса ({@code ActionType.PASS}).
     * <p/>
     * Сила паса является относительной величиной и должна лежать в интервале от {@code 0.0} до {@code 1.0}.
     * Значения, выходящие за указанный интервал, будут приведены к ближайшей его границе.
     * К значению реальной силы паса применяется также поправочный коэффициент {@code game.passPowerFactor}.
     */
    public void setPassPower(double passPower) {
        this.passPower = passPower;
    }

    /**
     * @return Возвращает текущее направление паса.
     */
    public double getPassAngle() {
        return passAngle;
    }

    /**
     * Устанавливает направление паса ({@code ActionType.PASS}).
     * <p/>
     * Направление паса задаётся в радианах относительно текущего направления хоккеиста
     * и должно лежать в интервале от {@code -0.5 * game.passSector} до {@code 0.5 * game.passSector}.
     * Значения, выходящие за указанный интервал, будут приведены к ближайшей его границе.
     */
    public void setPassAngle(double passAngle) {
        this.passAngle = passAngle;
    }

    /**
     * @return Возвращает текущий индекс хоккеиста, на которого будет произведена замена,
     *         или {@code -1}, если хоккеист не был указан.
     */
    public int getTeammateIndex() {
        return teammateIndex;
    }

    /**
     * Устанавливает индекс хоккеиста для выполнения замены ({@code ActionType.SUBSTITUTE}).
     * <p/>
     * Индексация начинается с нуля. Значением по умолчанию является {@code -1}.
     * Если в команде игрока не существует хоккеиста с указанным индексом, то замена произведена не будет.
     */
    public void setTeammateIndex(int teammateIndex) {
        this.teammateIndex = teammateIndex;
    }
}

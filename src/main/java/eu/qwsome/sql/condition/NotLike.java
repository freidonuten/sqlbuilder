package eu.qwsome.sql.condition;

public class NotLike extends BiCondition {

    NotLike(ValueHolder first, ValueHolder second) {
        super(first, second);
    }

    @Override
    String getOperator() {
        return " not like ";
    }
}

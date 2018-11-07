package eu.qwsome.sql;

import eu.qwsome.sql.condition.Condition;
import eu.qwsome.sql.condition.ValueConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class simplifies dynamic sql generation.
 * @author Lukáš Kvídera
 */
public class Select {

  /**
   * Table in clause FROM.
   */
  private String sourceTable;

  /**
   * List of columns that will be selected.
   */
  private final List<String> columns = new ArrayList<>();
  private final List<Join> joins = new ArrayList<>();
  private Condition condition;

  /**
   * Creates select with single column.
   *
   * @param column name of column to be selected
   */
  private Select(final String column) {
    this.columns.add(column);
  }

  /**
   * Creates select with multiple columns.
   *
   * @param columns names of columns to be selected
   */
  private Select(final String[] columns) {
    this.columns.addAll(Arrays.asList(columns));
  }

  /**
   * This mehod creates a select instance that can be effectively used as following snippet:
   * <p>
   * {@code
   * select().from("table").toSql()
   * }
   * <p>
   * that is translated into:
   * <p>
   * {@code
   * SELECT * FROM table
   * }
   *
   * @return select builder
   */
  public static Select select() {
    return new Select("*");
  }

  /**
   * This mehod creates a select instance that can be effectively used as following snippet:
   * <p>
   * {@code
   * select("column").from("table").toSql()
   * }
   * <p>
   * that is translated into:
   * <p>
   * {@code
   * SELECT column FROM table
   * }
   *
   * @return select builder
   */
  public static Select select(final String column) {
    return new Select(column);
  }

  /**
   * This mehod creates a select instance that can be effecitvely used as following snippet:
   * <p>
   * {@code
   * select("column1", "column2", ...).from("table").toSql()
   * }
   * <p>
   * that is translated into:
   * <p>
   * {@code
   * SELECT column1, column2, ... FROM table
   * }
   *
   * @return select builder
   */
  public static Select select(final String... columns) {
    return new Select(columns);
  }

  /**
   * This method sets a source table for select.
   *
   * @param table to be used in FROM clause
   * @return next phase that allows only relevant methods
   */
  public TableSelectedPhase from(final String table) {
    this.sourceTable = table;
    return new TableSelectedPhase();
  }

  /**
   * Generates final SQL.
   *
   * @return generated SQL
   */
  public String toSql() {
    final StringBuilder builder = new StringBuilder();
    builder.append("SELECT ")
      .append(getColumns())
      .append(" FROM ")
      .append(this.sourceTable);

    if (!this.joins.isEmpty()) {
      for (final Join join : this.joins) {
        builder.append(" JOIN ")
          .append(join.joinTable)
          .append(" ON ")
          .append(join.condition.get());
      }
    }

    if (this.condition != null) {
      builder.append(" WHERE ").append(this.condition.get());
    }
    return builder.toString();
  }

  /**
   * Adds an order by clause to the statement.
   *
   * @param columns that defines ordering
   * @return order by clause
   */
  private String orderBy(final Column... columns) {
    return toSql() + " ORDER BY " + Arrays.stream(columns).map(Column::getSql).collect(Collectors.joining(","));
  }

  /**
   * This method generates a list of columns concatenated with comma.
   *
   * @return list of columns usable in SQL
   */
  private String getColumns() {
    return String.join(",", this.columns);
  }


  public class TableSelectedPhase {
    /**
     * @see Select#toSql()
     */
    public String toSql() {
      return Select.this.toSql();
    }

    /**
     * This method allows creation of where clause.
     *
     * @param condition condition used to filter data
     * @return next phase that allows only relevant methods
     */
    public ConditionsBuiltPhase where(final Condition condition) {
      Select.this.condition = condition;
      return new ConditionsBuiltPhase();
    }

    /**
     * Adds an order by clause to the statement.
     *
     * @param columns that defines ordering
     * @return order by clause
     */
    public String orderBy(final Column... columns) {
      return Select.this.orderBy(columns);
    }

    /**
     * Adds a join clause to the statement.
     *
     * @param joinTable table to be joined
     * @return next phase that allows only relevant methods
     */
    public JoinPhase join(final String joinTable) {
      return new JoinPhase(joinTable);
    }
  }

  public class ConditionsBuiltPhase {
    /**
     * @see Select#toSql()
     */
    public String toSql() {
      return Select.this.toSql();
    }

    public ValueConstructor toValues() {
      return Select.this.condition.getValues();
    }

    /**
     * Adds an order by clause to the statement.
     *
     * @param columns that defines ordering
     * @return order by clause
     */
    public String orderBy(final Column... columns) {
      return Select.this.orderBy(columns);
    }
  }


  private static class Join {

    private final String joinTable;

    private final Condition condition;

    public Join(final String joinTable, final Condition condition) {
      this.joinTable = joinTable;
      this.condition = condition;
    }
  }

  public class JoinPhase {

    private final String joinTable;

    private JoinPhase(final String joinTable) {
      this.joinTable = joinTable;
    }

    public TableSelectedPhase on(final Condition condition) {
      Select.this.joins.add(new Join(this.joinTable, condition));
      return new TableSelectedPhase();
    }

  }
}

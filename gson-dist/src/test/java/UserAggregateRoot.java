import team.emptyte.storage.aggregate.domain.AggregateRoot;

public class UserAggregateRoot extends AggregateRoot {
  public String name;

  /**
   * Initializes the Aggregate Root.
   * <p>
   * A non-null ID is mandatory to ensure the entity has a valid identity
   * from the moment of its creation.
   * </p>
   *
   * @param id The unique identifier for this aggregate. Must not be {@code null}.
   */
  public UserAggregateRoot(final String id, final String name) {
    super(id);
    this.name = name;
  }

  public String name() {
    return this.name;
  }

  public void setName(final String name) {
    this.name = name;
  }
}

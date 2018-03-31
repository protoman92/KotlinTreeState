/**
 * Created by haipham on 30/3/18.
 */

/**
 * This represents an immutable tree-based state object. All update methods
 * return a new instance instead of mutating the current object.
 */
interface TreeStateType<T> {
  /**
   * Check if the current [TreeStateType] is empty.
   */
  val isEmpty: Boolean

  /**
   * Get the nesting level, i.e. how deep the deepest child state is.
   */
  val nestingLevel: Int

  /**
   * Get the value at a [identifier], returning a [Try] indicating whether the
   * value is available or not.
   */
  fun valueAt(identifier: String): Try<T>

  /**
   * Get the child state at a [identifier], returning a [Try] indicating whether
   * the child state is available or not.
   */
  fun childStateAt(identifier: String): Try<TreeStateType<T>>

  /**
   * Update the child state at a [identifier].
   */
  fun updateChildState(identifier: String, state: TreeStateType<T>?): TreeStateType<T>

  /**
   * Convenient method to remove a child state at a [identifier].
   */
  fun removeChildState(identifier: String): TreeStateType<T> {
    return updateChildState(identifier, null)
  }

  /**
   * Map the value at a [identifier] using [selector] and return a new [TreeStateType].
   */
  fun mapValue(identifier: String, selector: (Try<T>) -> Try<T>): TreeStateType<T>

  /**
   * Get an empty [TreeStateType].
   */
  fun clear(): TreeStateType<T>

  /**
   * Update [value] at [identifier] and return a new [TreeStateType] without
   * mutating the current one. If [value] is null, remove the [identifier].
   */
  fun updateValue(identifier: String, value: T?): TreeStateType<T> {
    return mapValue(identifier) { Try.wrap(value) }
  }

  /**
   * Update all values from [pairs].
   */
  fun updateValues(pairs: Map<String, T>): TreeStateType<T> {
    var state = this
    pairs.forEach { a, b -> state = state.updateValue(a, b) }
    return state
  }

  /**
   * Remove the value at a [identifier] and return a new [TreeStateType].
   */
  fun removeValue(identifier: String): TreeStateType<T> {
    return updateValue(identifier, null)
  }

  /**
   * Remove all values at [identifiers].
   */
  fun removeValues(identifiers: Iterable<String>): TreeStateType<T> {
    var state = this
    identifiers.forEach { state = state.removeValue(it) }
    return state
  }
}

private typealias StateValues<T> = MutableMap<String, T>
private typealias ChildStates<T> = MutableMap<String, TreeStateType<T>>

/**
 * [TreeStateType] implementation.
 */
class TreeState<T> internal constructor(): TreeStateType<T> {
  companion object {
    /**
     * Get an empty [TreeState].
     */
    fun <T> empty() = builder<T>().build()

    /**
     * Get a new [Builder].
     */
    fun <T> builder() = Builder<T>()
  }

  internal var values: StateValues<T> = mutableMapOf()
  internal var childStates: ChildStates<T> = mutableMapOf()
  internal var childStateSeparator: Char = '.'

  override val isEmpty: Boolean get() {
    return values.isEmpty() && childStates.all { it.value.isEmpty }
  }

  override val nestingLevel: Int get() {
    return 1 + (childStates.map { it.value.nestingLevel }.max() ?: 0)
  }

  override fun toString(): String {
    return "Values: $values, childStates: $childStates"
  }

  /**
   * Clone the current [TreeState].
   */
  fun cloneBuilder() = builder<T>().withTreeState(this)

  override fun childStateAt(identifier: String): Try<TreeStateType<T>> {
    val separator = childStateSeparator
    val separated = identifier.split(separator)
    val first = Maybe.evaluate { separated[0] }.value

    if (separated.size == 1 && first != null && first.isNotEmpty()) {
      return Try.wrap(childStates[first], "No child state found at $identifier")
    } else if (first != null && first.isNotEmpty()) {
      val subId = separated.drop(1).joinToString(separator.toString())
      return childStateAt(first).flatMap { it.childStateAt(subId) }
    } else {
      return Try.failure("No child state found at $identifier")
    }
  }

  override fun valueAt(identifier: String): Try<T> {
    val separator = childStateSeparator
    val separated = identifier.split(separator)
    val first = Maybe.evaluate { separated[0] }.value

    if (separated.size == 1 && first != null && first.isNotEmpty()) {
      return Try.wrap(values[first], "No value at $identifier")
    } else if (first != null && first.isNotEmpty()) {
      val subId = separated.drop(1).joinToString(separator.toString())
      return childStateAt(first).flatMap { it.valueAt(subId) }
    } else {
      return Try.failure("No value at $identifier")
    }
  }

  override fun updateChildState(identifier: String, state: TreeStateType<T>?): TreeStateType<T> {
    val separator = childStateSeparator
    val separated = identifier.split(separator)
    val first = Maybe.evaluate { separated[0] }.value

    if (separated.size == 1 && first != null && first.isNotEmpty()) {
      return cloneBuilder().updateChildState(first, state).build()
    } else if (first != null && first.isNotEmpty()) {
      val subId = separated.drop(1).joinToString(separator.toString())

      val firstState = childStateAt(first)
        .getOrElse { Builder<T>().withChildStateSeparator(separator).build() }

      val updatedState = firstState.updateChildState(subId, state)
      return updateChildState(first, updatedState)
    } else {
      return this
    }
  }

  override fun mapValue(identifier: String, selector: (Try<T>) -> Try<T>): TreeStateType<T> {
    val separator = childStateSeparator
    val separated = identifier.split(separator)
    val first = Maybe.evaluate { separated[0] }.value

    if (separated.size == 1 && first != null && first.isNotEmpty()) {
      return cloneBuilder().mapValue(first, selector).build()
    } else if (first != null && first.isNotEmpty()) {
      val subId = separated.drop(1).joinToString(separator.toString())

      val childState = childStateAt(first)
        .getOrElse { Builder<T>().withChildStateSeparator(separator).build() }

      val updatedState = childState.mapValue(subId, selector)
      return cloneBuilder().updateChildState(first, updatedState).build()
    } else {
      return this
    }
  }

  override fun clear() = empty<T>()
}

/**
 * [Builder] for [TreeState].
 */
class Builder<T>() {
  private val state = TreeState<T>()

  /**
   * Set [TreeState.childStateSeparator].
   */
  fun withChildStateSeparator(separator: Char) = this.also {
    it.state.childStateSeparator = separator
  }

  /**
   * Set [TreeState.values].
   */
  fun withValues(values: StateValues<T>) = this.also {
    it.state.values.putAll(values)
  }

  /**
   * Set [TreeState.childStates].
   */
  fun withChildStates(childStates: ChildStates<T>) = this.also {
    it.state.childStates.putAll(childStates)
  }

  /**
   * Copy the properties of [state].
   */
  fun withTreeState(state: TreeState<T>) = this.also {
    it.withChildStateSeparator(state.childStateSeparator)
      .withValues(state.values)
      .withChildStates(state.childStates)
  }

  /**
   * Update [TreeState.childStates].
   */
  fun updateChildState(identifier: String, childState: TreeStateType<T>?) = this.also {
    if (childState != null) {
      it.state.childStates.put(identifier, childState)
    } else {
      it.state.childStates.remove(identifier)
    }
  }

  /**
   * Update [TreeState.values] at [identifier] with [selector].
   */
  fun mapValue(identifier: String, selector: (Try<T>) -> Try<T>) = this.also {
    val value = Maybe.evaluate {
      selector(it.state.valueAt(identifier)).value
    }.value

    if (value != null) {
      it.state.values.put(identifier, value)
    } else {
      it.state.values.remove(identifier)
    }
  }

  /**
   * Get the inner [state].
   */
  fun build(): TreeStateType<T> = state
}
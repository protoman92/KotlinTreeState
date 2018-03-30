/**
 * Created by haipham on 30/3/18.
 */

/**
 * This represents an immutable tree-based state object.
 */
interface TreeStateType<T> {
  /**
   * Check if the current [TreeStateType] is empty.
   */
  val isEmpty: Boolean

  /**
   * Get the nesting level, i.e. how deep the deepest substate is.
   */
  val nestingLevel: Int

  /**
   * Get the value at a [identifier], returning a [Try] indicating whether the
   * value is available or not.
   */
  fun valueAt(identifier: String): Try<T>

  /**
   * Get the substate at a [identifier], returning a [Try] indicating whether
   * the substate is available or not.
   */
  fun substateAt(identifier: String): Try<TreeStateType<T>>

  /**
   * Update the substate at a [identifier].
   */
  fun updateSubstate(identifier: String, substate: TreeStateType<T>?): TreeStateType<T>

  /**
   * Convenient method to remove a substate at a [identifier].
   */
  fun removeSubstate(identifier: String): TreeStateType<T> {
    return updateSubstate(identifier, null)
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
private typealias Substates<T> = MutableMap<String, TreeStateType<T>>

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
  internal var substates: Substates<T> = mutableMapOf()
  internal var substateSeparator: Char = '.'

  override val isEmpty: Boolean get() {
    return values.isEmpty() && substates.all { it.value.isEmpty }
  }

  override val nestingLevel: Int get() {
    return 1 + (substates.map { it.value.nestingLevel }.max() ?: 0)
  }

  override fun toString(): String {
    return "Values: $values, substates: $substates"
  }

  /**
   * Clone the current [TreeState].
   */
  fun cloneBuilder() = builder<T>().withTreeState(this)

  override fun substateAt(identifier: String): Try<TreeStateType<T>> {
    val separator = substateSeparator
    val separated = identifier.split(separator)
    val first = Maybe.evaluate { separated[0] }.value

    if (separated.size == 1 && first != null && first.isNotEmpty()) {
      return Try.wrap(substates[first], "No substate found at $identifier")
    } else if (first != null && first.isNotEmpty()) {
      val subId = separated.drop(1).joinToString(separator.toString())
      return substateAt(first).flatMap { it.substateAt(subId) }
    } else {
      return Try.failure("No substate found at $identifier")
    }
  }

  override fun valueAt(identifier: String): Try<T> {
    val separator = substateSeparator
    val separated = identifier.split(separator)
    val first = Maybe.evaluate { separated[0] }.value

    if (separated.size == 1 && first != null && first.isNotEmpty()) {
      return Try.wrap(values[first], "No value at $identifier")
    } else if (first != null && first.isNotEmpty()) {
      val subId = separated.drop(1).joinToString(separator.toString())
      return substateAt(first).flatMap { it.valueAt(subId) }
    } else {
      return Try.failure("No value at $identifier")
    }
  }

  override fun updateSubstate(identifier: String, substate: TreeStateType<T>?): TreeStateType<T> {
    val separator = substateSeparator
    val separated = identifier.split(separator)
    val first = Maybe.evaluate { separated[0] }.value

    if (separated.size == 1 && first != null && first.isNotEmpty()) {
      return cloneBuilder().updateSubstate(first, substate).build()
    } else if (first != null && first.isNotEmpty()) {
      val subId = separated.drop(1).joinToString(separator.toString())

      val firstSubstate = substateAt(first)
        .getOrElse { Builder<T>().withSubstateSeparator(separator).build() }

      val updatedSubstate = firstSubstate.updateSubstate(subId, substate)
      return updateSubstate(first, updatedSubstate)
    } else {
      return this
    }
  }

  override fun mapValue(identifier: String, selector: (Try<T>) -> Try<T>): TreeStateType<T> {
    val separator = substateSeparator
    val separated = identifier.split(separator)
    val first = Maybe.evaluate { separated[0] }.value

    if (separated.size == 1 && first != null && first.isNotEmpty()) {
      return cloneBuilder().mapValue(first, selector).build()
    } else if (first != null && first.isNotEmpty()) {
      val subId = separated.drop(1).joinToString(separator.toString())

      val substate = substateAt(first)
        .getOrElse { Builder<T>().withSubstateSeparator(separator).build() }

      val updatedSubstate = substate.mapValue(subId, selector)
      return cloneBuilder().updateSubstate(first, updatedSubstate).build()
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
   * Set [TreeState.substateSeparator].
   */
  fun withSubstateSeparator(separator: Char) = this.also {
    it.state.substateSeparator = separator
  }

  /**
   * Set [TreeState.values].
   */
  fun withStateValues(values: StateValues<T>) = this.also {
    it.state.values = values
  }

  /**
   * Set [TreeState.substates].
   */
  fun withSubstates(substates: Substates<T>) = this.also {
    it.state.substates = substates
  }

  /**
   * Copy the properties of [state].
   */
  fun withTreeState(state: TreeState<T>) = this.also {
    it.withSubstateSeparator(state.substateSeparator)
      .withStateValues(state.values)
      .withSubstates(state.substates)
  }

  /**
   * Update [TreeState.substates].
   */
  fun updateSubstate(identifier: String, substate: TreeStateType<T>?) = this.also {
    if (substate != null) {
      it.state.substates.put(identifier, substate)
    } else {
      it.state.substates.remove(identifier)
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
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
   * Get the value at a [path], returning a [Try] indicating whether the value
   * is available or not.
   */
  fun valueAt(path: String): Try<T>

  /**
   * Get the child state at a [path], returning a [Try] indicating whether the
   * child state is available or not.
   */
  fun childStateAt(path: String): Try<TreeStateType<T>>

  /**
   * Update the child state at a [path].
   */
  fun updateChildState(path: String, state: TreeStateType<T>?): TreeStateType<T>

  /**
   * Convenient method to remove a child state at a [path].
   */
  fun removeChildState(path: String): TreeStateType<T> {
    return updateChildState(path, null)
  }

  /**
   * Map the value at a [path] using [selector] and return a new [TreeStateType].
   */
  fun mapValue(path: String, selector: (Try<T>) -> Try<T>): TreeStateType<T>

  /**
   * Get an empty [TreeStateType].
   */
  fun clear(): TreeStateType<T>

  /**
   * Update [value] at [path] and return a new [TreeStateType] without mutating
   * the current one. If [value] is null, remove the [path].
   */
  fun updateValue(path: String, value: T?): TreeStateType<T> {
    return mapValue(path) { Try.wrap(value) }
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
   * Remove the value at a [path] and return a new [TreeStateType].
   */
  fun removeValue(path: String): TreeStateType<T> {
    return updateValue(path, null)
  }

  /**
   * Remove all values at [paths].
   */
  fun removeValues(paths: Iterable<String>): TreeStateType<T> {
    var state = this
    paths.forEach { state = state.removeValue(it) }
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
    @JvmStatic
    fun <T> empty() = builder<T>().build()

    /**
     * Get a new [Builder].
     */
    @JvmStatic
    fun <T> builder() = Builder<T>()
  }

  internal val values: StateValues<T> = mutableMapOf()
  internal val childStates: ChildStates<T> = mutableMapOf()
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

  override fun childStateAt(path: String): Try<TreeStateType<T>> {
    val separator = childStateSeparator
    val separated = path.split(separator)
    val first = Option.evaluate { separated[0] }.value

    return if (separated.size == 1 && first != null && first.isNotEmpty()) {
      Try.wrap(childStates[first], "No child state found at $path")
    } else if (first != null && first.isNotEmpty()) {
      val subId = separated.drop(1).joinToString(separator.toString())
      childStateAt(first).flatMap { it.childStateAt(subId) }
    } else {
      Try.failure("No child state found at $path")
    }
  }

  override fun valueAt(path: String): Try<T> {
    val separator = childStateSeparator
    val separated = path.split(separator)
    val first = Option.evaluate { separated[0] }.value

    return if (separated.size == 1 && first != null && first.isNotEmpty()) {
      Try.wrap(values[first], "No value at $path")
    } else if (first != null && first.isNotEmpty()) {
      val subId = separated.drop(1).joinToString(separator.toString())
      childStateAt(first).flatMap { it.valueAt(subId) }
    } else {
      Try.failure("No value at $path")
    }
  }

  override fun updateChildState(path: String, state: TreeStateType<T>?): TreeStateType<T> {
    val separator = childStateSeparator
    val separated = path.split(separator)
    val first = Option.evaluate { separated[0] }.value

    return if (separated.size == 1 && first != null && first.isNotEmpty()) {
      cloneBuilder().updateChildState(first, state).build()
    } else if (first != null && first.isNotEmpty()) {
      val subId = separated.drop(1).joinToString(separator.toString())

      val firstState = childStateAt(first)
        .getOrElse { Builder<T>().withChildStateSeparator(separator).build() }

      val updatedState = firstState.updateChildState(subId, state)
      updateChildState(first, updatedState)
    } else {
      this
    }
  }

  override fun mapValue(path: String, selector: (Try<T>) -> Try<T>): TreeStateType<T> {
    val separator = childStateSeparator
    val separated = path.split(separator)
    val first = Option.evaluate { separated[0] }.value

    return if (separated.size == 1 && first != null && first.isNotEmpty()) {
      cloneBuilder().mapValue(first, selector).build()
    } else if (first != null && first.isNotEmpty()) {
      val subId = separated.drop(1).joinToString(separator.toString())

      val childState = childStateAt(first)
        .getOrElse { Builder<T>().withChildStateSeparator(separator).build() }

      val updatedState = childState.mapValue(subId, selector)
      cloneBuilder().updateChildState(first, updatedState).build()
    } else {
      this
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

  @Suppress("MemberVisibilityCanBePrivate")
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
  fun updateChildState(path: String, childState: TreeStateType<T>?) = this.also {
    if (childState != null) {
      it.state.childStates.put(path, childState)
    } else {
      it.state.childStates.remove(path)
    }
  }

  /**
   * Update [TreeState.values] at [path] with [selector].
   */
  fun mapValue(path: String, selector: (Try<T>) -> Try<T>) = this.also {
    val value = Option.evaluate { selector(it.state.valueAt(path)).value }.value

    if (value != null) {
      it.state.values.put(path, value)
    } else {
      it.state.values.remove(path)
    }
  }

  /**
   * Get the inner [state].
   */
  fun build(): TreeStateType<T> = state
}
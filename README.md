# KotlinTreeState

[![Build Status](https://travis-ci.org/protoman92/KotlinTreeState.svg?branch=master)](https://travis-ci.org/protoman92/KotlinTreeState)

Type-safe Tree State implementation for Kotlin. This is ideal for use in conjunction with a Java/Kotlin-based Redux implementation.

We can use this as follows:

```kotlin
val state = TreeState.empty<Int>()
  .updateValue("a.b.c")
  .mapValue("a.b.c") { it.map { it * 2 } }
  .removeValue("a.b")

val value = state.valueAt("a.b.c").value
```

Since the state object is immutable (all update methods return new instances instead of mutating the current one), we will not have to worry about unintended side-effects.

The concept is similar to:

**TypeSafeState-JS**: <https://github.com/protoman92/TypeSafeState-JS>

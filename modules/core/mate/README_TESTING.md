# Mate Reducer Testing

This document describes how to use helper functions for testing reducers in Mate.

## Setup Helper Functions

To use helper functions, add dependency in `build.gradle.kts`:

```kotlin
dependencies {
    // Main library (for runtime use)
    implementation(project("path" to ":modules:core:mate"))
    
    // Test helpers (includes both main library and test helper functions)
    testImplementation(project("path" to ":modules:core:mate"))
}
```

## Required Imports

```kotlin
// Main imports for testing
import me.apomazkin.mate.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse  
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
```

## Available Helper Functions

### ReducerResult Helpers

These functions work with reducer execution result (`ReducerResult<State, Effect>`).

#### `assertState(expectedState, message = "...")`
Checks that state matches expected value.

```kotlin
val result = reducer.testReduce(initialState, message)
result.assertState(expectedState, "State should match expected")
```

#### `assertEffects(expectedEffects, message = "...")`
Checks that effects exactly match expected ones (order and count).

```kotlin
val result = reducer.testReduce(initialState, message)
result.assertEffects(setOf(ExpectedEffect()), "Effects should match expected")
```

#### `assertNoEffects(message = "...")`
Checks that there are no effects. **Most frequently used function in tests.**

```kotlin
val result = reducer.testReduce(initialState, message)
result.assertNoEffects("Should have no effects")
```

#### `assertSingleEffect<EFFECT_TYPE>(message = "...")`
Checks that there is exactly one effect of specified type. **Often used for side effects verification.**

```kotlin
val result = reducer.testReduce(initialState, message)
result.assertSingleEffect<DatasourceEffect.LoadData>("Should have LoadData effect")
```

#### `assertEffectsCount(expectedCount, message = "...")`
Checks exact number of effects.

```kotlin
val result = reducer.testReduce(initialState, message)
result.assertEffectsCount(2, "Should have exactly 2 effects")
```

#### `assertHasEffect<EFFECT_TYPE>(message = "...")`
Checks that there is an effect of specified type (can be among multiple effects).

```kotlin
val result = reducer.testReduce(initialState, message)
result.assertHasEffect<DatasourceEffect.DeleteWord>("Should have DeleteWord effect")
```

### MateReducer Helpers

#### `testReduce(initialState, message)`
Convenient function for testing single message. **Main function for unit tests.**

```kotlin
val result = reducer.testReduce(initialState, message)
```

#### `testScenario(initialState, vararg messages)`
Tests sequence of messages and returns results of each step. **Used for integration tests.**

```kotlin
val results = reducer.testScenario(
    initialState,
    Message1(),
    Message2(),
    Message3()
)

// results[0] - result after Message1
// results[1] - result after Message2  
// results[2] - result after Message3
```

### State Builder Helper

#### `toBuilder()` and `modify()`
Allows convenient creation of complex initial states through fluent API.

**Implementation:** `StateBuilder.kt` in `src/testFixtures/java/me/apomazkin/mate/`

```kotlin
val initialState = MyState()
    .toBuilder()
    .modify { it.copy(property1 = "value1") }
    .modify { it.copy(property2 = "value2") }
    .build()
```

### Basic ReducerResult Functions

#### `state()` and `effects()`
Extract state and effects from reducer result.

```kotlin
val result = reducer.testReduce(initialState, message)
val newState = result.state()
val effects = result.effects()
```

## Usage Examples

### Simple Single Message Test (from real tests)

```kotlin
@Test
fun `should show add word dialog when ShowAddWordDialog is received`() {
    // Test case 3: Standard case - adding word
    // Given
    val initialState = createTestState()

    // When
    val result = reducer.testReduce(initialState, Msg.ShowAddWordDialog())
    
    // Then
    // Main functionality check
    assertTrue("Add word dialog should be open", result.state().addWordDialogState.isOpen)
    assertTrue("Word value should be empty", result.state().addWordDialogState.wordValue.isEmpty())
    assertNull("Word id should be null", result.state().addWordDialogState.wordId)
    
    // Immutability checks - ALL other properties must remain unchanged
    assertEquals(
        "Loading state should remain unchanged",
        initialState.isLoading,
        result.state().isLoading
    )
    assertEquals(
        "Top bar state should remain unchanged",
        initialState.topBarState,
        result.state().topBarState
    )
    assertEquals(
        "Snackbar state should remain unchanged",
        initialState.snackbarState,
        result.state().snackbarState
    )
    
    // Effects check
    result.assertNoEffects("Should have no effects")
}
```

### Test with Single Effect

```kotlin
@Test
fun `should trigger term flow load when ChangeDict is received`() {
    // Test case 2: Standard case - changing dictionary
    // Given
    val initialState = createTestState()
    
    // When
    val result = reducer.testReduce(initialState, Msg.ChangeDict(current = testDictEntity))
    
    // Then
    // Main functionality check
    result.assertEffectsCount(1, "Should have exactly 1 effect")
    result.assertSingleEffect<DatasourceEffect.LoadTermFlow>("Should have LoadTermFlow effect")
    
    // Immutability checks - state should remain unchanged
    assertEquals("State should remain unchanged", initialState, result.state())
}
```

### Test with Multiple Effects

```kotlin
@Test
fun `should delete multiple words when DeleteWord is received`() {
    // Test case 7: Standard case - deleting multiple words
    // Given
    val initialState = createTestState().copy(
        topBarState = TopBarState(isActionMode = true),
        confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
            isOpen = true,
            wordIds = setOf(WordInfo(1, "1"), WordInfo(2, "2"))
        )
    )
    
    val wordIdsToDelete = setOf(WordInfo(1, "1"), WordInfo(2, "2"))
    
    // When
    val result = reducer.testReduce(initialState, Msg.DeleteWord(wordIdsToDelete))
    
    // Then
    // Main functionality check
    assertFalse("Action mode should be disabled", result.state().topBarState.isActionMode)
    assertFalse("Confirm delete dialog should be closed", result.state().confirmWordDeleteDialogState.isOpen)
    
    // Effects check - verifying multiple effects
    result.assertEffectsCount(2, "Should have exactly 2 effects")
    result.assertHasEffect<DatasourceEffect.DeleteWord>("Should have DeleteWord effect")
    result.assertHasEffect<UiEffect.ShowSnackbar>("Should have ShowSnackbar effect")
}
```

### Scenario Test (from real tests)

```kotlin
@Test
fun `should handle complete add word flow`() {
    // Test case 17: Standard case - complete word addition scenario
    // Given
    val initialState = createTestState()
    
    // When
    val results = reducer.testScenario(
        initialState,
        Msg.ShowAddWordDialog(),
        Msg.WordValueChange("new word"),
        Msg.AddWord("new word")
    )
    
    // Then
    // Step 1: ShowAddWordDialog
    val step1 = results[0]
    assertTrue("Step 1: Dialog should be open", step1.state().addWordDialogState.isOpen)
    assertTrue("Step 1: Word value should be empty", step1.state().addWordDialogState.wordValue.isEmpty())
    step1.assertNoEffects("Step 1: Should have no effects")
    
    // Step 2: WordValueChange
    val step2 = results[1]
    assertTrue("Step 2: Dialog should remain open", step2.state().addWordDialogState.isOpen)
    assertEquals("Step 2: Word value should be updated", "new word", step2.state().addWordDialogState.wordValue)
    step2.assertSingleEffect<DatasourceEffect.LoadTermFlow>("Step 2: Should have LoadTermFlow effect")
    
    // Step 3: AddWord
    val step3 = results[2]
    assertFalse("Step 3: Dialog should be closed", step3.state().addWordDialogState.isOpen)
    assertEquals("Step 3: Word value should be reset", "", step3.state().addWordDialogState.wordValue)
    step3.assertSingleEffect<DatasourceEffect.AddWord>("Step 3: Should have AddWord effect")
}
```

### Test Using State Builder

```kotlin
@Test
fun `should not update word value when WordValueChange is received and dialog is closed`() {
    // Test case 5: Boundary case - changing word value when dialog is closed
    // Given
    val initialState = createTestState().copy(
        addWordDialogState = AddWordDialogState(
            isOpen = false,
            wordValue = ""
        )
    )
    val newValue = "new word"
    
    // When
    val result = reducer.testReduce(initialState, Msg.WordValueChange(newValue))
    
    // Then
    // Main functionality check
    assertFalse("Add word dialog should remain closed", result.state().addWordDialogState.isOpen)
    assertEquals("Word value should remain unchanged", initialState.addWordDialogState.wordValue, result.state().addWordDialogState.wordValue)
    
    // Immutability checks - state should remain completely unchanged
    assertEquals("State should remain completely unchanged", initialState, result.state())
    result.assertNoEffects("Should have no effects")
}
```

### Using State Builder (from real code)

```kotlin
// In real tests, helper function createTestState is used
private fun createTestState(
    isLoading: Boolean = false,
    termListMap: Map<String, Flow<PagingData<TermUiItem>>> = mapOf("" to testTermFlow)
) = DictionaryTabState(
    isLoading = isLoading,
    termListMap = termListMap
)

// But StateBuilder can also be used for more complex cases:
val complexState = DictionaryTabState()
    .toBuilder()
    .modify { it.copy(isLoading = true) }
    .modify { it.copy(topBarState = TopBarState(isActionMode = true)) }
    .modify { it.copy(addWordDialogState = AddWordDialogState(isOpen = true, wordValue = "test")) }
    .build()
```

## Common Testing Patterns

### 1. Test Structure
Follow **Given-When-Then** template with mandatory checks:

```kotlin
@Test
fun `should update state when Message is received`() {
    // Test case N: Type - case description
    // Given
    val initialState = createTestState(...)
    
    // When
    val result = reducer.testReduce(initialState, message)
    
    // Then
    // Main functionality check
    assertEquals("Expected behavior", expectedValue, result.state().property)
    
    // Immutability checks - ALL other properties must remain unchanged
    assertEquals("Other property should remain unchanged", initialState.otherProperty, result.state().otherProperty)
    
    // Effects check
    result.assertNoEffects("Should have no effects") // or other effect checks
}
```

### 2. Immutability Checks
**CRITICALLY IMPORTANT**: Always check that only needed fields changed:

```kotlin
// ✅ Good - checking all other fields
assertEquals("Loading state should remain unchanged", initialState.isLoading, result.state().isLoading)
assertEquals("Top bar state should remain unchanged", initialState.topBarState, result.state().topBarState)
assertEquals("Dialog state should remain unchanged", initialState.dialogState, result.state().dialogState)

// ❌ Bad - not checking immutability of other fields
assertTrue("Dialog should be open", result.state().dialogState.isOpen)
```

### 3. Helper Functions Usage Frequency
Based on real project statistics:

- `assertNoEffects()` - **47 times** (most frequent)
- `assertSingleEffect<Type>()` - **8 times** 
- `assertEffectsCount()` - **2 times**
- `assertHasEffect<Type>()` - **2 times**
- `testScenario()` - **2 times** (for integration tests)

### 4. Effects Testing

```kotlin
// No effects (most common case)
result.assertNoEffects("Should have no effects")

// One effect of specific type
result.assertSingleEffect<DatasourceEffect.LoadData>("Should have LoadData effect")

// Multiple effects
result.assertEffectsCount(2, "Should have exactly 2 effects")
result.assertHasEffect<DatasourceEffect.DeleteWord>("Should have DeleteWord effect")
result.assertHasEffect<UiEffect.ShowSnackbar>("Should have ShowSnackbar effect")
```

## Best Practices

### Mandatory Requirements:
1. **Test Structure**: Use Given-When-Then with comments
2. **Numbering**: Add comment `// Test case N: Type - description`
3. **Immutability checks**: MUST check immutability of all other fields
4. **Informative messages**: Each assert should explain what went wrong

### Recommendations:
5. **Test names**: Format `should [expected behavior] when [condition]`
6. **Helper functions**: Create `createTestState()` to simplify data preparation  
7. **Effects**: Check both presence and absence of effects
8. **Scenarios**: Use `testScenario()` for integration tests
9. **State Builder**: Apply for complex states with multiple modifications

### Common Mistakes:
- ❌ Not checking immutability of other state fields
- ❌ Using non-informative messages in asserts
- ❌ Not testing effects
- ❌ Not adding comments with test case number and type

## Workspace Rules Compliance

All helper functions are designed according to project standards:

- ✅ Informative error messages
- ✅ Support for boundary, standard and edge cases  
- ✅ State immutability verification
- ✅ Test atomicity
- ✅ Readability and maintainability
- ✅ Error messages under 80 characters

## Additional Resources

### Source Files
- **Helper functions**: `src/testFixtures/java/me/apomazkin/mate/MateTestHelper.kt`
- **State Builder**: `src/testFixtures/java/me/apomazkin/mate/StateBuilder.kt`  
- **Usage examples**: `src/testFixtures/java/me/apomazkin/mate/MateReducerTestExample.kt`

### Real Examples
- **VocabularyTabReducerKtTest.kt** - 18 test cases, 882 lines, shows all patterns
- **SnackbarExtTest.kt** - extension function tests

### Mate Architecture
- **MateReducer.kt** - base reducer interface
- **Mate.kt** - main class with state and effects management
- **ReducerResult** - typealias for `Pair<State, Set<Effect>>`

Documentation updated based on real project code and contains actual usage examples.
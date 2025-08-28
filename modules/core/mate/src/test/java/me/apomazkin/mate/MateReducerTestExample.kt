package me.apomazkin.mate

import me.apomazkin.mate.test.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Example of using helper functions for testing reducers
 *
 * Test cases:
 * 1. Boundary case: increment count from zero
 * 2. Standard case: complete data loading flow
 * 3. Standard case: multiple state changes with builder
 */
class MateReducerTestExample {

    // Example data for demonstration
    data class ExampleState(
        val count: Int = 0,
        val isLoading: Boolean = false,
        val data: String = ""
    )

    sealed interface ExampleMessage {
        data object Increment : ExampleMessage
        data object Decrement : ExampleMessage
        data object LoadData : ExampleMessage
        data class DataLoaded(val data: String) : ExampleMessage
    }

    sealed interface ExampleEffect {
        data object LoadData : ExampleEffect
    }

    // Example simple reducer for demonstration
    private val exampleReducer = object : MateReducer<ExampleState, ExampleMessage, ExampleEffect> {
        override fun reduce(state: ExampleState, message: ExampleMessage): ReducerResult<ExampleState, ExampleEffect> {
            return when (message) {
                is ExampleMessage.Increment -> state.copy(count = state.count + 1) to emptySet()
                is ExampleMessage.Decrement -> state.copy(count = state.count - 1) to emptySet()
                is ExampleMessage.LoadData -> state.copy(isLoading = true) to setOf(ExampleEffect.LoadData)
                is ExampleMessage.DataLoaded -> state.copy(isLoading = false, data = message.data) to emptySet()
            }
        }
    }

    @Test
    fun `should increment count when Increment message is sent`() {
        // Test case 1: Boundary case - increment from zero
        val initialState = ExampleState(count = 0)
        val message = ExampleMessage.Increment
        
        val result = exampleReducer.testReduce(initialState, message)
        
        // Main functionality check
        assertEquals("Count should be incremented", 1, result.state().count)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged", 
            initialState.isLoading, 
            result.state().isLoading
        )
        assertEquals(
            "Data should remain unchanged", 
            initialState.data, 
            result.state().data
        )
        
        // Effects check
        result.assertNoEffects("Should have no effects for increment")
    }

    @Test
    fun `should handle complete data loading flow`() {
        // Test case 2: Standard case - complete data loading flow
        val initialState = ExampleState(count = 0)
        
        // Scenario: load data -> data loaded
        val results = exampleReducer.testScenario(
            initialState,
            ExampleMessage.LoadData,
            ExampleMessage.DataLoaded("test data")
        )
        
        // Check each step
        val step1 = results[0] // LoadData
        // Main functionality check
        assertTrue("Step 1: Should be loading", step1.state().isLoading)
        
        // Immutability checks for step 1
        assertEquals(
            "Step 1: Count should remain unchanged", 
            initialState.count, 
            step1.state().count
        )
        assertEquals(
            "Step 1: Data should remain unchanged", 
            initialState.data, 
            step1.state().data
        )
        
        // Effects check for step 1
        step1.assertSingleEffect<ExampleEffect.LoadData>("Step 1: Should have LoadData effect")
        
        val step2 = results[1] // DataLoaded
        // Main functionality checks
        assertFalse("Step 2: Should not be loading", step2.state().isLoading)
        assertEquals("Step 2: Data should be loaded", "test data", step2.state().data)
        
        // Immutability checks for step 2
        assertEquals(
            "Step 2: Count should remain unchanged", 
            initialState.count, 
            step2.state().count
        )
        
        // Effects check for step 2
        step2.assertNoEffects("Step 2: Should have no effects")
    }

    @Test
    fun `should handle multiple state changes with builder`() {
        // Test case 3: Standard case - multiple state changes
        val initialState = ExampleState()
            .toBuilder()
            .modify { it.copy(count = 5) }
            .modify { it.copy(data = "initial data") }
            .build()
        
        val result = exampleReducer.testReduce(initialState, ExampleMessage.Decrement)
        
        // Main functionality check
        assertEquals("Count should be decremented", 4, result.state().count)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Data should remain unchanged", 
            "initial data", 
            result.state().data
        )
        assertEquals(
            "Loading state should remain unchanged", 
            initialState.isLoading, 
            result.state().isLoading
        )
        
        // Effects check
        result.assertNoEffects("Should have no effects for decrement")
    }
}

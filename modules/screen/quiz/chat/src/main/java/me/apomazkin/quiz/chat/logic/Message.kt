package me.apomazkin.quiz.chat.logic


sealed interface Msg {
    
    /**
     * Message to prepare the quiz
     */
    data object PrepareToStart : Msg
    
    /**
     * Message to start the quiz
     */
    data object Start : Msg
    
    data class QuizData(val data: List<Pair<String, String>>) : Msg
    
    /**
     * Message to change the input text from the user
     */
    data class UserTextChange(val value: String) : Msg
    data object UserTextEnter : Msg
    
    data object Empty : Msg
}
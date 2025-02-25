package me.apomazkin.quiz.chat.logic

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.logic.ChatMessage.Companion.UserAction
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.ui.resource.ResourceManager

internal class ChatReducer(
    private val logger: LexemeLogger,
    private val resourceManager: ResourceManager,
) : MateReducer<ChatScreenState, Msg, Effect> {
    
    override fun reduce(
        state: ChatScreenState, message: Msg
    ): ReducerResult<ChatScreenState, Effect> {
        logger.log(message = "Reduce --prevState--: $state ")
        logger.log(message = "Reduce ---message---: $message ")
        return when (message) {
            is Msg.PrepareToStart -> state
                .stopLoading()
                .systemMessage(
                    message = welcomeMessage()
                        .toMessageContent()
                ) to emptySet<Effect>()
            
            is Msg.Start -> state to setOf(DatasourceEffect.LoadQuiz)
            
            is Msg.QuizLoaded -> state
                .startQuiz()
                .userMessage(
                    message = startUserMessage()
                        .toMessageContent()
                )
                .systemMessage(
                    message = MessageContent.create(text = message.content)
                ) to setOf(DatasourceEffect.NextQuestion)
            
            is Msg.QuizReLoaded -> state
                .userMessage(
                    message = continueUserMessage()
                        .toMessageContent()
                )
                .systemMessage(
                    message = MessageContent.create(text = message.content)
                ) to setOf(DatasourceEffect.NextQuestion)
            
            is Msg.NextQuestion -> state
                .systemMessage(message = message.content)
                .showUserActions()
                .enableUserInput() to emptySet()
            
            is Msg.UserTextChange -> state
                .userTextChange(message.value) to emptySet()
            
            is Msg.UserAttempt -> state
                .userTextEnter()
                .clearUserInput()
                .hideUserActions()
                .disableUserInput() to setOf(
                DatasourceEffect.CheckAnswer(
                    message.value
                )
            )
            
            is Msg.GetAnswer -> state
                .userMessage(message = showAnswerUserMessage().toMessageContent())
                .clearUserInput()
                .disableUserInput()
                .hideUserActions()
                .disableUserInput() to setOf(DatasourceEffect.GetAnswer)
            
            is Msg.ShowAnswer -> state
                .systemMessage(message.value) to
                    setOf(DatasourceEffect.NextQuestion)
            
            is Msg.Skip -> state
                .clearUserInput()
                .disableUserInput()
                .hideUserActions()
                .disableUserInput() to setOf(DatasourceEffect.Skip)
            
            is Msg.Skipped -> state
                .userMessage(
                    message = skipUserMessage().toMessageContent()
                ) to setOf(DatasourceEffect.NextQuestion)
            
            is Msg.Assessment -> state
                .systemMessage(
                    message = message.value
                ) to setOf(DatasourceEffect.NextQuestion)
            
            
            is Msg.SessionOver -> state
                .systemMessage(
                    message = completionMessage().toMessageContent(
                        buttons = listOf(
                            ChatMessage.ChatButton(
                                R.string.chat_quiz_system_btn_continue,
                                UserAction.CONTINUE
                            ),
                            ChatMessage.ChatButton(
                                R.string.chat_quiz_system_btn_result,
                                UserAction.SUMMARY
                            ),
                            ChatMessage.ChatButton(
                                R.string.chat_quiz_system_btn_finish,
                                UserAction.EXIT
                            ),
                        )
                    ),
                ) to emptySet()
            
            is Msg.UserAction -> {
                val effects = when (message.action) {
                    UserAction.CONTINUE -> setOf(DatasourceEffect.ReLoadQuiz)
                    UserAction.SUMMARY -> setOf(DatasourceEffect.Summary)
                    UserAction.EXIT -> emptySet()
                    UserAction.LAST_SESSION_SUMMARY -> setOf(
                        DatasourceEffect.SessionSummary(all = false)
                    )
                    
                    UserAction.FULL_QUIZ_SUMMARY -> setOf(
                        DatasourceEffect.SessionSummary(all = true)
                    )
                }
                
                val newState = when (message.action) {
                    UserAction.CONTINUE -> state
                    UserAction.SUMMARY -> state
                        .userMessage(
                            message = MessageContent.create(
                                showAssessmentUserMessage()
                            )
                        )
                    
                    UserAction.EXIT -> state.exit()
                    UserAction.LAST_SESSION_SUMMARY -> state
                        .userMessage(
                            message = MessageContent.create(
                                lastSessionUserMessage()
                            )
                        )
                    
                    UserAction.FULL_QUIZ_SUMMARY -> state
                        .userMessage(
                            message = MessageContent.create(
                                fullQuizUserMessage()
                            )
                        )
                }
                
                newState to effects
            }
            
            is Msg.SummaryOptions -> state
                .systemMessage(
                    message = message.value,
                ) to setOf()
            
            is Msg.Summary -> state
                .systemMessage(message.value)
                .systemMessage(
                    message = completionMessage2().toMessageContent(
                        buttons = listOf(
                            ChatMessage.ChatButton(
                                R.string.chat_quiz_system_btn_continue,
                                UserAction.CONTINUE
                            ),
                            ChatMessage.ChatButton(
                                R.string.chat_quiz_system_btn_finish,
                                UserAction.EXIT
                            )
                        )
                    )
                )
                .clearUserInput()
                .hideUserActions()
                .disableUserInput() to emptySet()
            
            is Msg.Empty -> state to emptySet()
        }.also {
            logger.log(message = "Reduce --newState--: ${it.state()} ")
            it.effects().forEach { effect ->
                logger.log(message = "Reduce --toEffect--: $effect ")
            }
        }
    }
    
    private fun welcomeMessage(): String {
        return resourceManager.stringByResId(R.string.chat_quiz_msg_system_welcome)
    }
    
    private fun startUserMessage(): String {
        return resourceManager.stringByResId(R.string.chat_quiz_msg_user_start)
    }
    
    private fun continueUserMessage(): String {
        return resourceManager.stringByResId(R.string.chat_quiz_msg_user_continue)
    }
    
    private fun showAnswerUserMessage(): String {
        return resourceManager.stringByResId(R.string.chat_quiz_msg_user_show_answer)
    }
    
    private fun skipUserMessage(): String {
        return resourceManager.stringByResId(R.string.chat_quiz_msg_user_skip)
    }
    
    private fun showAssessmentUserMessage(): String {
        return resourceManager.stringByResId(R.string.chat_quiz_system_btn_result)
    }
    
    private fun completionMessage(): String {
        return resourceManager.stringByResId(R.string.chat_quiz_msg_system_session_end)
    }
    
    private fun completionMessage2(): String {
        return resourceManager.stringByResId(R.string.chat_quiz_msg_system_session_end2)
    }
    
    private fun lastSessionUserMessage(): String {
        return resourceManager.stringByResId(R.string.chat_quiz_msg_system_statistic_session)
    }
    
    private fun fullQuizUserMessage(): String {
        return resourceManager.stringByResId(R.string.chat_quiz_msg_system_statistic_full)
    }
    
    private fun String.annotated(): AnnotatedString {
        return buildAnnotatedString {
            append(this@annotated)
        }
    }
    
    private fun String.toMessageContent(): MessageContent {
        return MessageContent.create(text = this.annotated())
    }
    
    private fun String.toMessageContent(
        buttons: List<ChatMessage.ChatButton>
    ): MessageContent {
        return MessageContent.create(
            text = this.annotated(),
            buttons = buttons
        )
    }
    
    
}
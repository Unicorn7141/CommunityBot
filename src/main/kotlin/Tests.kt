import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalCoalescingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalCoalescingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalMember
import com.kotlindiscord.kord.extensions.components.Component
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.chatCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralUserCommand
import com.kotlindiscord.kord.extensions.extensions.publicUserCommand
import com.kotlindiscord.kord.extensions.interactions.respond
import dev.kord.core.behavior.reply
import dev.kord.rest.builder.message.create.allowedMentions
import dev.kord.rest.builder.message.create.embed
import io.ktor.util.reflect.*
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class Tests : Extension() {
    override val name: String
        get() = "Test Commands"


    class GreetingArguments : Arguments() {
        val greet by optionalCoalescingString("greeting", "The greeting you'd like to send to a member")
    }

    class InfoArguments() : Arguments() {
        val member by optionalMember("member", "")
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun setup() {
        publicUserCommand {
            name = "greet"


            action {

                val target = targetUsers.toList()[0].asMember(guild?.id ?: error("Invalid guildId"))

                respond {
                    allowedMentions() {
                        users.add(target.id)
                    }
                    content = "${target.mention} you were greeted by ${user.mention}"
                }
            }
        }

        chatCommand(::InfoArguments) {
            name = "check"

            check { failIf(event.message.author?.id?.asString != "279879920040148992") }
            action {
                val ser = database.findOne { Server::id eq guild?.id?.asString } ?: error("OOp")
                val mem = ser.community.filter {
                    it.id == (arguments.member?.id?.asString ?: message.author!!.id.asString)
                }[0]

                message.reply {
                    content = mem.lastWork.toString()
                }
            }
        }
    }

}